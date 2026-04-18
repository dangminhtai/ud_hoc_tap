package com.duong.udhoctap.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.duong.udhoctap.core.database.converter.Converters
import com.duong.udhoctap.core.database.dao.DeckDao
import com.duong.udhoctap.core.database.dao.FlashcardDao
import com.duong.udhoctap.core.database.dao.ReviewLogDao
import com.duong.udhoctap.core.database.dao.TagDao
import com.duong.udhoctap.core.database.entity.*

@Database(
    entities = [
        DeckEntity::class,
        FlashcardEntity::class,
        ReviewLogEntity::class,
        TagEntity::class,
        DeckTagCrossRef::class,
        FlashcardTagCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun tagDao(): TagDao
}
