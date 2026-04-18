package com.duong.udhoctap.core.database.dao

import androidx.room.*
import com.duong.udhoctap.core.database.entity.TagEntity
import com.duong.udhoctap.core.database.entity.DeckTagCrossRef
import com.duong.udhoctap.core.database.entity.FlashcardTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): TagEntity?

    @Query("""
        SELECT t.* FROM tags t 
        INNER JOIN deck_tag_cross_ref dt ON t.id = dt.tagId 
        WHERE dt.deckId = :deckId
    """)
    fun getTagsForDeck(deckId: Long): Flow<List<TagEntity>>

    @Query("""
        SELECT t.* FROM tags t 
        INNER JOIN flashcard_tag_cross_ref ft ON t.id = ft.tagId 
        WHERE ft.flashcardId = :flashcardId
    """)
    fun getTagsForFlashcard(flashcardId: Long): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeckTagCrossRef(crossRef: DeckTagCrossRef)

    @Delete
    suspend fun deleteDeckTagCrossRef(crossRef: DeckTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFlashcardTagCrossRef(crossRef: FlashcardTagCrossRef)

    @Delete
    suspend fun deleteFlashcardTagCrossRef(crossRef: FlashcardTagCrossRef)
}
