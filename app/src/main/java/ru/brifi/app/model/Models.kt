package ru.brifi.app.model

import java.time.LocalDate

object TaskStatuses {
    const val IN_PROGRESS = "В работе"
    const val IN_QUEUE = "В очереди"
    const val REVISIONS = "Правки"
    const val CLIENT_APPROVAL = "Согласование с клиентом"
    const val WAITING_TZ = "Ждет ТЗ/Материалов"
    const val UPLOAD_TO_DRIVE = "Загрузить на диск"
    const val READY = "Макет готов"
    const val CANCELLED = "Отмена"

    val all: List<String> = listOf(
        IN_PROGRESS,
        IN_QUEUE,
        REVISIONS,
        CLIENT_APPROVAL,
        WAITING_TZ,
        UPLOAD_TO_DRIVE,
        READY,
        CANCELLED
    )

    fun isCompleted(status: String): Boolean {
        val s = status.trim().lowercase()
        return s == READY.lowercase() || s == "готово"
    }

    fun isCancelled(status: String): Boolean {
        return status.trim().lowercase() == CANCELLED.lowercase()
    }

    fun isActive(status: String): Boolean {
        return !isCompleted(status) && !isCancelled(status)
    }
}

data class OrderTask(
    val id: String,
    val rowNumber: Int,
    val orderNumber: String,
    val title: String,
    val amount: Long,
    val createdDateRaw: String,
    val createdDate: LocalDate?,
    val status: String,
    val paymentStatus: String,
    val deadlineRaw: String,
    val deadline: LocalDate?,
    val urgency: String,
    val note: String,
    val link: String,
    val source: String = "Google Таблица",
    val isLocalOnly: Boolean = false
) {
    val urgencyRank: Int
        get() = when (urgency.trim()) {
            "!!!" -> 3
            "!!" -> 2
            "!" -> 1
            else -> 0
        }

    val isActive: Boolean
        get() = TaskStatuses.isActive(status)

    val isCompleted: Boolean
        get() = TaskStatuses.isCompleted(status)
}

data class WeeklyPayout(
    val weekLabel: String,
    val startDateLabel: String,
    val payoutStatus: String,
    val amount: Long
)

data class MonthlyIncome(
    val monthLabel: String,
    val monthNumber: Int,
    val year: Int,
    val ordersCount: Int,
    val commission50: Long,
    val salary: Long,
    val totalIncome: Long
)

data class FinanceSummary(
    val latestWeek: WeeklyPayout,
    val allTimeTotalIncome: Long,
    val allTimeCommission50: Long,
    val allTimeSalary: Long,
    val currentMonthLabel: String,
    val currentMonthTotalIncome: Long,
    val currentMonthCommission50: Long,
    val currentMonthSalary: Long,
    val avgMonthlyCommission50: Long = 0L,
    val avgMonthlyTotalIncome: Long = 0L,
    val paidOrdersCount: Int = 0,
    val freeOrdersCount: Int = 0,
    val totalOrdersCount: Int = 0,
    val monthlyBreakdown: List<MonthlyIncome> = emptyList()
)

data class LocalTaskOverride(
    val status: String? = null,
    val note: String? = null,
    val link: String? = null,
    val deleted: Boolean = false
)

enum class BrifiTab {
    HOME,
    STATS,
    PROFILE
}

enum class BrifiStory {
    PRODUCTION,
    CHECKLIST
}
