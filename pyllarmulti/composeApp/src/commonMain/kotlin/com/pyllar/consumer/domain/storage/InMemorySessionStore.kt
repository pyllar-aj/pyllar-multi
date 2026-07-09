package com.pyllar.consumer.domain.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemorySessionStore {
    private val store = mutableMapOf<String, String>()
    private val lock = Mutex()

    suspend fun saveValue(key: String, value: String) {
        lock.withLock {
            store[key] = value
        }
    }

    suspend fun getValue(key: String): String? {
        return lock.withLock {
            store[key]
        }
    }

    suspend fun clear() {
        lock.withLock {
            store.clear()
        }
    }
}
