package com.duong.udhoctap.core.database.dao

import androidx.room.*
import com.duong.udhoctap.core.database.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY createdAt DESC")
    fun getFlashcardsByDeckId(deckId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun getFlashcardById(id: Long): FlashcardEntity?

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND isDraft = 0 ORDER BY createdAt DESC")
    fun getNonDraftByDeckId(deckId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND dueDate <= :now AND isDraft = 0")
    suspend fun getDueCards(deckId: Long, now: Long = System.currentTimeMillis()): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND isDraft = 0")
    suspend fun getAllCardsForQuiz(deckId: Long): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE isDraft = 1 AND deckId = :deckId")
    fun getDraftCards(deckId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId")
    fun getCardCount(deckId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards")
    fun getTotalCardCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE dueDate <= :now AND isDraft = 0")
    fun getTotalDueCardCount(now: Long = System.currentTimeMillis()): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteFlashcardById(id: Long)
}
