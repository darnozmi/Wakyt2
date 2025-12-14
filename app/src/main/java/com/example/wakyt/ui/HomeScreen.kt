package com.example.wakyt.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wakyt.R
import androidx.compose.runtime.collectAsState
import com.example.wakyt.data.AppRepository
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// --- UI Models (mapped from repository data) ---
enum class TaskStatus { DONE, FAILED, PLANNED }

data class Task(
    val id: String,
    val title: String,
    val time: String, // simple label like "09:00"
    val dateOffsetDays: Int, // 0 = today, positive = future days, negative = past days
    val status: TaskStatus
)

data class Project(
    val id: String,
    val iconRes: Int?,
    val groupName: String,
    val projectName: String,
    val tasks: List<Task>
)

data class TaskGroup(
    val id: String,
    val iconRes: Int?,
    val name: String,
    val tasks: List<Task>
)

enum class ProgressPeriod { TODAY, WEEK, MONTH }

// --- Date helpers (API 24 compatible) ---
private fun calendarToday(): Calendar = Calendar.getInstance()

private fun isInToday(offset: Int): Boolean = offset == 0

private fun isInCurrentWeek(offset: Int): Boolean {
    // Consider current week as offsets from -6..0..+6 around today
    return offset in -6..6
}

private fun isInCurrentMonth(offset: Int): Boolean {
    // Build two calendars and compare year/month
    val cal = calendarToday()
    val todayYear = cal.get(Calendar.YEAR)
    val todayMonth = cal.get(Calendar.MONTH)
    val other = calendarToday().apply { add(Calendar.DAY_OF_YEAR, offset) }
    return other.get(Calendar.YEAR) == todayYear && other.get(Calendar.MONTH) == todayMonth
}

private fun currentWeekRangeLabel(): String {
    val cal = calendarToday()
    val firstDayOfWeek = cal.firstDayOfWeek
    // Move to start of week
    while (cal.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) cal.add(Calendar.DAY_OF_MONTH, -1)
    val start = cal.clone() as Calendar
    val end = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
    val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
    return "${fmt.format(start.time)}–${fmt.format(end.time)}"
}

private fun currentMonthLabel(): String {
    val fmt = SimpleDateFormat("LLLL", Locale.getDefault())
    return fmt.format(calendarToday().time)
}

// --- Sample data ---
private fun computeDateOffsetDays(dateStr: String?): Int {
    if (dateStr.isNullOrBlank()) return 0
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance().apply {
            time = fmt.parse(dateStr)!!
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = calendarToday()
        val diffMillis = cal.timeInMillis - today.timeInMillis
        (diffMillis / (24 * 60 * 60 * 1000)).toInt()
    } catch (e: ParseException) {
        0
    }
}

//

// --- Home Screen ---
@Composable
fun HomeScreen(
    userName: String,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onViewTasks: (ProgressPeriod) -> Unit,
    onAddTask: (Project) -> Unit,
    onOpenTaskGroup: (TaskGroup) -> Unit,
) {
    // Observe repository flows
    val repoProjects by AppRepository.projects.collectAsState(initial = emptyList())
    val repoGroups by AppRepository.taskGroups.collectAsState(initial = emptyList())
    val repoTasks by AppRepository.tasks.collectAsState(initial = emptyList())

    // Map repository models to UI models used in this screen
    val projects: List<Project> = remember(repoProjects, repoGroups, repoTasks) {
        repoProjects.map { p ->
            val groupName = repoGroups.firstOrNull { it.id == p.groupId }?.name ?: ""
            val ptasks = repoTasks.filter { it.projectId == p.id }.map { t ->
                Task(
                    id = t.id,
                    title = t.name,
                    time = t.time ?: "",
                    dateOffsetDays = computeDateOffsetDays(t.date),
                    status = TaskStatus.PLANNED
                )
            }
            Project(
                id = p.id,
                iconRes = null,
                groupName = if (groupName.isBlank()) "Project" else groupName,
                projectName = p.name,
                tasks = ptasks
            )
        }
    }
    val groups: List<TaskGroup> = remember(repoProjects, repoGroups, repoTasks) {
        repoGroups.map { g ->
            val gProjectIds = repoProjects.filter { it.groupId == g.id }.map { it.id }.toSet()
            val gtasks = repoTasks.filter { it.projectId == null || gProjectIds.contains(it.projectId) }.map { t ->
                Task(
                    id = t.id,
                    title = t.name,
                    time = t.time ?: "",
                    dateOffsetDays = computeDateOffsetDays(t.date),
                    status = TaskStatus.PLANNED
                )
            }
            TaskGroup(
                id = g.id,
                iconRes = null,
                name = g.name,
                tasks = gtasks
            )
        }
    }
    // Keep runtime task status updates so progress indicators react dynamically
    val statusOverrides = remember { mutableStateMapOf<String, TaskStatus>() }
    var period by remember { mutableStateOf(ProgressPeriod.TODAY) }
    var expandedProjectId by remember { mutableStateOf<String?>(null) }

    val periodTitle: String = when (period) {
        ProgressPeriod.TODAY -> "Your today’s task almost done!"
        ProgressPeriod.WEEK -> "Past week progress"
        ProgressPeriod.MONTH -> "${currentMonthLabel()} progress"
    }
    val periodSubtitle: String? = when (period) {
        ProgressPeriod.TODAY -> null
        ProgressPeriod.WEEK -> currentWeekRangeLabel()
        ProgressPeriod.MONTH -> null
    }

    val periodFilter: (Int) -> Boolean = when (period) {
        ProgressPeriod.TODAY -> { offset -> isInToday(offset) }
        ProgressPeriod.WEEK -> { offset -> isInCurrentWeek(offset) }
        ProgressPeriod.MONTH -> { offset -> isInCurrentMonth(offset) }
    }

    val allPeriodTasks = remember(period, statusOverrides) {
        projects.flatMap { it.tasks }
            .map { t -> if (statusOverrides.containsKey(t.id)) t.copy(status = statusOverrides[t.id]!!) else t }
            .filter { periodFilter(it.dateOffsetDays) }
    }
    val doneCount = allPeriodTasks.count { it.status == TaskStatus.DONE }
    val totalCount = allPeriodTasks.size.coerceAtLeast(1)
    val progress = doneCount.toFloat() / totalCount.toFloat()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileHeader(
                userName = userName,
                onOpenProfile = onOpenProfile,
                onOpenSettings = onOpenSettings
            )
        }

        item {
            ProgressOverviewCard(
                title = periodTitle,
                subtitle = periodSubtitle,
                progress = progress,
                period = period,
                onChangePeriod = { period = it },
                onViewTasks = { onViewTasks(period) }
            )
        }

        // In Progress Section
        item {
            val activeProjects = projects.map { p ->
                val ptasks = p.tasks
                    .map { t -> if (statusOverrides.containsKey(t.id)) t.copy(status = statusOverrides[t.id]!!) else t }
                    .filter { periodFilter(it.dateOffsetDays) }
                val d = ptasks.count { it.status == TaskStatus.DONE }
                val t = ptasks.size.coerceAtLeast(1)
                val pr = d.toFloat() / t.toFloat()
                Triple(p, ptasks, pr)
            }.filter { it.second.isNotEmpty() }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "In Progress (${activeProjects.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(activeProjects) { (project, _, pr) ->
                        ProjectCard(
                            project = project,
                            progress = pr,
                            isSelected = project.id == expandedProjectId,
                        ) {
                            expandedProjectId = if (expandedProjectId == project.id) null else project.id
                        }
                    }
                }
            }
        }

        // Expanded task list for selected project
        item {
            val project = projects.find { it.id == expandedProjectId }
            if (project != null) {
                val ptasks = project.tasks
                    .map { t -> if (statusOverrides.containsKey(t.id)) t.copy(status = statusOverrides[t.id]!!) else t }
                    .filter { periodFilter(it.dateOffsetDays) }
                    .sortedBy { it.time }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${project.groupName} · ${project.projectName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    ptasks.forEach { task ->
                        TaskRow(
                            task = task,
                            onMarkDone = { statusOverrides[task.id] = TaskStatus.DONE },
                            onAddTask = { onAddTask(project) }
                        )
                    }
                }
            }
        }

        // Task Groups section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Task Groups",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.forEach { group ->
                        val periodTasks = group.tasks
                            .map { t -> if (statusOverrides.containsKey(t.id)) t.copy(status = statusOverrides[t.id]!!) else t }
                            .filter { periodFilter(it.dateOffsetDays) }
                        val d = periodTasks.count { it.status == TaskStatus.DONE }
                        val t = periodTasks.size.coerceAtLeast(1)
                        val pr = d.toFloat() / t.toFloat()
                        TaskGroupRow(group, pr) { onOpenTaskGroup(group) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ProfileHeader(
    userName: String,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable { onOpenProfile() },
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Hello!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun ProgressOverviewCard(
    title: String,
    subtitle: String?,
    progress: Float,
    period: ProgressPeriod,
    onChangePeriod: (ProgressPeriod) -> Unit,
    onViewTasks: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onViewTasks) { Text("View Task") }
            }
            Spacer(Modifier.size(12.dp))
            Box(contentAlignment = Alignment.TopEnd) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = progress, strokeWidth = 8.dp, modifier = Modifier.size(72.dp))
                    Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Today Progress") }, onClick = { onChangePeriod(ProgressPeriod.TODAY); menuExpanded = false })
                    DropdownMenuItem(text = { Text("Weekly Progress") }, onClick = { onChangePeriod(ProgressPeriod.WEEK); menuExpanded = false })
                    DropdownMenuItem(text = { Text("Monthly Progress") }, onClick = { onChangePeriod(ProgressPeriod.MONTH); menuExpanded = false })
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    progress: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    ElevatedCard(
        onClick = onClick,
        shape = shape,
        modifier = Modifier.size(width = 220.dp, height = 120.dp),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(project.groupName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(project.projectName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onMarkDone: () -> Unit,
    onAddTask: () -> Unit
) {
    val bg = when (task.status) {
        TaskStatus.DONE -> MaterialTheme.colorScheme.secondaryContainer
        TaskStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        TaskStatus.PLANNED -> Color.Transparent
    }
    val content = when (task.status) {
        TaskStatus.DONE -> MaterialTheme.colorScheme.onSecondaryContainer
        TaskStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
        TaskStatus.PLANNED -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(task.time, fontWeight = FontWeight.SemiBold, color = content)
        Text(task.title, modifier = Modifier.weight(1f), color = content)
        if (task.status == TaskStatus.FAILED) {
            OutlinedButton(onClick = onMarkDone) { Text("✔") }
            Button(onClick = onAddTask) { Text("＋") }
        }
    }
}

@Composable
private fun TaskGroupRow(group: TaskGroup, progress: Float, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Placeholder circle icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(group.name.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("${group.tasks.size} tasks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = progress, strokeWidth = 6.dp, modifier = Modifier.size(48.dp))
            Text("${(progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
