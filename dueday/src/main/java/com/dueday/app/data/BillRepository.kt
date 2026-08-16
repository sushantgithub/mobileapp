package com.dueday.app.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class BillRepository(context: Context) {
    private val file = File(context.filesDir, "bills.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): List<Bill> {
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<BillStore>(file.readText()).bills
        }.getOrDefault(emptyList())
    }

    fun save(bills: List<Bill>) {
        file.writeText(json.encodeToString(BillStore(bills = bills)))
    }
}
