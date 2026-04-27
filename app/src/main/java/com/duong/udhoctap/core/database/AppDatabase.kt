package com.duong.udhoctap.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun tagDao(): TagDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flashcards ADD COLUMN questionType TEXT NOT NULL DEFAULT 'flashcard'")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN explanation TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
