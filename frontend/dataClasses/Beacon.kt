package com.example.nawigacja_wewnatrz_budynku.dataClasses

import com.google.gson.annotations.SerializedName
import java.sql.Timestamp

data class Beacon(
    @SerializedName("beacon_id")
    val id: Int,
    val mac: String,
    val y: Int,
    val x: Int,
    val floor: Int,
    var rssi: Int = Int.MIN_VALUE,
    @SerializedName("battery_level")
    var batteryLevel: Double,
    @SerializedName("battery_level_timestamp")
    var batteryLevelTimestamp : Timestamp ?= null,
    var rssiBuffer : MutableList<Int> = mutableListOf(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
)