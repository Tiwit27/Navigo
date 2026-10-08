package com.example.nawigacja_wewnatrz_budynku.dataClasses

import android.graphics.Bitmap
import com.google.gson.annotations.SerializedName

data class Institution(
    val name : String,
    @SerializedName("short_name")
    val shortName : String,
    var logo : Bitmap ?= null
)
