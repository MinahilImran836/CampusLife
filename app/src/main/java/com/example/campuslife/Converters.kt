package com.example.campuslife.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromList(list: MutableList<String>?): String {
        return json.encodeToString(list ?: mutableListOf())
    }

    @TypeConverter
    fun toList(value: String?): MutableList<String> {
        return if (value == null) mutableListOf()
        else json.decodeFromString(value)
    }

    // 🎯 ADD THIS: Converts high-dimensional FloatArray vectors to string JSON arrays for Room
    @TypeConverter
    fun fromFloatArray(array: FloatArray?): String {
        if (array == null) return "[]"
        val floatList = array.toList()
        return json.encodeToString(floatList)
    }

    @TypeConverter
    fun toFloatArray(value: String?): FloatArray {
        if (value.isNullOrEmpty() || value == "[]") return floatArrayOf()
        return try {
            val list: List<Float> = json.decodeFromString(value)
            list.toFloatArray()
        } catch (e: Exception) {
            floatArrayOf()
        }
    }
}