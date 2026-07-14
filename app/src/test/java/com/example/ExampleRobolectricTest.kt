package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.ClipboardRepositoryImpl
import com.example.data.repository.GeminiAiRepositoryImpl
import com.example.presentation.clipboard_list.ClipboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private suspend fun waitTasks() {
    for (i in 1..10) {
        delay(50)
        ShadowLooper.idleMainLooper()
    }
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Clipboard AI", appName)
  }

  @Test
  fun `test export and import backups`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    
    val repository = ClipboardRepositoryImpl(db.clipboardDao())
    val aiRepository = GeminiAiRepositoryImpl()
    val viewModel = ClipboardViewModel(repository, aiRepository)
    
    // Add manual item
    viewModel.addManualItem("Hello Test Clipboard Item")
    
    // Wait for insertion to propagate
    waitTasks()
    var items = repository.getAllItems().first()
    assertEquals(1, items.size)
    assertEquals("Hello Test Clipboard Item", items[0].text)
    
    // Create temp file for backup
    val tempFile = File.createTempFile("clipboard_backup", ".json")
    tempFile.deleteOnExit()
    val uri = Uri.fromFile(tempFile)
    
    // Export backup
    var exportSuccess = false
    viewModel.exportBackup(
        contentResolver = context.contentResolver,
        uri = uri,
        onSuccess = { exportSuccess = true },
        onError = { 
            it.printStackTrace()
            fail("Export failed: ${it.localizedMessage}") 
        }
    )
    
    // Allow coroutines to complete the write
    waitTasks()
    assertTrue(exportSuccess)
    assertTrue(tempFile.exists() && tempFile.length() > 0)
    
    // Read the temp file content and verify it contains our text
    val jsonContent = tempFile.readText()
    assertTrue(jsonContent.contains("Hello Test Clipboard Item"))
    
    // Now delete the item from the DB
    db.clipboardDao().deleteItem(items[0])
    waitTasks()
    assertEquals(0, repository.getAllItems().first().size)
    
    // Import backup from the file
    var importSuccess = false
    var importedCount = 0
    viewModel.importBackup(
        contentResolver = context.contentResolver,
        uri = uri,
        onSuccess = { count ->
            importSuccess = true
            importedCount = count
        },
        onError = { 
            it.printStackTrace()
            fail("Import failed: ${it.localizedMessage}") 
        }
    )
    
    waitTasks()
    assertTrue(importSuccess)
    assertEquals(1, importedCount)
    
    // Verify item is restored
    val restoredItems = repository.getAllItems().first()
    assertEquals(1, restoredItems.size)
    assertEquals("Hello Test Clipboard Item", restoredItems[0].text)
    
    db.close()
  }

  @Test
  fun `test privacy toggle masking`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    
    val repository = ClipboardRepositoryImpl(db.clipboardDao())
    val aiRepository = GeminiAiRepositoryImpl()
    val viewModel = ClipboardViewModel(repository, aiRepository)

    // Verify initial state of privacy masking
    assertTrue(viewModel.isPrivacyModeEnabled.value)

    // Toggle privacy masking to false
    viewModel.togglePrivacyMode()
    assertTrue(!viewModel.isPrivacyModeEnabled.value)

    // Toggle back to true
    viewModel.togglePrivacyMode()
    assertTrue(viewModel.isPrivacyModeEnabled.value)

    db.close()
  }

  @Test
  fun `test batch selection actions`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    
    val repository = ClipboardRepositoryImpl(db.clipboardDao())
    val aiRepository = GeminiAiRepositoryImpl()
    val viewModel = ClipboardViewModel(repository, aiRepository)

    // Add manual items
    viewModel.addManualItem("Clipboard item 1")
    viewModel.addManualItem("Clipboard item 2")
    viewModel.addManualItem("Clipboard item 3")

    waitTasks()
    val dbItems = repository.getAllItems().first()
    assertEquals(3, dbItems.size)

    val item1 = dbItems.find { it.text == "Clipboard item 1" }!!
    val item2 = dbItems.find { it.text == "Clipboard item 2" }!!
    val item3 = dbItems.find { it.text == "Clipboard item 3" }!!

    val id1 = item1.id
    val id2 = item2.id
    val id3 = item3.id

    // Initially selection is empty
    assertTrue(viewModel.selectedItemIds.value.isEmpty())

    // Select first item
    viewModel.toggleSelection(id1)
    assertEquals(setOf(id1), viewModel.selectedItemIds.value)

    // Select second item
    viewModel.toggleSelection(id2)
    assertEquals(setOf(id1, id2), viewModel.selectedItemIds.value)

    // Deselect first item
    viewModel.toggleSelection(id1)
    assertEquals(setOf(id2), viewModel.selectedItemIds.value)

    // Select all items
    viewModel.selectAll(listOf(id1, id2, id3))
    assertEquals(setOf(id1, id2, id3), viewModel.selectedItemIds.value)

    // Mass categorize selected items to URL
    viewModel.categorizeSelected("URL")
    waitTasks()

    val updatedItems = repository.getAllItems().first()
    assertEquals(3, updatedItems.size)
    assertTrue(updatedItems.all { it.category == "URL" })
    // Verify selection is cleared after categorization action
    assertTrue(viewModel.selectedItemIds.value.isEmpty())

    // Re-select all for mass export and mass delete
    viewModel.selectAll(listOf(id1, id2, id3))
    val tempExportFile = File.createTempFile("selected_backup", ".json")
    tempExportFile.deleteOnExit()
    val exportUri = Uri.fromFile(tempExportFile)

    var exportSuccess = false
    viewModel.exportSelected(
        contentResolver = context.contentResolver,
        uri = exportUri,
        onSuccess = { exportSuccess = true },
        onError = { fail("Export selected failed: ${it.localizedMessage}") }
    )

    waitTasks()
    assertTrue(exportSuccess)
    assertTrue(tempExportFile.exists() && tempExportFile.length() > 0)
    assertTrue(tempExportFile.readText().contains("Clipboard item 1"))
    assertTrue(tempExportFile.readText().contains("Clipboard item 2"))
    assertTrue(tempExportFile.readText().contains("Clipboard item 3"))

    // Selection should be cleared after exporting selected
    assertTrue(viewModel.selectedItemIds.value.isEmpty())

    // Select items again for deletion
    viewModel.selectAll(listOf(id1, id3))
    viewModel.deleteSelected()
    waitTasks()

    // Verify deleted items are gone
    val remainingItems = repository.getAllItems().first()
    assertEquals(1, remainingItems.size)
    assertEquals(id2, remainingItems[0].id)
    assertEquals("Clipboard item 2", remainingItems[0].text)

    // Verify selection is cleared
    assertTrue(viewModel.selectedItemIds.value.isEmpty())

    db.close()
  }
}

