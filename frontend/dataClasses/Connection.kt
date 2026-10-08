package com.example.nawigacja_wewnatrz_budynku.dataClasses

import com.google.gson.annotations.SerializedName

data class Connection(
    @SerializedName("connection_id")
    val connectionId: Int,
    @SerializedName("bottom_x")
    val bottomX: Int,
    @SerializedName("bottom_y")
    val bottomY: Int,
    @SerializedName("bottom_floor")
    val bottomFloor: Int,
    @SerializedName("top_x")
    val topX: Int,
    @SerializedName("top_y")
    val topY: Int,
    @SerializedName("top_floor")
    val topFloor: Int
)
