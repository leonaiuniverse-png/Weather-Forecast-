package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareUtils {

    fun shareWeatherDashboard(
        context: Context,
        view: View,
        cityName: String,
        tempFormatted: String,
        conditionTitle: String
    ) {
        try {
            val width = view.width
            val height = view.height
            if (width <= 0 || height <= 0) {
                Toast.makeText(context, "Rendering dashboard...", Toast.LENGTH_SHORT).show()
                return
            }

            // Create Bitmap from View
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)

            // Save to internal cache directory for FileProvider
            val cacheFolder = File(context.cacheDir, "images").apply {
                if (!exists()) mkdirs()
            }
            val imageFile = File(cacheFolder, "skyglass_weather_snapshot.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }

            // Generate content URI via FileProvider
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, imageFile)

            // Build Native Share Intent
            val shareSummary = "🌤️ Current weather in $cityName: $tempFormatted, $conditionTitle. Shared via SkyGlass Weather."
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, shareSummary)
                putExtra(Intent.EXTRA_SUBJECT, "$cityName Weather Snapshot")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Weather Snapshot").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Unable to share snapshot: ${e.localizedMessage ?: "Unknown error"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
