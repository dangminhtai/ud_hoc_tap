package com.duong.udhoctap.core.data.repository

import com.duong.udhoctap.core.database.dao.TagDao
import com.duong.udhoctap.core.database.entity.DeckTagCrossRef
import com.duong.udhoctap.core.database.entity.FlashcardTagCrossRef
import com.duong.udhoctap.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
) : TagRepository {
    override fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    override suspend fun getTagById(tagId: Long): TagEntity? = tagDao.getTagById(tagId)

    override fun getTagsForDeck(deckId: Long): Flow<List<TagEntity>> = tagDao.getTagsForDeck(deckId)

    override fun getTagsForFlashcard(flashcardId: Long): Flow<List<TagEntity>> =
        tagDao.getTagsForFlashcard(flashcardId)

    override suspend fun insertTag(tag: TagEntity): Long = tagDao.insertTag(tag)

    override suspend fun updateTag(tag: TagEntity) = tagDao.updateTag(tag)

    override suspend fun deleteTag(tag: TagEntity) = tagDao.deleteTag(tag)

    override suspend fun insertDeckTagCrossRef(crossRef: DeckTagCrossRef) = tagDao.insertDeckTagCrossRef(crossRef)

    override suspend fun deleteDeckTagCrossRef(crossRef: DeckTagCrossRef) = tagDao.deleteDeckTagCrossRef(crossRef)

    override suspend fun insertFlashcardTagCrossRef(crossRef: FlashcardTagCrossRef) =
        tagDao.insertFlashcardTagCrossRef(crossRef)

    override suspend fun deleteFlashcardTagCrossRef(crossRef: FlashcardTagCrossRef) =
        tagDao.deleteFlashcardTagCrossRef(crossRef)
}
