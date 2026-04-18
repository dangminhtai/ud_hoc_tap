package com.duong.udhoctap.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "flashcard_tag_cross_ref",
    primaryKeys = ["flashcardId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = FlashcardEntity::class,
            parentColumns = ["id"],
            childColumns = ["flashcardId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["flashcardId"]),
        Index(value = ["tagId"])
    ]
)
data class FlashcardTagCrossRef(
    val flashcardId: Long,
    val tagId: Long
)
