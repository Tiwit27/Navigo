package com.example.nawigacja_wewnatrz_budynku.viewModels

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nawigacja_wewnatrz_budynku.BleScanner
import com.example.nawigacja_wewnatrz_budynku.api.RetrofitClient
import com.example.nawigacja_wewnatrz_budynku.api.dataClasses.ApiResponse
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Beacon
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Connection
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Floor
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Institution
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Room
import com.example.nawigacja_wewnatrz_budynku.otherScripts.BroadcastListener
import com.example.nawigacja_wewnatrz_budynku.otherScripts.MapCreator
import com.example.nawigacja_wewnatrz_budynku.otherScripts.NetworkListener
import com.example.nawigacja_wewnatrz_budynku.otherScripts.db
import com.example.nawigacja_wewnatrz_budynku.sealedClasses.BleScannerState
import com.example.nawigacja_wewnatrz_budynku.sealedClasses.InstitutionLogoError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainViewModel : ViewModel()
{
    var networkListener : NetworkListener?= null
    var bleScanner: BleScanner ?= null
        private set
    private val mapCreator = MapCreator()
    var institutionId = 0

    private suspend fun fetchBeacons(institutionId: Int) : Result<ApiResponse<Array<Beacon>>>
    {
        return try {
            val response = RetrofitClient.api.getBeacons(institutionId)
            if(!response.status)
            {
                return Result.failure(Exception(response.error))
            }
            response.data.map { it.rssiBuffer = mutableListOf(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE) }
            db.beacons = response.data.toMutableList()
            Result.success(response)
        }catch (e:Exception)
        {
            Result.failure(e)
        }
    }

    private suspend fun fetchFloors(institutionId: Int) : Result<ApiResponse<Array<Floor>>>
    {
        return try {
            val response = RetrofitClient.api.getFloors(institutionId)
            if(!response.status)
            {
                return Result.failure(Exception(response.error))
            }
            for (floor in response.data)
            {
                floor.map = mapCreator.createBitmapFromMatrix(floor.grid)
            }
            db.floors = response.data.toMutableList()
            Result.success(response)
        }catch (e:Exception)
        {
            Result.failure(e)
        }
    }

    private suspend fun fetchRooms(institutionId: Int): Result<ApiResponse<Array<Room>>>
    {
        return try {
            val response = RetrofitClient.api.getRooms(institutionId)
            if(!response.status)
            {
                return Result.failure(Exception(response.error))
            }
            db.rooms = response.data.toMutableList()
            Result.success(response)
        }catch (e:Exception)
        {
            Result.failure(e)
        }
    }

    private suspend fun fetchInstitutionName(institutionId: Int): Result<ApiResponse<Institution>>
    {
        return try {
            val response = RetrofitClient.api.getInstitutionName(institutionId)
            if(!response.status)
            {
                return Result.failure(Exception(response.error))
            }
            db.institution = response.data
            Result.success(response)
        }catch (e:Exception)
        {
            Result.failure(e)
        }
    }

    private suspend fun fetchConnections(institutionId: Int): Result<ApiResponse<Array<Connection>>>
    {
        return try {
            val response = RetrofitClient.api.getConnections(institutionId)
            if(!response.status)
            {
                return Result.failure(Exception(response.error))
            }
            db.connections = response.data.toMutableList()
            Result.success(response)
        }catch (e:Exception)
        {
            Result.failure(e)
        }
    }

    private suspend fun fetchInstitutionLogo(institutionId: Int): Result<Bitmap?>
    {
        return try {
            val response = RetrofitClient.api.getInstitutionLogo(institutionId)
            val responseBody = response.body()
            if(!response.isSuccessful)
            {
                return Result.failure(Exception("ERROR"))
            }
            if(response.headers()["Content-Type"] != "image/png")
            {
                val json = responseBody!!.string()
                val jsonObject = JSONObject(json)
                if(!jsonObject.getBoolean("status"))
                {
                    val error = when(val errorMessage = jsonObject.getString("error")){
                        "WRONG_INSTITUTION_ID" -> InstitutionLogoError.WrongInstitutionId
                        else -> InstitutionLogoError.Unknown(errorMessage)
                    }
                    return Result.failure(error)
                }
                return Result.success(null)
            }
            else
            {
                val bitmap = withContext(Dispatchers.IO){
                    val inputStream = responseBody?.byteStream()
                    val bmp = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    bmp
                }
                db.institution?.logo = bitmap
                Result.success(bitmap)
            }
        }catch (e:Exception)
        {
            Result.failure(e)
        }
    }
    private suspend fun fetchInstitutionId(mac: String): Result<Int>
    {
        return try {
            val response = RetrofitClient.api.getInstitutionId(mac)
            if(!response.status)
            {
                return Result.failure(Exception(response.error))
            }
            Result.success(response.data)
        }catch (e:Exception)
        {
            Result.failure(e)
        }
    }

    suspend fun fetchAll(mac: String):Result<Int>
    {
        fetchInstitutionId(mac)
            .onSuccess { institutionId ->
                fetchBeacons(institutionId).onFailure { return Result.failure(Exception(it.message + "1")) }
                fetchFloors(institutionId).onFailure { return Result.failure(Exception(it.message + "2")) }
                fetchRooms(institutionId).onFailure { return Result.failure(Exception(it.message + "3")) }
                fetchInstitutionName(institutionId).onFailure { return Result.failure(Exception(it.message + "4")) }
                fetchConnections(institutionId).onFailure { return Result.failure(Exception(it.message + "5"))}
                fetchInstitutionLogo(institutionId).onFailure { return Result.failure(Exception(it.message + "6")) }
                return Result.success(institutionId)
            }
        return Result.failure(Exception("API Error"))
    }

    //timeout for bleScanning
    private var timeoutJob: Job?= null
    fun startTimeout()
    {
        timeoutJob = viewModelScope.launch {
            delay(20000)
            _bleScannerState.value = BleScannerState.Timeout
            bleScanner?.stopScan()
        }
    }
    fun stopTimeout()
    {
        timeoutJob?.cancel()
    }


    //bleScanner functions
    private val _bleScannerState = MutableStateFlow<BleScannerState>(BleScannerState.NoBluetoothOrNetwork)
    val bleScannerState : StateFlow<BleScannerState> = _bleScannerState.asStateFlow()

    private val _bluetoothStatus = MutableStateFlow(false)
    val bluetoothStatus : StateFlow<Boolean> = _bluetoothStatus.asStateFlow()

    private val _networkStatus = MutableStateFlow(false)
    val networkStatus : StateFlow<Boolean> = _networkStatus.asStateFlow()

    fun initBleScanner(context: Context)
    {
        bleScanner = BleScanner(context) { result ->
            bleScanner?.stopScan()
            _bleScannerState.value = BleScannerState.LoadingData
            viewModelScope.launch {
                fetchAll(result.device.address)
                    .onSuccess {
                        _bleScannerState.value = BleScannerState.Success
                        institutionId = it
                    }
                    .onFailure {
                        Log.d("ERROR_BLE", it.message.toString())
                        bleScanner?.startScan()
                    }
            }
        }
        startTimeout()
        bleScanner?.startScan()
    }

    fun changeBleScannerState(state: BleScannerState)
    {
        _bleScannerState.value = state
    }

    fun setBroadcastListener(context: Context)
    {
        //register changes in bluetooth
        val broadcastListener = BroadcastListener(
            onBluetoothOn = { _ ->
                _bluetoothStatus.value = true
            },
            onBluetoothOff = { _ ->
                _bluetoothStatus.value = false
            }
        )
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(broadcastListener, filter)
        _bluetoothStatus.value = broadcastListener.isBluetoothEnabled(context)
    }

    fun setNetworkListener(context: Context)
    {
        networkListener = NetworkListener(context,
            onNetworkOn = {
                _networkStatus.value = true
            },
            onNetworkOff = {
                _networkStatus.value = false
            })
        networkListener?.startListening()
        _networkStatus.value = networkListener?.isInternetAvailable(context) ?: false
    }



    suspend fun fetchInstitutionData() : Result<Institution>
    {
        return try {
            val response = RetrofitClient.api.getInstitutionName(institutionId)
            val institutionData = response.data
            //fetchInstitutionLogo
            val logoResponse = RetrofitClient.api.getInstitutionLogo(institutionId)
            val responseBody = logoResponse.body()
            if(!logoResponse.isSuccessful)
            {
                return Result.failure(Exception("ERROR"))
            }
            if(logoResponse.headers()["Content-Type"] == "image/png")
            {
                val bitmap = withContext(Dispatchers.IO){
                    val inputStream = responseBody?.byteStream()
                    val bmp = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    bmp
                }
                institutionData.logo = bitmap
                Log.d("TEST", "4")
            }
            db.institution = institutionData
            Result.success(institutionData)
        }catch (e:Exception) {
            Result.failure(e)
        }
    }
}