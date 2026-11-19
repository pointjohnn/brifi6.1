package ru.brifi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.brifi.app.data.BrifiRepository
import ru.brifi.app.data.SheetsParser
import ru.brifi.app.model.BrifiStory
import ru.brifi.app.model.BrifiTab
import ru.brifi.app.model.FinanceSummary
import ru.brifi.app.model.OrderTask
import ru.brifi.app.model.TaskStatuses
import ru.brifi.app.model.WeeklyPayout
import ru.brifi.app.ui.BrifiColors
import ru.brifi.app.ui.BrifiTheme
import ru.brifi.app.ui.ChecklistStoryScreen
import ru.brifi.app.ui.ConfirmPayoutModal
import ru.brifi.app.ui.CreateTaskModal
import ru.brifi.app.ui.DeleteConfirmModal
import ru.brifi.app.ui.FloatingBottomBar
import ru.brifi.app.ui.HomeScreen
import ru.brifi.app.ui.ProductionStoryScreen
import ru.brifi.app.ui.ProfileScreen
import ru.brifi.app.ui.SaveLayoutLinkModal
import ru.brifi.app.ui.SplashScreen
import ru.brifi.app.ui.StatsScreen
import ru.brifi.app.ui.StatusPickerModal
import ru.brifi.app.ui.SyncSettingsModal
import ru.brifi.app.ui.TaskDetailModal

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = BrifiRepository(applicationContext)
        setContent {
            BrifiTheme {
                BrifiApp(
                    repository = repository,
                    lifecycle = this.lifecycle
                )
            }
        }
    }
}

@Composable
fun BrifiApp(
    repository: BrifiRepository,
    lifecycle: Lifecycle
) {
    val scope = rememberCoroutineScope()

    var showSplash by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(BrifiTab.HOME) }
    var activeStory by remember { mutableStateOf<BrifiStory?>(null) }
    var tasks by remember { mutableStateOf<List<OrderTask>>(emptyList()) }
    var finance by remember {
        mutableStateOf(
            FinanceSummary(
                latestWeek = WeeklyPayout("", "текущей недели", "Загрузка...", 0L),
                allTimeTotalIncome = 0L,
                allTimeCommission50 = 0L,
                allTimeSalary = 0L,
                currentMonthLabel = "Этот месяц",
                currentMonthTotalIncome = 0L,
                currentMonthCommission50 = 0L,
                currentMonthSalary = 0L
            )
        )
    }
    var lastUpdatedText by remember { mutableStateOf(repository.getLastSyncText()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf<String?>(null) }
    var webhookUrl by remember { mutableStateOf(repository.getWebhookUrl()) }
    var statsWeeksAgo by remember { mutableIntStateOf(0) }
    var checklistChecked by remember { mutableStateOf(repository.getChecklistChecked()) }

    var showCreateModal by remember { mutableStateOf(false) }
    var showSaveLinkModal by remember { mutableStateOf(false) }
    var showConfirmPayoutModal by remember { mutableStateOf(false) }
    var detailTask by remember { mutableStateOf<OrderTask?>(null) }
    var statusPickerTask by remember { mutableStateOf<OrderTask?>(null) }
    var deleteConfirmTask by remember { mutableStateOf<OrderTask?>(null) }
    var showSyncSettings by remember { mutableStateOf(false) }

    fun applySyncResult(result: BrifiRepository.SyncResult) {
        tasks = result.tasks
        finance = result.finance
        lastUpdatedText = result.lastUpdatedText
        statusBanner = result.errorMessage
    }

    fun triggerRefresh(showToastOnError: Boolean = true) {
        if (isRefreshing) return
        scope.launch {
            isRefreshing = true
            val result = repository.refreshFromGoogleSheets()
            applySyncResult(result)
            if (!showToastOnError && result.errorMessage != null) {
                statusBanner = null
            }
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        repository.loadCached()?.let { applySyncResult(it) }
        val refreshJob = launch {
            isRefreshing = true
            val result = repository.refreshFromGoogleSheets()
            applySyncResult(result)
            isRefreshing = false
        }
        delay(1000L)
        showSplash = false
        refreshJob.join()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5L * 60L * 1000L)
            triggerRefresh(showToastOnError = false)
        }
    }

    DisposableEffect(lifecycle) {
        var firstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (firstResume) {
                    firstResume = false
                } else {
                    triggerRefresh(showToastOnError = false)
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (showSplash) {
        SplashScreen(lastUpdated = lastUpdatedText)
        return
    }

    // Полноэкранные сторис («Производство» / «Чек-лист»)
    when (activeStory) {
        BrifiStory.PRODUCTION -> {
            ProductionStoryScreen(onClose = { activeStory = null })
            return
        }
        BrifiStory.CHECKLIST -> {
            ChecklistStoryScreen(
                checkedIndices = checklistChecked,
                onToggleIndex = { idx ->
                    val next = if (idx in checklistChecked) checklistChecked - idx else checklistChecked + idx
                    checklistChecked = next
                    repository.saveChecklistChecked(next)
                },
                onResetAll = {
                    checklistChecked = emptySet()
                    repository.saveChecklistChecked(emptySet())
                },
                onClose = { activeStory = null }
            )
            return
        }
        null -> Unit
    }

    val inProgressTasks = remember(tasks) {
        SheetsParser.selectInProgressTasks(tasks)
    }
    val otherTasks = remember(tasks, inProgressTasks) {
        SheetsParser.selectOtherActiveTasks(tasks, inProgressTasks)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrifiColors.BgBlack)
    ) {
        when (currentTab) {
            BrifiTab.HOME -> HomeScreen(
                allTasks = tasks,
                inProgressTasks = inProgressTasks,
                otherTasks = otherTasks,
                lastUpdatedText = lastUpdatedText,
                isRefreshing = isRefreshing,
                statusBanner = statusBanner,
                onOpenStory = { activeStory = it },
                onCreateTaskClick = { showCreateModal = true },
                onSaveLayoutClick = { showSaveLinkModal = true },
                onOpenDetails = { detailTask = it },
                onChangeStatus = { statusPickerTask = it },
                onMarkDone = { task ->
                    scope.launch {
                        tasks = tasks.map {
                            if (it.id == task.id) it.copy(status = TaskStatuses.READY) else it
                        }
                        val syncMsg = repository.updateTaskStatus(task, TaskStatuses.READY)
                        if (syncMsg != null) statusBanner = syncMsg
                    }
                },
                onTakeIntoWork = { task ->
                    scope.launch {
                        tasks = tasks.map {
                            if (it.id == task.id) it.copy(status = TaskStatuses.IN_PROGRESS) else it
                        }
                        val syncMsg = repository.updateTaskStatus(task, TaskStatuses.IN_PROGRESS)
                        if (syncMsg != null) statusBanner = syncMsg
                    }
                },
                onRefresh = { triggerRefresh(showToastOnError = true) }
            )

            BrifiTab.STATS -> StatsScreen(
                allTasks = tasks,
                totalOrdersCount = finance.totalOrdersCount.coerceAtLeast(tasks.size),
                weeksAgo = statsWeeksAgo,
                onPrevWeek = { statsWeeksAgo++ },
                onNextWeek = { if (statsWeeksAgo > 0) statsWeeksAgo-- },
                onOpenDetails = { detailTask = it },
                onChangeStatus = { statusPickerTask = it }
            )

            BrifiTab.PROFILE -> ProfileScreen(
                finance = finance,
                isRefreshing = isRefreshing,
                hasWebhookConfigured = webhookUrl.isNotBlank(),
                onConfirmPayoutClick = { showConfirmPayoutModal = true },
                onOpenSyncSettings = { showSyncSettings = true },
                onRefresh = { triggerRefresh(showToastOnError = true) }
            )
        }

        FloatingBottomBar(
            currentTab = currentTab,
            onSelectTab = { currentTab = it },
            onAddClick = { showCreateModal = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showCreateModal) {
        CreateTaskModal(
            onDismiss = { showCreateModal = false },
            onSave = { title, orderNumber, note, deadline ->
                showCreateModal = false
                scope.launch {
                    val (created, syncMsg) = repository.createTask(title, orderNumber, note, deadline)
                    tasks = listOf(created) + tasks
                    if (syncMsg != null) statusBanner = syncMsg
                }
            }
        )
    }

    if (showSaveLinkModal) {
        val activeForLink = remember(inProgressTasks, otherTasks) { inProgressTasks + otherTasks }
        SaveLayoutLinkModal(
            activeTasks = activeForLink,
            onDismiss = { showSaveLinkModal = false },
            onSaveLink = { targetTask, url ->
                showSaveLinkModal = false
                scope.launch {
                    tasks = tasks.map {
                        if (it.id == targetTask.id) it.copy(link = url) else it
                    }
                    val syncMsg = repository.saveOrderLink(targetTask, url)
                    statusBanner = syncMsg ?: "Ссылка сохранена для ${targetTask.orderNumber}"
                }
            }
        )
    }

    if (showConfirmPayoutModal) {
        ConfirmPayoutModal(
            weekLabel = finance.latestWeek.startDateLabel,
            amountText = SheetsParser.formatMoney(finance.latestWeek.amount),
            onDismiss = { showConfirmPayoutModal = false },
            onConfirm = { methodText ->
                showConfirmPayoutModal = false
                scope.launch {
                    finance = finance.copy(
                        latestWeek = finance.latestWeek.copy(payoutStatus = methodText)
                    )
                    val syncMsg = repository.confirmWeeklyPayout(methodText)
                    statusBanner = syncMsg ?: "Оплата отмечена: $methodText"
                }
            }
        )
    }

    detailTask?.let { current ->
        TaskDetailModal(
            task = current,
            onDismiss = { detailTask = null },
            onSaveNote = { newNote ->
                detailTask = null
                scope.launch {
                    tasks = tasks.map {
                        if (it.id == current.id) it.copy(note = newNote.trim()) else it
                    }
                    val syncMsg = repository.updateTaskNote(current, newNote)
                    if (syncMsg != null) statusBanner = syncMsg
                }
            },
            onOpenStatusPicker = {
                detailTask = null
                statusPickerTask = current
            },
            onRequestDelete = {
                detailTask = null
                deleteConfirmTask = current
            }
        )
    }

    statusPickerTask?.let { target ->
        StatusPickerModal(
            currentStatus = target.status,
            onDismiss = { statusPickerTask = null },
            onSelect = { newStatus ->
                statusPickerTask = null
                scope.launch {
                    tasks = tasks.map {
                        if (it.id == target.id) it.copy(status = newStatus) else it
                    }
                    val syncMsg = repository.updateTaskStatus(target, newStatus)
                    if (syncMsg != null) statusBanner = syncMsg
                }
            }
        )
    }

    deleteConfirmTask?.let { target ->
        DeleteConfirmModal(
            taskTitle = target.title,
            onDismiss = { deleteConfirmTask = null },
            onConfirm = {
                deleteConfirmTask = null
                scope.launch {
                    tasks = tasks.filterNot { it.id == target.id }
                    val syncMsg = repository.deleteTask(target)
                    if (syncMsg != null) statusBanner = syncMsg
                }
            }
        )
    }

    if (showSyncSettings) {
        SyncSettingsModal(
            currentUrl = webhookUrl,
            onDismiss = { showSyncSettings = false },
            onSave = { newUrl ->
                repository.saveWebhookUrl(newUrl)
                webhookUrl = newUrl
                showSyncSettings = false
                statusBanner = if (newUrl.isNotBlank()) {
                    "Синхронизация с таблицей подключена ✓"
                } else {
                    "Обратная запись отключена"
                }
            }
        )
    }
}
