package com.duong.udhoctap.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = FlashcardEntity::class,
            parentColumns = ["id"],
            childColumns = ["flashcardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["flashcardId"])]
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val flashcardId: Long,
    val rating: Int, // 1=Again, 2=Hard, 3=Good, 4=Easy
    val reviewedAt: Long = System.currentTimeMillis(),
    val scheduledDays: Long = 0,
    val elapsedDays: Long = 0,
    val stability: Float = 0f,
    val difficulty: Float = 0f,
    val state: String = "New"
)
