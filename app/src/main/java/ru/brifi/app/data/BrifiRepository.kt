package ru.brifi.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.brifi.app.model.FinanceSummary
import ru.brifi.app.model.LocalTaskOverride
import ru.brifi.app.model.OrderTask
import ru.brifi.app.model.TaskStatuses
import ru.brifi.app.model.WeeklyPayout
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BrifiRepository(context: Context) {

    private val prefs = context.getSharedPreferences("brifi_v6_prefs", Context.MODE_PRIVATE)

    companion object {
        const val SHEET_URL =
            "https://docs.google.com/spreadsheets/d/${SheetsParser.SPREADSHEET_ID}/edit?gid=${SheetsParser.ORDERS_GID}#gid=${SheetsParser.ORDERS_GID}"
        const val STATS_SHEET_URL =
            "https://docs.google.com/spreadsheets/d/${SheetsParser.SPREADSHEET_ID}/edit?gid=${SheetsParser.STATS_GID}#gid=${SheetsParser.STATS_GID}"

        const val URL_TG_CHAT = "https://web.telegram.org/k/#-5439581270"
        const val URL_PRINTOFFICE = "https://printoffice24.com/dealsList"
        const val URL_MAIL_RU =
            "https://app.mail.ru/mail/inbox/?source=mail-superapp&platform=web&wv-dark=0&wssa-logs-nonce=a8b293f9-4377-4914-870d-9f1526ef7c2c"
        const val URL_YANDEX_DISK = "https://disk.yandex.ru/client/disk/"
        const val URL_MAX = "https://web.max.ru/104972940"

        private const val KEY_ORDERS_CSV = "cache_orders_csv"
        private const val KEY_STATS_CSV = "cache_stats_csv"
        private const val KEY_LAST_SYNC = "last_sync_time"
        private const val KEY_OVERRIDES = "local_overrides_json"
        private const val KEY_LOCAL_TASKS = "local_tasks_json"
        private const val KEY_WEBHOOK_URL = "apps_script_webhook_url"
        private const val KEY_PAYOUT_OVERRIDE = "latest_payout_override"
        private const val KEY_CHECKLIST_STATE = "checklist_checked_indices"

        private val SYNC_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }

    data class SyncResult(
        val tasks: List<OrderTask>,
        val finance: FinanceSummary,
        val lastUpdatedText: String,
        val fromCache: Boolean = false,
        val errorMessage: String? = null
    )

    fun getWebhookUrl(): String = prefs.getString(KEY_WEBHOOK_URL, "").orEmpty().trim()

    fun saveWebhookUrl(url: String) {
        prefs.edit().putString(KEY_WEBHOOK_URL, url.trim()).apply()
    }

    fun getLastSyncText(): String = prefs.getString(KEY_LAST_SYNC, "Ещё не обновлялось").orEmpty()

    fun getChecklistChecked(): Set<Int> {
        val raw = prefs.getString(KEY_CHECKLIST_STATE, "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun saveChecklistChecked(indices: Set<Int>) {
        prefs.edit().putString(KEY_CHECKLIST_STATE, indices.sorted().joinToString(",")).apply()
    }

    suspend fun loadCached(): SyncResult? = withContext(Dispatchers.IO) {
        val ordersCsv = prefs.getString(KEY_ORDERS_CSV, null) ?: return@withContext null
        val statsCsv = prefs.getString(KEY_STATS_CSV, "").orEmpty()
        buildSyncResult(ordersCsv, statsCsv, getLastSyncText(), fromCache = true, error = null)
    }

    suspend fun refreshFromGoogleSheets(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val ordersCsv = fetchText(SheetsParser.ORDERS_CSV_URL)
            val statsCsv = runCatching { fetchText(SheetsParser.STATS_CSV_URL) }.getOrDefault("")
            val nowText = LocalDateTime.now().format(SYNC_FMT)

            prefs.edit()
                .putString(KEY_ORDERS_CSV, ordersCsv)
                .putString(KEY_STATS_CSV, statsCsv)
                .putString(KEY_LAST_SYNC, nowText)
                .apply()

            buildSyncResult(ordersCsv, statsCsv, nowText, fromCache = false, error = null)
        } catch (e: Exception) {
            val cachedOrders = prefs.getString(KEY_ORDERS_CSV, null)
            val cachedStats = prefs.getString(KEY_STATS_CSV, "").orEmpty()
            if (cachedOrders != null) {
                buildSyncResult(
                    cachedOrders,
                    cachedStats,
                    getLastSyncText(),
                    fromCache = true,
                    error = "Офлайн-режим: ${e.message ?: "нет сети"}"
                )
            } else {
                SyncResult(
                    tasks = loadLocalTasks(),
                    finance = FinanceSummary(
                        latestWeek = WeeklyPayout("", "текущей недели", "Нет связи", 0L),
                        allTimeTotalIncome = 0L,
                        allTimeCommission50 = 0L,
                        allTimeSalary = 0L,
                        currentMonthLabel = "Этот месяц",
                        currentMonthTotalIncome = 0L,
                        currentMonthCommission50 = 0L,
                        currentMonthSalary = 0L
                    ),
                    lastUpdatedText = getLastSyncText(),
                    fromCache = true,
                    errorMessage = "Не удалось загрузить таблицу: ${e.message ?: "ошибка сети"}"
                )
            }
        }
    }

    private fun buildSyncResult(
        ordersCsv: String,
        statsCsv: String,
        lastSync: String,
        fromCache: Boolean,
        error: String?
    ): SyncResult {
        val parsedOrders = SheetsParser.parseOrdersSheet(ordersCsv)
        val overrides = loadOverrides()
        val localTasks = loadLocalTasks()

        val mergedSheetTasks = parsedOrders.tasks.mapNotNull { task ->
            val ov = overrides[task.id]
            if (ov?.deleted == true) return@mapNotNull null
            task.copy(
                status = ov?.status ?: task.status,
                note = ov?.note ?: task.note,
                link = ov?.link ?: task.link
            )
        }

        val mergedLocalTasks = localTasks.mapNotNull { task ->
            val ov = overrides[task.id]
            if (ov?.deleted == true) return@mapNotNull null
            task.copy(
                status = ov?.status ?: task.status,
                note = ov?.note ?: task.note,
                link = ov?.link ?: task.link
            )
        }

        val allTasks = mergedLocalTasks + mergedSheetTasks
        val payoutOverride = prefs.getString(KEY_PAYOUT_OVERRIDE, null)
        val latestWeek = if (!payoutOverride.isNullOrBlank()) {
            parsedOrders.latestWeeklyPayout.copy(payoutStatus = payoutOverride)
        } else {
            parsedOrders.latestWeeklyPayout
        }

        val finance = SheetsParser.parseFinanceStats(
            statsCsvText = statsCsv,
            latestWeek = latestWeek,
            today = LocalDate.now(),
            fallbackOrdersCount = parsedOrders.tasks.size
        )

        return SyncResult(
            tasks = allTasks,
            finance = finance,
            lastUpdatedText = lastSync,
            fromCache = fromCache,
            errorMessage = error
        )
    }

    suspend fun updateTaskStatus(task: OrderTask, newStatus: String): String? = withContext(Dispatchers.IO) {
        val overrides = loadOverrides().toMutableMap()
        val current = overrides[task.id] ?: LocalTaskOverride()
        overrides[task.id] = current.copy(status = newStatus)
        saveOverrides(overrides)

        val payload = JSONObject().apply {
            put("action", "updateStatus")
            put("rowNumber", task.rowNumber)
            put("orderNumber", task.orderNumber)
            put("title", task.title)
            put("status", newStatus)
        }
        postToWebhookIfConfigured(payload)
    }

    suspend fun updateTaskNote(task: OrderTask, newNote: String): String? = withContext(Dispatchers.IO) {
        val overrides = loadOverrides().toMutableMap()
        val current = overrides[task.id] ?: LocalTaskOverride()
        overrides[task.id] = current.copy(note = newNote.trim())
        saveOverrides(overrides)

        val payload = JSONObject().apply {
            put("action", "updateNote")
            put("rowNumber", task.rowNumber)
            put("orderNumber", task.orderNumber)
            put("title", task.title)
            put("note", newNote.trim())
        }
        postToWebhookIfConfigured(payload)
    }

    suspend fun saveOrderLink(task: OrderTask, url: String): String? = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        val overrides = loadOverrides().toMutableMap()
        val current = overrides[task.id] ?: LocalTaskOverride()
        overrides[task.id] = current.copy(link = cleanUrl)
        saveOverrides(overrides)

        val payload = JSONObject().apply {
            put("action", "saveLink")
            put("rowNumber", task.rowNumber)
            put("orderNumber", task.orderNumber)
            put("title", task.title)
            put("link", cleanUrl)
        }
        postToWebhookIfConfigured(payload)
    }

    suspend fun confirmWeeklyPayout(payoutStatusText: String): String? = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_PAYOUT_OVERRIDE, payoutStatusText).apply()
        val payload = JSONObject().apply {
            put("action", "confirmPayout")
            put("payoutStatus", payoutStatusText)
        }
        postToWebhookIfConfigured(payload)
    }

    suspend fun createTask(
        title: String,
        orderNumber: String,
        note: String,
        deadline: LocalDate
    ): Pair<OrderTask, String?> = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val cleanTitle = title.trim()
        val cleanOrder = orderNumber.trim().ifBlank { "Новый" }
        val cleanNote = note.trim()
        val deadlineStr = SheetsParser.formatDate(deadline)
        val todayStr = SheetsParser.formatDate(today)

        val newTask = OrderTask(
            id = "local_${System.currentTimeMillis()}",
            rowNumber = 0,
            orderNumber = cleanOrder,
            title = cleanTitle,
            amount = 0L,
            createdDateRaw = todayStr,
            createdDate = today,
            status = TaskStatuses.IN_QUEUE,
            paymentStatus = "Не оплачено",
            deadlineRaw = deadlineStr,
            deadline = deadline,
            urgency = "!!",
            note = cleanNote,
            link = "",
            source = if (getWebhookUrl().isNotBlank()) "Google Таблица" else "Локально",
            isLocalOnly = getWebhookUrl().isBlank()
        )

        val locals = loadLocalTasks().toMutableList()
        locals.add(0, newTask)
        saveLocalTasks(locals)

        val payload = JSONObject().apply {
            put("action", "create")
            put("orderNumber", cleanOrder)
            put("title", cleanTitle)
            put("note", cleanNote)
            put("deadline", deadlineStr)
            put("createdDate", todayStr)
            put("status", TaskStatuses.IN_QUEUE)
            put("urgency", "!!")
        }
        val syncMsg = postToWebhookIfConfigured(payload)
        newTask to syncMsg
    }

    suspend fun deleteTask(task: OrderTask): String? = withContext(Dispatchers.IO) {
        val overrides = loadOverrides().toMutableMap()
        val current = overrides[task.id] ?: LocalTaskOverride()
        overrides[task.id] = current.copy(deleted = true)
        saveOverrides(overrides)

        val payload = JSONObject().apply {
            put("action", "delete")
            put("rowNumber", task.rowNumber)
            put("orderNumber", task.orderNumber)
            put("title", task.title)
        }
        postToWebhookIfConfigured(payload)
    }

    private fun postToWebhookIfConfigured(payload: JSONObject): String? {
        val webhook = getWebhookUrl()
        if (webhook.isBlank()) return null
        return try {
            val conn = (URL(webhook).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = true
                connectTimeout = 10000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
            }
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..399) {
                "Синхронизировано с таблицей ✓"
            } else {
                "Сохранено локально (код таблицы: $code)"
            }
        } catch (e: Exception) {
            "Сохранено локально (нет связи с таблицей)"
        }
    }

    private fun fetchText(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 12000
            readTimeout = 15000
            setRequestProperty("User-Agent", "BrifiAndroid/6.0")
        }
        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            throw IllegalStateException("HTTP $code")
        }
        val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        conn.disconnect()
        return body
    }

    private fun loadOverrides(): Map<String, LocalTaskOverride> {
        val raw = prefs.getString(KEY_OVERRIDES, null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, LocalTaskOverride>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val item = obj.getJSONObject(k)
                map[k] = LocalTaskOverride(
                    status = if (item.has("status") && !item.isNull("status")) item.getString("status") else null,
                    note = if (item.has("note") && !item.isNull("note")) item.getString("note") else null,
                    link = if (item.has("link") && !item.isNull("link")) item.getString("link") else null,
                    deleted = item.optBoolean("deleted", false)
                )
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun saveOverrides(map: Map<String, LocalTaskOverride>) {
        val obj = JSONObject()
        map.forEach { (k, v) ->
            obj.put(
                k,
                JSONObject().apply {
                    if (v.status != null) put("status", v.status)
                    if (v.note != null) put("note", v.note)
                    if (v.link != null) put("link", v.link)
                    put("deleted", v.deleted)
                }
            )
        }
        prefs.edit().putString(KEY_OVERRIDES, obj.toString()).apply()
    }

    private fun loadLocalTasks(): List<OrderTask> {
        val raw = prefs.getString(KEY_LOCAL_TASKS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<OrderTask>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val createdRaw = o.optString("createdDateRaw", "")
                val deadlineRaw = o.optString("deadlineRaw", "")
                list.add(
                    OrderTask(
                        id = o.getString("id"),
                        rowNumber = 0,
                        orderNumber = o.optString("orderNumber", "Новый"),
                        title = o.optString("title", "Задача"),
                        amount = o.optLong("amount", 0L),
                        createdDateRaw = createdRaw,
                        createdDate = SheetsParser.parseDate(createdRaw),
                        status = o.optString("status", TaskStatuses.IN_QUEUE),
                        paymentStatus = o.optString("paymentStatus", "Не оплачено"),
                        deadlineRaw = deadlineRaw,
                        deadline = SheetsParser.parseDate(deadlineRaw),
                        urgency = o.optString("urgency", "!!"),
                        note = o.optString("note", ""),
                        link = o.optString("link", ""),
                        source = o.optString("source", "Локально"),
                        isLocalOnly = true
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun saveLocalTasks(tasks: List<OrderTask>) {
        val arr = JSONArray()
        tasks.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("orderNumber", t.orderNumber)
                    put("title", t.title)
                    put("amount", t.amount)
                    put("createdDateRaw", t.createdDateRaw)
                    put("status", t.status)
                    put("paymentStatus", t.paymentStatus)
                    put("deadlineRaw", t.deadlineRaw)
                    put("urgency", t.urgency)
                    put("note", t.note)
                    put("link", t.link)
                    put("source", t.source)
                }
            )
        }
        prefs.edit().putString(KEY_LOCAL_TASKS, arr.toString()).apply()
    }
}
