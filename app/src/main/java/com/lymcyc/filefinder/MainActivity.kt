package com.lymcyc.filefinder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.lymcyc.filefinder.data.AppDatabase
import com.lymcyc.filefinder.data.FileEntity
import com.lymcyc.filefinder.service.IndexService
import com.lymcyc.filefinder.ui.theme.FileFinderTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FileFinderTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SearchScreen(
                        onRequestPermission = ::requestStoragePermission,
                        onStartIndex = ::startIndexService
                    )
                }
            }
        }
        if (hasStoragePermission()) startIndexService()
    }

    private fun hasStoragePermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    private fun startIndexService() {
        startForegroundService(Intent(this, IndexService::class.java))
    }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun SearchScreen(
    onRequestPermission: () -> Unit,
    onStartIndex: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FileEntity>>(emptyList()) }
    val totalCount by db.fileDao().countFlow().collectAsState(initial = 0)

    val hasPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true
    }

    LaunchedEffect(Unit) {
        snapshotFlow { query }
            .debounce(150)
            .distinctUntilChanged()
            .collect { q ->
                results = if (q.isBlank()) emptyList()
                else db.fileDao().search("%${q.lowercase()}%")
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FileFinder") },
                actions = {
                    IconButton(onClick = onStartIndex) {
                        Icon(Icons.Default.Refresh, contentDescription = "重建索引")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()) {

            if (!hasPermission) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("需要文件访问权限", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("授予后将开始建立索引", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRequestPermission) { Text("去授权") }
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("文件名 / 拼音首字母（如 bgs）") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "已索引 $totalCount 个 · 命中 ${results.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results, key = { it.path }) { file ->
                    FileRow(file = file, onClick = { openFile(context, file) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun FileRow(file: FileEntity, onClick: () -> Unit) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (file.isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(
                file.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (!file.isDir) Text(formatSize(file.size), style = MaterialTheme.typography.bodySmall)
            Text(sdf.format(Date(file.modified)), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1fK".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1fM".format(mb)
    return "%.1fG".format(mb / 1024.0)
}

private fun openFile(context: Context, file: FileEntity) {
    val f = File(file.path)
    if (!f.exists()) return
    if (file.isDir) return
    val uri = try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
    } catch (e: Exception) {
        return
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeOf(file.name))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "打开").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun mimeOf(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "pdf" -> "application/pdf"
    "txt", "log", "md", "json", "xml", "csv" -> "text/plain"
    "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "image/*"
    "mp4", "mkv", "avi", "mov", "webm" -> "video/*"
    "mp3", "wav", "flac", "m4a", "ogg" -> "audio/*"
    "doc", "docx" -> "application/msword"
    "xls", "xlsx" -> "application/vnd.ms-excel"
    "ppt", "pptx" -> "application/vnd.ms-powerpoint"
    "zip", "rar", "7z" -> "application/zip"
    else -> "*/*"
}
