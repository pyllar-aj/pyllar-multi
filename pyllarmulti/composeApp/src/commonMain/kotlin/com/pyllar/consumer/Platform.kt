package com.pyllar.consumer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform