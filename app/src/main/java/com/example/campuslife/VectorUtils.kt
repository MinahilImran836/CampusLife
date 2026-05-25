package com.example.campuslife.utils

import kotlin.math.sqrt

object VectorUtils {

    /**
     * Parses the Room database string formatting "[0.123, -0.456, ...]"
     * clean back into a native Kotlin FloatArray for semantic calculations.
     */
    fun parseStringToFloatArray(embeddingString: String?): FloatArray {
        if (embeddingString.isNullOrEmpty()) return floatArrayOf()

        try {
            val cleanStr = embeddingString.replace("[", "").replace("]", "").trim()
            if (cleanStr.isEmpty()) return floatArrayOf()

            val tokens = cleanStr.split(",")
            val floatArray = FloatArray(tokens.size)
            for (i in tokens.indices) {
                floatArray[i] = tokens[i].trim().toFloat()
            }
            return floatArray
        } catch (e: Exception) {
            e.printStackTrace()
            return floatArrayOf()
        }
    }

    /**
     * Computes the Cosine Similarity metric between two high-dimensional arrays.
     * Returns a rating score value between -1.0 (opposite) and 1.0 (identical).
     */
    fun calculateCosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.isEmpty() || vectorB.isEmpty() || vectorA.size != vectorB.size) return 0f

        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f

        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }

        if (normA == 0.0f || normB == 0.0f) return 0f

        return (dotProduct / (sqrt(normA.toDouble()) * sqrt(normB.toDouble()))).toFloat()
    }
}