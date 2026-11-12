package com.example.model

data class ClippingItem(
    val id: String = System.currentTimeMillis().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "Manual Copy" // or "Auto-Copy on Exit"
)
