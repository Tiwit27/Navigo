package com.example.nawigacja_wewnatrz_budynku.otherScripts

import android.content.Context

fun Context.dpToPx(dp: Int): Int
{
    val scale = resources.displayMetrics.density
    return (dp * scale).toInt()
}