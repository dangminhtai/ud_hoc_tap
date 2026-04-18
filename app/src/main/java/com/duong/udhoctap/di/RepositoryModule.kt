package com.duong.udhoctap.di

import com.duong.udhoctap.core.data.repository.DeckRepository
import com.duong.udhoctap.core.data.repository.DeckRepositoryImpl
import com.duong.udhoctap.core.data.repository.FlashcardRepository
import com.duong.udhoctap.core.data.repository.FlashcardRepositoryImpl
import com.duong.udhoctap.core.data.repository.ReviewRepository
import com.duong.udhoctap.core.data.repository.ReviewRepositoryImpl
import com.duong.udhoctap.core.data.repository.TagRepository
import com.duong.udhoctap.core.data.repository.TagRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDeckRepository(impl: DeckRepositoryImpl): DeckRepository

    @Binds
    @Singleton
    abstract fun bindFlashcardRepository(impl: FlashcardRepositoryImpl): FlashcardRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository
}
