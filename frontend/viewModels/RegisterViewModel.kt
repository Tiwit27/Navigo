package com.example.nawigacja_wewnatrz_budynku.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.nawigacja_wewnatrz_budynku.api.RetrofitClient
import com.example.nawigacja_wewnatrz_budynku.api.dataClasses.ApiResponse
import com.example.nawigacja_wewnatrz_budynku.dataClasses.Account
import com.example.nawigacja_wewnatrz_budynku.otherScripts.db
import com.example.nawigacja_wewnatrz_budynku.sealedClasses.RegisterError

class RegisterViewModel : ViewModel() {
    suspend fun register(email: String, password: String) : Result<ApiResponse<Account>>
    {
        return try {
            val response = RetrofitClient.api.register(email, password, "77ea92438a93f6f240dca1f0a16cf2b581006aae")
            if(!response.status)
            {
                val error = when(response.error)
                {
                    "EMAIL_ALREADY_EXIST" -> RegisterError.EmailAlreadyExist
                    "INVALID_EMAIL_PATTERN" -> RegisterError.InvalidEmailPattern
                    "INVALID_PASSWORD_PATTERN" -> RegisterError.InvalidPasswordPattern
                    else -> RegisterError.Unknown(response.error)
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
}