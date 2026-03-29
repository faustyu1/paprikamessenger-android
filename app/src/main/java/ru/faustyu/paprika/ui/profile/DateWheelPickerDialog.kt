package ru.faustyu.paprika.ui.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.Calendar
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

/**
 * A single drum-roll wheel picker column.
 *
 * @param items        the real items to pick from (without padding)
 * @param selectedIndex 0-based index into [items] that is initially centred
 * @param onIndexChanged called with the new 0-based index every time the wheel settles
 * @param itemHeight    height of one row
 * @param visibleCount  total visible rows (must be odd so there is a clear centre)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp,
    visibleCount: Int = 5
) {
    require(visibleCount % 2 == 1) { "visibleCount must be odd" }

    val paddingCount = visibleCount / 2
    // Surround real items with empty padding so the first/last real item can be centred
    val paddedItems = remember(items, paddingCount) {
        List(paddingCount) { "" } + items + List(paddingCount) { "" }
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    )
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Notify parent when the wheel settles (skip the very first emission which fires
    // immediately on composition before the user has interacted at all)
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .drop(1)                // ignore initial false
            .filter { !it }         // only when scrolling stops
            .collect {
                val idx = listState.firstVisibleItemIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
                onIndexChanged(idx)
            }
    }

    // If the parent changes selectedIndex externally (e.g., day clamped), scroll there
    LaunchedEffect(selectedIndex) {
        val current = listState.firstVisibleItemIndex
        if (current != selectedIndex) {
            listState.animateScrollToItem(selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier.height(itemHeight * visibleCount)) {

        // ── Centre highlight bar ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = primaryColor.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(10.dp)
                )
        )

        // ── Scrollable list ─────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) {
            items(paddedItems.size) { rawIndex ->
                // The centre item index changes as user scrolls; derive distance from centre
                val centreIndex = listState.firstVisibleItemIndex + paddingCount
                val distance = kotlin.math.abs(rawIndex - centreIndex)

                val alpha = when (distance) {
                    0    -> 1.00f
                    1    -> 0.55f
                    else -> 0.22f
                }
                val weight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal
                val style  = if (distance == 0) MaterialTheme.typography.bodyLarge
                             else MaterialTheme.typography.bodyMedium

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text        = paddedItems[rawIndex],
                        style       = style,
                        fontWeight  = weight,
                        color       = onSurfaceColor.copy(alpha = alpha),
                        textAlign   = TextAlign.Center
                    )
                }
            }
        }

        // ── Top fade-out ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(itemHeight * paddingCount)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(surfaceColor, surfaceColor.copy(alpha = 0f))
                    )
                )
        )

        // ── Bottom fade-out ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(itemHeight * paddingCount)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(surfaceColor.copy(alpha = 0f), surfaceColor)
                    )
                )
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Date picker dialog
// ────────────────────────────────────────────────────────────────────────────

/**
 * A fully custom Compose date-picker dialog using three drum-roll wheels
 * (Day / Month / Year).  Max date is always today (no future birthdays).
 *
 * @param initialDate   existing date in "YYYY-MM-DD" format, or empty string
 * @param onDateSelected called with the chosen date in "YYYY-MM-DD" when the user taps OK
 * @param onDismiss     called when the dialog should be closed without a result
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DateWheelPickerDialog(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val today = Calendar.getInstance()
    val maxYear  = today.get(Calendar.YEAR)

    // Parse initial date, fall back to 25 years ago if blank/invalid
    var initYear  = maxYear - 25
    var initMonth = 1
    var initDay   = 1
    if (initialDate.isNotBlank()) {
        runCatching {
            val p = initialDate.split("-")
            initYear  = p[0].toInt().coerceIn(1900, maxYear)
            initMonth = p[1].toInt().coerceIn(1, 12)
            initDay   = p[2].toInt().coerceIn(1, 31)
        }
    }

    val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun",
                            "Jul","Aug","Sep","Oct","Nov","Dec")
    val years  = remember { (1900..maxYear).map { it.toString() } }

    var selectedYear  by remember { mutableIntStateOf(initYear) }
    var selectedMonth by remember { mutableIntStateOf(initMonth) }
    var selectedDay   by remember { mutableIntStateOf(initDay) }

    // Recompute max days whenever year / month changes
    val daysInMonth by remember(selectedYear, selectedMonth) {
        derivedStateOf {
            Calendar.getInstance().let { cal ->
                cal.set(selectedYear, selectedMonth - 1, 1)
                cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
        }
    }

    // Clamp day if the new month has fewer days
    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) selectedDay = daysInMonth
    }

    val days = remember(daysInMonth) {
        (1..daysInMonth).map { it.toString().padStart(2, '0') }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape         = RoundedCornerShape(24.dp),
            color         = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {

                // Title
                Text(
                    text       = "Date of Birth",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                // Live preview of selected date
                Text(
                    text  = "%02d %s %04d".format(
                        selectedDay.coerceIn(1, daysInMonth),
                        monthNames.getOrElse(selectedMonth - 1) { "?" },
                        selectedYear
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                // Column headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Day",   modifier = Modifier.weight(1f),   textAlign = TextAlign.Center,
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Month", modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center,
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Year",  modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center,
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(4.dp))

                // Three wheels — Day picker is keyed on daysInMonth so it reinitialises
                // automatically when the selectable range changes
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    key(daysInMonth) {
                        WheelPicker(
                            items          = days,
                            selectedIndex  = (selectedDay - 1).coerceIn(0, daysInMonth - 1),
                            onIndexChanged = { selectedDay = it + 1 },
                            modifier       = Modifier.weight(1f)
                        )
                    }

                    WheelPicker(
                        items          = monthNames,
                        selectedIndex  = (selectedMonth - 1).coerceIn(0, 11),
                        onIndexChanged = { selectedMonth = it + 1 },
                        modifier       = Modifier.weight(1.4f)
                    )

                    WheelPicker(
                        items          = years,
                        selectedIndex  = (selectedYear - 1900).coerceIn(0, years.size - 1),
                        onIndexChanged = { selectedYear = it + 1900 },
                        modifier       = Modifier.weight(1.3f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Buttons
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val clampedDay = selectedDay.coerceIn(1, daysInMonth)
                            onDateSelected("%04d-%02d-%02d".format(selectedYear, selectedMonth, clampedDay))
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
