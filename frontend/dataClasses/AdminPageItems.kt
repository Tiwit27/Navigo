package com.example.nawigacja_wewnatrz_budynku.dataClasses

data class AdminPageItems(
    val iconResId: Int,
    val label: String,
    val callback: () -> Unit
)
