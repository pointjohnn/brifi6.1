package ru.brifi.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.brifi.app.R
import ru.brifi.app.data.BrifiRepository
import ru.brifi.app.data.SheetsParser
import ru.brifi.app.model.BrifiStory
import ru.brifi.app.model.BrifiTab
import ru.brifi.app.model.FinanceSummary
import ru.brifi.app.model.OrderTask
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

/**
 * Экран входа (Splash) — точно по Брифи v2.pdf
 */
@Composable
fun SplashScreen(lastUpdated: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrifiColors.BgBlack)
    ) {
        BreathingAuroraCanvas(
            primaryGlow = BrifiColors.GlowBlue,
            centerGlow = BrifiColors.AccentIndigo,
            secondaryGlow = BrifiColors.AccentCyan,
            showBottomGlow = true
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Разработано",
                color = Color(0xFFB4B4D0),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Image(
                painter = painterResource(id = R.drawable.point_logo),
                contentDescription = "ПОИНТ.",
                contentScale = ContentScale.FillHeight,
                modifier = Modifier.height(14.dp)
            )
        }

        BrifiLogoImage(
            height = 36.dp,
            modifier = Modifier.align(Alignment.Center)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Дата обновления данных",
                color = Color(0xFFC4C8E0),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = lastUpdated,
                color = BrifiColors.TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Цветная подложка со скруглёнными краями под датой дедлайна (красный / жёлтый / зелёный по срочности)
 */
@Composable
private fun UrgencyDeadlinePill(task: OrderTask) {
    val (bg, fg) = urgencyBadgeColors(task.urgency)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "📅 ${task.deadlineRaw.ifBlank { SheetsParser.formatDate(task.deadline) }}",
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * Плавный капсульный слайдер-кнопка точно по Figma:
 * Полная капсула (RoundedCornerShape 50%), тень под фиолетовой кнопкой,
 * векторные шевроны >>> (#6C61FF) и кликабельный текст справа.
 */
@Composable
private fun BrifiSlideActionBar(
    thumbLabel: String,
    thumbColor: Color,
    thumbBorderColor: Color,
    thumbTextColor: Color,
    rightActionLabel: String,
    rightActionUnderline: Boolean,
    trackBorderColor: Color? = null,
    heightDp: Int = 36,
    enabled: Boolean = true,
    onSlideComplete: () -> Unit,
    onRightActionClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dragX = remember { Animatable(0f) }

    var trackWidthPx by remember { mutableIntStateOf(0) }
    var thumbWidthPx by remember { mutableIntStateOf(0) }
    var ticked35 by remember { mutableStateOf(false) }
    var ticked70 by remember { mutableStateOf(false) }

    val maxDragPx = (trackWidthPx - thumbWidthPx - 12).coerceAtLeast(1).toFloat()
    val progress = (dragX.value / maxDragPx).coerceIn(0f, 1f)
    val pillShape = RoundedCornerShape(50)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .shadow(elevation = 3.dp, shape = pillShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(pillShape)
            .background(BrifiColors.TextWhite)
            .then(
                if (trackBorderColor != null) Modifier.border(1.dp, trackBorderColor, pillShape)
                else Modifier
            )
            .onSizeChanged { trackWidthPx = it.width }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Правый текст / ссылка смены статуса
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.47f)
                .graphicsLayer { alpha = (1f - progress * 1.5f).coerceIn(0f, 1f) }
                .clickable(enabled = enabled && progress < 0.08f) { onRightActionClick() }
                .padding(end = 8.dp, start = 4.dp, top = 2.dp, bottom = 2.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = rightActionLabel,
                color = BrifiColors.TextDarkSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (rightActionUnderline) TextDecoration.Underline else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Фиолетовая кнопка-ползунок с тенью + векторные >>>
        Row(
            modifier = Modifier
                .offset { IntOffset(dragX.value.roundToInt(), 0) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .onSizeChanged { thumbWidthPx = it.width }
                    .shadow(
                        elevation = 5.dp,
                        shape = pillShape,
                        ambientColor = BrifiColors.AccentIndigoStroke,
                        spotColor = Color.Black
                    )
                    .clip(pillShape)
                    .background(thumbColor)
                    .border(1.dp, thumbBorderColor, pillShape)
                    .pointerInput(enabled, maxDragPx) {
                        if (!enabled) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragStart = {
                                ticked35 = false
                                ticked70 = false
                                triggerBrifiHaptic(context)
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val next = (dragX.value + dragAmount).coerceIn(0f, maxDragPx)
                                val p = next / maxDragPx
                                if (p >= 0.35f && !ticked35) {
                                    ticked35 = true
                                    triggerBrifiHaptic(context)
                                }
                                if (p >= 0.70f && !ticked70) {
                                    ticked70 = true
                                    triggerBrifiHaptic(context)
                                }
                                scope.launch { dragX.snapTo(next) }
                            },
                            onDragEnd = {
                                if (dragX.value >= maxDragPx * 0.68f) {
                                    scope.launch {
                                        dragX.animateTo(
                                            maxDragPx,
                                            animationSpec = tween(150, easing = FastOutSlowInEasing)
                                        )
                                        triggerBrifiConfirmHaptic(context)
                                        onSlideComplete()
                                        dragX.snapTo(0f)
                                    }
                                } else {
                                    scope.launch {
                                        dragX.animateTo(
                                            0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.85f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    dragX.animateTo(
                                        0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.85f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        )
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = thumbLabel,
                    color = thumbTextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            TripleChevronsIcon(
                color = thumbColor.copy(alpha = (0.95f - progress * 0.95f).coerceIn(0f, 0.95f)),
                modifier = Modifier.size(width = 24.dp, height = 12.dp)
            )
        }
    }
}

/**
 * Функциональная боковая полоса справа от «Другие задачи»
 */
@Composable
private fun InteractiveSideRail(
    totalCount: Int,
    currentIndex: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onJumpTo: (Int) -> Unit
) {
    val dotSlots = minOf(totalCount, 4)
    val activeSlot = if (totalCount <= 1) 0 else ((currentIndex.toFloat() / (totalCount - 1)) * (dotSlots - 1)).roundToInt()

    Column(
        modifier = Modifier
            .width(22.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BrifiColors.ScrollRail)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = "▲",
            color = BrifiColors.ScrollRailIcon,
            fontSize = 9.sp,
            modifier = Modifier
                .clickable { onPrev() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
        for (slot in 0 until dotSlots) {
            val isActive = slot == activeSlot
            val targetIdx = if (dotSlots <= 1) 0 else ((slot.toFloat() / (dotSlots - 1)) * (totalCount - 1)).roundToInt()
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(if (isActive) 22.dp else 5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isActive) BrifiColors.AccentIndigo else BrifiColors.ScrollRailIcon.copy(alpha = 0.6f))
                    .clickable { onJumpTo(targetIdx) }
            )
        }
        Text(
            text = "▼",
            color = BrifiColors.ScrollRailIcon,
            fontSize = 9.sp,
            modifier = Modifier
                .clickable { onNext() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * По-настоящему плавная 3-слойная стопка «Другие задачи» точно как на «Главный экран.jpg»:
 * - Предыдущая карточка выглядывает сверху (scale 0.895, alpha 0.48)
 * - Текущая карточка по центру с мягкой объёмной тенью
 * - Следующая карточка выглядывает снизу (scale 0.895, alpha 0.48)
 * - Переключение свайпом влево/вправо, тапом по верхней/нижней карточке или боковой полосой
 * - НИКАКОГО конфликта с вертикальным скроллом экрана, 100% GPU graphicsLayer без рывков!
 */
@Composable
private fun SmoothCardStackDeck(
    items: List<OrderTask>,
    autoAdvanceDelayMs: Long = 10_000L,
    onOpenDetails: (OrderTask) -> Unit,
    onTakeIntoWork: (OrderTask) -> Unit,
    onChangeStatus: (OrderTask) -> Unit
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var currentIndex by remember(items.size) { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, items.lastIndex)
    val prevIndex = if (items.size > 1) (safeIndex - 1 + items.size) % items.size else safeIndex
    val nextIndex = if (items.size > 1) (safeIndex + 1) % items.size else safeIndex

    // Прогресс перехода: 0f = покой, -1f = переход к следующей (карточка уходит вверх), +1f = к предыдущей (вниз)
    val transition = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }

    fun animateStep(direction: Int) {
        if (items.size <= 1 || isAnimating) return
        scope.launch {
            isAnimating = true
            val target = if (direction > 0) -1f else 1f
            transition.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
            currentIndex = (safeIndex + direction + items.size) % items.size
            transition.snapTo(0f)
            triggerBrifiHaptic(context)
            isAnimating = false
        }
    }

    // Плавное автопереключение каждые 10 секунд
    LaunchedEffect(items.size, safeIndex, isDragging, isAnimating) {
        if (items.size > 1 && !isDragging && !isAnimating) {
            delay(autoAdvanceDelayMs)
            animateStep(1)
        }
    }

    val peekOffsetPx = with(density) { 26.dp.toPx() }
    val swipeThresholdPx = with(density) { 72.dp.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(292.dp),
            contentAlignment = Alignment.Center
        ) {
            val t = transition.value // [-1f..+1f]

            // 1. Верхняя выглядывающая карточка (предыдущая)
            if (items.size > 1) {
                val topProgress = t.coerceAtLeast(0f) // 0 -> 1 когда тянем к предыдущей
                val topScale = 0.895f + topProgress * 0.105f
                val topAlpha = (0.48f + topProgress * 0.52f).coerceIn(0f, 1f)
                val topTranslationY = -peekOffsetPx * (1f - topProgress)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (topProgress > 0.5f) 4f else 1f)
                        .graphicsLayer {
                            translationY = topTranslationY
                            scaleX = topScale
                            scaleY = topScale
                            alpha = topAlpha
                        }
                        .clickable { animateStep(-1) }
                ) {
                    OtherTaskCardBody(
                        task = items[prevIndex],
                        isInteractive = false,
                        onOpenDetails = {},
                        onTakeIntoWork = {},
                        onChangeStatus = {}
                    )
                }
            }

            // 2. Нижняя выглядывающая карточка (следующая)
            if (items.size > 1) {
                val bottomProgress = (-t).coerceAtLeast(0f) // 0 -> 1 когда тянем к следующей
                val bottomScale = 0.895f + bottomProgress * 0.105f
                val bottomAlpha = (0.48f + bottomProgress * 0.52f).coerceIn(0f, 1f)
                val bottomTranslationY = peekOffsetPx * (1f - bottomProgress)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (bottomProgress > 0.5f) 4f else 2f)
                        .graphicsLayer {
                            translationY = bottomTranslationY
                            scaleX = bottomScale
                            scaleY = bottomScale
                            alpha = bottomAlpha
                        }
                        .clickable { animateStep(1) }
                ) {
                    OtherTaskCardBody(
                        task = items[nextIndex],
                        isInteractive = false,
                        onOpenDetails = {},
                        onTakeIntoWork = {},
                        onChangeStatus = {}
                    )
                }
            }

            // 3. Передняя активная карточка (свайп влево/вправо для переключения стопки)
            val absT = abs(t).coerceIn(0f, 1f)
            val frontScale = 1f - absT * 0.105f
            val frontAlpha = (1f - absT * 0.52f).coerceIn(0.48f, 1f)
            val frontTranslationY = t * peekOffsetPx
            val frontTranslationX = t * with(density) { 18.dp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (absT > 0.5f) 1.5f else 3f)
                    .graphicsLayer {
                        translationY = frontTranslationY
                        translationX = frontTranslationX
                        scaleX = frontScale
                        scaleY = frontScale
                        alpha = frontAlpha
                    }
                    .pointerInput(items.size, safeIndex) {
                        if (items.size <= 1) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragStart = { isDragging = true },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val delta = dragAmount / (swipeThresholdPx * 2.4f)
                                val nextVal = (transition.value + delta).coerceIn(-1f, 1f)
                                scope.launch { transition.snapTo(nextVal) }
                            },
                            onDragEnd = {
                                isDragging = false
                                val v = transition.value
                                when {
                                    v <= -0.26f -> animateStep(1)
                                    v >= 0.26f -> animateStep(-1)
                                    else -> scope.launch {
                                        transition.animateTo(
                                            0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.85f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                scope.launch {
                                    transition.animateTo(
                                        0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.85f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        )
                    }
            ) {
                OtherTaskCardBody(
                    task = items[safeIndex],
                    isInteractive = true,
                    onOpenDetails = { onOpenDetails(items[safeIndex]) },
                    onTakeIntoWork = { onTakeIntoWork(items[safeIndex]) },
                    onChangeStatus = { onChangeStatus(items[safeIndex]) }
                )
            }
        }

        if (items.size > 1) {
            Spacer(modifier = Modifier.width(8.dp))
            InteractiveSideRail(
                totalCount = items.size,
                currentIndex = safeIndex,
                onPrev = { animateStep(-1) },
                onNext = { animateStep(1) },
                onJumpTo = { idx ->
                    if (!isAnimating && idx != safeIndex) {
                        currentIndex = idx.coerceIn(0, items.lastIndex)
                        triggerBrifiHaptic(context)
                    }
                }
            )
        }
    }
}

/**
 * Вкладка 1 — Главный экран (1-в-1 по Figma «Главный экран.jpg» и SVG-спецификации)
 */
@Composable
fun HomeScreen(
    allTasks: List<OrderTask>,
    inProgressTasks: List<OrderTask>,
    otherTasks: List<OrderTask>,
    lastUpdatedText: String,
    isRefreshing: Boolean,
    statusBanner: String?,
    onOpenStory: (BrifiStory) -> Unit,
    onCreateTaskClick: () -> Unit,
    onSaveLayoutClick: () -> Unit,
    onOpenDetails: (OrderTask) -> Unit,
    onChangeStatus: (OrderTask) -> Unit,
    onMarkDone: (OrderTask) -> Unit,
    onTakeIntoWork: (OrderTask) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val activeCount = remember(allTasks) { allTasks.count { it.isActive } }
    val deadlineTodayCount = remember(allTasks, today) { allTasks.count { it.isActive && it.deadline == today } }

    // Точные скругления из Figma: 95px / 2.62 ≈ 36.dp
    val sectionShape = RoundedCornerShape(36.dp)
    val pillButtonShape = RoundedCornerShape(46.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 148.dp)
    ) {
        // Верхняя зона с 3-сферным дышащим градиентом (#4638FF / #6C61FF / #61CDFF)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrifiColors.BgBlack)
        ) {
            BreathingAuroraCanvas(
                primaryGlow = BrifiColors.GlowBlue,
                centerGlow = BrifiColors.AccentIndigo,
                secondaryGlow = BrifiColors.AccentCyan,
                showBottomGlow = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 20.dp, bottom = 14.dp)
            ) {
                // Логотип БРИФИ слева
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BrifiLogoImage(height = 24.dp)
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 4 квадратные плитки #0E0E0E со скруглением 20.dp и тенью (как в Figma)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TopInfoTile(
                        modifier = Modifier.weight(1f),
                        label = "Активных\nзадач",
                        onClick = null
                    ) {
                        Text(
                            text = activeCount.toString(),
                            color = BrifiColors.AccentIndigo,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TopInfoTile(
                        modifier = Modifier.weight(1f),
                        label = "Дедлайн\nсегодня!",
                        onClick = null
                    ) {
                        Text(
                            text = deadlineTodayCount.toString(),
                            color = BrifiColors.AccentCyanTile,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TopInfoTile(
                        modifier = Modifier.weight(1f),
                        label = "Справка по\nпроизводству",
                        onClick = {
                            triggerBrifiHaptic(context)
                            onOpenStory(BrifiStory.PRODUCTION)
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_story_printer),
                            contentDescription = "Справка по производству",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    TopInfoTile(
                        modifier = Modifier.weight(1f),
                        label = "Чек-лист\nперед загрузкой",
                        onClick = {
                            triggerBrifiHaptic(context)
                            onOpenStory(BrifiStory.CHECKLIST)
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_story_checklist),
                            contentDescription = "Чек-лист",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2 большие кнопки-капсулы (#0E0E0E, обводка 1.dp #414141, полное скругление pill)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                            .shadow(10.dp, pillButtonShape, ambientColor = Color.Black, spotColor = Color.Black)
                            .clip(pillButtonShape)
                            .background(BrifiColors.TileDark)
                            .border(1.dp, BrifiColors.TileBorder, pillButtonShape)
                            .clickable {
                                triggerBrifiHaptic(context)
                                onCreateTaskClick()
                            }
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CreateTaskActionIcon()
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Создать задачу",
                            color = BrifiColors.TextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                            .shadow(10.dp, pillButtonShape, ambientColor = Color.Black, spotColor = Color.Black)
                            .clip(pillButtonShape)
                            .background(BrifiColors.TileDark)
                            .border(1.dp, BrifiColors.TileBorder, pillButtonShape)
                            .clickable {
                                triggerBrifiHaptic(context)
                                onSaveLayoutClick()
                            }
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        SaveUrlActionIcon()
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Сохранить макет",
                            color = BrifiColors.TextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (!statusBanner.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrifiColors.SheetHome)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = statusBanner,
                    color = BrifiColors.AccentCyanLight,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Секция 1 — «В работе:» (полная ширина, #1A1A1A, скругление 36.dp как на Главный экран.jpg)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(sectionShape)
                .background(BrifiColors.SheetHome)
                .padding(top = 20.dp, bottom = 14.dp)
        ) {
            Text(
                text = "В работе:",
                color = BrifiColors.TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (inProgressTasks.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { inProgressTasks.size })

                LaunchedEffect(inProgressTasks.size, pagerState.currentPage, pagerState.isScrollInProgress) {
                    if (inProgressTasks.size > 1 && !pagerState.isScrollInProgress) {
                        delay(10_000L)
                        val nextPage = (pagerState.currentPage + 1) % inProgressTasks.size
                        pagerState.animateScrollToPage(
                            page = nextPage,
                            animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
                        )
                        triggerBrifiHaptic(context)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 2,
                    contentPadding = PaddingValues(
                        start = 22.dp,
                        end = if (inProgressTasks.size > 1) 104.dp else 22.dp
                    ),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val task = inProgressTasks[page]
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    val absOff = abs(pageOffset).coerceIn(0f, 1f)
                    val scale = 1f - absOff * 0.03f
                    val alpha = 1f - absOff * 0.16f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                    ) {
                        ActiveWorkDarkCard(
                            task = task,
                            onOpenDetails = { onOpenDetails(task) },
                            onChangeStatus = { onChangeStatus(task) },
                            onMarkDone = { onMarkDone(task) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(BrifiColors.WorkCardDark)
                        .border(1.dp, BrifiColors.WorkCardBorder, RoundedCornerShape(24.dp))
                        .padding(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет активных задач — всё выполнено!",
                        color = BrifiColors.TextMutedLight,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Дата обновления данных: $lastUpdatedText",
                color = BrifiColors.TextFooterMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Секция 2 — «Другие задачи:» (полная ширина, #1A1A1A, скругление 36.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(sectionShape)
                .background(BrifiColors.SheetHome)
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Text(
                text = "Другие задачи:",
                color = BrifiColors.TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (otherTasks.isNotEmpty()) {
                SmoothCardStackDeck(
                    items = otherTasks,
                    autoAdvanceDelayMs = 10_000L,
                    onOpenDetails = onOpenDetails,
                    onTakeIntoWork = onTakeIntoWork,
                    onChangeStatus = onChangeStatus
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(BrifiColors.CardDark)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "В очереди других задач нет",
                        color = Color(0xFF90909E),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Секция 3 — «Last but not least» (6 компактных тёмных плиток #1A1A1A, rx = 10.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Last but not least",
                color = BrifiColors.TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickLinkItem(
                    iconRes = R.drawable.ic_quick_tg,
                    label = "Чат тг",
                    modifier = Modifier.weight(1f),
                    onClick = { openUrl(context, BrifiRepository.URL_TG_CHAT) }
                )
                QuickLinkItem(
                    iconRes = R.drawable.ic_quick_sheets,
                    label = "Таблица",
                    modifier = Modifier.weight(1f),
                    onClick = { openUrl(context, BrifiRepository.SHEET_URL) }
                )
                QuickLinkItem(
                    iconRes = R.drawable.ic_quick_print,
                    label = "Принт",
                    modifier = Modifier.weight(1f),
                    onClick = { openUrl(context, BrifiRepository.URL_PRINTOFFICE) }
                )
                QuickLinkItem(
                    iconRes = R.drawable.ic_quick_mail,
                    label = "Mail",
                    modifier = Modifier.weight(1f),
                    onClick = { openUrl(context, BrifiRepository.URL_MAIL_RU) }
                )
                QuickLinkItem(
                    iconRes = R.drawable.ic_quick_disk,
                    label = "Диск",
                    modifier = Modifier.weight(1f),
                    onClick = { openUrl(context, BrifiRepository.URL_YANDEX_DISK) }
                )
                QuickLinkItem(
                    iconRes = R.drawable.ic_quick_max,
                    label = "Макс",
                    modifier = Modifier.weight(1f),
                    onClick = { openUrl(context, BrifiRepository.URL_MAX) }
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))
        RefreshFooter(isRefreshing = isRefreshing, onRefresh = onRefresh)
    }
}

@Composable
private fun TopInfoTile(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tileShape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(10.dp, tileShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(tileShape)
                .background(BrifiColors.TileDark),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = BrifiColors.TextWhite,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun QuickLinkItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(cardShape)
            .background(BrifiColors.SheetHome)
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = BrifiColors.TextWhite,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 6.dp, top = 4.dp)
        )
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(34.dp)
                .offset(x = 4.dp, y = 4.dp)
        )
    }
}

@Composable
private fun ActiveWorkDarkCard(
    task: OrderTask,
    onOpenDetails: () -> Unit,
    onChangeStatus: () -> Unit,
    onMarkDone: () -> Unit
) {
    // Точные параметры Figma: #2C2C2C, обводка #606060, скругление 24.dp, тень
    val cardShape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, cardShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(cardShape)
            .background(BrifiColors.WorkCardDark)
            .border(1.dp, BrifiColors.WorkCardBorder, cardShape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(task.orderNumber)
                        if (task.urgency.isNotBlank()) append("   ${task.urgency}")
                    },
                    color = BrifiColors.TextMutedLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onOpenDetails() },
                contentAlignment = Alignment.Center
            ) {
                PlusIcon(color = BrifiColors.TextWhite, strokeFraction = 0.10f, modifier = Modifier.size(18.dp))
            }
        }

        if (task.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = task.note,
                color = BrifiColors.TextMutedLight,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF202024))
                    .clickable { onOpenDetails() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UrgencyDeadlinePill(task = task)
            Text(
                text = "🖥️ ${task.source}",
                color = BrifiColors.TextMutedLight,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        BrifiSlideActionBar(
            thumbLabel = "Готово",
            thumbColor = BrifiColors.AccentIndigo,
            thumbBorderColor = BrifiColors.AccentIndigoStroke,
            thumbTextColor = BrifiColors.TextWhite,
            rightActionLabel = "Изменить статус заказа",
            rightActionUnderline = true,
            trackBorderColor = BrifiColors.WorkCardBorder,
            heightDp = 34,
            enabled = true,
            onSlideComplete = onMarkDone,
            onRightActionClick = onChangeStatus
        )
    }
}

@Composable
private fun OtherTaskCardBody(
    task: OrderTask,
    isInteractive: Boolean = true,
    onOpenDetails: () -> Unit,
    onTakeIntoWork: () -> Unit,
    onChangeStatus: () -> Unit
) {
    // Точные параметры Figma: #E7E7E7, скругление 32.dp (90px/2.62), выраженная тень 16.dp
    val cardShape = RoundedCornerShape(32.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, cardShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(cardShape)
            .background(BrifiColors.DeckFrontLight)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = BrifiColors.TextBlack,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(task.orderNumber)
                        if (task.urgency.isNotBlank()) append("   ${task.urgency}")
                    },
                    color = BrifiColors.TextMutedDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(BrifiColors.WorkCardDark)
                    .then(if (isInteractive) Modifier.clickable { onOpenDetails() } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                PlusIcon(color = BrifiColors.TextWhite, strokeFraction = 0.13f, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Белое поле заметок (Figma rx ≈ 10.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrifiColors.TextWhite)
                .then(if (isInteractive) Modifier.clickable { onOpenDetails() } else Modifier)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = task.note.ifBlank { "Поле для заметок" },
                color = if (task.note.isNotBlank()) BrifiColors.TextBlack else BrifiColors.TextMutedDark,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UrgencyDeadlinePill(task = task)
            Text(
                text = "🖥️ ${task.source}",
                color = BrifiColors.TextMetaDark,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        BrifiSlideActionBar(
            thumbLabel = "Взять в работу",
            thumbColor = BrifiColors.AccentIndigo,
            thumbBorderColor = BrifiColors.AccentIndigo,
            thumbTextColor = BrifiColors.TextWhite,
            rightActionLabel = task.status,
            rightActionUnderline = false,
            trackBorderColor = null,
            heightDp = 38,
            enabled = isInteractive,
            onSlideComplete = onTakeIntoWork,
            onRightActionClick = onChangeStatus
        )
    }
}

/**
 * Вкладка 2 — Статистика (точно по Figma clip3_0_1 и Брифи v2.pdf)
 */
@Composable
fun StatsScreen(
    allTasks: List<OrderTask>,
    totalOrdersCount: Int,
    weeksAgo: Int,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onOpenDetails: (OrderTask) -> Unit,
    onChangeStatus: (OrderTask) -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val tomorrow = remember(today) { today.plusDays(1) }

    val tasksForToday = remember(allTasks, today) {
        allTasks.count { it.isActive && it.deadline == today }
    }
    val doneToday = remember(allTasks, today) {
        allTasks.count { it.isCompleted && (it.deadline == today || it.createdDate == today) }
    }
    val tasksForTomorrow = remember(allTasks, tomorrow) {
        allTasks.count { it.isActive && it.deadline == tomorrow }
    }

    val (weekStart, weekEnd) = remember(today, weeksAgo) {
        SheetsParser.weekBounds(today, weeksAgo)
    }
    val weekTasks = remember(allTasks, weekStart, weekEnd) {
        SheetsParser.filterTasksForWeek(allTasks, weekStart, weekEnd)
    }

    val sheetShape = RoundedCornerShape(36.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 148.dp)
    ) {
        BrifiBreathingHeader(
            centered = true,
            primaryGlow = BrifiColors.AccentCyan,
            centerGlow = BrifiColors.AccentCyanTile,
            secondaryGlow = BrifiColors.GlowBlue
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(BrifiColors.SheetDark)
                .border(1.dp, BrifiColors.SheetDarkBorder, sheetShape)
                .padding(horizontal = 18.dp, vertical = 22.dp)
        ) {
            Text(
                text = "Статистика",
                color = BrifiColors.TextWhite,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiTile(
                    value = totalOrdersCount.toString(),
                    title = "Задач всего",
                    bgColor = BrifiColors.KpiTile1,
                    textColor = BrifiColors.TextWhite,
                    modifier = Modifier.weight(1f)
                )
                KpiTile(
                    value = tasksForToday.toString(),
                    title = "Задачи на сегодня",
                    bgColor = BrifiColors.KpiTile2,
                    textColor = BrifiColors.TextWhite,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiTile(
                    value = doneToday.toString(),
                    title = "Задача выполнена\nсегодня",
                    bgColor = BrifiColors.KpiTile3,
                    textColor = BrifiColors.TextWhite,
                    modifier = Modifier.weight(1f)
                )
                KpiTile(
                    value = tasksForTomorrow.toString(),
                    title = "Задач на завтра",
                    bgColor = BrifiColors.KpiTile4,
                    textColor = BrifiColors.AccentDeepBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Заказы за неделю",
                color = BrifiColors.TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            if (weekTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(BrifiColors.TileDark)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "За эту неделю заказов не найдено",
                        color = Color(0xFF90909E),
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    weekTasks.forEach { task ->
                        StatsWeekTaskCard(
                            task = task,
                            onOpenDetails = { onOpenDetails(task) },
                            onChangeStatus = { onChangeStatus(task) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹",
                    color = BrifiColors.AccentIndigo,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            triggerBrifiHaptic(context)
                            onPrevWeek()
                        }
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
                Text(
                    text = SheetsParser.formatWeekStart(weekStart),
                    color = Color(0xFFB8B8C8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline
                )
                Text(
                    text = "›",
                    color = if (weeksAgo > 0) BrifiColors.AccentIndigo else Color(0xFF555562),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = weeksAgo > 0) {
                            triggerBrifiHaptic(context)
                            onNextWeek()
                        }
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun KpiTile(
    value: String,
    title: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val tileShape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .height(116.dp)
            .shadow(8.dp, tileShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(tileShape)
            .background(bgColor)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = value,
            color = textColor,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun StatsWeekTaskCard(
    task: OrderTask,
    onOpenDetails: () -> Unit,
    onChangeStatus: () -> Unit
) {
    val cardShape = RoundedCornerShape(26.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, cardShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(cardShape)
            .background(BrifiColors.StatsCardLight)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = BrifiColors.TextBlack,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append(task.orderNumber)
                        if (task.urgency.isNotBlank()) append("   ${task.urgency}")
                    },
                    color = BrifiColors.TextMutedDark,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { onOpenDetails() },
                contentAlignment = Alignment.Center
            ) {
                PlusIcon(color = BrifiColors.TextBlack, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UrgencyDeadlinePill(task = task)
            Text(
                text = "🖥️ ${task.source}",
                color = BrifiColors.TextMetaDark,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(BrifiColors.BgBlack)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(BrifiColors.TextWhite)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = task.status,
                    color = BrifiColors.TextBlack,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Text(
                text = "Изменить статус заказа",
                color = BrifiColors.TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { onChangeStatus() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Вкладка 3 — Профиль / Финансовая аналитика (1-в-1 по Figma clip2_0_1 и Брифи v2.pdf!)
 */
@Composable
fun ProfileScreen(
    finance: FinanceSummary,
    isRefreshing: Boolean,
    hasWebhookConfigured: Boolean,
    onConfirmPayoutClick: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val sheetShape = RoundedCornerShape(36.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 152.dp)
    ) {
        BrifiBreathingHeader(
            centered = true,
            primaryGlow = BrifiColors.GlowBlue,
            centerGlow = BrifiColors.AccentIndigo,
            secondaryGlow = BrifiColors.AccentIndigoActive
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(BrifiColors.SheetDark)
                .border(1.dp, BrifiColors.SheetDarkBorder, sheetShape)
                .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Привет,",
                color = BrifiColors.TextWhite,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Поинт Джон",
                color = BrifiColors.AccentIndigo,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "За неделю от ${finance.latestWeek.startDateLabel}",
                color = Color(0xFF9E9EAC),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Ты нахастлил",
                color = BrifiColors.TextWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Главная чёрная карточка суммы с пунктирной рамкой как в Figma
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(BrifiColors.BgBlack)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val strokePx = 1.5.dp.toPx()
                    val r = 26.dp.toPx()
                    drawRoundRect(
                        color = BrifiColors.ProfileDashedBorder,
                        topLeft = Offset(strokePx / 2f, strokePx / 2f),
                        size = Size(size.width - strokePx, size.height - strokePx),
                        cornerRadius = CornerRadius(r, r),
                        style = Stroke(
                            width = strokePx,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 14.dp, start = 14.dp, end = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = SheetsParser.formatMoney(finance.latestWeek.amount),
                        color = BrifiColors.TextWhite,
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141418))
                            .border(1.dp, Color(0xFF24242C), RoundedCornerShape(16.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Переводом на карту + Подтвердить оплату (#6157E5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(BrifiColors.BgBlack)
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = finance.latestWeek.payoutStatus,
                        color = Color(0xFFC0C0CE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(6.dp, RoundedCornerShape(50), ambientColor = BrifiColors.AccentIndigoActive, spotColor = Color.Black)
                        .clip(RoundedCornerShape(50))
                        .background(BrifiColors.AccentIndigoActive)
                        .clickable {
                            triggerBrifiHaptic(context)
                            onConfirmPayoutClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Подтвердить оплату",
                        color = BrifiColors.TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5-плиточная сетка аналитики (#0E0E0E, rx = 24.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileIncomeTile(
                    amount = SheetsParser.formatMoney(finance.allTimeTotalIncome),
                    label = "Заработал за все время",
                    modifier = Modifier.weight(1f)
                )
                ProfileIncomeTile(
                    amount = SheetsParser.formatMoney(finance.currentMonthTotalIncome),
                    label = "Заработал за этот месяц",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileIncomeTile(
                        amount = SheetsParser.formatMoney(finance.avgMonthlyCommission50),
                        label = "Средний процент в месяц",
                        heightDp = 84
                    )
                    ProfileIncomeTile(
                        amount = SheetsParser.formatMoney(finance.avgMonthlyTotalIncome),
                        label = "Средний доход в месяц",
                        heightDp = 84
                    )
                }

                PaidFreePieTile(
                    paid = finance.paidOrdersCount,
                    free = finance.freeOrdersCount,
                    modifier = Modifier
                        .weight(1f)
                        .height(178.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            RefreshFooter(isRefreshing = isRefreshing, onRefresh = onRefresh)

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (hasWebhookConfigured) "Синхронизация с таблицей: активна ✓" else "Настроить синхронизацию с таблицей",
                color = if (hasWebhookConfigured) Color(0xFF66D17A) else Color(0xFF8C8C9A),
                fontSize = 11.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { onOpenSyncSettings() }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ProfileIncomeTile(
    amount: String,
    label: String,
    heightDp: Int = 90,
    modifier: Modifier = Modifier
) {
    val tileShape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .shadow(8.dp, tileShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(tileShape)
            .background(BrifiColors.ProfileTileDark)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = amount,
            color = BrifiColors.TextWhite,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color(0xFF9898A6),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PaidFreePieTile(
    paid: Int,
    free: Int,
    modifier: Modifier = Modifier
) {
    val total = (paid + free).coerceAtLeast(1)
    val paidRatio = if (paid + free == 0) 0.71f else (paid.toFloat() / total.toFloat())
    val paidSweep = 360f * paidRatio
    val freeSweep = 360f - paidSweep
    val tileShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .shadow(8.dp, tileShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(tileShape)
            .background(BrifiColors.ProfileTileDark)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(2.dp))
        Canvas(modifier = Modifier.size(104.dp)) {
            val d = size.minDimension
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            val arcSize = Size(d, d)
            drawArc(
                color = BrifiColors.AccentIndigoActive,
                startAngle = -90f,
                sweepAngle = paidSweep,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
            drawArc(
                color = BrifiColors.AccentSoftBlue,
                startAngle = -90f + paidSweep,
                sweepAngle = freeSweep,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
        }
        Text(
            text = "Платные/бесплатные",
            color = Color(0xFF9898A6),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun RefreshFooter(
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = !isRefreshing) { onRefresh() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    color = BrifiColors.AccentIndigo,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = if (isRefreshing) "Обновление..." else "Обновить данные",
                color = Color(0xFFC8C8D6),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        BrifiLogoImage(height = 20.dp)
    }
}

/**
 * Нижняя плавающая стеклянная панель навигации 1-в-1 по «Главный экран.jpg»:
 * - Полупрозрачная капсула с тонкой обводкой и глубокой тенью
 * - Внутренняя подсвеченная капсула вокруг активной вкладки
 * - Иконка дома со стилизованной «Б», 3 столбца 53/80/40, профиль
 * - Отдельная круглая кнопка «+» справа
 */
@Composable
fun FloatingBottomBar(
    currentTab: BrifiTab,
    onSelectTab: (BrifiTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 42.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .shadow(18.dp, pillShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(pillShape)
                .background(BrifiColors.NavPillBg)
                .border(1.dp, BrifiColors.NavPillBorder, pillShape)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabButton(
                isActive = currentTab == BrifiTab.HOME,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(BrifiTab.HOME) }
            ) {
                NavHomeIcon(
                    color = if (currentTab == BrifiTab.HOME) BrifiColors.NavActive else BrifiColors.NavInactive,
                    cutoutColor = if (currentTab == BrifiTab.HOME) Color(0xFF1D1D22) else BrifiColors.BgBlack
                )
            }
            NavTabButton(
                isActive = currentTab == BrifiTab.STATS,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(BrifiTab.STATS) }
            ) {
                NavStatsIcon(
                    color = if (currentTab == BrifiTab.STATS) BrifiColors.NavActive else BrifiColors.NavInactive
                )
            }
            NavTabButton(
                isActive = currentTab == BrifiTab.PROFILE,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(BrifiTab.PROFILE) }
            ) {
                NavProfileIcon(
                    color = if (currentTab == BrifiTab.PROFILE) BrifiColors.NavActive else BrifiColors.NavInactive
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Box(
            modifier = Modifier
                .size(58.dp)
                .shadow(18.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(CircleShape)
                .background(BrifiColors.NavPillBg)
                .border(1.dp, BrifiColors.NavPillBorder, CircleShape)
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            PlusIcon(color = BrifiColors.NavInactive, strokeFraction = 0.11f, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun NavTabButton(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val activePill = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(activePill)
            .then(
                if (isActive) {
                    Modifier
                        .background(BrifiColors.NavActivePillBg)
                        .border(1.dp, BrifiColors.NavActivePillBorder, activePill)
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
