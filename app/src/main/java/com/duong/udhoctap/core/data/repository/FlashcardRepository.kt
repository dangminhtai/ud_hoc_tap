package com.duong.udhoctap.core.data.repository

import com.duong.udhoctap.core.database.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

interface FlashcardRepository {
    fun getFlashcardsByDeckId(deckId: Long): Flow<List<FlashcardEntity>>
    suspend fun getFlashcardById(id: Long): FlashcardEntity?
    fun getNonDraftByDeckId(deckId: Long): Flow<List<FlashcardEntity>>
    suspend fun getDueCards(deckId: Long, now: Long = System.currentTimeMillis()): List<FlashcardEntity>
    suspend fun getAllCardsForQuiz(deckId: Long): List<FlashcardEntity>
    fun getDraftCards(deckId: Long): Flow<List<FlashcardEntity>>
    fun getCardCount(deckId: Long): Flow<Int>
    fun getTotalCardCount(): Flow<Int>
    fun getTotalDueCardCount(now: Long = System.currentTimeMillis()): Flow<Int>
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long
    suspend fun updateFlashcard(flashcard: FlashcardEntity)
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)
    suspend fun deleteFlashcardById(id: Long)
}
