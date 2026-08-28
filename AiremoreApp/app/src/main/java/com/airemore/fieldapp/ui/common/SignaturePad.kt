package com.airemore.fieldapp.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream

/**
 * A simple finger-drawable signature pad. Call [onSaved] with the local
 * file path of the exported PNG once the user is done — that path gets
 * stored on the form entity and converted to base64 at sync time (see
 * PmRepository.trySyncOne).
 */
@Composable
fun SignaturePad(
    existingPath: String?,
    fileNamePrefix: String,
    onSaved: (String) -> Unit,
    onCleared: () -> Unit,
) {
    val context = LocalContext.current
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var hasDrawn by remember { mutableStateOf(existingPath != null) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val p = Path().apply { moveTo(offset.x, offset.y) }
                                currentPath = p
                                paths.add(p)
                                hasDrawn = true
                            },
                            onDrag = { change, _ ->
                                currentPath?.lineTo(change.position.x, change.position.y)
                                // force redraw by replacing the last element
                                if (paths.isNotEmpty()) paths[paths.lastIndex] = paths.last()
                            },
                        )
                    },
            ) {
                canvasSize = size
                paths.forEach { p ->
                    drawPath(p, color = Color.Black, style = Stroke(width = 5f))
                }
            }
            if (!hasDrawn) {
                Text(
                    "Pumirma dito gamit ang daliri",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = {
                paths.clear()
                currentPath = null
                hasDrawn = false
                onCleared()
            }) { Text("I-clear") }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    if (!hasDrawn || canvasSize.width <= 0f) return@Button
                    val bitmap = Bitmap.createBitmap(canvasSize.width.toInt(), canvasSize.height.toInt(), Bitmap.Config.ARGB_8888)
                    val androidCanvas = AndroidCanvas(bitmap)
                    androidCanvas.drawColor(android.graphics.Color.WHITE)
                    val paint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        style = Paint.Style.STROKE
                        strokeWidth = 5f
                        isAntiAlias = true
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    paths.forEach { composePath ->
                        androidCanvas.drawPath(composePath.asAndroidPath(), paint)
                    }
                    val dir = File(context.filesDir, "signatures").apply { mkdirs() }
                    val file = File(dir, "${fileNamePrefix}_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                    onSaved(file.absolutePath)
                },
                enabled = hasDrawn,
            ) { Text("I-save ang Pirma") }
        }
        if (existingPath != null) {
            Text("May naka-save nang pirma. Gumuhit ulit para palitan.", fontSize = 11.sp)
        }
    }
}

