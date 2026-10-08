package com.example.nawigacja_wewnatrz_budynku.otherScripts

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BroadcastListener(
    private val onBluetoothOff: ((Context) -> Unit) ?= null,
    private val onBluetoothOn: ((Context) -> Unit) ?= null
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
            if (state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF) {
//                val intentToMainPage = Intent(context, Main_Page::class.java)
//                intentToMainPage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                context?.startActivity(intentToMainPage)
//                Toast.makeText(context, "Bluetooth jest wymagany do nawigacji", Toast.LENGTH_SHORT).show()
                context?.let { onBluetoothOff?.invoke(it) }
            }
            else if(state == BluetoothAdapter.STATE_ON)
            {
                context?.let{ onBluetoothOn?.invoke(it) }
            }
        }
    }

    fun isBluetoothEnabled(context: Context) : Boolean{
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        return bluetoothAdapter?.isEnabled ?: false
    }
}
