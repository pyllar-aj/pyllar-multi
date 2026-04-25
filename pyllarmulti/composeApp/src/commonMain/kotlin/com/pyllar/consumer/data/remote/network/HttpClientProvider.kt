package com.pyllar.consumer.data.remote.network

import io.ktor.client.HttpClient

/**
 * Platform HTTP client factory.
 *
 * actual implementations live in androidMain/iosMain and configure
 * engines, logging, and timeouts appropriately.
 */
expect fun createHttpClient(): HttpClient

