package com.duong.udhoctap.core.database.dao

import androidx.room.*
import com.duong.udhoctap.core.database.entity.ReviewLogEntity
import com.duong.udhoctap.core.database.entity.WeakCardStat
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewLogDao {

    @Query("SELECT * FROM review_logs WHERE flashcardId = :flashcardId ORDER BY reviewedAt DESC")
    fun getReviewsByFlashcardId(flashcardId: Long): Flow<List<ReviewLogEntity>>

    @Query("SELECT COUNT(*) FROM review_logs WHERE reviewedAt >= :startOfDay")
    fun getTodayReviewCount(startOfDay: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM review_logs 
        WHERE reviewedAt >= :startOfDay 
        AND rating >= 3
    """)
    fun getTodayCorrectCount(startOfDay: Long): Flow<Int>

    @Query("""
        SELECT DISTINCT date(reviewedAt / 1000, 'unixepoch', 'localtime') as reviewDate 
        FROM review_logs 
        ORDER BY reviewDate DESC
    """)
    suspend fun getReviewDates(): List<String>

    @Query("""
        SELECT COUNT(*) FROM review_logs 
        WHERE reviewedAt >= :startTime AND reviewedAt < :endTime
    """)
    suspend fun getReviewCountBetween(startTime: Long, endTime: Long): Int

    @Query("""
        SELECT flashcardId, COUNT(*) as againCount
        FROM review_logs
        WHERE rating = 1
        GROUP BY flashcardId
        ORDER BY againCount DESC
    """)
    suspend fun getWeakCardStats(): List<WeakCardStat>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewLog(reviewLog: ReviewLogEntity): Long

    @Query("DELETE FROM review_logs WHERE flashcardId = :flashcardId")
    suspend fun deleteByFlashcardId(flashcardId: Long)
}
