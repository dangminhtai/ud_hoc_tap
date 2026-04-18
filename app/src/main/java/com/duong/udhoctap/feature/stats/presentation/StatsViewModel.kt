package com.duong.udhoctap.feature.stats.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duong.udhoctap.core.data.repository.DeckRepository
import com.duong.udhoctap.core.data.repository.FlashcardRepository
import com.duong.udhoctap.core.data.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class StatsUiState(
    val todayReviewed: Int = 0,
    val streak: Int = 0,
    val accuracy: Int = 0,
    val weeklyData: List<Int> = List(7) { 0 },
    val totalCards: Int = 0,
    val totalReviews: Int = 0,
    val totalDecks: Int = 0
)

private data class StatsSnapshot(
    val todayReviewed: Int,
    val correctCount: Int,
    val totalDecks: Int,
    val totalCards: Int
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        val now = LocalDate.now()
        val startOfToday = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            combine(
                reviewRepository.getTodayReviewCount(startOfToday),
                reviewRepository.getTodayCorrectCount(startOfToday),
                deckRepository.getAllDecks(),
                flashcardRepository.getTotalCardCount()
            ) { todayCount, correctCount, decks, totalCards ->
                StatsSnapshot(
                    todayReviewed = todayCount,
                    correctCount = correctCount,
                    totalDecks = decks.size,
                    totalCards = totalCards
                )
            }.collect { snapshot ->
                val accuracy = if (snapshot.todayReviewed > 0) {
                    (snapshot.correctCount * 100) / snapshot.todayReviewed
                } else {
                    0
                }

                val weeklyData = mutableListOf<Int>()
                for (daysAgo in 6 downTo 0) {
                    val day = now.minusDays(daysAgo.toLong())
                    val dayStart = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val dayEnd = day.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    weeklyData += reviewRepository.getReviewCountBetween(dayStart, dayEnd)
                }

                val dates = reviewRepository.getReviewDates()
                val streak = calculateStreak(dates)
                val totalReviews = reviewRepository.getReviewCountBetween(0L, Long.MAX_VALUE)

                _uiState.value = StatsUiState(
                    todayReviewed = snapshot.todayReviewed,
                    streak = streak,
                    accuracy = accuracy,
                    weeklyData = weeklyData,
                    totalCards = snapshot.totalCards,
                    totalReviews = totalReviews,
                    totalDecks = snapshot.totalDecks
                )
            }
        }
    }

    private fun calculateStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0

        var streak = 0
        var currentDate = LocalDate.now()

        // Check if studied today
        val todayStr = currentDate.toString()
        if (dates.firstOrNull() != todayStr) {
            // Check yesterday
            currentDate = currentDate.minusDays(1)
            if (dates.firstOrNull() != currentDate.toString()) return 0
        }

        for (dateStr in dates) {
            if (dateStr == currentDate.toString()) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }
}
