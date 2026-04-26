package com.duong.udhoctap.core.data.repository

import com.duong.udhoctap.core.database.entity.ReviewLogEntity
import com.duong.udhoctap.core.database.entity.WeakCardStat
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun getReviewsByFlashcardId(flashcardId: Long): Flow<List<ReviewLogEntity>>
    fun getTodayReviewCount(startOfDay: Long): Flow<Int>
    fun getTodayCorrectCount(startOfDay: Long): Flow<Int>
    suspend fun getReviewDates(): List<String>
    suspend fun getReviewCountBetween(startTime: Long, endTime: Long): Int
    suspend fun getWeakCardStats(): List<WeakCardStat>
    suspend fun insertReviewLog(reviewLog: ReviewLogEntity): Long
    suspend fun deleteByFlashcardId(flashcardId: Long)
}
