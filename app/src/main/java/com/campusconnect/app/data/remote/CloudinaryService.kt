package com.campusconnect.app.data.remote

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudinaryService @Inject constructor(
    private val cloudinaryApi: CloudinaryApi,
    private val context: Context
) {
    suspend fun uploadImage(imageUri: Uri): Result<String> = runCatching {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: error("Cannot open image")
        val tempFile = File.createTempFile("notice_", ".jpg", context.cacheDir)
        FileOutputStream(tempFile).use { output -> inputStream.copyTo(output) }

        val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
        val uploadPreset = "CampusConnect"
            .toRequestBody("text/plain".toMediaTypeOrNull())

        val response = cloudinaryApi.uploadImage(filePart, uploadPreset)
        tempFile.delete()

        if (response.isSuccessful) {
            response.body()?.secure_url ?: error("Empty response")
        } else {
            error("Upload failed: ${response.code()}")
        }
    }
}