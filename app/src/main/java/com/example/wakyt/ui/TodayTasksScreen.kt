package com.example.wakyt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wakyt.data.AppRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class TodayFilter { ALL, TODO, IN_PROGRESS, COMPLETED }

private data class TodayTask(
    val id: String,
    val project: String,
    val subGroup: String,
    val title: String,
    val time24: String, // "09:00"
    val dateOffsetDays: Int,
    val status: TodayFilter // use TODO/IN_PROGRESS/COMPLETED
)

private fun computeOffsetFromDateStr(dateStr: String?): Int {
    if (dateStr.isNullOrBlank()) return 0
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val target = Calendar.getInstance().apply {
        time = fmt.parse(dateStr)!!
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diff = target.timeInMillis - today.timeInMillis
    return (diff / (24 * 60 * 60 * 1000)).toInt()
}

private fun sortKey(time24: String?): Int {
    return try {
        val parts = (time24 ?: "00:00").split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        h * 60 + m
    } catch (_: Exception) { 0 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayTasksScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedDateOffset by remember { mutableStateOf(0) } // 0 = today
    var selectedFilter by remember { mutableStateOf(TodayFilter.ALL) }
    // Observe repository for live updates
    val repoTasks by AppRepository.tasks.collectAsState(initial = emptyList())
    val repoProjects by AppRepository.projects.collectAsState(initial = emptyList())
    val repoGroups by AppRepository.taskGroups.collectAsState(initial = emptyList())

    val tasks = remember(repoTasks, repoProjects, repoGroups) {
        repoTasks.map { t ->
            val proj = t.projectId?.let { id -> repoProjects.firstOrNull { it.id == id } }
            val group = proj?.groupId?.let { gid -> repoGroups.firstOrNull { it.id == gid } }
            TodayTask(
                id = t.id,
                project = proj?.name ?: "",
                subGroup = group?.name ?: "",
                title = t.name,
                time24 = t.time ?: "",
                dateOffsetDays = computeOffsetFromDateStr(t.date),
                status = TodayFilter.TODO
            )
        }.sortedWith(compareBy<TodayTask> { it.dateOffsetDays }.thenBy { sortKey(it.time24) }.thenBy { it.title })
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Today’s Tasks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            CalendarSection(
                isExpanded = isExpanded,
                selectedOffset = selectedDateOffset,
                tasksPerDate = remember(tasks) { tasks.groupBy { it.dateOffsetDays }.mapValues { it.value.size } },
                onSelectOffset = { selectedDateOffset = it },
                onToggleExpand = { isExpanded = !isExpanded }
            )

            Spacer(Modifier.height(12.dp))

            FilterSection(
                selected = selectedFilter,
                onSelect = { selectedFilter = it }
            )

            Spacer(Modifier.height(12.dp))

            TaskListSection(
                tasks = tasks,
                selectedOffset = selectedDateOffset,
                filter = selectedFilter
            )
        }
    }
}

@Composable
private fun CalendarSection(
    isExpanded: Boolean,
    selectedOffset: Int,
    tasksPerDate: Map<Int, Int>,
    onSelectOffset: (Int) -> Unit,
    onToggleExpand: () -> Unit
) {
    val monthLabel = remember(selectedOffset) { monthLabelForOffset(selectedOffset) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(isExpanded) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 12) {
                            if (!isExpanded) onToggleExpand()
                        } else if (dragAmount < -12) {
                            if (isExpanded) onToggleExpand()
                        }
                    }
                )
            }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Transparent)
                    .padding(4.dp)
                    .pointerInput(Unit) {
                        // Tap month to expand/collapse
                        detectVerticalDragGestures(onVerticalDrag = { _, _ -> })
                    }
            )
            Text(
                text = if (isExpanded) "Swipe up to collapse" else "Swipe down to expand",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

        if (!isExpanded) {
            CompactCalendarRow(
                selectedOffset = selectedOffset,
                tasksPerDate = tasksPerDate,
                onSelectOffset = onSelectOffset
            )
        } else {
            ExpandedMonthGrid(
                baseOffset = selectedOffset,
                selectedOffset = selectedOffset,
                tasksPerDate = tasksPerDate,
                onSelectOffset = onSelectOffset
            )
        }
    }
}

@Composable
private fun CompactCalendarRow(
    selectedOffset: Int,
    tasksPerDate: Map<Int, Int>,
    onSelectOffset: (Int) -> Unit
) {
    val range = -15..15
    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(range.count()) { index ->
            val offset = range.first + index
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            val dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
            val isSelected = offset == selectedOffset
            val count = tasksPerDate[offset] ?: 0
            val alpha = when {
                count == 0 -> 1f
                count >= 5 -> 0.55f
                else -> 1f - (count * 0.1f)
            }
            DateChip(
                dayOfWeek = dayLabel,
                dayNumber = dayNum,
                selected = isSelected,
                alpha = alpha,
                onClick = { onSelectOffset(offset) }
            )
        }
    }
}

@Composable
private fun DateChip(dayOfWeek: String, dayNumber: Int, selected: Boolean, alpha: Float, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg.copy(alpha = alpha))
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(onVerticalDrag = { _, _ -> })
            }
            .padding(0.dp)
            .clickableNoRipple { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(dayOfWeek.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = fg)
        Text(dayNumber.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun ExpandedMonthGrid(
    baseOffset: Int,
    selectedOffset: Int,
    tasksPerDate: Map<Int, Int>,
    onSelectOffset: (Int) -> Unit
) {
    // Build a grid for the current month derived from baseOffset
    val baseCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, baseOffset) }
    val year = baseCal.get(Calendar.YEAR)
    val month = baseCal.get(Calendar.MONTH)
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingBlanks = (firstDayOfWeek - cal.firstDayOfWeek + 7) % 7
    val totalCells = leadingBlanks + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(rows) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    if (cellIndex < leadingBlanks || cellIndex >= totalCells) {
                        Box(modifier = Modifier.weight(1f).height(40.dp)) {}
                    } else {
                        val day = cellIndex - leadingBlanks + 1
                        val cellCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                        }
                        val offset = daysBetweenToday(cellCal)
                        val isSelected = offset == selectedOffset
                        val count = tasksPerDate[offset] ?: 0
                        val alpha = when {
                            count == 0 -> 1f
                            count >= 5 -> 0.55f
                            else -> 1f - (count * 0.1f)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
                                )
                                .clickableNoRipple { onSelectOffset(offset) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day.toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(selected: TodayFilter, onSelect: (TodayFilter) -> Unit) {
    val filters = listOf(TodayFilter.ALL, TodayFilter.TODO, TodayFilter.IN_PROGRESS, TodayFilter.COMPLETED)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters) { f ->
            val label = when (f) {
                TodayFilter.ALL -> "All"
                TodayFilter.TODO -> "To Do"
                TodayFilter.IN_PROGRESS -> "In Progress"
                TodayFilter.COMPLETED -> "Completed"
            }
            AssistChip(
                onClick = { onSelect(f) },
                label = { Text(label) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected == f) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    labelColor = if (selected == f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun TaskListSection(tasks: List<TodayTask>, selectedOffset: Int, filter: TodayFilter) {
    val filtered = tasks
        .asSequence()
        .filter { it.dateOffsetDays == selectedOffset }
        .filter {
            when (filter) {
                TodayFilter.ALL -> true
                TodayFilter.TODO, TodayFilter.IN_PROGRESS, TodayFilter.COMPLETED -> it.status == filter
            }
        }
        .sortedBy { it.time24 }
        .toList()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filtered) { task ->
            TaskCard(task)
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    text = "No tasks",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TaskCard(task: TodayTask) {
    val statusLabel = when (task.status) {
        TodayFilter.TODO -> "To-Do"
        TodayFilter.IN_PROGRESS -> "In Progress"
        TodayFilter.COMPLETED -> "Done"
        TodayFilter.ALL -> "" // not used
    }
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Group icon placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = task.project.first().uppercase(), fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("${task.project}: ${task.subGroup}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatTime(task.time24), style = MaterialTheme.typography.labelMedium)
                Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- Helpers ---
private fun daysBetweenToday(target: Calendar): Int {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val other = (target.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diffMs = other.timeInMillis - today.timeInMillis
    return (diffMs / (24L * 60L * 60L * 1000L)).toInt()
}

private fun monthLabelForOffset(offset: Int): String {
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
    val fmt = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
    return fmt.format(cal.time)
}

private fun formatTime(time24: String): String {
    return try {
        val inFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val outFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        val d = inFmt.parse(time24)
        if (d != null) outFmt.format(d) else time24
    } catch (e: Exception) {
        time24
    }
}
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(
    role = Role.Button,
    indication = null,
    interactionSource = remember { MutableInteractionSource() }
) { onClick() }
