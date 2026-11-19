package ru.brifi.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.brifi.app.R

private val PRODUCTION_ITEMS = listOf(
    "Идальго: Визитки, бирки, листовки",
    "АДС: Таблички ПВХ, баннеры, самоклейки",
    "РомаПринто: ДТФ, фотографии, индивидуальные позиции, холст на подрамнике",
    "БигНет (УФ 360): УФ Печать, ПВХ (УФ), Самоклейки (УФ)",
    "Фотопринт: Фотографии"
)

private val CHECKLIST_ITEMS = listOf(
    "Размер макета дважды сверен с заказом.",
    "Текст, телефоны, адреса и имена проверены.",
    "Изображения качественные, QR-код работает.",
    "Есть вылеты, контур реза и кривые — если требуются.",
    "Итоговый файл открыт и проверен после экспорта.",
    "В названии есть номер заказа, изделие и размер.",
    "В папке нет старых версий, дублей и лишних файлов.",
    "Загружены все нужные файлы.",
    "Файл открывается с диска, ссылка правильная."
)

/**
 * Полноэкранный сторис «Справка по производству» — закрывается свайпом вниз или крестиком
 */
@Composable
fun ProductionStoryScreen(
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissOffsetY = remember { Animatable(0f) }
    val thresholdPx = with(density) { 120.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = dismissOffsetY.value.coerceAtLeast(0f)
                alpha = (1f - (dismissOffsetY.value / (thresholdPx * 3f))).coerceIn(0.4f, 1f)
            }
            .background(BrifiColors.BgBlack)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0 || dismissOffsetY.value > 0f) {
                            scope.launch {
                                dismissOffsetY.snapTo((dismissOffsetY.value + dragAmount).coerceAtLeast(0f))
                            }
                        }
                    },
                    onDragEnd = {
                        if (dismissOffsetY.value > thresholdPx) {
                            onClose()
                        } else {
                            scope.launch {
                                dismissOffsetY.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { dismissOffsetY.animateTo(0f) }
                    }
                )
            }
    ) {
        BreathingAuroraCanvas(
            primaryGlow = BrifiColors.AccentIndigo,
            secondaryGlow = BrifiColors.AccentDeepBlue,
            showBottomGlow = false
        )

        // 3D-принтер в правом нижнем углу точно как на макете
        Image(
            painter = painterResource(id = R.drawable.ic_story_printer),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(210.dp)
                .navigationBarsPadding()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            // Индикатор свайпа + крестик закрытия
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x55FFFFFF))
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x44000000))
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = BrifiColors.TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Производство",
                color = BrifiColors.TextWhite,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                PRODUCTION_ITEMS.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = BrifiColors.TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = item,
                            color = BrifiColors.TextWhite,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            BrifiLogoImage(
                height = 20.dp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

/**
 * Полноэкранный интерактивный сторис «Чек-лист перед загрузкой» с чекбоксами, вибрацией и закрытием свайпом/крестиком
 */
@Composable
fun ChecklistStoryScreen(
    checkedIndices: Set<Int>,
    onToggleIndex: (Int) -> Unit,
    onResetAll: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissOffsetY = remember { Animatable(0f) }
    val thresholdPx = with(density) { 120.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = dismissOffsetY.value.coerceAtLeast(0f)
                alpha = (1f - (dismissOffsetY.value / (thresholdPx * 3f))).coerceIn(0.4f, 1f)
            }
            .background(BrifiColors.BgBlack)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0 || dismissOffsetY.value > 0f) {
                            scope.launch {
                                dismissOffsetY.snapTo((dismissOffsetY.value + dragAmount).coerceAtLeast(0f))
                            }
                        }
                    },
                    onDragEnd = {
                        if (dismissOffsetY.value > thresholdPx) {
                            onClose()
                        } else {
                            scope.launch {
                                dismissOffsetY.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { dismissOffsetY.animateTo(0f) }
                    }
                )
            }
    ) {
        BreathingAuroraCanvas(
            primaryGlow = BrifiColors.AccentCyan,
            secondaryGlow = BrifiColors.AccentIndigo,
            showBottomGlow = false
        )

        // Планшет чек-листа в правом нижнем углу точно как на макете
        Image(
            painter = painterResource(id = R.drawable.ic_story_checklist),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(165.dp)
                .navigationBarsPadding()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x55FFFFFF))
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (checkedIndices.isNotEmpty()) {
                        Text(
                            text = "Сбросить",
                            color = BrifiColors.AccentCyanLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable {
                                    triggerBrifiHaptic(context)
                                    onResetAll()
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x44000000))
                            .border(1.dp, Color(0x44FFFFFF), CircleShape)
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("×", color = BrifiColors.TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Чек лист",
                    color = BrifiColors.TextWhite,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${checkedIndices.size} / ${CHECKLIST_ITEMS.size}",
                    color = BrifiColors.AccentCyanLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                CHECKLIST_ITEMS.forEachIndexed { idx, item ->
                    val checked = idx in checkedIndices
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (checked) Color(0x2258B1D8) else Color.Transparent)
                            .clickable {
                                triggerBrifiHaptic(context)
                                onToggleIndex(idx)
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp, end = 12.dp)
                                .size(22.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (checked) BrifiColors.AccentCyan else Color.Transparent)
                                .border(
                                    width = 1.5.dp,
                                    color = if (checked) BrifiColors.AccentCyan else Color(0x88FFFFFF),
                                    shape = RoundedCornerShape(7.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked) {
                                Text(
                                    text = "✓",
                                    color = BrifiColors.BgBlack,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Text(
                            text = item,
                            color = if (checked) Color(0xFF9FB4C2) else BrifiColors.TextWhite,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
                Spacer(modifier = Modifier.height(60.dp))
            }

            BrifiLogoImage(
                height = 20.dp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}
