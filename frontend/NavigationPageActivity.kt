package com.example.nawigacja_wewnatrz_budynku

import android.animation.ObjectAnimator
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.content.IntentFilter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.nawigacja_wewnatrz_budynku.otherScripts.AStar
import com.example.nawigacja_wewnatrz_budynku.otherScripts.BroadcastListener
import com.example.nawigacja_wewnatrz_budynku.otherScripts.BitmapPainter
import com.example.nawigacja_wewnatrz_budynku.otherScripts.db
import android.bluetooth.le.ScanResult
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBar.LayoutParams
import androidx.core.animation.doOnEnd
import com.airbnb.lottie.LottieAnimationView
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Point3D
import com.example.nawigacja_wewnatrz_budynku.otherScripts.MapScaler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import androidx.core.view.isInvisible
import androidx.core.view.isVisible

class NavigationPageActivity : AppCompatActivity() {
    private val nearestMutex = Mutex()
    private val bleDispatchersScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    //private val viewModel: Navigation_ViewModel by viewModels()
    private lateinit var bleScanner: BleScanner
    private lateinit var broadcastListener: BroadcastListener
    private lateinit var imageView : ImageView
    private var id : Int = 0
    private var roomName = ""
    private lateinit var bitmapPainter : BitmapPainter
    private lateinit var stairsText : TextView
    private lateinit var lookAroundView: LinearLayout
    private lateinit var perspectiveModeButton: ImageButton


//    val connections = listOf(
//        Pair(Point3D(7, 7, 0), Point3D(7, 7, 1))
//    )
    private var nearestBeacon: String ?= null
    private var currentFloor = 0
    private val astar = AStar()
    var path: List<Point3D> = emptyList()

    private enum class Perspective{ TWO_D, THREE_D}
    private var perspectiveMode = Perspective.THREE_D

    fun logGrid(grid: Array<IntArray>) {
        for ((i, row) in grid.withIndex()) {
            val rowStr = row.joinToString(" ")
            Log.d("GRID", "[$i] $rowStr")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_navigation_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<TextView>(R.id.loadingText).text = "Kalibracja pozycji"
        lookAroundView = findViewById(R.id.lookAround)
        imageView = findViewById(R.id.map)
        id = intent.getIntExtra("Id", 1)
        roomName = intent.getStringExtra("RoomName").toString()
        bitmapPainter = BitmapPainter(this)
        stairsText = findViewById(R.id.stairs)
        perspectiveModeButton = findViewById(R.id.perspectiveChange)

        findViewById<TextView>(R.id.navigationToRoomText).text = "Nawigacja do pokoju ${roomName}"

        perspectiveModeButton.setOnClickListener {
            val startRotation = imageView.rotationX
            val endRotation: Float
            if(perspectiveMode == Perspective.THREE_D)
            {
                perspectiveMode = Perspective.TWO_D
                endRotation = 0f
                perspectiveModeButton.setImageResource(R.drawable.three_d)
            }
            else
            {
                perspectiveMode = Perspective.THREE_D
                endRotation = 30f
                perspectiveModeButton.setImageResource(R.drawable.two_d)
            }
            ObjectAnimator.ofFloat(imageView, "rotationX", startRotation, endRotation).apply {
                duration = 400
                interpolator = DecelerateInterpolator()
                start()
            }
            DrawMap()
        }

        initBleScanner()
    }

    private fun processBleData(result: ScanResult)
    {
        val oldNearestBeacon = nearestBeacon
        bleDispatchersScope.launch {
            if(nearestBeacon != result.device.address)
            {
                nearestMutex.withLock {
                    //set new nearestBeacon
                    if (nearestBeacon == null)
                    {
                        nearestBeacon = result.device.address
                        currentFloor = db.beaconsByMac[nearestBeacon]!!.floor
                    }
                    else
                    {
                        db.beaconsByMac[nearestBeacon]?.let { beacon ->
                            if(beacon.rssiBuffer[2] >= beacon.rssiBuffer[1] && beacon.rssiBuffer[1] >= beacon.rssiBuffer[0] && beacon.rssi < result.rssi + 10)
                            {
                                nearestBeacon = result.device.address
                            }
                            else if(beacon.rssi < result.rssi)
                            {
                                nearestBeacon = result.device.address
                            }
                        }
                    }
                    //set new Floor
                    if(currentFloor != db.beaconsByMac[nearestBeacon]!!.floor)
                    {
                        currentFloor = db.beaconsByMac[nearestBeacon]!!.floor
                    }
                }
            }
            if(nearestBeacon != null && findViewById<LinearLayout>(R.id.loading).isInvisible && db.beaconsByMac[nearestBeacon]?.id == id)
            {
                withContext(Dispatchers.Main)
                {
                    lookAroundView.visibility = View.VISIBLE
                }
            }
            else if(lookAroundView.isVisible)
            {
                withContext(Dispatchers.Main)
                {
                    lookAroundView.visibility = View.INVISIBLE
                }
            }
//            else if(findViewById<LinearLayout>(R.id.loading).isVisible && db.beaconsByMac[nearestBeacon]?.id != id)
//            {
//                withContext(Dispatchers.Main)
//                {
//                    Log.d("TEST1", "yy")
//                    findViewById<LinearLayout>(R.id.loading).visibility = View.INVISIBLE
//                    findViewById<LottieAnimationView>(R.id.lottieAnim).apply {
//                        setAnimation(R.raw.loading1)
//                        speed = 0.6F
//                    }
//                    findViewById<LottieAnimationView>(R.id.lottieAnim).playAnimation()
//                    findViewById<TextView>(R.id.loadingText).text = "Kalibracja pozycji"
//                }
//            }
            //Calculate path
            if(nearestBeacon != oldNearestBeacon)
            {
                Log.d("DrawLojek", "Rysuje")
                path = astar.aStarMultiFloor(db.floorGrids, Point3D(db.beaconsByMac[nearestBeacon]!!.x,db.beaconsByMac[nearestBeacon]!!.y, db.beaconsByMac[nearestBeacon]!!.floor), Point3D(db.beaconsById[id]!!.x,db.beaconsById[id]!!.y, db.beaconsById[id]!!.floor), db.connectionsPair)
                withContext(Dispatchers.Main)
                {
                    if(path.isNotEmpty())
                    {
                        var lines = bitmapPainter.drawPath(path, imageView, db.floorsByFloorNumber[currentFloor]!!.grid)
                        if(lines == 0)
                        {
                            if(path[0].floor > path[1].floor)
                            {
                                stairsText.text = "Zejdz po schodach w dół"
                            }
                            else
                            {
                                stairsText.text = "Wejdz po schodach w górę"
                            }
                            stairsText.visibility = View.VISIBLE
                        }
                        else
                        {
                            stairsText.visibility = View.INVISIBLE
                        }
                        Log.d("path", "$path, ${db.beaconsByMac[nearestBeacon]!!.id}")
                        if(path.size > 1)
                        {
                            DrawMap();
                        }
                    }
                    findViewById<LinearLayout>(R.id.loading).visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun DrawMap()
    {
        val mapScaler = MapScaler(imageView, db.floorsByFloorNumber[currentFloor]!!.grid, bitmapPainter.tileSize, bitmapPainter.tileSize, db.beaconsByMac[nearestBeacon]!!.x, db.beaconsByMac[nearestBeacon]!!.y, path)
        val rotation = mapScaler.RotateMap()
        if(perspectiveMode == Perspective.THREE_D)
        {
            mapScaler.CenterUserAtBottom()
        }
        else
        {
            mapScaler.CenterMap()
        }
        bitmapPainter.drawIcons(imageView, rotation)
    }

    private fun initBleScanner()
    {
        bleScanner = BleScanner(this) {result ->
            Log.d("BLE", "${db.beaconsByMac[result.device.address]?.id}")
            //read and save new data package
            db.beaconsByMac[result.device.address]?.let { beacon ->
                beacon.rssi = result.rssi
                beacon.rssiBuffer.add(0,result.rssi)
                if(beacon.rssiBuffer.size > 3)
                {
                    beacon.rssiBuffer.removeAt(3)
                }
                Log.d("BufferLojek", "${beacon.id} buffer new->old: ${beacon.rssiBuffer[0]} ${beacon.rssiBuffer[1]} ${beacon.rssiBuffer[2]}")
            }
            processBleData(result)

            //old long method
//            runOnUiThread {
//                if(!db.beaconsByMac.containsKey(result.device.address))
//                {
//                    return@runOnUiThread
//                }
//                db.beaconsByMac[result.device.address]?.let { beacon ->
//                    beacon.rssi = result.rssi
//                    beacon.rssiBuffer.add(0,result.rssi)
//                    if(beacon.rssiBuffer.size > 3)
//                    {
//                        beacon.rssiBuffer.removeAt(3)
//                    }
//                    Log.d("BufferLojek", "${beacon.id} buffer new->old: ${beacon.rssiBuffer[0]} ${beacon.rssiBuffer[1]} ${beacon.rssiBuffer[2]}")
//                }
//                if (nearestBeacon == null)
//                {
//                    nearestBeacon = result.device.address
//                    currentFloor = db.beaconsByMac[nearestBeacon]!!.floor
//                }
//                //new method of choosing nearest beacon. NEED TESTS IN REAL ENVIRONMENT
//                else
//                {
//                    //trzeba dopracować podczas testów, bo tutaj będzie wywalać błędy z powodu outOfIndex, bo za 1 i drugim odczytem rssiBufor nie będzie mieć liczb
//                    db.beaconsByMac[nearestBeacon]?.let { beacon ->
//                        if(beacon.rssiBuffer[2] >= beacon.rssiBuffer[1] && beacon.rssiBuffer[1] >= beacon.rssiBuffer[0] && beacon.rssi < result.rssi + 10)
//                        {
//                            nearestBeacon = result.device.address
//                        }
//                        else if(beacon.rssi < result.rssi)
//                        {
//                            nearestBeacon = result.device.address
//                        }
//                    }
//                }
//                Log.d("NEAREST", db.beaconsByMac[nearestBeacon]?.id.toString())
//                Log.d("LINE", "--------------------------------------------")
//                if(currentFloor != db.beaconsByMac[nearestBeacon]!!.floor)
//                {
//                    currentFloor = db.beaconsByMac[nearestBeacon]!!.floor
//                }
//                for(beacon in db.beacons!!)
//                {
//                    Log.d("RSSI", "${beacon.id} ${beacon.rssi}")
//                }
//                idText.text = db.beaconsByMac[nearestBeacon]!!.id.toString()
//                val path = astar.aStarMultiFloor(db.floorGrids, Point3D(db.beaconsByMac[nearestBeacon]!!.x,db.beaconsByMac[nearestBeacon]!!.y, db.beaconsByMac[nearestBeacon]!!.floor), Point3D(db.beaconsById[id]!!.x,db.beaconsById[id]!!.y, db.beaconsById[id]!!.floor), db.connectionsPair)
//                //Log.d("POINT", db.floorsByFloorNumber[currentFloor]!!.grid[db.beaconsByMac[nearestBeacon]!!.y][db.beaconsByMac[nearestBeacon]!!.x].toString())
//                //Log.d("POINT", db.floorsByFloorNumber[db.beaconsById[id]!!.floor]!!.grid[db.beaconsById[id]!!.y][db.beaconsById[id]!!.x].toString())
//                //logGrid(db.floorsByFloorNumber[db.beaconsById[id]!!.floor]!!.grid)
//                var lines = 0
//                if(path.isNotEmpty())
//                {
//                    lines = bitmapPainter.drawPath(path, imageView, db.floorsByFloorNumber[currentFloor]!!.grid)
//                    if(lines == 0)
//                    {
//                        if(path[0].floor > path[1].floor)
//                        {
//                            stairsText.text = "Zejdz po schodach w dół"
//                        }
//                        else
//                        {
//                            stairsText.text = "Wejdz po schodach w górę"
//                        }
//                        stairsText.visibility = View.VISIBLE
//                    }
//                    else
//                    {
//                        stairsText.visibility = View.INVISIBLE
//                    }
//                    Log.d("path", "$path, ${db.beaconsByMac[nearestBeacon]!!.id}")
//                    if(path.size > 1)
//                    {
//                        val mapScaler = MapScaler(imageView, db.floorsByFloorNumber[currentFloor]!!.grid, bitmapPainter.tileSize, bitmapPainter.tileSize, db.beaconsByMac[nearestBeacon]!!.x, db.beaconsByMac[nearestBeacon]!!.y, path)
//                        val rotation = mapScaler.RotateMap()
//                        mapScaler.CenterMap()
//                        bitmapPainter.drawIcons(imageView, rotation)
//                    }
//                }
//                findViewById<LinearLayout>(R.id.loading).visibility = View.INVISIBLE
////                val tempBeacons = db.beacons!!.toMutableList()
////                tempBeacons.sortBy { it.rssi }
////                if(tempBeacons[1].rssi != 0)
////                {
////                    findViewById<LinearLayout>(R.id.loading).visibility = View.INVISIBLE
////                }
//            }
        }
        bleScanner.startScan()
    }


    override fun onResume() {
        super.onResume()
        broadcastListener = BroadcastListener(
            onBluetoothOff = {context ->
                val intentToMainPage = Intent(context, MainPageActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(intentToMainPage)
                Toast.makeText(context, "Bluetooth jest wymagany do nawigacji", Toast.LENGTH_SHORT).show()
            }
        )

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(broadcastListener, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        bleScanner.stopScan()
        unregisterReceiver(broadcastListener)
    }

    override fun onPause() {
        super.onPause()
        bleScanner.stopScan()
    }
}
