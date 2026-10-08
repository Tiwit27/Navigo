package com.example.nawigacja_wewnatrz_budynku

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.nawigacja_wewnatrz_budynku.dataClasses.BeaconData
import com.example.nawigacja_wewnatrz_budynku.api.RetrofitClient
import com.example.nawigacja_wewnatrz_budynku.otherScripts.db

class BleScanner(
    private val context: Context,
    private val onDeviceFound: (result: ScanResult) -> Unit
) {

    // Nullable bluetoothAdapter
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothManager?.adapter
    }

    private var gatt: BluetoothGatt? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result == null)
            {
                return
            }
            //old method with UUID
            if(db.beacons == null)
            {
                val scanRecord = result.scanRecord?.bytes ?: return
                val beaconData = parseIBeacon(scanRecord)
                Log.d("BLE", "Odczytany UUID: ${beaconData?.uuid}, mac: ${result.device.address}")
                if (beaconData?.uuid == "D017927A-DCBA-42C4-AEAE-D97E1A8789B3") {
                    onDeviceFound(result)
                }
            }
            else
            {
                if(db.beaconsByMac.containsKey(result.device.address))
                {
                    onDeviceFound(result)
                }
            }
        }
//        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
//        override fun onScanResult(callbackType: Int, result: ScanResult) {
//            val scanRecord = result.scanRecord?.bytes ?: return
//            val beaconData = parseIBeacon(scanRecord)
//            Log.d("BLE", "Odczytany UUID: ${beaconData?.uuid}, mac: ${result.device.address}")
//            if (beaconData?.uuid == "D017927A-DCBA-42C4-AEAE-D97E1A8789B3") {
//                onDeviceFound(result, beaconData)
////                gatt = result.device.connectGatt(context, false, object : BluetoothGattCallback() {
////
////                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
////                    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
////                        Log.d("BLE", "onConnectionStateChange: status=$status, newState=$newState")
////                        if (newState == BluetoothGatt.STATE_CONNECTED) {
////                            gatt.discoverServices()
////                        }
////                    }
////
////                    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
////                        // UUID Battery Service i Battery Level
////                        Log.d("BLE", "onServicesDiscovered: status=$status")
////                        val service = gatt.getService(UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb"))
////                        val characteristic = service?.getCharacteristic(UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb"))
////                        characteristic?.let { if (ActivityCompat.checkSelfPermission(
////                                context,
////                                Manifest.permission.BLUETOOTH_CONNECT
////                            ) != PackageManager.PERMISSION_GRANTED
////                        ) {
////                            return
////                        }
////                            gatt.readCharacteristic(it) }
////                    }
////
////                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
////                    override fun onCharacteristicRead(
////                        gatt: BluetoothGatt,
////                        characteristic: BluetoothGattCharacteristic,
////                        status: Int
////                    ) {
////                        Log.d("BLE", "onCharacteristicRead: status=$status")
////                        val rawValue = characteristic.value[0].toInt() and 0xFF
////                        Log.d("BLE", "Jednorazowo odczytane rawValue: $rawValue")
////                        CoroutineScope(Dispatchers.IO).launch {
////                            UpdateBeaconBatteryLevel(result.device.address, (rawValue / 100.0) + 2)
////                        }
////                        gatt.disconnect() // od razu rozłączamy po odczycie
////                    }
////                })
//            }
//        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE", "Skanowanie nie powiodło się: $errorCode")
        }
    }

    fun startScan() {
        Log.d("SCANNING", "START Called from: ${Thread.currentThread().stackTrace[3]}")

        if (!hasScanPermission()) return

        val adapter = bluetoothAdapter
        if (adapter == null) {
            Log.e("BLE", "BluetoothAdapter is null, cannot scan")
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e("BLE", "BluetoothLeScanner is null, cannot scan")
            return
        }

        try {
            val manufacturerId = 0x004C // Apple
            val beaconPrefix = byteArrayOf(0x02, 0x15) // iBeacon header
            val beaconMask = byteArrayOf(0xFF.toByte(), 0xFF.toByte())

            val scanFilter = ScanFilter.Builder()
                .setManufacturerData(manufacturerId, beaconPrefix, beaconMask)
                .build()

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
        } catch (e: SecurityException) {
            Log.e("BLE", "Permission missing for BLE scan")
        } catch (e: Exception) {
            Log.e("BLE", "Unknown error in startScan", e)
        }
    }

    fun stopScan() {
        Log.d("SCANNING", "STOP Called from: ${Thread.currentThread().stackTrace[3]}")
        if (!hasScanPermission()) return

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            Log.e("BLE", "BluetoothLeScanner is null, cannot stop scan")
            return
        }

        try {
            scanner.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.e("BLE", "Permission missing to stop BLE scan")
        } catch (e: Exception) {
            Log.e("BLE", "Unknown error in stopScan", e)
        }
    }

    private fun hasScanPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun parseIBeacon(scanRecord: ByteArray): BeaconData? {
        var startByte = 2
        while (startByte <= scanRecord.size - 24) {
            val b2 = scanRecord[startByte + 2].toInt() and 0xFF
            val b3 = scanRecord[startByte + 3].toInt() and 0xFF

            if (b2 == 0x02 && b3 == 0x15) {
                val uuidBytes = scanRecord.copyOfRange(startByte + 4, startByte + 20)
                val hex = uuidBytes.joinToString("") { "%02x".format(it) }
                val uuid = "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"

                val major = ((scanRecord[startByte + 20].toInt() and 0xFF) shl 8) + (scanRecord[startByte + 21].toInt() and 0xFF)
                val minor = ((scanRecord[startByte + 22].toInt() and 0xFF) shl 8) + (scanRecord[startByte + 23].toInt() and 0xFF)

                return BeaconData(uuid.uppercase(), major, minor)
            }
            startByte++
        }
        return null
    }

    suspend fun UpdateBeaconBatteryLevel(mac: String, batteryLevel: Double) : Result<Unit>
    {
        return try {
            val response = RetrofitClient.api.updateBeaconBatteryLevel(mac, batteryLevel)
            if(!response.status)
            {
                return Result.failure(Throwable(response.error))
            }
            Result.success(Unit)
        }
        catch (e: Exception)
        {
            return Result.failure(e)
        }
    }


}
