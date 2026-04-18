package com.duong.udhoctap.core.data.repository

import com.duong.udhoctap.core.database.dao.ReviewLogDao
import com.duong.udhoctap.core.database.entity.ReviewLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ReviewRepositoryImpl @Inject constructor(
    private val reviewLogDao: ReviewLogDao
) : ReviewRepository {
    override fun getReviewsByFlashcardId(flashcardId: Long): Flow<List<ReviewLogEntity>> =
        reviewLogDao.getReviewsByFlashcardId(flashcardId)

    override fun getTodayReviewCount(startOfDay: Long): Flow<Int> = reviewLogDao.getTodayReviewCount(startOfDay)

    override fun getTodayCorrectCount(startOfDay: Long): Flow<Int> = reviewLogDao.getTodayCorrectCount(startOfDay)

    override suspend fun getReviewDates(): List<String> = reviewLogDao.getReviewDates()

    override suspend fun getReviewCountBetween(startTime: Long, endTime: Long): Int =
        reviewLogDao.getReviewCountBetween(startTime, endTime)

    override suspend fun insertReviewLog(reviewLog: ReviewLogEntity): Long = reviewLogDao.insertReviewLog(reviewLog)

    override suspend fun deleteByFlashcardId(flashcardId: Long) = reviewLogDao.deleteByFlashcardId(flashcardId)
}
