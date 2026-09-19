package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.SchoolRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("School Portal", appName)
  }

  @Test
  fun `test search history save, retrieve, delete, and clear`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context)
    val repository = SchoolRepository(database)

    // Initially clear any past history
    repository.clearSearchHistory()
    var searches = repository.recentSearches.first()
    assertEquals(0, searches.size)

    // Save initial query
    repository.saveSearchQuery("STU-10-A-01", "STUDENT")
    searches = repository.recentSearches.first()
    assertEquals(1, searches.size)
    assertEquals("STU-10-A-01", searches[0].query)
    assertEquals("STUDENT", searches[0].searchType)

    // Save second query
    repository.saveSearchQuery("Teacher Sharma", "TEACHER")
    searches = repository.recentSearches.first()
    assertEquals(2, searches.size)
    assertEquals("Teacher Sharma", searches[0].query)

    // Re-saving "STU-10-A-01" should move it to the top without duplicate
    repository.saveSearchQuery("STU-10-A-01", "STUDENT")
    searches = repository.recentSearches.first()
    assertEquals(2, searches.size)
    assertEquals("STU-10-A-01", searches[0].query)

    // Delete single item
    val itemToDelete = searches[0].id
    repository.deleteSearchById(itemToDelete)
    searches = repository.recentSearches.first()
    assertEquals(1, searches.size)
    assertEquals("Teacher Sharma", searches[0].query)

    // Clear all
    repository.clearSearchHistory()
    searches = repository.recentSearches.first()
    assertTrue(searches.isEmpty())
  }

  @Test
  fun `test barcode student id parsing and exam marks recording`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context)
    val repository = SchoolRepository(database)

    // Test barcode parsing logic for standard QR payload "SCH-1001:10:A:01:STU-10-A-01" or raw ID
    val scannedQrPayload = "SCH-1001:10:A:01:STU-10-A-01"
    val parsedStudentId = com.example.util.BarcodeUtil.parseStudentIdFromScan(scannedQrPayload)
    assertEquals("STU-10-A-01", parsedStudentId)

    val rawBarcodeId = "STU-10-A-01"
    assertEquals("STU-10-A-01", com.example.util.BarcodeUtil.parseStudentIdFromScan(rawBarcodeId))

    // Insert an exam result for student with customized subject
    val examResult = com.example.data.model.ExamResultEntity(
      studentId = "STU-10-A-01",
      schoolId = "SCH-1001",
      examType = "Monthly Exam - Sept 2026",
      examMonthYear = "2026-09",
      subject = "Robotics & AI",
      marksObtained = 95,
      maxMarks = 100,
      grade = "A+",
      remarks = "Outstanding performance"
    )
    repository.insertExamResult(examResult)

    val studentResults = repository.getResultsForStudent("STU-10-A-01").first()
    assertTrue(studentResults.any { it.subject == "Robotics & AI" && it.marksObtained == 95 })
  }

  @Test
  fun testTeacherStudentSearchAndBarcodeSelectionForNotificationDispatch() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context)
    val repository = SchoolRepository(database)

    val sampleStudent = com.example.data.model.StudentEntity(
      id = "STU-10-A-01",
      schoolId = "SCH-1001",
      schoolName = "Delhi Public Academy",
      name = "Rahul Sharma",
      className = "10",
      section = "A",
      rollNumber = "01",
      fatherName = "Ramesh Sharma",
      motherName = "Pooja Sharma",
      parentMobile = "9876543210",
      studentMobile = "9876543211",
      password = "pass",
      gender = "Male",
      casteCategory = "General",
      bloodGroup = "O+",
      photoUri = "",
      barcodeData = "SCH-1001:10:A:01:STU-10-A-01"
    )
    repository.insertStudent(sampleStudent)

    val students = repository.allStudents.first()
    assertTrue(students.isNotEmpty())

    // 1. Search by student name
    val searchByNameQuery = "Rahul"
    val matchedByName = students.filter { it.name.contains(searchByNameQuery, ignoreCase = true) }
    assertTrue(matchedByName.isNotEmpty())

    // 2. Search by barcode / ID
    val searchByBarcodeQuery = "STU-10-A-01"
    val matchedByBarcode = students.filter { it.id.contains(searchByBarcodeQuery, ignoreCase = true) }
    assertTrue(matchedByBarcode.isNotEmpty())

    // 3. Barcode scan resolution
    val scannedBarcodePayload = "SCH-1001:10:A:01:STU-10-A-01"
    val resolvedStudentId = com.example.util.BarcodeUtil.parseStudentIdFromScan(scannedBarcodePayload)
    assertEquals("STU-10-A-01", resolvedStudentId)

    val targetStudent = students.first { it.id == resolvedStudentId }
    assertEquals("Rahul Sharma", targetStudent.name)

    // 4. Dispatch targeted notification to scanned/selected student
    val notification = com.example.data.model.NotificationEntity(
      schoolId = targetStudent.schoolId,
      senderTeacherId = "TCH-001",
      senderName = "Mrs. Sunita Verma",
      targetType = "STUDENT",
      targetStudentId = targetStudent.id,
      title = "Science Project Submission Notice",
      message = "Dear Rahul, your science exhibition project is due next Monday.",
      imageUrl = null,
      isPaymentAlert = false,
      amountDue = null
    )
    repository.sendNotification(notification)

    // 5. Verify student receives targeted notification
    val studentNotices = repository.getNotificationsForStudent(
      schoolId = targetStudent.schoolId,
      studentId = targetStudent.id,
      className = targetStudent.className,
      section = targetStudent.section
    ).first()

    assertTrue(studentNotices.any { it.title == "Science Project Submission Notice" && it.targetStudentId == "STU-10-A-01" })
  }

  @Test
  fun `test firestore sync service initialization and offline graceful handling`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getDatabase(context)
    val syncService = com.example.data.sync.FirestoreSyncService(context, database)

    // Verify initial sync state
    val initialState = syncService.syncState.value
    assertEquals(false, initialState.isSyncing)

    // Execute Room to Firestore sync; should handle absence of FirebaseApp gracefully in unit test
    val result = syncService.syncRoomToFirestore()
    assertTrue(result is com.example.data.sync.SyncResult)

    val finalState = syncService.syncState.value
    assertEquals(false, finalState.isSyncing)
  }
}
