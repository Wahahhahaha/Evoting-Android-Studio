package com.example.e_voting

import android.graphics.BitmapFactory
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object CandidateImageLoader {

    fun loadInto(imageView: ImageView, picture: String) {
        val imageUrl = resolveImageUrl(picture)
        imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        imageView.tag = imageUrl

        if (imageUrl.isNullOrBlank()) {
            return
        }

        thread {
            val bitmap = runCatching {
                val connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    doInput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                val result = connection.inputStream.use { BitmapFactory.decodeStream(it) }
                connection.disconnect()
                result
            }.getOrNull()

            imageView.post {
                if (imageView.tag == imageUrl && bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun resolveImageUrl(picture: String): String? {
        if (picture.isBlank()) return null
        if (picture.startsWith("http://") || picture.startsWith("https://")) return picture

        val cleanPath = picture.trimStart('/')
        return "${ApiConfig.BASE_URL}$cleanPath"
    }
}
