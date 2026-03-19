package com.example.aiphysical.data.model

data class PointsLedgerEntry(
    val eventKey: String = "",
    val title: String = "",
    val description: String = "",
    val points: Int = 0,
    val awardedAt: Long = 0L,
    val sourceType: String = "",
    val sourceId: String = ""
)
