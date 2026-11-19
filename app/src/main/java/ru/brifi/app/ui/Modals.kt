package ru.brifi.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.brifi.app.data.BrifiRepository
import ru.brifi.app.data.SheetsParser
import ru.brifi.app.model.OrderTask
import ru.brifi.app.model.TaskStatuses
import java.time.LocalDate

@Composable
fun BrifiModalContainer(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(38.dp))
                .background(BrifiColors.BgBlack)
                .border(1.dp, BrifiColors.SheetDarkBorder, RoundedCornerShape(38.dp))
                .padding(22.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun BrifiOutlinedField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minHeightDp: Int = 50,
    singleLine: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = BrifiColors.TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeightDp.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF3E3E4A), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color(0xFF666674),
                    fontSize = 13.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(
                    color = BrifiColors.TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = GoogleSansFamily
                ),
                cursorBrush = SolidColor(BrifiColors.AccentIndigo),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Оверлей «Новая задача» (с подтверждением выхода «Точно удалить? / Не сохранять / СОХРАНИТЬ»)
 */
@Composable
fun CreateTaskModal(
    onDismiss: () -> Unit,
    onSave: (title: String, orderNumber: String, note: String, deadline: LocalDate) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var orderNumber by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var deadlineMode by remember { mutableIntStateOf(0) }
    var customDaysOffset by remember { mutableIntStateOf(2) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val selectedDate = when (deadlineMode) {
        0 -> today
        1 -> today.plusDays(1)
        else -> today.plusDays(customDaysOffset.toLong())
    }

    val hasDraft = title.isNotBlank() || orderNumber.isNotBlank() || note.isNotBlank()

    fun attemptClose() {
        if (hasDraft) {
            showDiscardConfirm = true
        } else {
            onDismiss()
        }
    }

    if (showDiscardConfirm) {
        DiscardDraftModal(
            canSave = title.isNotBlank(),
            onDiscard = {
                showDiscardConfirm = false
                onDismiss()
            },
            onSave = {
                showDiscardConfirm = false
                if (title.isNotBlank()) {
                    onSave(title, orderNumber, note, selectedDate)
                } else {
                    onDismiss()
                }
            },
            onCancel = { showDiscardConfirm = false }
        )
        return
    }

    BrifiModalContainer(onDismiss = { attemptClose() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Новая задача",
                    color = BrifiColors.TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(BrifiColors.SegmentDark)
                        .clickable { attemptClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = BrifiColors.TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            BrifiOutlinedField(
                label = "Название задачи",
                value = title,
                onValueChange = { title = it },
                placeholder = "Например: Визитки, 4+4, 1000шт, Дизлайн"
            )

            Spacer(modifier = Modifier.height(14.dp))

            BrifiOutlinedField(
                label = "Номер заказа",
                value = orderNumber,
                onValueChange = { orderNumber = it },
                placeholder = "Например: 26-001548"
            )

            Spacer(modifier = Modifier.height(14.dp))

            BrifiOutlinedField(
                label = "Детали и заметки",
                value = note,
                onValueChange = { note = it },
                placeholder = "Размеры, контакты, и другая информация",
                minHeightDp = 96,
                singleLine = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Дедлайн",
                color = BrifiColors.TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(BrifiColors.SegmentDark)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Сегодня", "Завтра", "Выбрать дату").forEachIndexed { idx, label ->
                    val active = deadlineMode == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (active) BrifiColors.AccentIndigo else Color.Transparent)
                            .clickable { deadlineMode = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = BrifiColors.TextWhite,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }

            if (deadlineMode == 2) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrifiColors.SegmentDark)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "−1 день",
                        color = BrifiColors.AccentIndigoSoft,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            if (customDaysOffset > 0) customDaysOffset--
                        }
                    )
                    Text(
                        text = SheetsParser.formatDate(selectedDate),
                        color = BrifiColors.TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "+1 день",
                        color = BrifiColors.AccentIndigoSoft,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { customDaysOffset++ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            val canSave = title.isNotBlank()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (canSave) BrifiColors.AccentIndigo else Color(0xFF2B2A52))
                    .clickable(enabled = canSave) {
                        onSave(title, orderNumber, note, selectedDate)
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "СОХРАНИТЬ",
                    color = BrifiColors.TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

/**
 * Оверлей подтверждения выхода без сохранения новой задачи (точно по Брифи v2.pdf)
 */
@Composable
fun DiscardDraftModal(
    canSave: Boolean,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    BrifiModalContainer(onDismiss = onCancel) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Точно удалить?",
                color = BrifiColors.TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Не сохранять",
                color = BrifiColors.TextMutedLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onDiscard() }
                    .padding(vertical = 8.dp, horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (canSave) BrifiColors.AccentIndigo else Color(0xFF2B2A52))
                    .clickable { if (canSave) onSave() else onCancel() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "СОХРАНИТЬ",
                    color = BrifiColors.TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * Модалка «Сохранить макет» — выбор заказа и запись ссылки в графу «Ссылка» таблицы
 */
@Composable
fun SaveLayoutLinkModal(
    activeTasks: List<OrderTask>,
    onDismiss: () -> Unit,
    onSaveLink: (OrderTask, String) -> Unit
) {
    var selectedTask by remember(activeTasks) { mutableStateOf(activeTasks.firstOrNull()) }
    var urlText by remember(selectedTask?.id) { mutableStateOf(selectedTask?.link.orEmpty()) }

    BrifiModalContainer(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Сохранить макет",
                    color = BrifiColors.TextWhite,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrifiColors.SegmentDark)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = BrifiColors.TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Выбери заказ:",
                color = Color(0xFF9E9EAC),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (activeTasks.isEmpty()) {
                Text(
                    text = "Нет активных заказов",
                    color = Color(0xFF888894),
                    fontSize = 14.sp
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeTasks.take(15).forEach { task ->
                        val isSelected = selectedTask?.id == task.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) BrifiColors.AccentIndigo else BrifiColors.SegmentDark)
                                .clickable {
                                    selectedTask = task
                                    urlText = task.link
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.title,
                                color = BrifiColors.TextWhite,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = task.orderNumber,
                                color = Color(0xFFD6D6F5),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            BrifiOutlinedField(
                label = "Ссылка на макет (графа «Ссылка»)",
                value = urlText,
                onValueChange = { urlText = it },
                placeholder = "https://disk.yandex.ru/d/...",
                minHeightDp = 56,
                singleLine = false
            )

            Spacer(modifier = Modifier.height(18.dp))

            val canSave = selectedTask != null && urlText.isNotBlank()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (canSave) BrifiColors.AccentIndigo else Color(0xFF2B2A52))
                    .clickable(enabled = canSave) {
                        selectedTask?.let { onSaveLink(it, urlText.trim()) }
                    }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "СОХРАНИТЬ В ТАБЛИЦУ",
                    color = BrifiColors.TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * Модалка «Подтвердить оплату» — выбор способа оплаты и отправка в таблицу
 */
@Composable
fun ConfirmPayoutModal(
    weekLabel: String,
    amountText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    BrifiModalContainer(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Подтвердить оплату",
                    color = BrifiColors.TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrifiColors.SegmentDark)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = BrifiColors.TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Неделя от $weekLabel  •  $amountText\nВыбери способ оплаты — статус сразу отправится в таблицу:",
                color = Color(0xFFA0A0B0),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            val methods = listOf(
                "Выплачено переводом" to "💳 Переводом на карту",
                "Выплачено из кассы" to "💵 Выплачено из кассы"
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                methods.forEach { (sheetValue, displayLabel) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(BrifiColors.AccentIndigo)
                            .clickable { onConfirm(sheetValue) }
                            .padding(vertical = 15.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayLabel,
                            color = BrifiColors.TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskDetailModal(
    task: OrderTask,
    onDismiss: () -> Unit,
    onSaveNote: (String) -> Unit,
    onOpenStatusPicker: () -> Unit,
    onRequestDelete: () -> Unit
) {
    var noteText by remember(task.id, task.note) { mutableStateOf(task.note) }
    val context = LocalContext.current

    BrifiModalContainer(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        color = BrifiColors.TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append(task.orderNumber)
                            if (task.urgency.isNotBlank()) append("  •  ${task.urgency}")
                        },
                        color = BrifiColors.AccentIndigoSoft,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(BrifiColors.SegmentDark)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = BrifiColors.TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BrifiColors.SegmentDark)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Статус", task.status, valueColor = BrifiColors.AccentCyanLight)
                DetailRow("Дедлайн", task.deadlineRaw.ifBlank { SheetsParser.formatDate(task.deadline) })
                DetailRow("Дата заказа", task.createdDateRaw.ifBlank { SheetsParser.formatDate(task.createdDate) })
                DetailRow("Сумма заказа", if (task.amount > 0) SheetsParser.formatMoney(task.amount, true) else "Бесплатно / 0 ₽")
                DetailRow("Оплата", task.paymentStatus)
                DetailRow("Источник", task.source)
                if (task.link.isNotBlank()) {
                    Text(
                        text = "Открыть ссылку на макет ↗",
                        color = BrifiColors.AccentCyanLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(task.link)))
                                }
                            }
                    )
                }
                Text(
                    text = "Открыть Google Таблицу ↗",
                    color = BrifiColors.TextMutedLight,
                    fontSize = 12.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clickable {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BrifiRepository.SHEET_URL)))
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrifiColors.SheetDark)
                    .clickable { onOpenStatusPicker() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Изменить статус заказа",
                    color = BrifiColors.TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BrifiOutlinedField(
                label = "Заметки по заказу",
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = "Размеры, правки, контакты клиента...",
                minHeightDp = 96,
                singleLine = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(BrifiColors.AccentIndigo)
                    .clickable { onSaveNote(noteText) }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "СОХРАНИТЬ ЗАМЕТКУ",
                    color = BrifiColors.TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Удалить задачу",
                color = Color(0xFFFF6B6B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestDelete() }
                    .padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = BrifiColors.TextWhite
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF9E9EAC), fontSize = 13.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun StatusPickerModal(
    currentStatus: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    BrifiModalContainer(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Статус заказа",
                    color = BrifiColors.TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrifiColors.SegmentDark)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = BrifiColors.TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskStatuses.all.forEach { status ->
                    val selected = status.equals(currentStatus, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) BrifiColors.AccentIndigo else BrifiColors.SegmentDark)
                            .clickable { onSelect(status) }
                            .padding(horizontal = 16.dp, vertical = 13.dp)
                    ) {
                        Text(
                            text = status,
                            color = BrifiColors.TextWhite,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmModal(
    taskTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    BrifiModalContainer(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Точно удалить?",
                color = BrifiColors.TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = taskTitle,
                color = Color(0xFFB0B0BE),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(BrifiColors.UrgencyRed)
                    .clickable { onConfirm() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "УДАЛИТЬ",
                    color = BrifiColors.TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Отмена",
                color = BrifiColors.TextMutedLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun SyncSettingsModal(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var url by remember(currentUrl) { mutableStateOf(currentUrl) }

    BrifiModalContainer(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Синхронизация с таблицей",
                    color = BrifiColors.TextWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrifiColors.SegmentDark)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = BrifiColors.TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Вставь URL веб-приложения Google Apps Script из файла google_apps_script.gs, чтобы статусы, ссылки макетов, подтверждение оплаты, новые задачи и заметки записывались прямо в Google Таблицу.",
                color = Color(0xFFB0B0BE),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            BrifiOutlinedField(
                label = "Apps Script Web App URL",
                value = url,
                onValueChange = { url = it },
                placeholder = "https://script.google.com/macros/s/.../exec",
                minHeightDp = 64,
                singleLine = false
            )
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(BrifiColors.AccentIndigo)
                    .clickable { onSave(url.trim()) }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "СОХРАНИТЬ",
                    color = BrifiColors.TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
