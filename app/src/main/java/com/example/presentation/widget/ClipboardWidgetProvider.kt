package com.example.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.room.Room
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.repository.GeminiAiRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClipboardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_TRIGGER_AI_CLEAN -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "clipboard_db"
                        ).fallbackToDestructiveMigration(dropAllTables = true).build()
                        
                        val dao = db.clipboardDao()
                        val items = dao.getAllItems().first()
                        if (items.isNotEmpty()) {
                            val latestItem = items.first()
                            val aiRepository = GeminiAiRepositoryImpl(context.applicationContext)
                            val analysisResult = aiRepository.analyzeClipboardContent(latestItem.text)
                            val summaryResult = aiRepository.summarize(latestItem.text)

                            var category = latestItem.category
                            var tags = latestItem.tags
                            var preview = latestItem.preview

                            analysisResult.onSuccess { analysis ->
                                category = analysis.category
                                tags = analysis.tags.joinToString(", ")
                            }

                            summaryResult.onSuccess { summaryText ->
                                if (summaryText.isNotBlank()) {
                                    preview = "✨ $summaryText"
                                }
                            }

                            val updated = latestItem.copy(
                                category = category,
                                tags = tags,
                                preview = preview
                            )
                            dao.updateItem(updated)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        updateAllWidgets(context)
                        pendingResult.finish()
                    }
                }
            }
            ACTION_REFRESH_WIDGET -> {
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_AI_CLEAN = "com.example.ACTION_TRIGGER_AI_CLEAN"
        const val ACTION_REFRESH_WIDGET = "com.example.ACTION_REFRESH_WIDGET"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ClipboardWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_clipboard)

            // Intent to open Main App
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_open_app, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_item_card, openAppPendingIntent)

            // Intent for AI Action
            val aiIntent = Intent(context, ClipboardWidgetProvider::class.java).apply {
                action = ACTION_TRIGGER_AI_CLEAN
            }
            val aiPendingIntent = PendingIntent.getBroadcast(
                context, 1, aiIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_ai_clean, aiPendingIntent)

            // Intent for Sync / Refresh
            val refreshIntent = Intent(context, ClipboardWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 2, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_sync, refreshPendingIntent)

            // Async load latest DB item
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "clipboard_db"
                    ).fallbackToDestructiveMigration(dropAllTables = true).build()

                    val dao = db.clipboardDao()
                    val items = dao.getAllItems().first()
                    if (items.isNotEmpty()) {
                        val latest = items.first()
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val timeStr = sdf.format(Date(latest.timestamp))

                        views.setTextViewText(R.id.widget_item_preview, latest.preview.ifBlank { latest.text })
                        views.setTextViewText(R.id.widget_item_category, latest.category.ifBlank { "Text" })
                        views.setTextViewText(R.id.widget_item_time, timeStr)
                    } else {
                        views.setTextViewText(R.id.widget_item_preview, "No clipboard items saved yet.")
                        views.setTextViewText(R.id.widget_item_category, "Empty")
                        views.setTextViewText(R.id.widget_item_time, "--:--")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
