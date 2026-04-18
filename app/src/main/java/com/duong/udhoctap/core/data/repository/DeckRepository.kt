package com.duong.udhoctap.core.data.repository

import com.duong.udhoctap.core.database.entity.DeckEntity
import kotlinx.coroutines.flow.Flow

interface DeckRepository {
    fun getAllDecks(): Flow<List<DeckEntity>>
    suspend fun getDeckById(deckId: Long): DeckEntity?
    fun getDeckByIdFlow(deckId: Long): Flow<DeckEntity?>
    fun searchDecks(query: String): Flow<List<DeckEntity>>
    fun getCardCount(deckId: Long): Flow<Int>
    fun getDueCardCount(deckId: Long, now: Long = System.currentTimeMillis()): Flow<Int>
    suspend fun insertDeck(deck: DeckEntity): Long
    suspend fun updateDeck(deck: DeckEntity)
    suspend fun deleteDeck(deck: DeckEntity)
    suspend fun deleteDeckById(deckId: Long)
}
