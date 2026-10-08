package com.example.nawigacja_wewnatrz_budynku.viewModels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.nawigacja_wewnatrz_budynku.api.RetrofitClient
import com.example.nawigacja_wewnatrz_budynku.api.dataClasses.ApiResponse
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Account
import com.example.nawigacja_wewnatrz_budynku.otherScripts.NetworkListener
import com.example.nawigacja_wewnatrz_budynku.otherScripts.SecurePrefs
import com.example.nawigacja_wewnatrz_budynku.otherScripts.db
import com.example.nawigacja_wewnatrz_budynku.sealedClasses.AutoLoginError
import com.example.nawigacja_wewnatrz_budynku.sealedClasses.LoginError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class LoginViewModel : ViewModel() {

    private val _networkStatus = MutableStateFlow(false)
    val networkStatus : StateFlow<Boolean> = _networkStatus.asStateFlow()

    var networkListener : NetworkListener ?= null

    suspend fun login(email: String, password: String) : Result<ApiResponse<Account>>
    {
        return try {
            val response = RetrofitClient.api.login(email, password, "77ea92438a93f6f240dca1f0a16cf2b581006aae")
            if(!response.status)
            {
                val error = when(response.error)
                {
                    "EMAIL_DONT_EXIST" -> LoginError.EmailDontExit
                    "WRONG_PASSWORD" -> LoginError.WrongPassword
                    else -> LoginError.Unknown(response.error)
                }
                return Result.failure(error)
            }
            db.account = response.data
            db.token = response.token
            Log.d("TOKEN", db.token.toString())
            Result.success(response)
        }catch (e:Exception) {
            Result.failure(e)
        }
    }
    suspend fun autoLogin() : Result<ApiResponse<Account>>
    {
        return try {
            val token = SecurePrefs.sharedPreferences.getString("auth_token", null)
            Log.d("TOKEN", token.toString())
            if(token != null)
            {
                val response = RetrofitClient.api.autoLogin(token)
                if(!response.status)
                {
                    val error = when(response.error)
                    {
                        "EMPTY_TOKEN" -> AutoLoginError.EmptyToken
                        "WRONG_TOKEN_VERSION" -> AutoLoginError.WrongTokenVersion
                        "ACCOUNT_DELETED" -> AutoLoginError.AccountDeleted
                        else -> AutoLoginError.Unknown(response.error)
                    }
                    return Result.failure(error)
                }
                db.account = response.data
                Result.success(response)
            }
            else
            {
                Result.failure(AutoLoginError.EmptyToken)
            }
        }catch (e:IOException){
            Result.failure(AutoLoginError.NoInternetConnection)
        }
        catch (e:Exception) {
            Result.failure(e)
        }
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
        if(networkListener?.isInternetAvailable(context) == true)
        {
            _networkStatus.value = true
        }
        else
        {
            _networkStatus.value = false
        }
    }
}