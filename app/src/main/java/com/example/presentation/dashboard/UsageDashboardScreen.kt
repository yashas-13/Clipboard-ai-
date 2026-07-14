package com.example.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.clipboard_list.ClipboardViewModel
import com.example.presentation.clipboard_list.DashboardStats
import com.example.presentation.clipboard_list.CategoryCount
import com.example.presentation.clipboard_list.DailyCount
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleContainer
import com.example.ui.theme.Slate400

fun getCategoryColor(category: String, isDark: Boolean): Color {
    return when (category) {
        "Code" -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
        "Sensitive" -> if (isDark) Color(0xFFEF4444) else Color(0xFF991B1B)
        "URL" -> if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
        "Email" -> if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E)
        "Phone" -> if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
        "OTP" -> if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
        "JSON" -> if (isDark) Color(0xFFF0ABFC) else Color(0xFFA21CAF)
        "Markdown" -> if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA)
        "Rich Text" -> if (isDark) Color(0xFFF9A8D4) else Color(0xFFBE185D)
        "Image" -> if (isDark) Color(0xFFC084FC) else Color(0xFF6D28D9)
        else -> if (isDark) Color(0xFFE2E2E2) else Color(0xFF3B82F6)
    }
}

@Composable
fun UsageDashboardScreen(
    viewModel: ClipboardViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    if (stats.totalItems == 0) {
        EmptyDashboardState(modifier)
    } else {
        DashboardContent(stats = stats, modifier = modifier)
    }
}

@Composable
fun EmptyDashboardState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.InsertChartOutlined,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Activity Stats Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Copy text or URLs to start tracking usage analytics dynamically.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun DashboardContent(
    stats: DashboardStats,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Stats Row
        item {
            QuickStatsGrid(stats = stats)
        }

        // Daily Activity Chart Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_activity_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Total copies recorded over the last 7 days",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    DailyActivityBarChart(
                        dailyStats = stats.dailyStats,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }

        // Category Distribution Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_distribution_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Distribution of copied content by category type",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        CategoryDonutChart(
                            stats = stats.categoryStats,
                            modifier = Modifier
                                .size(130.dp)
                                .weight(1f)
                        )

                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stats.categoryStats.take(4).forEach { cat ->
                                val totalCount = stats.categoryStats.sumOf { it.count }
                                val percent = if (totalCount > 0) (cat.count * 100) / totalCount else 0
                                CategoryLegendRow(
                                    category = cat.category,
                                    count = cat.count,
                                    percentage = percent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStatsGrid(stats: DashboardStats) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickStatsCard(
                title = "Total Clips",
                value = stats.totalItems.toString(),
                icon = Icons.Default.ContentCopy,
                color = PrimaryPurple,
                containerColor = PrimaryPurpleContainer.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f)
            )
            QuickStatsCard(
                title = "Avg Length",
                value = "${stats.averageLength} ch",
                icon = Icons.Default.TextFields,
                color = Color(0xFF0EA5E9),
                containerColor = Color(0xFFE0F2FE),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickStatsCard(
                title = "Word Count",
                value = stats.totalWords.toString(),
                icon = Icons.Default.MenuBook,
                color = Color(0xFF10B981),
                containerColor = Color(0xFFD1FAE5),
                modifier = Modifier.weight(1f)
            )
            QuickStatsCard(
                title = "Total Chars",
                value = stats.totalChars.toString(),
                icon = Icons.Default.FontDownload,
                color = Color(0xFFF59E0B),
                containerColor = Color(0xFFFEF3C7),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickStatsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Card(
        modifier = modifier.testTag("stats_card_$title"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) color.copy(alpha = 0.2f) else containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDark) color else color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate400
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DailyActivityBarChart(
    dailyStats: List<DailyCount>,
    modifier: Modifier = Modifier
) {
    val maxCount = dailyStats.maxOfOrNull { it.count } ?: 0
    val displayMax = if (maxCount == 0) 1 else maxCount
    val axisLabelColor = Slate400
    val density = LocalDensity.current

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Horizontal reference lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepHeight = size.height / 3f
                for (i in 1..3) {
                    val y = size.height - (i * stepHeight)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Bars layout
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyStats.forEachIndexed { index, day ->
                    val fraction = day.count.toFloat() / displayMax
                    val fillPercent = if (day.count > 0) maxOf(fraction, 0.08f) else 0f

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (day.count > 0) {
                            Text(
                                text = day.count.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Gradient rounded bar
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .fillMaxHeight(fillPercent)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            PrimaryPurple,
                                            PrimaryPurple.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-Axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dailyStats.forEach { day ->
                Text(
                    text = day.dateLabel,
                    fontSize = 10.sp,
                    color = axisLabelColor,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    stats: List<CategoryCount>,
    modifier: Modifier = Modifier
) {
    val total = stats.sumOf { it.count }
    val strokeWidth = 14.dp
    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = size.minDimension
            val ringRadius = minDim / 2f - strokeWidthPx / 2f

            if (total == 0) {
                // Placeholder grey ring
                drawCircle(
                    color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                    radius = ringRadius,
                    style = Stroke(width = strokeWidthPx)
                )
            } else {
                var startAngle = -90f
                stats.forEach { item ->
                    val sweepAngle = (item.count.toFloat() / total) * 360f
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = getCategoryColor(item.category, isDark),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)
                        )
                        startAngle += sweepAngle
                    }
                }
            }
        }

        // Inner stats label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total",
                fontSize = 11.sp,
                color = Slate400,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = total.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CategoryLegendRow(
    category: String,
    count: Int,
    percentage: Int
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(getCategoryColor(category, isDark))
            )
            Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "($percentage%)",
                fontSize = 11.sp,
                color = Slate400
            )
        }
    }
}
