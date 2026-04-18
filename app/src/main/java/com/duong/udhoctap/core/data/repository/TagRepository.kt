package com.duong.udhoctap.core.data.repository

import com.duong.udhoctap.core.database.entity.DeckTagCrossRef
import com.duong.udhoctap.core.database.entity.FlashcardTagCrossRef
import com.duong.udhoctap.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<TagEntity>>
    suspend fun getTagById(tagId: Long): TagEntity?
    fun getTagsForDeck(deckId: Long): Flow<List<TagEntity>>
    fun getTagsForFlashcard(flashcardId: Long): Flow<List<TagEntity>>
    suspend fun insertTag(tag: TagEntity): Long
    suspend fun updateTag(tag: TagEntity)
    suspend fun deleteTag(tag: TagEntity)
    suspend fun insertDeckTagCrossRef(crossRef: DeckTagCrossRef)
    suspend fun deleteDeckTagCrossRef(crossRef: DeckTagCrossRef)
    suspend fun insertFlashcardTagCrossRef(crossRef: FlashcardTagCrossRef)
    suspend fun deleteFlashcardTagCrossRef(crossRef: FlashcardTagCrossRef)
}
