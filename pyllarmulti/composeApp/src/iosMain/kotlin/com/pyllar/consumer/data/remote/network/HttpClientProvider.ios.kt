package com.pyllar.consumer.data.remote.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

import com.pyllar.consumer.config.IS_DEBUG

actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }
        )
    }
    install(Logging) {
        level = if (IS_DEBUG) LogLevel.ALL else LogLevel.NONE
        logger = object : Logger {
            override fun log(message: String) {
                if (IS_DEBUG) {
                    println("HTTP(iOS): $message")
                }
            }
        }
    }
}

