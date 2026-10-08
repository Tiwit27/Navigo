package com.example.nawigacja_wewnatrz_budynku.otherScripts

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.NetworkCapabilities
import android.util.Log

class NetworkListener(
    context: Context,
    private val onNetworkOn: (() -> Unit) ?= null,
    private val onNetworkOff: (() -> Unit) ?= null
) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onNetworkOn?.invoke()
        }

        override fun onLost(network: Network) {
            onNetworkOff?.invoke()
        }
    }

    fun startListening() {
        Log.d("NETWORK", "START")
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun stopListening() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    fun isInternetAvailable(context: Context) : Boolean{
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)
        val isAvailable = network != null && capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        return isAvailable
    }
}