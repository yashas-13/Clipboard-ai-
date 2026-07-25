package com.example.presentation.dashboard

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ClipboardItemEntity
import com.example.presentation.clipboard_list.ClipboardViewModel
import com.example.presentation.clipboard_list.DashboardStats
import com.example.presentation.clipboard_list.CategoryCount
import com.example.presentation.clipboard_list.DailyCount
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleContainer
import com.example.ui.theme.Slate400

fun getCategoryColor(category: String, isDark: Boolean): Color {
    return when (category) {
        "Code" -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
        "Sensitive" -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
        "URL" -> if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
        "Email" -> if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488)
        "Phone" -> if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
        "OTP" -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
        "JSON" -> if (isDark) Color(0xFFE879F9) else Color(0xFFC026D3)
        "Markdown" -> if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
        "Rich Text" -> if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)
        "Image" -> if (isDark) Color(0xFFC084FC) else Color(0xFF9333EA)
        else -> if (isDark) Color(0xFFFACC15) else Color(0xFFEAB308)
    }
}

@Composable
fun UsageDashboardScreen(
    viewModel: ClipboardViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()

    // Cork / Craft Paper Background Box
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF14131A) else Color(0xFFFAF6EE))
            .drawBehind {
                // Draw craft paper grid / grain overlay
                val gridStep = 40.dp.toPx()
                val lineColor = if (isDark) Color(0xFF262430) else Color(0xFFEFE8DA)
                var x = 0f
                while (x < size.width) {
                    drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += gridStep
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += gridStep
                }
            }
    ) {
        if (stats.totalItems == 0) {
            PaperEmptyBoard()
        } else {
            PaperBoardDashboardContent(stats = stats, viewModel = viewModel)
        }
    }
}

@Composable
fun PaperEmptyBoard() {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Pinned Empty Memo Paper
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .rotate(-1.5f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E1C26) else Color(0xFFFFFDF9)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF333042) else Color(0xFFE8DFC8))
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Tape Strip
                PaperTapeStrip(modifier = Modifier.align(Alignment.CenterHorizontally))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurpleContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Empty Visual Summary Board",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Copy text, links, or code to automatically populate this paper board with frequency charts, category insights, and copy velocity trends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                PaperStampBadge(
                    text = "AWAITING CLIPBOARD DATA",
                    color = Color(0xFFD97706)
                )
            }
        }
    }
}

@Composable
fun PaperBoardDashboardContent(
    stats: DashboardStats,
    viewModel: ClipboardViewModel
) {
    var selectedFilterIndex by remember { mutableStateOf(0) }
    val filterTabs = listOf("Board Overview", "Frequency Trends", "Categories")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp, top = 12.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Board Paper Header & Index Tabs
        item {
            PaperBoardHeader(
                selectedTab = selectedFilterIndex,
                onTabSelect = { selectedFilterIndex = it },
                tabs = filterTabs
            )
        }

        when (selectedFilterIndex) {
            0 -> {
                // Board Overview: Sticky Notes + Daily Bar + Category Donut + Top Copied
                item {
                    PaperStickyNotesGrid(stats = stats)
                }

                item {
                    PaperDailyActivityCard(stats = stats)
                }

                item {
                    PaperCategoryBreakdownCard(stats = stats)
                }

                if (stats.mostCopiedSnippet != null) {
                    item {
                        PaperTopCopiedCard(
                            item = stats.mostCopiedSnippet,
                            viewModel = viewModel
                        )
                    }
                }

                item {
                    PaperSourceAppsAndTagsCard(stats = stats)
                }

                item {
                    PaperAiSummaryNote(stats = stats)
                }
            }
            1 -> {
                // Frequency Trends Detail View
                item {
                    PaperDailyActivityCard(stats = stats)
                }

                item {
                    PaperHourlyFrequencyHeatmapCard(stats = stats)
                }

                if (stats.mostCopiedSnippet != null) {
                    item {
                        PaperTopCopiedCard(
                            item = stats.mostCopiedSnippet,
                            viewModel = viewModel
                        )
                    }
                }
            }
            2 -> {
                // Category & Source Breakdown Detail View
                item {
                    PaperCategoryBreakdownCard(stats = stats)
                }

                item {
                    PaperSourceAppsAndTagsCard(stats = stats)
                }
            }
        }
    }
}

// --- Paper Styling Components ---

@Composable
fun PushPin(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp)
            .shadow(2.dp, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFEF4444), Color(0xFFB91C1C)),
                    center = Offset(radius * 0.7f, radius * 0.7f),
                    radius = radius
                )
            )
            // Pin highlight point
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = radius * 0.3f,
                center = Offset(radius * 0.6f, radius * 0.6f)
            )
        }
    }
}

@Composable
fun PaperTapeStrip(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .width(60.dp)
            .height(14.dp)
            .rotate(-2f)
            .clip(RoundedCornerShape(2.dp))
            .background(if (isDark) Color(0x33FFFFFF) else Color(0xDDEFE3C3))
            .border(
                1.dp,
                if (isDark) Color(0x22FFFFFF) else Color(0x55D6C5A0),
                RoundedCornerShape(2.dp)
            )
    )
}

@Composable
fun PaperStampBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .rotate(-3f)
            .border(2.dp, color.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 1.sp
        )
    }
}

// --- Header & Navigation Tabs ---

@Composable
fun PaperBoardHeader(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    tabs: List<String>
) {
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E1C26) else Color(0xFFFFFDF9)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF333042) else Color(0xFFE8DFC8))
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                PushPin(modifier = Modifier.align(Alignment.TopEnd))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📌 VISUAL CLIPBOARD BOARD",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple,
                            letterSpacing = 1.5.sp
                        )
                        PaperStampBadge(text = "LIVE INSIGHTS", color = Color(0xFF059669))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "History Summary & Frequency Analytics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tactile Paper Index Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val isSelected = index == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(
                            if (isSelected) {
                                if (isDark) Color(0xFF2A2636) else Color(0xFFFFFDF9)
                            } else {
                                if (isDark) Color(0xFF181620) else Color(0xFFEDE6D8)
                            }
                        )
                        .border(
                            1.dp,
                            if (isSelected) PrimaryPurple else (if (isDark) Color(0xFF333042) else Color(0xFFD6C8B0)),
                            RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        )
                        .clickable { onTabSelect(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) PrimaryPurple else Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// --- Card 1: Sticky Notes Grid ---

@Composable
fun PaperStickyNotesGrid(stats: DashboardStats) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Yellow Sticky Note: Clips & Velocity
        Card(
            modifier = Modifier
                .weight(1f)
                .rotate(-1.8f)
                .testTag("sticky_note_total"),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF3B3213) else Color(0xFFFEF9C3)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF715F1F) else Color(0xFFFDE047))
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                PaperTapeStrip(modifier = Modifier.align(Alignment.TopEnd).offset(y = (-6).dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFFACC15) else Color(0xFFCA8A04),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TOTAL CLIPS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFDE047) else Color(0xFF854D0E)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${stats.totalItems}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFFFEF08A) else Color(0xFF713F12)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚡ ~${String.format("%.1f", stats.copyVelocityPerDay)} clips/day",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFFDE047) else Color(0xFFA16207)
                    )
                }
            }
        }

        // Pink Sticky Note: Memory & Words
        Card(
            modifier = Modifier
                .weight(1f)
                .rotate(1.5f)
                .testTag("sticky_note_words"),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF3B1E2B) else Color(0xFFFCE7F3)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF831843) else Color(0xFFFBCFE8))
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                PaperTapeStrip(modifier = Modifier.align(Alignment.TopStart).offset(y = (-6).dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FontDownload,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WORD VOLUME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFBCFE8) else Color(0xFF9D174D)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${stats.totalWords}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFFFCE7F3) else Color(0xFF831843)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Avg ${stats.averageLength} chars/snippet",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFF472B6) else Color(0xFFBE185D)
                    )
                }
            }
        }
    }
}

// --- Card 2: Daily Activity Paper Bar Chart ---

@Composable
fun PaperDailyActivityCard(stats: DashboardStats) {
    val isDark = isSystemInDarkTheme()
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_activity_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1C26) else Color(0xFFFFFDF9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF333042) else Color(0xFFE8DFC8))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PushPin()
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "7-Day Copy Frequency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap any paper bar for precise count",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }
                PaperStampBadge(text = "FREQUENCY", color = PrimaryPurple)
            }

            Spacer(modifier = Modifier.height(20.dp))

            val maxCount = stats.dailyStats.maxOfOrNull { it.count } ?: 1
            val displayMax = if (maxCount == 0) 1 else maxCount

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        if (isDark) Color(0xFF171520) else Color(0xFFFAF7F0),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isDark) Color(0xFF282536) else Color(0xFFE8DFC8),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                // Background ruler grid lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lines = 4
                    val stepY = size.height / lines
                    for (i in 1 until lines) {
                        val y = i * stepY
                        drawLine(
                            color = if (isDark) Color(0xFF2B283B) else Color(0xFFE5DCC6),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    stats.dailyStats.forEachIndexed { index, day ->
                        val fraction = day.count.toFloat() / displayMax
                        val animatedHeightFraction by animateFloatAsState(
                            targetValue = if (day.count > 0) maxOf(fraction, 0.12f) else 0.04f,
                            animationSpec = tween(600), label = "BarHeight"
                        )
                        val isSelected = selectedBarIndex == index

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedBarIndex = if (isSelected) null else index },
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isSelected || day.count > 0) {
                                Text(
                                    text = "${day.count}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            // Paper Bar Strip
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .fillMaxHeight(animatedHeightFraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isSelected) {
                                            Brush.verticalGradient(
                                                colors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                                            )
                                        } else if (day.count > 0) {
                                            Brush.verticalGradient(
                                                colors = listOf(PrimaryPurple, PrimaryPurple.copy(alpha = 0.5f))
                                            )
                                        } else {
                                            SolidColor(if (isDark) Color(0xFF2D2A3A) else Color(0xFFE2D9C5))
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.White else Color.Transparent,
                                        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = day.dateLabel,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimaryPurple else Slate400,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (selectedBarIndex != null) {
                val day = stats.dailyStats[selectedBarIndex!!]
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryPurpleContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📅 ${day.dateLabel}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${day.count} items recorded",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryPurple
                        )
                    }
                }
            }
        }
    }
}

// --- Card 3: Hourly Activity Heatmap ---

@Composable
fun PaperHourlyFrequencyHeatmapCard(stats: DashboardStats) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1C26) else Color(0xFFFFFDF9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF333042) else Color(0xFFE8DFC8))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Peak Time Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Peak hour slot: ${stats.peakHourLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
                PaperStampBadge(text = "24-HR PATTERN", color = Color(0xFF0284C7))
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxHourly = stats.hourlyStats.maxOrNull() ?: 1
            val displayMax = if (maxHourly == 0) 1 else maxHourly

            // 24 Hour Matrix Grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (h in 0..11) {
                        val count = stats.hourlyStats[h]
                        val alpha = if (count > 0) maxOf(0.2f, count.toFloat() / displayMax) else 0.05f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryPurple.copy(alpha = alpha))
                                .border(0.5.dp, if (isDark) Color(0xFF38334A) else Color(0xFFE8DFC8), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${h}h",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (alpha > 0.5f) Color.White else Slate400
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (h in 12..23) {
                        val count = stats.hourlyStats[h]
                        val alpha = if (count > 0) maxOf(0.2f, count.toFloat() / displayMax) else 0.05f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryPurple.copy(alpha = alpha))
                                .border(0.5.dp, if (isDark) Color(0xFF38334A) else Color(0xFFE8DFC8), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${h}h",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (alpha > 0.5f) Color.White else Slate400
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Card 4: Category Breakdown ---

@Composable
fun PaperCategoryBreakdownCard(stats: DashboardStats) {
    val isDark = isSystemInDarkTheme()
    val totalCount = stats.categoryStats.sumOf { it.count }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_distribution_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1C26) else Color(0xFFFFFDF9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF333042) else Color(0xFFE8DFC8))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Category Composition",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Saved item categorization breakdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
                PaperStampBadge(text = "CLASSIFIED", color = Color(0xFF16A34A))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Paper Donut Ring Chart
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val strokeWidth = 16.dp
                    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val minDim = size.minDimension
                        val radius = minDim / 2f - strokePx / 2f

                        if (totalCount == 0) {
                            drawCircle(
                                color = if (isDark) Color(0xFF2D2A3A) else Color(0xFFE8DFC8),
                                radius = radius,
                                style = Stroke(width = strokePx)
                            )
                        } else {
                            var startAngle = -90f
                            stats.categoryStats.forEach { cat ->
                                val sweep = (cat.count.toFloat() / totalCount) * 360f
                                if (sweep > 0f) {
                                    drawArc(
                                        color = getCategoryColor(cat.category, isDark),
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalCount",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ITEMS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Category List Progress Bars
                Column(
                    modifier = Modifier.weight(1.3f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    stats.categoryStats.take(5).forEach { cat ->
                        val percent = if (totalCount > 0) (cat.count * 100) / totalCount else 0
                        val catColor = getCategoryColor(cat.category, isDark)

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                    Text(
                                        text = cat.category,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${cat.count} ($percent%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate400
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Paper Measuring Tape Meter
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isDark) Color(0xFF2A2738) else Color(0xFFE8DFC8))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(percent / 100f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(catColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Card 5: Top Copied Snippet ("Copy Count Champion") ---

@Composable
fun PaperTopCopiedCard(
    item: ClipboardItemEntity,
    viewModel: ClipboardViewModel
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .rotate(-0.8f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1C26) else Color(0xFFFFFDF9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF333042) else Color(0xFFE8DFC8))
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            PushPin(modifier = Modifier.align(Alignment.TopEnd))

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaperStampBadge(text = "👑 MOST COPIED SNIPPET", color = Color(0xFFDC2626))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PrimaryPurpleContainer.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "Copied ${item.copyCount}x",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lined Notebook Snippet Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF171520) else Color(0xFFFBF8F0))
                        .border(1.dp, if (isDark) Color(0xFF2A2738) else Color(0xFFE8DFC8), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = item.preview.ifBlank { item.text },
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Source: ${item.sourceApp} • Category: ${item.category}",
                        fontSize = 11.sp,
                        color = Slate400
                    )

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(item.text))
                            Toast.makeText(context, "Copied snippet to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Again", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- Card 6: Source Apps & Top Tags ---

@Composable
fun PaperSourceAppsAndTagsCard(stats: DashboardStats) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1C26) else Color(0xFFFFFDF9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF333042) else Color(0xFFE8DFC8))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Source Applications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Applications generating saved clipboard items",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (stats.topSourceApps.isEmpty()) {
                Text("No source data recorded yet", fontSize = 12.sp, color = Slate400)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val maxAppCount = stats.topSourceApps.maxOfOrNull { it.second } ?: 1
                    stats.topSourceApps.forEach { (appName, count) ->
                        val percent = (count.toFloat() / maxAppCount)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = appName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1.2f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .weight(2f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (isDark) Color(0xFF2D2A3A) else Color(0xFFE8DFC8))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(percent)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(PrimaryPurple)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "$count",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400
                            )
                        }
                    }
                }
            }

            if (stats.topTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = if (isDark) Color(0xFF2D2A3A) else Color(0xFFE8DFC8))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Frequent Tags",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stats.topTags) { (tag, count) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryPurpleContainer.copy(alpha = 0.3f),
                            border = BorderStroke(0.5.dp, PrimaryPurple.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPurple
                                )
                                Text(
                                    text = "($count)",
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Card 7: AI / Smart Paper Board Summary Note ---

@Composable
fun PaperAiSummaryNote(stats: DashboardStats) {
    val isDark = isSystemInDarkTheme()
    val topCategory = stats.categoryStats.maxByOrNull { it.count }?.category ?: "General"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .rotate(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF262013) else Color(0xFFFEFCE8)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF715F1F) else Color(0xFFFEF08A))
    ) {
        Box(modifier = Modifier.padding(18.dp)) {
            PaperTapeStrip(modifier = Modifier.align(Alignment.TopEnd).offset(y = (-6).dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFFACC15) else Color(0xFFCA8A04),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PAPER BOARD ASSISTANT TAKEAWAY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFDE047) else Color(0xFF854D0E),
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "• Peak activity occurs around ${stats.peakHourLabel}.\n" +
                           "• Primary category: $topCategory (${stats.categoryStats.find { it.category == topCategory }?.count ?: 0} clips).\n" +
                           "• Saved ${stats.totalWords} words across ${stats.totalItems} entries with an average clip speed of ~${String.format("%.1f", stats.copyVelocityPerDay)} clips/day.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFFEF08A) else Color(0xFF713F12),
                    lineHeight = 20.sp
                )
            }
        }
    }
}
