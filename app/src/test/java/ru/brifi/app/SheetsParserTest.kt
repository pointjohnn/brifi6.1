package ru.brifi.app

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.brifi.app.data.SheetsParser
import ru.brifi.app.model.TaskStatuses
import java.time.LocalDate

class SheetsParserTest {

    private val sampleOrdersCsv = """
Номер заказа,Сумма,Дата,Статус,Оплата,Дедлайн,Срочность,Комментарии,Ссылка,Сумма заказов,Мой процент,
26-001590,500,18.11.2026,В очереди,Оплачено клиентом,25.11.2026,!,Низкая срочность,Ссылка,203440,101720,
26-001594,1200,18.11.2026,В очереди,Оплачено клиентом,20.11.2026,!!,Мейк кофе меню. Правки,Ссылка,,,
26-001595,1500,18.11.2026,В очереди,Оплачено клиентом,19.11.2026,!!!,Срочный макет,Ссылка,,,
26-001598,0,18.11.2026,В работе,Бесплатно,20.11.2026,!,Дтф на футболку | Заметка: Размер L,https://disk.yandex.ru/d/test,,,
26-001601,1200,18.11.2026,В работе,Оплачено клиентом,19.11.2026,!!!,"Реклама в КУБ ММА, срочно",Ссылка,Итого за неделю 16.11-22.11.2026,,
26-001540,3400,10.11.2026,Макет готов,Оплачено клиентом,12.11.2026,✓,МАНГАЛ HOUSE,Ссылка,Ожидает оплаты,,2600
,,,,,,,,Ссылка,,,
    """.trimIndent()

    private val sampleStatsCsv = """
,📊 Аналитика и статистика заказов,,,,,,,
,ВСЕГО ЗАКАЗОВ,,ОПЛАЧЕНО ЗАКАЗОВ,,,НЕ ОПЛАЧЕНО,,
,194,,133,,,5,,
,Статус готовности,Количество,Доля,,Тип заказа,Количество,Доля,
,Готово,180,0.9278,,Платные,138,0.7113,
,Не готово,14,0.0721,,Бесплатные,56,0.2886,
,СРЕДНИЙ ДОХОД В МЕСЯЦ,,ПОСТОЯННЫЙ ОКЛАД,,,СРЕДНИЙ % С ЗАКАЗОВ,,
,41953.33,,25000.0,,,16953.33,,
,Месяц,Заказов,Доход (50%),,Оклад,Итого доход,,
,Октябрь 2026,40,18200,,25000,43200,,
,Ноябрь 2026,32,14600,,25000,39600,,
,Итого,194,101720,,150000,251720,,
    """.trimIndent()

    @Test
    fun parsesOrdersLinksNotesAndWeeklyPayout() {
        val result = SheetsParser.parseOrdersSheet(sampleOrdersCsv)
        assertEquals(6, result.tasks.size)
        assertEquals(101720L, result.totalCommission50FromSheet)
        assertEquals("16.11.2026", result.latestWeeklyPayout.startDateLabel)
        assertEquals("Ожидает оплаты", result.latestWeeklyPayout.payoutStatus)
        assertEquals(2600L, result.latestWeeklyPayout.amount)

        val dtfTask = result.tasks.first { it.orderNumber == "26-001598" }
        assertEquals(TaskStatuses.IN_PROGRESS, dtfTask.status)
        assertEquals("Дтф на футболку", dtfTask.title)
        assertEquals("Размер L", dtfTask.note)
        assertEquals("https://disk.yandex.ru/d/test", dtfTask.link)
    }

    @Test
    fun sortsBothInProgressAndOtherTasksStrictlyByUrgency() {
        val result = SheetsParser.parseOrdersSheet(sampleOrdersCsv)
        val inProgress = SheetsParser.selectInProgressTasks(result.tasks)
        assertEquals(2, inProgress.size)
        assertEquals("26-001601", inProgress[0].orderNumber)
        assertEquals("26-001598", inProgress[1].orderNumber)

        val others = SheetsParser.selectOtherActiveTasks(result.tasks, inProgress)
        assertEquals(3, others.size)
        assertEquals("26-001595", others[0].orderNumber)
        assertEquals("26-001594", others[1].orderNumber)
        assertEquals("26-001590", others[2].orderNumber)
    }

    @Test
    fun parsesFullFinanceAnalyticsIncludingAveragesAndPaidFreeSplit() {
        val orders = SheetsParser.parseOrdersSheet(sampleOrdersCsv)
        val finance = SheetsParser.parseFinanceStats(
            statsCsvText = sampleStatsCsv,
            latestWeek = orders.latestWeeklyPayout,
            today = LocalDate.of(2026, 11, 18),
            fallbackOrdersCount = orders.tasks.size
        )

        assertEquals(251720L, finance.allTimeTotalIncome)
        assertEquals(39600L, finance.currentMonthTotalIncome)
        assertEquals(41953L, finance.avgMonthlyTotalIncome)
        assertEquals(16953L, finance.avgMonthlyCommission50)
        assertEquals(138, finance.paidOrdersCount)
        assertEquals(56, finance.freeOrdersCount)
        assertEquals(194, finance.totalOrdersCount)
    }

    @Test
    fun filtersTasksByWeekOffset() {
        val orders = SheetsParser.parseOrdersSheet(sampleOrdersCsv)
        val today = LocalDate.of(2026, 11, 18)
        val (curStart, curEnd) = SheetsParser.weekBounds(today, 0)
        val curWeekTasks = SheetsParser.filterTasksForWeek(orders.tasks, curStart, curEnd)
        assertEquals(5, curWeekTasks.size)
        assertEquals("Неделя от 16.11.2026", SheetsParser.formatWeekStart(curStart))

        val (prevStart, prevEnd) = SheetsParser.weekBounds(today, 1)
        val prevWeekTasks = SheetsParser.filterTasksForWeek(orders.tasks, prevStart, prevEnd)
        assertEquals(1, prevWeekTasks.size)
        assertEquals("26-001540", prevWeekTasks[0].orderNumber)
    }
}
