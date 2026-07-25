package com.example.data.worker

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.ClipboardClassifier
import com.example.data.local.ClipboardItemEntity
import com.example.domain.rules.SmartFolderEngine
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ClipboardSyncWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic background clipboard sync work...")
        
        return try {
            val db = Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                "clipboard_db"
            ).fallbackToDestructiveMigration(dropAllTables = true).build()

            val dao = db.clipboardDao()

            // 1. Process and auto-classify existing items that need tags or categories
            val allItems = dao.getAllItems().first()
            var updatedCount = 0

            allItems.forEach { item ->
                var needsUpdate = false
                var newCategory = item.category
                var newTags = item.tags

                if (newCategory.isBlank() || newCategory == "Text" || newCategory == "General") {
                    val classified = ClipboardClassifier.classify(item.text)
                    if (classified != newCategory) {
                        newCategory = classified
                        needsUpdate = true
                    }
                }

                if (newTags.isBlank()) {
                    val generatedTagsList = SmartFolderEngine.generateAutoTags(item.copy(category = newCategory))
                    if (generatedTagsList.isNotEmpty()) {
                        newTags = generatedTagsList.joinToString(",")
                        needsUpdate = true
                    }
                }

                if (needsUpdate) {
                    dao.updateItem(item.copy(category = newCategory, tags = newTags))
                    updatedCount++
                }
            }

            // 2. Evaluate Smart Rules for automatic folder grouping
            val smartRules = dao.getAllSmartRules().first()
            if (smartRules.isNotEmpty()) {
                val currentItems = dao.getAllItems().first()
                val groups = dao.getAllGroups().first()

                for (rule in smartRules) {
                    if (!rule.isEnabled) continue
                    val targetGroup = groups.find { it.name.equals(rule.targetFolderName, ignoreCase = true) } ?: continue
                    val matchingItems = currentItems.filter { SmartFolderEngine.matchesRule(it, rule) }

                    for (match in matchingItems) {
                        try {
                            dao.insertGroupCrossRef(
                                com.example.data.local.GroupItemCrossRefEntity(targetGroup.id, match.id)
                            )
                        } catch (e: Exception) {
                            // Cross-ref might already exist
                        }
                    }
                }
            }

            // 3. Try reading system clipboard safely in background if permitted
            try {
                val clipboardManager = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
                    val clip = clipboardManager.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val clipText = clip.getItemAt(0).text?.toString()
                        if (!clipText.isNullOrBlank()) {
                            val existing = allItems.find { it.text == clipText }
                            if (existing == null) {
                                val category = ClipboardClassifier.classify(clipText)
                                val preview = if (clipText.length > 100) clipText.take(100) + "..." else clipText
                                val autoTags = SmartFolderEngine.generateAutoTags(
                                    ClipboardItemEntity(text = clipText, category = category, preview = preview, sourceApp = "Background Sync")
                                ).joinToString(",")

                                val newItem = ClipboardItemEntity(
                                    text = clipText,
                                    category = category,
                                    tags = autoTags,
                                    preview = preview,
                                    timestamp = System.currentTimeMillis(),
                                    sourceApp = "Background Sync"
                                )
                                dao.insertItem(newItem)
                                Log.d(TAG, "New clipboard item synced from background!")
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Background clipboard access restricted by Android OS: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking system clipboard: ${e.message}")
            }

            Log.d(TAG, "Background sync completed successfully. Updated $updatedCount items.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing background clipboard sync worker", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "ClipboardSyncWorker"
        const val PERIODIC_WORK_NAME = "clipboard_periodic_sync_work"
        const val ONE_TIME_WORK_NAME = "clipboard_one_time_sync_work"
    }
}

object ClipboardSyncScheduler {
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<ClipboardSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ClipboardSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }

    fun runOneTimeSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val oneTimeSync = OneTimeWorkRequestBuilder<ClipboardSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ClipboardSyncWorker.ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeSync
        )
    }
}
