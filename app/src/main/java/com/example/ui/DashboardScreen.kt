package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.SpacesViewModel
import com.example.data.VoiceLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DomainStats(
    val title: String,
    val activeAgents: Int,
    val status: String,
    val icon: ImageVector,
    val description: String,
    val category: String
)

val domains = listOf(
    DomainStats(
        title = "Trading & SIP",
        activeAgents = 10,
        status = "Optimal",
        icon = Icons.Filled.ShowChart,
        description = "Analyzes and operates Stock market investments, optional business ideas, SWPs, and active SIPs autonomously.",
        category = "Investments"
    ),
    DomainStats(
        title = "Accounting",
        activeAgents = 7,
        status = "Awaiting Review",
        icon = Icons.Filled.AccountBalance,
        description = "Handles set accounting, auto money-lending tracking, asset control, and dynamic balance sheet evaluation.",
        category = "Finance"
    ),
    DomainStats(
        title = "E-Commerce",
        activeAgents = 4,
        status = "Syncing",
        icon = Icons.Filled.Storefront,
        description = "Oversees e-commerce stores, handles logistics routing, inventory health analysis, and purchase tracking.",
        category = "Finance"
    ),
    DomainStats(
        title = "HR & Ops",
        activeAgents = 5,
        status = "Idle",
        icon = Icons.Filled.Work,
        description = "Assigns day-to-day work, provides one-on-one manager logic, and manages physical & virtual task routing.",
        category = "Finance"
    ),
    DomainStats(
        title = "Code Auto-Defense",
        activeAgents = 2,
        status = "Active Scanning",
        icon = Icons.Filled.Code,
        description = "Analyzes online Python & JavaScript environments, identifies safe defense measures, and implements cyber protections.",
        category = "Security"
    ),
    DomainStats(
        title = "Global Surveillance",
        activeAgents = 1,
        status = "Monitoring",
        icon = Icons.Filled.Security,
        description = "Integrates automatic environmental safety diagnostics, vocal translation monitors, and candidate camera streams.",
        category = "Security"
    )
)

@Composable
fun InteractiveAnalyticsChartCard() {
    var isGrowthMode by remember { mutableStateOf(true) }
    
    val labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul")
    val values = if (isGrowthMode) {
        listOf(12000f, 15500f, 18200f, 16800f, 22400f, 28000f, 35400f)
    } else {
        listOf(8400f, 9200f, 7100f, 11500f, 6300f, 8900f, 5200f)
    }
    val formattedTexts = if (isGrowthMode) {
        listOf("$12k", "$15.5k", "$18.2k", "$16.8k", "$22.4k", "$28k", "$35.4k")
    } else {
        listOf("$8.4k", "$9.2k", "$7.1k", "$11.5k", "$6.3k", "$8.9k", "$5.2k")
    }
    
    val themeColor = if (isGrowthMode) {
        Color(0xFF00C853) // Emerald Green
    } else {
        Color(0xFFE91E63) // Neon Pink/Red for Expenses
    }

    var selectedPointIndex by remember(isGrowthMode) { mutableStateOf(6) } // Start focused on July (latest index)

    // Capture colors outside Canvas so we don't call Composable gets inside the draw block
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("finance_analytics_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Visual Financial Intelligence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isGrowthMode) "SIP & SWP Asset Valuation Growth" else "Day-to-Day Operations & Logistics Costs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = if (isGrowthMode) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector Tab Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { isGrowthMode = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("chart_mode_growth"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGrowthMode) themeColor.copy(alpha = 0.15f) else Color.Transparent,
                        contentColor = if (isGrowthMode) themeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Investment Growth", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { isGrowthMode = false },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("chart_mode_expenses"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isGrowthMode) themeColor.copy(alpha = 0.15f) else Color.Transparent,
                        contentColor = if (!isGrowthMode) themeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Expense Breakdown", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Canvas Chart
            val density = LocalDensity.current
            val textMeasurer = rememberTextMeasurer()
            
            val paddingLeftPx = with(density) { 45.dp.toPx() }
            val paddingRightPx = with(density) { 15.dp.toPx() }
            val paddingTopPx = with(density) { 20.dp.toPx() }
            val paddingBottomPx = with(density) { 30.dp.toPx() }

            // Precalculate pixel metrics to avoid calling dp.toPx() inside Canvas draw context
            val gridStrokeWidthPx = with(density) { 1.dp.toPx() }
            val labelPaddingXPx = with(density) { 6.dp.toPx() }
            val labelPaddingYPx = with(density) { 6.dp.toPx() }
            val lineStrokeWidthPx = with(density) { 3.dp.toPx() }
            val guidelineStrokeWidthPx = with(density) { 1.dp.toPx() }
            val circleGlowRadiusPx = with(density) { 11.dp.toPx() }
            val circleCenterRadiusPx = with(density) { 6.dp.toPx() }
            val circleInnerWhiteRadiusPx = with(density) { 2.5f.dp.toPx() }
            val tooltipPaddingXPx = with(density) { 16.dp.toPx() }
            val tooltipPaddingYPx = with(density) { 8.dp.toPx() }
            val tooltipOffsetPx = with(density) { 10.dp.toPx() }
            val tooltipTextOffsetPx = with(density) { 8.dp.toPx() }
            val tooltipTextOffsetYPx = with(density) { 4.dp.toPx() }
            val tooltipCornerRadiusPx = with(density) { 6.dp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(values, isGrowthMode) {
                            detectTapGestures { offset ->
                                val drawableWidth = size.width - paddingLeftPx - paddingRightPx
                                if (drawableWidth > 0) {
                                    var closestIndex = 0
                                    var minDistanceX = Float.MAX_VALUE
                                    for (i in values.indices) {
                                        val pX = paddingLeftPx + (i.toFloat() / (values.size - 1)) * drawableWidth
                                        val dist = kotlin.math.abs(offset.x - pX)
                                        if (dist < minDistanceX) {
                                            minDistanceX = dist
                                            closestIndex = i
                                        }
                                    }
                                    selectedPointIndex = closestIndex
                                }
                            }
                        }
                ) {
                    val drawableWidth = size.width - paddingLeftPx - paddingRightPx
                    val drawableHeight = size.height - paddingTopPx - paddingBottomPx

                    if (drawableWidth <= 0 || drawableHeight <= 0) return@Canvas

                    val maxValue = values.maxOrNull() ?: 1f
                    val minValue = values.minOrNull() ?: 0f
                    
                    val rangeMargin = if (isGrowthMode) 4000f else 1500f
                    val maxWithMargin = maxValue + rangeMargin
                    val minWithMargin = maxOf(0f, minValue - rangeMargin)
                    val valueRange = if (maxWithMargin == minWithMargin) 1f else (maxWithMargin - minWithMargin)

                    // Calculate point offsets
                    val points = List(values.size) { i ->
                        val pX = paddingLeftPx + (i.toFloat() / (values.size - 1)) * drawableWidth
                        val pY = paddingTopPx + drawableHeight - ((values[i] - minWithMargin) / valueRange) * drawableHeight
                        Offset(pX, pY)
                    }

                    // 1. Gridlines (3 horizontal dotted lines)
                    val gridEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    val labelStyle = TextStyle(
                        color = onSurfaceVariantColor.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )

                    for (j in 0..3) {
                        val gridY = paddingTopPx + (j.toFloat() / 3) * drawableHeight
                        // Grid Line
                        drawLine(
                            color = onSurfaceColor.copy(alpha = 0.1f),
                            start = Offset(paddingLeftPx, gridY),
                            end = Offset(size.width - paddingRightPx, gridY),
                            pathEffect = gridEffect,
                            strokeWidth = gridStrokeWidthPx
                        )
                        // Grid Value Labels
                        val gridVal = maxWithMargin - j * (valueRange / 3f)
                        val valString = if (gridVal >= 1000f) {
                            String.format(Locale.US, "$%.1fk", gridVal / 1000f)
                        } else {
                            String.format(Locale.US, "$%.0f", gridVal)
                        }
                        val measuredValText = textMeasurer.measure(valString, style = labelStyle)
                        drawText(
                            textLayoutResult = measuredValText,
                            topLeft = Offset(
                                paddingLeftPx - measuredValText.size.width - labelPaddingXPx,
                                gridY - measuredValText.size.height / 2f
                            )
                        )
                    }

                    // 2. Bezier Path Construction and Gradient Shade
                    if (points.isNotEmpty()) {
                        val strokePath = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val pPrev = points[i - 1]
                                val pCurrent = points[i]
                                val controlX1 = pPrev.x + (pCurrent.x - pPrev.x) / 2f
                                val controlY1 = pPrev.y
                                val controlX2 = pPrev.x + (pCurrent.x - pPrev.x) / 2f
                                val controlY2 = pCurrent.y
                                cubicTo(controlX1, controlY1, controlX2, controlY2, pCurrent.x, pCurrent.y)
                            }
                        }

                        // Fill color gradient under the curve
                        val fillPath = Path().apply {
                            addPath(strokePath)
                            lineTo(points.last().x, paddingTopPx + drawableHeight)
                            lineTo(points.first().x, paddingTopPx + drawableHeight)
                            close()
                        }

                        val fillBrush = Brush.verticalGradient(
                            colors = listOf(
                                themeColor.copy(alpha = 0.25f),
                                themeColor.copy(alpha = 0.01f)
                            ),
                            startY = paddingTopPx,
                            endY = paddingTopPx + drawableHeight
                        )

                        // Draw shaded area
                        drawPath(path = fillPath, brush = fillBrush)

                        // Draw smooth line
                        drawPath(
                            path = strokePath,
                            color = themeColor,
                            style = Stroke(width = lineStrokeWidthPx, cap = StrokeCap.Round)
                        )
                    }

                    // 3. X-axis Labels (Months)
                    labels.forEachIndexed { i, labelText ->
                        val p = points[i]
                        val measuredText = textMeasurer.measure(labelText, style = labelStyle)
                        drawText(
                            textLayoutResult = measuredText,
                            topLeft = Offset(
                                p.x - measuredText.size.width / 2f,
                                size.height - paddingBottomPx + labelPaddingYPx
                            )
                        )
                    }

                    // 4. Highlight Selected Segment / Interactive Tooltip
                    if (selectedPointIndex in points.indices) {
                        val selPoint = points[selectedPointIndex]
                        
                        // Vertical guideline
                        drawLine(
                            color = themeColor.copy(alpha = 0.25f),
                            start = Offset(selPoint.x, paddingTopPx),
                            end = Offset(selPoint.x, paddingTopPx + drawableHeight),
                            strokeWidth = guidelineStrokeWidthPx,
                            pathEffect = gridEffect
                        )

                        // Anchor dots
                        drawCircle(
                            color = themeColor.copy(alpha = 0.2f),
                            radius = circleGlowRadiusPx,
                            center = selPoint
                        )
                        drawCircle(
                            color = themeColor,
                            radius = circleCenterRadiusPx,
                            center = selPoint
                        )
                        drawCircle(
                            color = Color.White,
                            radius = circleInnerWhiteRadiusPx,
                            center = selPoint
                        )

                        // Float Tooltip Box
                        val tooltipText = "${labels[selectedPointIndex]}: ${formattedTexts[selectedPointIndex]}"
                        val tooltipStyle = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val measuredTooltip = textMeasurer.measure(tooltipText, style = tooltipStyle)
                        val tooltipWidth = measuredTooltip.size.width + tooltipPaddingXPx
                        val tooltipHeight = measuredTooltip.size.height + tooltipPaddingYPx

                        var tooltipX = selPoint.x - tooltipWidth / 2f
                        if (tooltipX < paddingLeftPx) tooltipX = paddingLeftPx
                        if (tooltipX + tooltipWidth > size.width - paddingRightPx) {
                            tooltipX = size.width - paddingRightPx - tooltipWidth
                        }

                        val tooltipY = maxOf(paddingTopPx - tooltipOffsetPx, selPoint.y - tooltipHeight - tooltipOffsetPx)

                        drawRoundRect(
                            color = Color(0xE51D1D24), // High contrast dark capsule backplate
                            topLeft = Offset(tooltipX, tooltipY),
                            size = Size(tooltipWidth, tooltipHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(tooltipCornerRadiusPx, tooltipCornerRadiusPx)
                        )

                        drawText(
                            textLayoutResult = measuredTooltip,
                            topLeft = Offset(tooltipX + tooltipTextOffsetPx, tooltipY + tooltipTextOffsetYPx)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Info Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isGrowthMode) "SIP Investment Projection" else "Automated Budget Optimization",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isGrowthMode) "Target Valuation Portfolio: $50,000" else "Target Burn Constraint: <$6,000",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isGrowthMode) "+195% Growth" else "-38% Lower Burn",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = themeColor
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    spacesViewModel: SpacesViewModel = viewModel()
) {
    var selectedDomainForDetails by remember { mutableStateOf<DomainStats?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val filteredDomains = remember(selectedCategory) {
        if (selectedCategory == "All") {
            domains
        } else {
            domains.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }
    val chunkedDomains = remember(filteredDomains) { filteredDomains.chunked(2) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OmniAgent Network",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Automated Spaces & Voice Log Dashboard",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Central Voice command ledger history button
                Button(
                    onClick = { showHistoryDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("open_history_dialog_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Command History",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Expanded Interactive Recharts Analytics section
        item {
            InteractiveAnalyticsChartCard()
        }

        // Centralized Layout Stateful View Selector
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sub_agent_view_selector"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Sub-Agent Deck Control",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Styled Horizontal Segment Buttons (all equal width weights)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val categories = listOf("All", "Finance", "Investments", "Security")
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            val activeColor = when (cat) {
                                "Finance" -> Color(0xFFE91E63)     // Expenses Pink Accent
                                "Investments" -> Color(0xFF00C853) // Growth Green Accent
                                "Security" -> Color(0xFF2196F3)    // Defensive Security Blue Accent
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Button(
                                onClick = { selectedCategory = cat },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("sub_agent_tab_${cat.lowercase()}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) activeColor.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                elevation = null,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val icon = when (cat) {
                                        "Finance" -> Icons.Filled.AccountBalanceWallet
                                        "Investments" -> Icons.Filled.ShowChart
                                        "Security" -> Icons.Filled.Shield
                                        else -> Icons.Filled.AllInclusive
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Brief functional summary matching the selection with a smooth cross-fade animation
                    AnimatedContent(
                        targetState = selectedCategory,
                        transitionSpec = {
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                        },
                        label = "SummaryTextTransition"
                    ) { currentCat ->
                        val summaryText = when (currentCat) {
                            "Finance" -> "Operating Accounting ledgers, E-Commerce routing logistics, and supervisor HR workflows."
                            "Investments" -> "Monitoring active stock market operations, SWP assets, and compounding SIP portfolios."
                            "Security" -> "Shielding online running scripts, sandbox environments, and real-time audio surveillance."
                            else -> "Showing all 6 autonomous sub-agents currently deployed across the OmniAgent network."
                        }

                        val activeColor = when (currentCat) {
                            "Finance" -> Color(0xFFE91E63)
                            "Investments" -> Color(0xFF00C853)
                            "Security" -> Color(0xFF2196F3)
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(activeColor, CircleShape)
                            )
                            Text(
                                text = summaryText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // Section Title with smooth scale/fade entry animation
        item {
            AnimatedContent(
                targetState = selectedCategory,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                },
                label = "TitleTransition"
            ) { currentCat ->
                Text(
                    text = if (currentCat == "All") "Target Supervisor Spaces" else "$currentCat Supervisor Spaces",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // Agent Domain Grid (rendered inside a single item using AnimatedContent to support seamless transitions)
        item {
            AnimatedContent(
                targetState = chunkedDomains,
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)) +
                            slideInVertically(
                                initialOffsetY = { 40 },
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                            ) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing))
                },
                label = "GridTransition"
            ) { currentChunks ->
                if (currentChunks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No sub-agents found in this category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        currentChunks.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    DomainCard(
                                        domain = pair[0],
                                        onClick = { selectedDomainForDetails = pair[0] }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    if (pair.size > 1) {
                                        DomainCard(
                                            domain = pair[1],
                                            onClick = { selectedDomainForDetails = pair[1] }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clearance spaces for navigation overlay and margin
        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    // Detail Dialog with real-time Room Log viewer & direct additions
    selectedDomainForDetails?.let { domain ->
        SpaceDetailDialog(
            domain = domain,
            spacesViewModel = spacesViewModel,
            onDismiss = { selectedDomainForDetails = null }
        )
    }

    if (showHistoryDialog) {
        VoiceCommandHistoryDialog(
            spacesViewModel = spacesViewModel,
            onDismiss = { showHistoryDialog = false }
        )
    }
}

@Composable
fun DomainCard(domain: DomainStats, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() }
            .testTag("domain_card_${domain.title.replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = domain.icon,
                    contentDescription = domain.title,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column {
                Text(
                    text = domain.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${domain.activeAgents} Agents",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = domain.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceDetailDialog(
    domain: DomainStats,
    spacesViewModel: SpacesViewModel,
    onDismiss: () -> Unit
) {
    val logsFlow = remember(domain.title) { spacesViewModel.getLogsForDomain(domain.title) }
    val logs by logsFlow.collectAsState(initial = emptyList())
    var quickNoteInput by remember { mutableStateOf("") }
    
    val dateSdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = domain.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = domain.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                Text(
                    text = domain.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Agent Target Level: 10 → 1 Step Branch Transformation",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Activity History & Voice Logs",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Log Entries List
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MicNone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Awaiting first Voice-to-Text log command.\nLogged commands will persist here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs) { log ->
                            LogItemRow(log = log, sdf = dateSdf, onDelete = {
                                spacesViewModel.deleteVoiceLog(log.id)
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick directive addition input field
                OutlinedTextField(
                    value = quickNoteInput,
                    onValueChange = { quickNoteInput = it },
                    placeholder = { Text("Quick manual status/directive...") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_quick_note"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (quickNoteInput.isNotBlank()) {
                                spacesViewModel.saveVoiceLog(
                                    domainTitle = domain.title,
                                    transcription = quickNoteInput,
                                    isVoice = false
                                )
                                quickNoteInput = ""
                            }
                        }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add quick directive", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_dismiss_button")
            ) {
                Text("Close Console")
            }
        },
        dismissButton = {
            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = { spacesViewModel.clearLogs(domain.title) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("dialog_clear_all_button")
                ) {
                    Text("Clear All")
                }
            }
        }
    )
}

@Composable
fun LogItemRow(log: VoiceLogEntity, sdf: SimpleDateFormat, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Icon (Voice vs Manual Keyboard)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (log.isVoice) Color.Green.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (log.isVoice) Icons.Default.Mic else Icons.Default.EditNote,
                        contentDescription = null,
                        tint = if (log.isVoice) Color.Green else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                Column {
                    Text(
                        text = log.transcription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sdf.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete log",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
