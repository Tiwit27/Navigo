package com.example.nawigacja_wewnatrz_budynku.api.dataClasses

data class ApiResponse<T>(
    val status: Boolean,
    val data: T,
    val error: String ?= null,
    val token: String ?= null
)
