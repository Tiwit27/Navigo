package com.example.nawigacja_wewnatrz_budynku.otherScripts

import com.example.nawigacja_wewnatrz_budynku.dataClasses.Account
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Beacon
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Connection
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Floor
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Point3D
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Room
import androidx.core.content.edit
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Institution

typealias db = DataBase

object DataBase {
    //beacons
    var beacons : MutableList<Beacon> ?= null
        set(value) {
            field = value
            beaconsByMac = field?.associateBy { it.mac } ?: emptyMap()
            beaconsById = field?.associateBy { it.id } ?: emptyMap()
        }
    lateinit var beaconsByMac : Map<String, Beacon>
        private set
    lateinit var beaconsById : Map<Int, Beacon>
        private set

    //floors
    var floors : MutableList<Floor> ?= null
        set(value) {
            field = value
            floorsByFloorNumber = field?.associateBy { it.floorNumber } ?: emptyMap()
            floorGrids = field?.associate { it.floorNumber to it.grid } ?: emptyMap()
        }
    lateinit var floorsByFloorNumber : Map<Int,Floor>
        private set
    lateinit var floorGrids : Map<Int, Array<IntArray>>
        private set


    //rooms
    var rooms : MutableList<Room> ?= null
        set(value) {
            value?.sortBy { it.name }
            field = value
        }

    //institution
    var institution : Institution ?= null

    //connections
    var connections : MutableList<Connection> ?= null
        set(value) {
            field = value
            connectionsPair = value?.map {
                Pair(
                    Point3D(it.bottomX, it.bottomY, it.bottomFloor),
                    Point3D(it.topX, it.topY, it.topFloor)
                )
            } ?: emptyList()
        }
    lateinit var connectionsPair : List<Pair<Point3D, Point3D>>
        private set

    //account
    var account : Account ?= null
    var token : String ?= null
        set(value) {
            field = value
            SecurePrefs.sharedPreferences.edit { putString("auth_token", value) }
        }
}