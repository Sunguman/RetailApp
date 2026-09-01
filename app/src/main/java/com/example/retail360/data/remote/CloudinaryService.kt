package com.example.retail360.data.remote

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Uploads a local image to Cloudinary using an UNSIGNED upload preset
 * (no secrets on device) and returns the resulting secure URL.
 *
 * Create the preset in Cloudinary console → Settings → Upload → Add preset
 * (signing mode = Unsigned) and put its name in UPLOAD_PRESET below.
 */
class CloudinaryService {

    suspend fun upload(localUri: Uri, folder: String): String? =
        suspendCancellableCoroutine { cont ->
            MediaManager.get().upload(localUri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", folder)
                .option("cloud_name", CLOUD_NAME)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, total: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        android.util.Log.d("Cloudinary", "Uploaded: ${resultData["secure_url"]}")
                        if (cont.isActive) cont.resume(resultData["secure_url"] as? String)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        android.util.Log.e("Cloudinary", "Upload failed: ${error.description} (code ${error.code})")
                        if (cont.isActive) cont.resume(null)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        android.util.Log.w("Cloudinary", "Rescheduled: ${error.description}")
                    }
                })
                .dispatch()
        }

    companion object {
        private const val CLOUD_NAME = "pwgntjrp"
        private const val UPLOAD_PRESET = "retail360_unsigned"
    }
}

