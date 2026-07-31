package com.snoretracker.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromSnoreEventList(value: List<SnoreEvent>?): String {
        if (value == null) return "[]"
        return gson.toJson(value)
    }

    @TypeConverter
    fun toSnoreEventList(value: String?): List<SnoreEvent> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<SnoreEvent>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
