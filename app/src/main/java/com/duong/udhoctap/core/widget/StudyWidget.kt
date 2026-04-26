package com.duong.udhoctap.core.widget
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.duong.udhoctap.MainActivity
import com.duong.udhoctap.core.database.dao.FlashcardDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class StudyWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun flashcardDao(): FlashcardDao
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dueCount = try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java
            )
            entryPoint.flashcardDao().getTotalDueCardCount().first()
        } catch (e: Exception) {
            0
        }

        provideContent {
            GlanceTheme {
                WidgetContent(dueCount = dueCount)
            }
        }
    }
}

@Composable
private fun WidgetContent(dueCount: Int) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF6C63FF)))
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$dueCount",
            style = TextStyle(
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color.White)
            )
        )
        Text(
            text = "thẻ cần ôn",
            style = TextStyle(
                fontSize = 13.sp,
                color = ColorProvider(Color(0xCCFFFFFF))
            )
        )
        Spacer(modifier = GlanceModifier.height(10.dp))
        Text(
            text = "Nhấn để ôn tập →",
            style = TextStyle(
                fontSize = 12.sp,
                color = ColorProvider(Color(0x99FFFFFF))
            )
        )
    }
}

class StudyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudyWidget()
}
