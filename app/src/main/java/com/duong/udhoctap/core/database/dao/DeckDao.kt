package com.duong.udhoctap.core.database.dao

import androidx.room.*
import com.duong.udhoctap.core.database.entity.DeckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    @Query("SELECT * FROM decks ORDER BY updatedAt DESC")
    fun getAllDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :deckId")
    suspend fun getDeckById(deckId: Long): DeckEntity?

    @Query("SELECT * FROM decks WHERE id = :deckId")
    fun getDeckByIdFlow(deckId: Long): Flow<DeckEntity?>

    @Query("SELECT * FROM decks WHERE name LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchDecks(query: String): Flow<List<DeckEntity>>

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId")
    fun getCardCount(deckId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId AND dueDate <= :now AND isDraft = 0")
    fun getDueCardCount(deckId: Long, now: Long = System.currentTimeMillis()): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: Long)
}
