package com.example.aiphysical.data.model

data class KpiData(
    val burnoutIndex: Float = 0f,      // 0–100
    val avgStressLevel: Float = 0f,    // 0–100
    val courseEngagement: Float = 0f,  // 0–100
    // Extended metrics
    val avgBurnout: Float = 0f,
    val avgEmotion: Float = 50f,
    val avgMotivation: Float = 50f,
    val avgAnxiety: Float = 0f
)

/** One data point in the 30-day emotional trend chart. dayOffset: 0 = today, -29 = 30 days ago */
data class TrendPoint(
    val dayOffset: Int = 0,
    val stressValue: Float = 0f,
    val burnoutValue: Float = 0f
)
