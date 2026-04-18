package com.duong.udhoctap.core.data.repository

import com.duong.udhoctap.core.database.dao.DeckDao
import com.duong.udhoctap.core.database.entity.DeckEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeckRepositoryImpl @Inject constructor(
    private val deckDao: DeckDao
) : DeckRepository {
    override fun getAllDecks(): Flow<List<DeckEntity>> = deckDao.getAllDecks()

    override suspend fun getDeckById(deckId: Long): DeckEntity? = deckDao.getDeckById(deckId)

    override fun getDeckByIdFlow(deckId: Long): Flow<DeckEntity?> = deckDao.getDeckByIdFlow(deckId)

    override fun searchDecks(query: String): Flow<List<DeckEntity>> = deckDao.searchDecks(query)

    override fun getCardCount(deckId: Long): Flow<Int> = deckDao.getCardCount(deckId)

    override fun getDueCardCount(deckId: Long, now: Long): Flow<Int> = deckDao.getDueCardCount(deckId, now)

    override suspend fun insertDeck(deck: DeckEntity): Long = deckDao.insertDeck(deck)

    override suspend fun updateDeck(deck: DeckEntity) = deckDao.updateDeck(deck)

    override suspend fun deleteDeck(deck: DeckEntity) = deckDao.deleteDeck(deck)

    override suspend fun deleteDeckById(deckId: Long) = deckDao.deleteDeckById(deckId)
}
