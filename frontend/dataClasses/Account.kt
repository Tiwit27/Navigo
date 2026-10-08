package com.example.nawigacja_wewnatrz_budynku.dataClasses

import com.google.gson.annotations.SerializedName

data class Account(
    @SerializedName("account_id")
    val accountId: Int,
    val email: String,
    @SerializedName("institutions_admin")
    val institutionsAdmin: List<Int>
)
