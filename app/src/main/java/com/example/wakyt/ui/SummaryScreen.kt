package com.example.wakyt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wakyt.data.AppRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    onBack: (() -> Unit)? = null
) {
    // Calendar state (moved from Today screen)
    var isExpanded by remember { mutableStateOf(false) }
    var selectedDateOffset by remember { mutableStateOf(0) }

    // Data for calendar marker counts
    val repoTasks by AppRepository.tasks.collectAsState(initial = emptyList())
    val tasksPerDate = remember(repoTasks) {
        repoTasks.groupBy { computeOffsetFromDateStr(it.date) }.mapValues { it.value.size }
    }

    // Expandable hierarchy state
    var expandedGroupId by remember { mutableStateOf<String?>(null) }
    var expandedProjectId by remember { mutableStateOf<String?>(null) }

    val groups by AppRepository.taskGroups.collectAsState(initial = emptyList())
    val projects by AppRepository.projects.collectAsState(initial = emptyList())
    val tasks by AppRepository.tasks.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Table of Contents") },
                actions = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
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
            // Calendar on top
            CalendarSection(
                isExpanded = isExpanded,
                selectedOffset = selectedDateOffset,
                tasksPerDate = tasksPerDate,
                onSelectOffset = { selectedDateOffset = it },
                onToggleExpand = { isExpanded = !isExpanded }
            )

            Spacer(Modifier.height(12.dp))

            // Hierarchy: Groups -> Projects -> Tasks
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups) { g ->
                    val isGroupExpanded = expandedGroupId == g.id
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                expandedGroupId = if (isGroupExpanded) null else g.id
                                if (!isGroupExpanded) expandedProjectId = null
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(g.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (isGroupExpanded) {
                                val groupProjects = projects.filter { it.groupId == g.id }
                                groupProjects.forEach { p ->
                                    val isProjectExpanded = expandedProjectId == p.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                expandedProjectId = if (isProjectExpanded) null else p.id
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(p.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                        AssistChip(
                                            onClick = { expandedProjectId = if (isProjectExpanded) null else p.id },
                                            label = { Text(if (isProjectExpanded) "Hide" else "Show tasks") },
                                            colors = AssistChipDefaults.assistChipColors()
                                        )
                                    }
                                    if (isProjectExpanded) {
                                        val pTasks = tasks.filter { it.projectId == p.id }
                                            .sortedWith(compareBy({ it.date ?: "" }, { it.time ?: "" }, { it.name }))
                                        Column(modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)) {
                                            pTasks.forEach { t ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = (t.time ?: ""), modifier = Modifier.width(64.dp))
                                                    Text(text = t.name)
                                                }
                                            }
                                            if (pTasks.isEmpty()) {
                                                Text("No tasks", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- Calendar parts copied from TodayTasksScreen (kept private here) ----

@Composable
private fun CalendarSection(
    isExpanded: Boolean,
    selectedOffset: Int,
    tasksPerDate: Map<Int, Int>,
    onSelectOffset: (Int) -> Unit,
    onToggleExpand: () -> Unit
) {
    val monthLabel = monthLabelForOffset(selectedOffset)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(isExpanded) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount: Float ->
                        if (dragAmount > 12f) {
                            if (!isExpanded) onToggleExpand()
                        } else if (dragAmount < -12f) {
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
    val today = Calendar.getInstance()
    val fmtDay = SimpleDateFormat("EEE", Locale.getDefault())
    val dayStart = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -3) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        for (i in -3..3) {
            val cal = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
            val dayOfWeek = fmtDay.format(cal.time).take(3)
            val dayNumber = cal.get(Calendar.DAY_OF_MONTH)
            val selected = i == selectedOffset
            val count = tasksPerDate[i] ?: 0
            DateChip(dayOfWeek, dayNumber, selected, alpha = if (count > 0) 1f else 0.4f) {
                onSelectOffset(i)
            }
        }
    }
}

@Composable
private fun DateChip(
    dayOfWeek: String,
    dayNumber: Int,
    selected: Boolean,
    alpha: Float,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg.copy(alpha = alpha))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(dayOfWeek.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = fg)
        Text("$dayNumber", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun ExpandedMonthGrid(
    baseOffset: Int,
    selectedOffset: Int,
    tasksPerDate: Map<Int, Int>,
    onSelectOffset: (Int) -> Unit
) {
    val base = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, baseOffset) }
    val monthStart = (base.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    // Determine first day cell (start of week)
    val firstDayOfWeek = monthStart.firstDayOfWeek
    while (monthStart.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) monthStart.add(Calendar.DAY_OF_MONTH, -1)
    val grid = (0 until 42).map { (monthStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, it) } }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0 until 6) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val idx = row * 7 + col
                    val cal = grid[idx]
                    val offset = daysBetweenToday(cal)
                    val inMonth = cal.get(Calendar.MONTH) == base.get(Calendar.MONTH)
                    val selected = offset == selectedOffset
                    val count = tasksPerDate[offset] ?: 0
                    val alpha = if (inMonth) 1f else 0.35f
                    DateChip(
                        dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time).take(3),
                        dayNumber = cal.get(Calendar.DAY_OF_MONTH),
                        selected = selected,
                        alpha = if (count > 0) alpha else alpha * 0.6f,
                        onClick = { onSelectOffset(offset) }
                    )
                }
            }
        }
    }
}

private fun daysBetweenToday(target: Calendar): Int {
    val t = (target.clone() as Calendar).apply {
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
    val diff = t.timeInMillis - today.timeInMillis
    return (diff / (24 * 60 * 60 * 1000)).toInt()
}

private fun monthLabelForOffset(offset: Int): String {
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, offset) }
    val fmt = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
    return fmt.format(cal.time)
}

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
    return daysBetweenToday(target)
}
