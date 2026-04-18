package com.duong.udhoctap.core.data.repository

import com.duong.udhoctap.core.database.dao.FlashcardDao
import com.duong.udhoctap.core.database.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FlashcardRepositoryImpl @Inject constructor(
    private val flashcardDao: FlashcardDao
) : FlashcardRepository {
    override fun getFlashcardsByDeckId(deckId: Long): Flow<List<FlashcardEntity>> =
        flashcardDao.getFlashcardsByDeckId(deckId)

    override suspend fun getFlashcardById(id: Long): FlashcardEntity? = flashcardDao.getFlashcardById(id)

    override fun getNonDraftByDeckId(deckId: Long): Flow<List<FlashcardEntity>> =
        flashcardDao.getNonDraftByDeckId(deckId)

    override suspend fun getDueCards(deckId: Long, now: Long): List<FlashcardEntity> =
        flashcardDao.getDueCards(deckId, now)

    override suspend fun getAllCardsForQuiz(deckId: Long): List<FlashcardEntity> =
        flashcardDao.getAllCardsForQuiz(deckId)

    override fun getDraftCards(deckId: Long): Flow<List<FlashcardEntity>> = flashcardDao.getDraftCards(deckId)

    override fun getCardCount(deckId: Long): Flow<Int> = flashcardDao.getCardCount(deckId)

    override fun getTotalCardCount(): Flow<Int> = flashcardDao.getTotalCardCount()

    override fun getTotalDueCardCount(now: Long): Flow<Int> = flashcardDao.getTotalDueCardCount(now)

    override suspend fun insertFlashcard(flashcard: FlashcardEntity): Long = flashcardDao.insertFlashcard(flashcard)

    override suspend fun updateFlashcard(flashcard: FlashcardEntity) = flashcardDao.updateFlashcard(flashcard)

    override suspend fun deleteFlashcard(flashcard: FlashcardEntity) = flashcardDao.deleteFlashcard(flashcard)

    override suspend fun deleteFlashcardById(id: Long) = flashcardDao.deleteFlashcardById(id)
}
