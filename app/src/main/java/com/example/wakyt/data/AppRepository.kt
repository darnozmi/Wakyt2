package com.example.wakyt.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

// Domain models kept minimal to match current UI needs
data class TaskGroup(
    val id: String,
    val name: String,
    val iconLabel: String // simple 1-letter placeholder shown in a circle
)

data class Project(
    val id: String,
    val name: String,
    val groupId: String?,
    val start: Calendar,
    val end: Calendar,
    val logoUri: Uri? = null
)

enum class TaskState { TODO, IN_PROGRESS, DONE }

data class Task(
    val id: String,
    val name: String,
    val projectId: String?,
    val date: String?, // yyyy-MM-dd (optional for now)
    val time: String?,  // HH:mm (optional for now)
    val status: TaskState = TaskState.TODO
)

data class UserProfile(
    val name: String,
    val email: String,
    val photoUri: Uri? = null,
)

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 10,
    val theme: String = "light", // light|dark (simple placeholder)
    val language: String = "en"
)

/**
 * Single Source of Truth for Task Groups, Projects, and Tasks.
 * Simple JSON-persistent implementation with StateFlow so UI across screens updates immediately.
 */
object AppRepository {
    // Persistence
    private const val FILE_NAME = "wakyt_repository.json"
    private var appContext: Context? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // State
    private val _taskGroups = MutableStateFlow<List<TaskGroup>>(emptyList())
    val taskGroups: StateFlow<List<TaskGroup>> = _taskGroups.asStateFlow()

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    // Profile/settings/session
    private val _userProfile = MutableStateFlow(UserProfile(name = "John Doe", email = "john@example.com", photoUri = null))
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Simple id generator (persisted)
    private var counter: Long = System.currentTimeMillis()
    private fun nextId(prefix: String) = "$prefix${counter++}"

    /** Must be called once at app start to load persisted data. */
    fun init(context: Context) {
        if (appContext != null) return // already initialized
        appContext = context.applicationContext
        // Load from disk; if none, seed defaults and save
        ioScope.launch {
            val file = File(appContext!!.filesDir, FILE_NAME)
            if (file.exists()) {
                runCatching { loadFromFile(file) }.onFailure { /* fallback to seed */ seedDefaultsAndSave() }
            } else {
                seedDefaultsAndSave()
            }
        }
    }

    // CRUD — add methods used by Add screens
    fun addTaskGroup(name: String, iconLabel: String = name.firstOrNull()?.uppercase() ?: "G"): TaskGroup {
        val new = TaskGroup(id = nextId("g"), name = name, iconLabel = iconLabel)
        _taskGroups.value = _taskGroups.value + new
        persistAsync()
        return new
    }

    fun addProject(
        name: String,
        groupId: String?,
        start: Calendar,
        end: Calendar,
        logoUri: Uri? = null
    ): Project {
        // Ensure group exists if provided
        if (groupId != null && _taskGroups.value.none { it.id == groupId }) {
            throw IllegalArgumentException("Group $groupId does not exist")
        }
        val new = Project(id = nextId("p"), name = name, groupId = groupId, start = start, end = end, logoUri = logoUri)
        _projects.value = _projects.value + new
        persistAsync()
        return new
    }

    fun addTask(
        name: String,
        projectId: String?,
        date: String?,
        time: String?
    ): Task {
        if (projectId != null && _projects.value.none { it.id == projectId }) {
            throw IllegalArgumentException("Project $projectId does not exist")
        }
        val new = Task(id = nextId("t"), name = name, projectId = projectId, date = date, time = time, status = TaskState.TODO)
        _tasks.value = _tasks.value + new
        persistAsync()
        return new
    }

    fun updateTaskStatus(taskId: String, status: TaskState) {
        val updated = _tasks.value.map { if (it.id == taskId) it.copy(status = status) else it }
        _tasks.value = updated
        persistAsync()
    }

    fun updateProfile(name: String? = null, email: String? = null, photoUri: Uri? = null) {
        val curr = _userProfile.value
        _userProfile.value = curr.copy(
            name = name ?: curr.name,
            email = email ?: curr.email,
            photoUri = photoUri ?: curr.photoUri
        )
        persistAsync()
    }

    fun updateSettings(
        notificationsEnabled: Boolean? = null,
        reminderMinutesBefore: Int? = null,
        theme: String? = null,
        language: String? = null,
    ) {
        val curr = _settings.value
        _settings.value = curr.copy(
            notificationsEnabled = notificationsEnabled ?: curr.notificationsEnabled,
            reminderMinutesBefore = reminderMinutesBefore ?: curr.reminderMinutesBefore,
            theme = theme ?: curr.theme,
            language = language ?: curr.language,
        )
        persistAsync()
    }

    fun setLoggedIn(value: Boolean) {
        _isLoggedIn.value = value
        persistAsync()
    }

    // ---------------- Persistence helpers ----------------
    private fun seedDefaultsAndSave() {
        val work = TaskGroup(id = "g1", name = "Work", iconLabel = "W")
        val personal = TaskGroup(id = "g2", name = "Personal", iconLabel = "P")
        _taskGroups.value = listOf(work, personal)

        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val end = (start.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val p1 = Project(id = "p1", name = "Android App", groupId = work.id, start = start, end = end)
        _projects.value = listOf(p1)
        _tasks.value = emptyList()
        _userProfile.value = UserProfile(name = "John Doe", email = "john@example.com", photoUri = null)
        _settings.value = AppSettings()
        _isLoggedIn.value = true
        persistAsync()
    }

    private fun persistAsync() {
        val ctx = appContext ?: return
        val snapshotGroups = _taskGroups.value
        val snapshotProjects = _projects.value
        val snapshotTasks = _tasks.value
        val currentCounter = counter
        ioScope.launch {
            val file = File(ctx.filesDir, FILE_NAME)
            val root = JSONObject().apply {
                put("counter", currentCounter)
                put("isLoggedIn", _isLoggedIn.value)
                put("groups", JSONArray().apply {
                    snapshotGroups.forEach { g ->
                        put(JSONObject().apply {
                            put("id", g.id)
                            put("name", g.name)
                            put("iconLabel", g.iconLabel)
                        })
                    }
                })
                put("projects", JSONArray().apply {
                    snapshotProjects.forEach { p ->
                        put(JSONObject().apply {
                            put("id", p.id)
                            put("name", p.name)
                            put("groupId", p.groupId)
                            put("start", p.start.timeInMillis)
                            put("end", p.end.timeInMillis)
                            put("logoUri", p.logoUri?.toString())
                        })
                    }
                })
                put("tasks", JSONArray().apply {
                    snapshotTasks.forEach { t ->
                        put(JSONObject().apply {
                            put("id", t.id)
                            put("name", t.name)
                            put("projectId", t.projectId)
                            put("date", t.date)
                            put("time", t.time)
                            put("status", t.status.name)
                        })
                    }
                })
                put("profile", JSONObject().apply {
                    val up = _userProfile.value
                    put("name", up.name)
                    put("email", up.email)
                    put("photoUri", up.photoUri?.toString())
                })
                put("settings", JSONObject().apply {
                    val s = _settings.value
                    put("notificationsEnabled", s.notificationsEnabled)
                    put("reminderMinutesBefore", s.reminderMinutesBefore)
                    put("theme", s.theme)
                    put("language", s.language)
                })
            }
            file.writeText(root.toString())
        }
    }

    private fun loadFromFile(file: File) {
        val text = file.readText()
        val root = JSONObject(text)
        counter = root.optLong("counter", System.currentTimeMillis())
        _isLoggedIn.value = root.optBoolean("isLoggedIn", true)
        val groupsJson = root.optJSONArray("groups") ?: JSONArray()
        val projectsJson = root.optJSONArray("projects") ?: JSONArray()
        val tasksJson = root.optJSONArray("tasks") ?: JSONArray()

        val loadedGroups = mutableListOf<TaskGroup>()
        for (i in 0 until groupsJson.length()) {
            val o = groupsJson.getJSONObject(i)
            loadedGroups.add(
                TaskGroup(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    iconLabel = o.optString("iconLabel", "G")
                )
            )
        }

        val loadedProjects = mutableListOf<Project>()
        for (i in 0 until projectsJson.length()) {
            val o = projectsJson.getJSONObject(i)
            val startMillis = o.getLong("start")
            val endMillis = o.getLong("end")
            loadedProjects.add(
                Project(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    groupId = if (o.isNull("groupId")) null else o.getString("groupId"),
                    start = Calendar.getInstance().apply { timeInMillis = startMillis },
                    end = Calendar.getInstance().apply { timeInMillis = endMillis },
                    logoUri = o.optString("logoUri", null)?.let { if (it.isBlank()) null else Uri.parse(it) }
                )
            )
        }

        val loadedTasks = mutableListOf<Task>()
        for (i in 0 until tasksJson.length()) {
            val o = tasksJson.getJSONObject(i)
            loadedTasks.add(
                Task(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    projectId = if (o.isNull("projectId")) null else o.getString("projectId"),
                    date = o.optString("date", null).let { if (it.isNullOrBlank()) null else it },
                    time = o.optString("time", null).let { if (it.isNullOrBlank()) null else it },
                    status = runCatching { TaskState.valueOf(o.optString("status", TaskState.TODO.name)) }.getOrDefault(TaskState.TODO)
                )
            )
        }

        _taskGroups.value = loadedGroups
        _projects.value = loadedProjects
        _tasks.value = loadedTasks

        // Profile & settings
        root.optJSONObject("profile")?.let { p ->
            _userProfile.value = UserProfile(
                name = p.optString("name", "John Doe"),
                email = p.optString("email", "john@example.com"),
                photoUri = p.optString("photoUri", null)?.let { if (it.isBlank()) null else Uri.parse(it) }
            )
        }
        root.optJSONObject("settings")?.let { s ->
            _settings.value = AppSettings(
                notificationsEnabled = s.optBoolean("notificationsEnabled", true),
                reminderMinutesBefore = s.optInt("reminderMinutesBefore", 10),
                theme = s.optString("theme", "light"),
                language = s.optString("language", "en")
            )
        }
    }
}
