package com.duong.udhoctap.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["deckId"])]
)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deckId: Long,
    val front: String,
    val back: String,
    val imageUri: String? = null,
    val isDraft: Boolean = false,
    // Question type: "flashcard" (default), "essay", "multiple_choice"
    val questionType: String = "flashcard",
    // Extra explanation shown after answering
    val explanation: String = "",
    // FSRS fields (Phase 4, defaults for now)
    val stability: Float = 0f,
    val difficulty: Float = 0f,
    val retrievability: Float = 1f,
    val dueDate: Long = System.currentTimeMillis(),
    val lapses: Int = 0,
    val reps: Int = 0,
    val state: String = "New", // New, Learning, Review, Relearning
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
