package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey val id: String, // e.g. "SCH-101"
    val name: String,
    val registrationId: String,
    val address: String = "Main Campus",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey val id: String, // e.g. "TCH-501"
    val schoolId: String,
    val schoolName: String,
    val name: String,
    val email: String,
    val mobile: String,
    val password: String,
    val subject: String = "General",
    val assignedClass: String = "Class 10-A"
)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String, // Barcode ID, e.g. "STU-10-A-01"
    val schoolId: String,
    val schoolName: String,
    val name: String,
    val className: String, // e.g. "10"
    val section: String,   // e.g. "A"
    val rollNumber: String, // e.g. "01"
    val fatherName: String,
    val motherName: String,
    val parentMobile: String,
    val studentMobile: String,
    val password: String,
    val gender: String, // "Male", "Female", "Other"
    val casteCategory: String, // "General", "OBC", "SC", "ST"
    val bloodGroup: String, // "O+", "A+", "B+", "AB+", etc.
    val photoUri: String = "", // URI or sample avatar key
    val barcodeData: String = "", // generated barcode data
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val studentName: String,
    val schoolId: String,
    val className: String,
    val section: String,
    val rollNumber: String,
    val type: String, // "ENTRY" or "EXIT"
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // "2026-09-17"
    val timeString: String, // "08:30 AM"
    val recordedByTeacherId: String
)

@Entity(tableName = "exam_results")
data class ExamResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val schoolId: String,
    val examType: String, // "Monthly Exam - Aug 2026", "Annual Exam - 2026"
    val examMonthYear: String, // "2026-08"
    val subject: String,
    val marksObtained: Int,
    val maxMarks: Int = 100,
    val grade: String,
    val remarks: String = ""
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val schoolId: String,
    val senderTeacherId: String,
    val senderName: String,
    val targetType: String, // "STUDENT", "CLASS", "ALL_SCHOOL"
    val targetStudentId: String? = null,
    val targetClassName: String? = null,
    val targetSection: String? = null,
    val title: String,
    val message: String,
    val imageUrl: String? = null,
    val isPaymentAlert: Boolean = false,
    val amountDue: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val imageUrl: String,
    val purchaseUrl: String,
    val price: Double,
    val category: String, // "Books", "Uniform", "Stationery", "Electronics", "General"
    val inStock: Boolean = true,
    val targetAudience: String = "ALL", // "ALL", "STUDENT", "TEACHER"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val configKey: String = "GLOBAL_CONFIG",
    val appName: String = "School Portal",
    val logoUrl: String = ""
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchType: String = "ALL", // "ALL", "TEACHER", "STUDENT"
    val timestamp: Long = System.currentTimeMillis()
)
