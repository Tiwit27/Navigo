package com.example.nawigacja_wewnatrz_budynku.dataClasses

import com.google.gson.annotations.SerializedName

data class Room(
    @SerializedName("room_id")
    val roomId : Int,
    val name : String,
    @SerializedName("beacon_id")
    val beaconId : Int
)
