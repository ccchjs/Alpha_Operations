package com.airemore.fieldapp.sync

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

fun String.toRequestBody(): RequestBody = this.toRequestBody("application/json; charset=utf-8".toMediaType())

/**
 * Builds the multipart photo parts for a unit-based form (PM/Repair).
 * Field names MUST end in "[]" so PHP groups same-named files into a
 * proper array in $_FILES — see api/pm_save.php / api/repair_save.php,
 * which read `$_FILES["photo_before_{unitIndex}"]` as an array.
 *
 * @param unitPhotos list of (unitIndex, Pair(beforePaths, afterPaths))
 */
fun buildPhotoParts(unitPhotos: List<Pair<Int, Pair<List<String>, List<String>>>>): List<MultipartBody.Part> {
    val parts = mutableListOf<MultipartBody.Part>()
    for ((unitIndex, beforeAfter) in unitPhotos) {
        val (before, after) = beforeAfter
        before.forEach { path -> filePartOrNull(path, "photo_before_$unitIndex[]")?.let(parts::add) }
        after.forEach { path -> filePartOrNull(path, "photo_after_$unitIndex[]")?.let(parts::add) }
    }
    return parts
}

/** Installation photos are not per-unit — a single before/after set for the site visit. */
fun buildSitePhotoParts(before: List<String>, after: List<String>): List<MultipartBody.Part> {
    val parts = mutableListOf<MultipartBody.Part>()
    before.forEach { path -> filePartOrNull(path, "photo_before_0[]")?.let(parts::add) }
    after.forEach { path -> filePartOrNull(path, "photo_after_0[]")?.let(parts::add) }
    return parts
}

private fun filePartOrNull(path: String, fieldName: String): MultipartBody.Part? {
    val file = File(path)
    if (!file.exists() || file.length() == 0L) return null
    val mediaType = when (file.extension.lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "heic", "heif" -> "image/heic"
        else -> "image/jpeg"
    }.toMediaType()
    return MultipartBody.Part.createFormData(fieldName, file.name, file.asRequestBody(mediaType))
}
