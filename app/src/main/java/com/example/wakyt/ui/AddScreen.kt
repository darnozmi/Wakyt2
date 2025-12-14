package com.example.wakyt.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class AddMode { TASK, PROJECT }

private enum class Repeat { NONE, DAILY, WEEKLY, CUSTOM }

private data class TaskGroupItem(
    val id: String,
    val name: String,
    val iconLabel: String // simple 1-letter placeholder shown in a circle
)

private data class ProjectItem(
    val id: String,
    val name: String,
    val groupId: String?,
    val start: Calendar,
    val end: Calendar
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen() {
    // Default mode is Add Task
    var mode by remember { mutableStateOf(AddMode.TASK) }

    // In-memory groups and projects to support selection/validation
    val groups = remember {
        mutableStateListOf(
            TaskGroupItem("g1", "Work", "W"),
            TaskGroupItem("g2", "Personal", "P")
        )
    }
    val projects = remember {
        // Create one sample project within the current month
        val start = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        val end = (start.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }
        mutableStateListOf(
            ProjectItem("p1", "Android App", "g1", start, end)
        )
    }

    val title = when (mode) { AddMode.TASK -> "Add Task"; AddMode.PROJECT -> "Add Project" }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    // Mode switch icon – tapping toggles between modes
                    IconButton(onClick = { mode = if (mode == AddMode.TASK) AddMode.PROJECT else AddMode.TASK }) {
                        // Use a double arrow up/down style icon similar to Vercel (UnfoldMore)
                        Icon(Icons.Default.UnfoldMore, contentDescription = "Switch Mode")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: notifications/settings */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                },
                scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
            )
        }
        , containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (mode) {
                AddMode.TASK -> AddTaskForm(projects)
                AddMode.PROJECT -> AddProjectForm(groups, projects)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

// ---------------- Add Project ----------------
@Composable
private fun AddProjectForm(
    groups: MutableList<TaskGroupItem>,
    projects: MutableList<ProjectItem>,
) {
    val context = LocalContext.current

    // Task group select
    var selectedGroup by remember { mutableStateOf<TaskGroupItem?>(groups.firstOrNull()) }
    var isGroupDialogOpen by remember { mutableStateOf(false) }
    var isNewGroupDialogOpen by remember { mutableStateOf(false) }

    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }

    val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val today = remember { Calendar.getInstance() }
    var startDate by remember { mutableStateOf((today.clone() as Calendar)) }
    var endDate by remember { mutableStateOf((today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }) }

    var logoPlaceholder by remember { mutableStateOf("📁") }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    val pickLogoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        logoUri = uri
    }

    val isValid = projectName.isNotBlank() && !startDate.after(endDate)

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Task Group")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { isGroupDialogOpen = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) { Text(selectedGroup?.iconLabel ?: "?") }
                Text(selectedGroup?.name ?: "Select group")
            }

            if (isGroupDialogOpen) {
                AlertDialog(
                    onDismissRequest = { isGroupDialogOpen = false },
                    confirmButton = {},
                    title = { Text("Select Task Group") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Add new group icon/card
                            OutlinedButton(onClick = { isGroupDialogOpen = false; isNewGroupDialogOpen = true }) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Task Group")
                                    Text("Add a New Task Group")
                                }
                            }
                            // Horizontal scroll of existing groups
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(groups) { g ->
                                    OutlinedButton(onClick = { selectedGroup = g; isGroupDialogOpen = false }) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) { Text(g.iconLabel) }
                                            Text(g.name)
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }

            if (isNewGroupDialogOpen) {
                var newGroupName by remember { mutableStateOf("") }
                var selectedIcon by remember { mutableStateOf("G") }
                AlertDialog(
                    onDismissRequest = { isNewGroupDialogOpen = false },
                    confirmButton = {
                        TextButton(onClick = {
                            if (newGroupName.isNotBlank()) {
                                val item = TaskGroupItem("g" + (groups.size + 1), newGroupName, selectedIcon)
                                groups.add(0, item)
                                selectedGroup = item
                                isNewGroupDialogOpen = false
                            }
                        }) { Text("Create") }
                    },
                    dismissButton = { TextButton(onClick = { isNewGroupDialogOpen = false }) { Text("Cancel") } },
                    title = { Text("New Task Group") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Simple icon picker: a few letters
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("G", "W", "P", "S", "T").forEach { label ->
                                    OutlinedButton(onClick = { selectedIcon = label }) { Text(label) }
                                }
                            }
                            OutlinedTextField(
                                value = newGroupName,
                                onValueChange = { newGroupName = it },
                                label = { Text("Task group name") },
                                singleLine = true
                            )
                        }
                    }
                )
            }

            SectionHeader("Project Name")
            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text("Project Name") },
                singleLine = true
            )

            SectionHeader("Project Description")
            OutlinedTextField(
                value = projectDescription,
                onValueChange = { projectDescription = it },
                label = { Text("Project Description") },
                minLines = 3
            )

            SectionHeader("Project Duration")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                DateField(label = "Start Date", date = startDate, onPick = { picked -> startDate = picked })
                DateField(label = "End Date", date = endDate, onPick = { picked -> endDate = picked })
            }
            if (startDate.after(endDate)) {
                Text("Start date must be before or equal to End date", color = MaterialTheme.colorScheme.error)
            }

            SectionHeader("Project Logo")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (logoUri != null) {
                    AsyncImage(
                        model = logoUri,
                        contentDescription = "Project logo",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                } else {
                    Text(
                        logoPlaceholder,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    )
                }
                OutlinedButton(onClick = { pickLogoLauncher.launch("image/*") }) { Text("Change Logo") }
            }

            Button(
                onClick = {
                    // Save into in-memory projects list
                    val item = ProjectItem(
                        id = "p" + (projects.size + 1),
                        name = projectName.trim(),
                        groupId = selectedGroup?.id,
                        start = (startDate.clone() as Calendar),
                        end = (endDate.clone() as Calendar)
                    )
                    projects.add(0, item)
                    // Optional: clear fields
                    projectName = ""
                    projectDescription = ""
                },
                enabled = isValid
            ) { Text("Add Project") }
        }
    }
}

@Composable
private fun DateField(label: String, date: Calendar, onPick: (Calendar) -> Unit) {
    val ctx = LocalContext.current
    val fmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    OutlinedButton(onClick = {
        val y = date.get(Calendar.YEAR)
        val m = date.get(Calendar.MONTH)
        val d = date.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(ctx, { _, yy, mm, dd ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, yy)
                set(Calendar.MONTH, mm)
                set(Calendar.DAY_OF_MONTH, dd)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onPick(cal)
        }, y, m, d).show()
    }) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(fmt.format(date.time), fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------------- Add Task ----------------
@Composable
private fun AddTaskForm(projects: List<ProjectItem>) {
    val context = LocalContext.current
    var taskName by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }

    var date by remember { mutableStateOf(Calendar.getInstance()) }
    var timeHour by remember { mutableStateOf(9) }
    var timeMinute by remember { mutableStateOf(0) }

    var repeat by remember { mutableStateOf(Repeat.NONE) }
    // Custom days of week selection (Mon..Sun)
    val daysOfWeek = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
    val selectedDays = remember { mutableStateListOf<Int>() } // 0..6

    var notificationsEnabled by remember { mutableStateOf(true) }

    // Assign to Project (optional)
    var assignExpanded by remember { mutableStateOf(false) }
    var assignedProject by remember { mutableStateOf<ProjectItem?>(null) }

    // Validation against project dates
    val dateValid: Boolean = assignedProject?.let { p ->
        val allDates: List<Calendar> = when (repeat) {
            Repeat.NONE -> listOf(date)
            Repeat.DAILY -> listOf(date) // simplified: just check first occurrence
            Repeat.WEEKLY -> listOf(date)
            Repeat.CUSTOM -> listOf(date) // Validation simplified; actual recurrence not generated here
        }
        allDates.all { !it.before(p.start) && !it.after(p.end) }
    } ?: true

    val isValid = taskName.isNotBlank() && dateValid

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Task Name")
            OutlinedTextField(
                value = taskName,
                onValueChange = { taskName = it },
                label = { Text("Task Name") },
                singleLine = true
            )

            SectionHeader("Task Description")
            OutlinedTextField(
                value = taskDescription,
                onValueChange = { taskDescription = it },
                label = { Text("Task Description") },
                minLines = 3
            )

            SectionHeader("Date & Time")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(label = "Date", date = date, onPick = { date = it })
                OutlinedButton(onClick = {
                    TimePickerDialog(context, { _, h, m ->
                        timeHour = h; timeMinute = m
                    }, timeHour, timeMinute, false).show()
                }) {
                    Column { Text("Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(formatTime(timeHour, timeMinute), fontWeight = FontWeight.SemiBold) }
                }
            }

            SectionHeader("Repeat")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatChip("None", repeat == Repeat.NONE) { repeat = Repeat.NONE }
                RepeatChip("Every day", repeat == Repeat.DAILY) { repeat = Repeat.DAILY }
                RepeatChip("Once a week", repeat == Repeat.WEEKLY) { repeat = Repeat.WEEKLY }
                RepeatChip("Custom", repeat == Repeat.CUSTOM) { repeat = Repeat.CUSTOM }
            }
            if (repeat == Repeat.CUSTOM) {
                // Show selectable days of week horizontally
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(daysOfWeek.size) { index ->
                        val selected = selectedDays.contains(index)
                        OutlinedButton(onClick = {
                            if (selected) selectedDays.remove(index) else selectedDays.add(index)
                        }) {
                            Text(daysOfWeek[index])
                        }
                    }
                }
                if (selectedDays.isNotEmpty()) {
                    val label = selectedDays.sorted().joinToString { daysOfWeek[it] }
                    Text("Repeats on: $label", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SectionHeader("Notifications")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enable notifications")
                Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
            }

            SectionHeader("Assign to Project (Optional)")
            Box {
                OutlinedButton(onClick = { assignExpanded = true }) { Text(assignedProject?.name ?: "None") }
                DropdownMenu(expanded = assignExpanded, onDismissRequest = { assignExpanded = false }) {
                    DropdownMenuItem(text = { Text("None") }, onClick = { assignedProject = null; assignExpanded = false })
                    projects.forEach { p ->
                        DropdownMenuItem(text = { Text(p.name) }, onClick = { assignedProject = p; assignExpanded = false })
                    }
                }
            }

            if (!dateValid && assignedProject != null) {
                Text(
                    "This task cannot be scheduled within the selected project timeframe.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(onClick = { /* TODO: create Task */ }, enabled = isValid) { Text("Add Task") }
        }
    }
}

@Composable
private fun RepeatChip(text: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier,
        border = if (selected) null else null
    ) { Text(text) }
}

private fun sameDay(a: Calendar, b: Calendar): Boolean {
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

private fun formatTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
}
