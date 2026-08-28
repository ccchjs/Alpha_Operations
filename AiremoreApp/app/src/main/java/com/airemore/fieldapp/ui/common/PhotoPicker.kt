package com.airemore.fieldapp.ui.common

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File

/**
 * Shows up to [maxPhotos] thumbnails plus an "add" tile. Tapping "add"
 * opens a small chooser (Camera / Gallery). Whatever the user picks gets
 * copied into app-private storage immediately (files/unit_photos/) so the
 * photo survives even if the original (e.g. a gallery image) is later
 * deleted, and so the sync worker always has a stable local path to
 * upload from — completely offline until the actual sync attempt.
 */
@Composable
fun PhotoPickerRow(
    label: String,
    photos: List<String>,
    maxPhotos: Int = 2,
    onChange: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var showChooser by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && pendingCameraFile != null) {
            onChange(photos + pendingCameraFile!!.absolutePath)
        }
        pendingCameraUri = null
        pendingCameraFile = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val copied = copyUriToAppStorage(context, uri)
            if (copied != null) onChange(photos + copied)
        }
    }

    Column {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(photos) { path ->
                Box(Modifier.size(80.dp)) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(20.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onChange(photos - path) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            if (photos.size < maxPhotos) {
                item {
                    Box(
                        Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .clickable { showChooser = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = "Add photo", tint = Color.Gray)
                    }
                }
            }
        }
    }

    if (showChooser) {
        AlertDialog(
            onDismissRequest = { showChooser = false },
            title = { Text("Magdagdag ng Larawan") },
            text = { Text("Pumili ng source.") },
            confirmButton = {
                TextButton(onClick = {
                    showChooser = false
                    val (uri, file) = createCameraOutputUri(context)
                    pendingCameraUri = uri
                    pendingCameraFile = file
                    cameraLauncher.launch(uri)
                }) { Text("Camera") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChooser = false
                    galleryLauncher.launch("image/*")
                }) { Text("Gallery") }
            },
        )
    }
}

private fun createCameraOutputUri(context: Context): Pair<Uri, File> {
    val dir = File(context.filesDir, "unit_photos").apply { mkdirs() }
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return uri to file
}

private fun copyUriToAppStorage(context: Context, uri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "unit_photos").apply { mkdirs() }
        val outFile = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        }
        outFile.absolutePath
    } catch (e: Exception) {
        null
    }
}
