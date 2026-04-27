package com.duong.udhoctap.di

import android.content.Context
import androidx.room.Room
import com.duong.udhoctap.core.database.AppDatabase
import com.duong.udhoctap.core.database.dao.DeckDao
import com.duong.udhoctap.core.database.dao.FlashcardDao
import com.duong.udhoctap.core.database.dao.ReviewLogDao
import com.duong.udhoctap.core.database.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ud_hoc_tap_db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideDeckDao(db: AppDatabase): DeckDao = db.deckDao()

    @Provides
    fun provideFlashcardDao(db: AppDatabase): FlashcardDao = db.flashcardDao()

    @Provides
    fun provideReviewLogDao(db: AppDatabase): ReviewLogDao = db.reviewLogDao()

    @Provides
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
}
