package com.example.nawigacja_wewnatrz_budynku.dataClasses

import android.graphics.Bitmap
import com.google.gson.annotations.SerializedName
import androidx.core.graphics.createBitmap

data class Floor(
    @SerializedName("floor_id")
    val floorId : Int,
    @SerializedName("floor_number")
    val floorNumber : Int,
    val grid: Array<IntArray>,
    var map : Bitmap = createBitmap(0, 0)
)
