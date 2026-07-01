package com.example.mandatoryhours

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.mandatoryhours.ui.theme.MandatoryHoursTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val REQUIRED_HOURS = 60L

data class VolunteerEntry(
    val job: String,
    val startTime: Long,
    val endTime: Long? = null,
    val durationSeconds: Long = 0L
)

enum class AppScreen { MAIN, TABLE, PROFILE }

fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
}

fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkTheme by remember { mutableStateOf(false) }
            MandatoryHoursTheme(darkTheme = darkTheme) {
                MandatoryHoursApp(
                    darkTheme = darkTheme,
                    onDarkThemeChange = { darkTheme = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandatoryHoursApp(
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit
) {
    var selectedScreen by remember { mutableStateOf(AppScreen.MAIN) }
    val entries = remember { mutableStateListOf<VolunteerEntry>() }
    var currentJob by remember { mutableStateOf("") }
    var showJobDialog by remember { mutableStateOf(false) }
    var showManualEntryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var startButtonText by remember { mutableStateOf("Start") }
    var stopButtonText by remember { mutableStateOf("Stop") }
    var startButtonColor by remember { mutableStateOf(Color(0xFF4CAF50)) }
    var stopButtonColor by remember { mutableStateOf(Color(0xFFCC2F2F)) }
    var editingEntryIndex by remember { mutableStateOf<Int?>(null) }
    var editingJob by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var activeJob by remember { mutableStateOf<String?>(null) }
    var sessionStartTime by remember { mutableStateOf<Long?>(null) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(activeJob, sessionStartTime) {
        if (activeJob != null && sessionStartTime != null) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - sessionStartTime!!) / 1000).toInt()
                delay(1000)
            }
        }
    }

    val totalHours = entries.sumOf { it.durationSeconds } / 3600.0

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mandatory Hours") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedScreen == AppScreen.MAIN,
                    onClick = { selectedScreen = AppScreen.MAIN },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    label = { Text("Main") }
                )
                NavigationBarItem(
                    selected = selectedScreen == AppScreen.TABLE,
                    onClick = { selectedScreen = AppScreen.TABLE },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text("Table") }
                )
                NavigationBarItem(
                    selected = selectedScreen == AppScreen.PROFILE,
                    onClick = { selectedScreen = AppScreen.PROFILE },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedScreen) {
            AppScreen.MAIN -> MainScreen(
                modifier = Modifier.padding(innerPadding),
                activeJob = activeJob,
                elapsedSeconds = elapsedSeconds,
                startButtonText = startButtonText,
                stopButtonText = stopButtonText,
                startButtonColor = startButtonColor,
                stopButtonColor = stopButtonColor,
                onStartStopClick = {
                    if (activeJob == null) {
                        showJobDialog = true
                    } else {
                        val start = sessionStartTime ?: System.currentTimeMillis()
                        val end = System.currentTimeMillis()
                        val duration = ((end - start) / 1000).coerceAtLeast(0)
                        val lastOpen = entries.indexOfLast { it.endTime == null }
                        if (lastOpen >= 0) {
                            val updated = entries[lastOpen].copy(endTime = end, durationSeconds = duration)
                            entries[lastOpen] = updated
                        }
                        activeJob = null
                        sessionStartTime = null
                        elapsedSeconds = 0
                    }
                }
            )

            AppScreen.TABLE -> TableScreen(
                modifier = Modifier.padding(innerPadding),
                entries = entries,
                onAddEntry = { showManualEntryDialog = true },
                onEditEntry = { index ->
                    editingEntryIndex = index
                    editingJob = entries[index].job
                    showEditDialog = true
                },
                onDeleteEntry = { index ->
                    if (index in entries.indices) {
                        entries.removeAt(index)
                    }
                }
            )

            AppScreen.PROFILE -> ProfileScreen(
                modifier = Modifier.padding(innerPadding),
                totalHours = totalHours,
                requiredHours = REQUIRED_HOURS,
                darkTheme = darkTheme,
                onDarkThemeChange = onDarkThemeChange
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .zIndex(2f),
            contentAlignment = Alignment.TopEnd
        ) {
            FloatingActionButton(
                onClick = { showSettingsDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = 8.dp, end = 8.dp)
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        if (showJobDialog) {
            JobDialog(
                currentJob = currentJob,
                onJobChange = { currentJob = it },
                onDismiss = {
                    showJobDialog = false
                    currentJob = ""
                },
                onConfirm = {
                    if (currentJob.isNotBlank()) {
                        val startTime = System.currentTimeMillis()
                        entries.add(VolunteerEntry(job = currentJob.trim(), startTime = startTime))
                        activeJob = currentJob.trim()
                        sessionStartTime = startTime
                        elapsedSeconds = 0
                    }
                    showJobDialog = false
                    currentJob = ""
                }
            )
        }

        if (showEditDialog && editingEntryIndex != null) {
            EditEntryDialog(
                currentJob = editingJob,
                onJobChange = { editingJob = it },
                onDismiss = {
                    showEditDialog = false
                    editingEntryIndex = null
                    editingJob = ""
                },
                onConfirm = {
                    val index = editingEntryIndex
                    if (index != null && index in entries.indices && editingJob.isNotBlank()) {
                        entries[index] = entries[index].copy(job = editingJob.trim())
                    }
                    showEditDialog = false
                    editingEntryIndex = null
                    editingJob = ""
                }
            )
        }

        if (showManualEntryDialog) {
            ManualEntryDialog(
                currentJob = currentJob,
                onJobChange = { currentJob = it },
                onDismiss = {
                    showManualEntryDialog = false
                    currentJob = ""
                },
                onConfirm = {
                    if (currentJob.isNotBlank()) {
                        entries.add(
                            VolunteerEntry(
                                job = currentJob.trim(),
                                startTime = System.currentTimeMillis(),
                                durationSeconds = 0L
                            )
                        )
                    }
                    showManualEntryDialog = false
                    currentJob = ""
                }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                startButtonText = startButtonText,
                stopButtonText = stopButtonText,
                startButtonColor = startButtonColor,
                stopButtonColor = stopButtonColor,
                onStartTextChange = { startButtonText = it },
                onStopTextChange = { stopButtonText = it },
                onStartColorChange = { startButtonColor = it },
                onStopColorChange = { stopButtonColor = it },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    activeJob: String?,
    elapsedSeconds: Int,
    startButtonText: String,
    stopButtonText: String,
    startButtonColor: Color,
    stopButtonColor: Color,
    onStartStopClick: () -> Unit
) {
    val isActive = activeJob != null
    val baseColor = if (isActive) stopButtonColor else startButtonColor
    val waveColor = if (isActive) stopButtonColor.copy(alpha = 0.27f) else startButtonColor.copy(alpha = 0.27f)

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearOutSlowInEasing)
        ),
        label = "waveScale"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearOutSlowInEasing)
        ),
        label = "waveAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(340.dp)
                    .background(waveColor.copy(alpha = waveAlpha), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size((320 * waveScale).dp)
                    .background(waveColor.copy(alpha = waveAlpha * 0.6f), CircleShape)
            )
            Button(
                onClick = onStartStopClick,
                modifier = Modifier
                    .size(320.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = baseColor
                ),
                shape = RoundedCornerShape(100)
            ) {
                Text(
                    text = if (isActive) stopButtonText else startButtonText,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TableScreen(
    modifier: Modifier = Modifier,
    entries: List<VolunteerEntry>,
    onAddEntry: () -> Unit,
    onEditEntry: (Int) -> Unit,
    onDeleteEntry: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Volunteer table", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Each job is saved here for your bagrut hours.", modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            }
            Button(onClick = onAddEntry) {
                Text("Add")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Job", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Started", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Time", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("", modifier = Modifier.width(88.dp))
                }
                if (entries.isEmpty()) {
                    Text("No jobs added yet", modifier = Modifier.padding(top = 12.dp))
                } else {
                    LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                        itemsIndexed(entries) { index, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(entry.job, modifier = Modifier.weight(1f))
                                Text(formatTimestamp(entry.startTime), modifier = Modifier.weight(1f))
                                Text(formatDuration(entry.durationSeconds), modifier = Modifier.weight(1f))
                                Row(modifier = Modifier.width(88.dp), horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { onEditEntry(index) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { onDeleteEntry(index) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
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

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    totalHours: Double,
    requiredHours: Long,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Profile", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Progress to 60 hours", fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(Locale.getDefault(), "%.1f / %d hours", totalHours, requiredHours),
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                LinearProgressIndicator(
                    progress = { (totalHours / requiredHours).coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Appearance", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Switch between light and dark mode",
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DarkModeToggle(darkTheme = darkTheme, onDarkThemeChange = onDarkThemeChange)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Goal")
                Text(
                    text = "You need to complete 60 volunteer hours before your bagrut exam.",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun DarkModeToggle(darkTheme: Boolean, onDarkThemeChange: (Boolean) -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFF1F2937) else Color(0xFFE9EEF7),
        animationSpec = tween(300)
    )
    val thumbColor by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFFFFFFFF) else Color(0xFF1F2937),
        animationSpec = tween(300)
    )
    val iconTint by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFFFFD54F) else Color(0xFFFFD54F),
        animationSpec = tween(300)
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (darkTheme) 40.dp else 2.dp,
        animationSpec = tween(300)
    )

    Box(
        modifier = Modifier
            .width(92.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable { onDarkThemeChange(!darkTheme) }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(40.dp)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (darkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsDialog(
    startButtonText: String,
    stopButtonText: String,
    startButtonColor: Color,
    stopButtonColor: Color,
    onStartTextChange: (String) -> Unit,
    onStopTextChange: (String) -> Unit,
    onStartColorChange: (Color) -> Unit,
    onStopColorChange: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize button") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startButtonText,
                    onValueChange = onStartTextChange,
                    label = { Text("Start text") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = stopButtonText,
                    onValueChange = onStopTextChange,
                    label = { Text("Stop text") },
                    singleLine = true
                )
                ColorPickerRow("Start color", startButtonColor, onStartColorChange)
                ColorPickerRow("Stop color", stopButtonColor, onStopColorChange)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun ColorPickerRow(label: String, selectedColor: Color, onColorSelected: (Color) -> Unit) {
    Column {
        Text(label, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            listOf(Color(0xFF4CAF50), Color(0xFF2E7D32), Color(0xFF1976D2), Color(0xFFFF9800), Color(0xFFCC2F2F), Color(0xFF9C27B0)).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onColorSelected(color) }
                        .drawBehind {
                            if (selectedColor == color) {
                                drawCircle(color = Color.White, radius = size.minDimension / 2.2f)
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun JobDialog(
    currentJob: String,
    onJobChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What job are you starting?") },
        text = {
            OutlinedTextField(
                value = currentJob,
                onValueChange = onJobChange,
                label = { Text("Job name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ManualEntryDialog(
    currentJob: String,
    onJobChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add manual entry") },
        text = {
            OutlinedTextField(
                value = currentJob,
                onValueChange = onJobChange,
                label = { Text("Job name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditEntryDialog(
    currentJob: String,
    onJobChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit volunteer job") },
        text = {
            OutlinedTextField(
                value = currentJob,
                onValueChange = onJobChange,
                label = { Text("Job name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MandatoryHoursAppPreview() {
    MandatoryHoursTheme {
        MandatoryHoursApp(darkTheme = false, onDarkThemeChange = {})
    }
}