package com.example.aiphysical

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform