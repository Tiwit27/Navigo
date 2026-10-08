package com.example.nawigacja_wewnatrz_budynku.otherScripts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.set
import androidx.core.graphics.createBitmap

class MapCreator {
    fun createBitmapFromMatrix(matrix: Array<IntArray>): Bitmap {
        val width = matrix[0].size
        val height = matrix.size

        val bitmap = createBitmap(width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = matrix[y][x]
                val color = when(value) {
                    0 -> Color.WHITE
                    1 -> Color.BLACK
                    else -> Color.GRAY
                }
                bitmap[x, y] = color
            }
        }
        return bitmap
    }

    fun saveBitmapToInternalStorage(bitmap: Bitmap, context: Context, filename: String): Boolean {
        return try {
            context.openFileOutput(filename, Context.MODE_PRIVATE).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}