package ru.brifi.app.data

import ru.brifi.app.model.FinanceSummary
import ru.brifi.app.model.MonthlyIncome
import ru.brifi.app.model.OrderTask
import ru.brifi.app.model.TaskStatuses
import ru.brifi.app.model.WeeklyPayout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object SheetsParser {

    const val SPREADSHEET_ID = "1iG8EavyDrQ2GmElpKSgjjuxF09Cz5Jw2HR54HuJSnhA"
    const val ORDERS_GID = "0"
    const val STATS_GID = "987654321"

    val ORDERS_CSV_URL =
        "https://docs.google.com/spreadsheets/d/$SPREADSHEET_ID/export?format=csv&gid=$ORDERS_GID"
    val STATS_CSV_URL =
        "https://docs.google.com/spreadsheets/d/$SPREADSHEET_ID/export?format=csv&gid=$STATS_GID"

    private const val NOTE_DELIMITER = " | Заметка:"
    private val RU_DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private val RU_MONTHS = mapOf(
        "январь" to 1, "января" to 1,
        "февраль" to 2, "февраля" to 2,
        "март" to 3, "марта" to 3,
        "апрель" to 4, "апреля" to 4,
        "май" to 5, "мая" to 5,
        "июнь" to 6, "июня" to 6,
        "июль" to 7, "июля" to 7,
        "август" to 8, "августа" to 8,
        "сентябрь" to 9, "сентября" to 9,
        "октябрь" to 10, "октября" to 10,
        "ноябрь" to 11, "ноября" to 11,
        "декабрь" to 12, "декабря" to 12
    )

    fun parseCsv(csvText: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val clean = csvText.removePrefix("\uFEFF")

        while (i < clean.length) {
            val c = clean[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < clean.length && clean[i + 1] == '"') {
                        field.append('"')
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(field.toString())
                        field.clear()
                    }
                    '\r' -> {
                        if (i + 1 < clean.length && clean[i + 1] == '\n') i++
                        currentRow.add(field.toString())
                        field.clear()
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(field.toString())
                        field.clear()
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(field.toString())
            rows.add(currentRow.toList())
        }
        return rows
    }

    fun parseMoney(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val cleaned = raw
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace("₽", "")
            .replace("руб.", "", ignoreCase = true)
            .replace("руб", "", ignoreCase = true)
            .trim()
        val match = Regex("-?\\d+(?:[.,]\\d+)?").find(cleaned) ?: return 0L
        val normalized = match.value.replace(',', '.')
        return normalized.toDoubleOrNull()?.toLong() ?: 0L
    }

    fun formatMoney(amount: Long, withSpaceBeforeSymbol: Boolean = false): String {
        val negative = amount < 0
        val digits = kotlin.math.abs(amount).toString()
        val sb = StringBuilder()
        for (idx in digits.indices) {
            if (idx > 0 && (digits.length - idx) % 3 == 0) {
                sb.append(' ')
            }
            sb.append(digits[idx])
        }
        val suffix = if (withSpaceBeforeSymbol) " ₽" else "₽"
        return (if (negative) "-" else "") + sb.toString() + suffix
    }

    fun parseDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim().substringBefore(" ")
        Regex("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})$").matchEntire(s)?.let { m ->
            return runCatching {
                LocalDate.of(m.groupValues[3].toInt(), m.groupValues[2].toInt(), m.groupValues[1].toInt())
            }.getOrNull()
        }
        Regex("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d{2})$").matchEntire(s)?.let { m ->
            return runCatching {
                LocalDate.of(2000 + m.groupValues[3].toInt(), m.groupValues[2].toInt(), m.groupValues[1].toInt())
            }.getOrNull()
        }
        Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$").matchEntire(s)?.let { m ->
            return runCatching {
                LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
            }.getOrNull()
        }
        Regex("^(\\d{1,2})/(\\d{1,2})/(\\d{4})$").matchEntire(s)?.let { m ->
            return runCatching {
                LocalDate.of(m.groupValues[3].toInt(), m.groupValues[1].toInt(), m.groupValues[2].toInt())
            }.getOrNull()
        }
        return null
    }

    fun formatDate(date: LocalDate?): String {
        return date?.format(RU_DATE_FMT) ?: "Без даты"
    }

    data class OrdersSheetResult(
        val tasks: List<OrderTask>,
        val latestWeeklyPayout: WeeklyPayout,
        val totalCommission50FromSheet: Long
    )

    fun parseOrdersSheet(csvText: String): OrdersSheetResult {
        val rows = parseCsv(csvText)
        val tasks = mutableListOf<OrderTask>()
        var totalCommission50 = 0L
        var latestWeek = WeeklyPayout(
            weekLabel = "",
            startDateLabel = "текущей недели",
            payoutStatus = "Ожидает оплаты",
            amount = 0L
        )

        if (rows.size > 1) {
            val row2 = rows[1]
            if (row2.size > 10) {
                totalCommission50 = parseMoney(row2[10])
            }
        }

        for (idx in 1 until rows.size) {
            val row = rows[idx]
            val colA = row.getOrNull(0)?.trim().orEmpty()
            val colB = row.getOrNull(1)?.trim().orEmpty()
            val colC = row.getOrNull(2)?.trim().orEmpty()
            val colD = row.getOrNull(3)?.trim().orEmpty()
            val colE = row.getOrNull(4)?.trim().orEmpty()
            val colF = row.getOrNull(5)?.trim().orEmpty()
            val colG = row.getOrNull(6)?.trim().orEmpty()
            val colH = row.getOrNull(7)?.trim().orEmpty()
            val colI = row.getOrNull(8)?.trim().orEmpty()

            val colJ = row.getOrNull(9)?.trim().orEmpty()
            if (colJ.startsWith("Итого за неделю", ignoreCase = true)) {
                val weekRange = colJ.removePrefix("Итого за неделю").trim()
                val startDate = extractWeekStartDate(weekRange)
                val nextRow = rows.getOrNull(idx + 1)
                val statusText = nextRow?.getOrNull(9)?.trim().orEmpty()
                    .ifBlank { "Ожидает оплаты" }
                val amountVal = parseMoney(nextRow?.getOrNull(11))
                    .takeIf { it > 0L }
                    ?: parseMoney(nextRow?.getOrNull(10))
                latestWeek = WeeklyPayout(
                    weekLabel = weekRange,
                    startDateLabel = startDate,
                    payoutStatus = statusText,
                    amount = amountVal
                )
            }

            val hasOrderData = colA.isNotBlank() || colH.isNotBlank() || colD.isNotBlank()
            if (!hasOrderData) continue

            val splitIdx = colH.indexOf(NOTE_DELIMITER)
            val parsedTitle: String
            val parsedNote: String
            if (splitIdx >= 0) {
                parsedTitle = colH.substring(0, splitIdx).trim()
                parsedNote = colH.substring(splitIdx + NOTE_DELIMITER.length).trim()
            } else {
                parsedTitle = colH
                parsedNote = ""
            }

            val title = parsedTitle.ifBlank { colA.ifBlank { "Заказ #${idx + 1}" } }
            val orderNum = colA.ifBlank { "Без номера" }
            val status = colD.ifBlank { TaskStatuses.IN_QUEUE }
            val createdDate = parseDate(colC)
            val deadline = parseDate(colF)
            val link = if (colI.startsWith("http://") || colI.startsWith("https://")) colI else ""

            tasks.add(
                OrderTask(
                    id = "sheet_r${idx + 1}_${orderNum}",
                    rowNumber = idx + 1,
                    orderNumber = orderNum,
                    title = title,
                    amount = parseMoney(colB),
                    createdDateRaw = colC,
                    createdDate = createdDate,
                    status = status,
                    paymentStatus = colE.ifBlank { "Не указано" },
                    deadlineRaw = colF,
                    deadline = deadline,
                    urgency = colG,
                    note = parsedNote,
                    link = link,
                    source = "Google Таблица",
                    isLocalOnly = false
                )
            )
        }

        return OrdersSheetResult(
            tasks = tasks.reversed(),
            latestWeeklyPayout = latestWeek,
            totalCommission50FromSheet = totalCommission50
        )
    }

    private fun extractWeekStartDate(weekRange: String): String {
        val fullMatch = Regex("(\\d{2}\\.\\d{2})\\s*-\\s*\\d{2}\\.\\d{2}\\.(\\d{4})").find(weekRange)
        if (fullMatch != null) {
            return "${fullMatch.groupValues[1]}.${fullMatch.groupValues[2]}"
        }
        val singleDate = Regex("\\d{2}\\.\\d{2}\\.\\d{4}").find(weekRange)
        if (singleDate != null) {
            return singleDate.value
        }
        return weekRange.ifBlank { "текущей недели" }
    }

    fun parseFinanceStats(
        statsCsvText: String,
        latestWeek: WeeklyPayout,
        today: LocalDate = LocalDate.now(),
        fallbackOrdersCount: Int = 0
    ): FinanceSummary {
        val rows = parseCsv(statsCsvText)
        val months = mutableListOf<MonthlyIncome>()
        var allTimeOrders = fallbackOrdersCount
        var allTimeCommission = 0L
        var allTimeSalary = 0L
        var allTimeTotal = 0L
        var avgMonthlyTotal = 0L
        var avgMonthlyCommission = 0L
        var paidCount = 0
        var freeCount = 0

        var inMonthlyTable = false

        for (idx in rows.indices) {
            val row = rows[idx]
            val joined = row.joinToString(" ").trim()

            // Parse Платные / Бесплатные counts
            for (c in row.indices) {
                val cell = row[c].trim()
                if (cell.equals("Платные", ignoreCase = true)) {
                    paidCount = parseMoney(row.getOrNull(c + 1)).toInt()
                } else if (cell.equals("Бесплатные", ignoreCase = true)) {
                    freeCount = parseMoney(row.getOrNull(c + 1)).toInt()
                }
            }

            // Parse Средний доход в месяц / Средний % с заказов
            if (joined.contains("СРЕДНИЙ ДОХОД В МЕСЯЦ", ignoreCase = true)) {
                val nextRow = rows.getOrNull(idx + 1)
                if (nextRow != null) {
                    val nums = extractNumericValues(nextRow)
                    if (nums.isNotEmpty()) avgMonthlyTotal = nums[0]
                    if (nums.size >= 3) avgMonthlyCommission = nums[2]
                    else if (nums.size == 2) avgMonthlyCommission = nums[1]
                }
            }

            if (joined.contains("Месяц", ignoreCase = true) && joined.contains("Итого доход", ignoreCase = true)) {
                inMonthlyTable = true
                continue
            }
            if (!inMonthlyTable) continue

            val labelIdx = row.indexOfFirst { it.trim().isNotEmpty() }
            if (labelIdx == -1) continue
            val label = row[labelIdx].trim()

            if (label.equals("Итого", ignoreCase = true)) {
                val nums = extractNumericValues(row.drop(labelIdx + 1))
                if (nums.size >= 4) {
                    allTimeOrders = nums[0].toInt()
                    allTimeCommission = nums[1]
                    allTimeSalary = nums[2]
                    allTimeTotal = nums[3]
                } else if (nums.size == 3) {
                    allTimeCommission = nums[0]
                    allTimeSalary = nums[1]
                    allTimeTotal = nums[2]
                }
                break
            }

            val parsedMonth = parseRussianMonthYear(label)
            if (parsedMonth != null) {
                val nums = extractNumericValues(row.drop(labelIdx + 1))
                val ordersCount = nums.getOrNull(0)?.toInt() ?: 0
                val commission50 = nums.getOrNull(1) ?: 0L
                val salary = nums.getOrNull(2) ?: 0L
                val total = nums.getOrNull(3) ?: (commission50 + salary)
                months.add(
                    MonthlyIncome(
                        monthLabel = label,
                        monthNumber = parsedMonth.first,
                        year = parsedMonth.second,
                        ordersCount = ordersCount,
                        commission50 = commission50,
                        salary = salary,
                        totalIncome = total
                    )
                )
            }
        }

        if (allTimeTotal == 0L && months.isNotEmpty()) {
            allTimeCommission = months.sumOf { it.commission50 }
            allTimeSalary = months.sumOf { it.salary }
            allTimeTotal = months.sumOf { it.totalIncome }
        }

        val activeMonths = months.filter { it.ordersCount > 0 || it.totalIncome > 0L }
        if (avgMonthlyTotal == 0L && activeMonths.isNotEmpty()) {
            avgMonthlyTotal = activeMonths.sumOf { it.totalIncome } / activeMonths.size
        }
        if (avgMonthlyCommission == 0L && activeMonths.isNotEmpty()) {
            avgMonthlyCommission = activeMonths.sumOf { it.commission50 } / activeMonths.size
        }

        val currentMonth = months.firstOrNull {
            it.year == today.year && it.monthNumber == today.monthValue
        } ?: months.lastOrNull { it.totalIncome > 0L || it.ordersCount > 0 }

        return FinanceSummary(
            latestWeek = latestWeek,
            allTimeTotalIncome = allTimeTotal,
            allTimeCommission50 = allTimeCommission,
            allTimeSalary = allTimeSalary,
            currentMonthLabel = currentMonth?.monthLabel ?: "Этот месяц",
            currentMonthTotalIncome = currentMonth?.totalIncome ?: 0L,
            currentMonthCommission50 = currentMonth?.commission50 ?: 0L,
            currentMonthSalary = currentMonth?.salary ?: 0L,
            avgMonthlyCommission50 = avgMonthlyCommission,
            avgMonthlyTotalIncome = avgMonthlyTotal,
            paidOrdersCount = paidCount,
            freeOrdersCount = freeCount,
            totalOrdersCount = if (allTimeOrders > 0) allTimeOrders else fallbackOrdersCount,
            monthlyBreakdown = months
        )
    }

    private fun extractNumericValues(cells: List<String>): List<Long> {
        val result = mutableListOf<Long>()
        for (cell in cells) {
            val trimmed = cell.trim()
            if (trimmed.isEmpty()) continue
            if (Regex(".*\\d.*").matches(trimmed)) {
                result.add(parseMoney(trimmed))
            }
        }
        return result
    }

    private fun parseRussianMonthYear(label: String): Pair<Int, Int>? {
        val parts = label.trim().lowercase().split(Regex("\\s+"))
        if (parts.isEmpty()) return null
        val monthNum = RU_MONTHS[parts[0]] ?: return null
        val year = parts.getOrNull(1)?.toIntOrNull() ?: LocalDate.now().year
        return monthNum to year
    }

    fun selectInProgressTasks(tasks: List<OrderTask>): List<OrderTask> {
        val active = tasks.filter { it.isActive }
        if (active.isEmpty()) return emptyList()

        val explicitlyInProgress = active.filter {
            it.status.equals(TaskStatuses.IN_PROGRESS, ignoreCase = true)
        }
        if (explicitlyInProgress.isNotEmpty()) {
            return explicitlyInProgress.sortedWith(
                compareByDescending<OrderTask> { it.urgencyRank }
                    .thenBy { it.deadline ?: LocalDate.MAX }
                    .thenByDescending { it.rowNumber }
            )
        }

        val fallback = active.sortedWith(
            compareByDescending<OrderTask> { it.urgencyRank }
                .thenBy { it.deadline ?: LocalDate.MAX }
        ).first()
        return listOf(fallback)
    }

    fun selectOtherActiveTasks(tasks: List<OrderTask>, inProgressTasks: List<OrderTask>): List<OrderTask> {
        val inProgressIds = inProgressTasks.map { it.id }.toSet()
        return tasks
            .filter { it.isActive && it.id !in inProgressIds }
            .sortedWith(
                compareByDescending<OrderTask> { it.urgencyRank }
                    .thenBy { it.deadline ?: LocalDate.MAX }
                    .thenByDescending { it.rowNumber }
            )
    }

    fun weekBounds(today: LocalDate, weeksAgo: Int): Pair<LocalDate, LocalDate> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .minusWeeks(weeksAgo.toLong())
        val sunday = monday.plusDays(6)
        return monday to sunday
    }

    fun formatWeekStart(start: LocalDate): String {
        return "Неделя от ${start.format(RU_DATE_FMT)}"
    }

    fun filterTasksForWeek(tasks: List<OrderTask>, start: LocalDate, end: LocalDate): List<OrderTask> {
        return tasks.filter { task ->
            val d = task.createdDate ?: task.deadline
            d != null && !d.isBefore(start) && !d.isAfter(end)
        }
    }
}
