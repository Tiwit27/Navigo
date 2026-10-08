package com.example.nawigacja_wewnatrz_budynku.otherScripts

import android.graphics.Bitmap
import android.graphics.Color

fun Bitmap.isBlackAndWhite(tolerance : Int = 13) : Boolean
{
    fun isAlmostWhite(r: Int, g: Int, b: Int) =
        r >= 255 - tolerance && g >= 255 - tolerance && b >= 255 - tolerance

    fun isAlmostBlack(r: Int, g: Int, b: Int) =
        r <= tolerance && g <= tolerance && b <= tolerance

    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = getPixel(x, y)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            if (!isAlmostWhite(r, g, b) && !isAlmostBlack(r, g, b)) {
                return false
            }
        }
    }
    return true
}