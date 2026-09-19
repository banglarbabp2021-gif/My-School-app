package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SchoolEntity::class,
        TeacherEntity::class,
        StudentEntity::class,
        AttendanceEntity::class,
        ExamResultEntity::class,
        NotificationEntity::class,
        ProductEntity::class,
        AppSettingsEntity::class,
        SearchHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun teacherDao(): TeacherDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun examResultDao(): ExamResultDao
    abstract fun notificationDao(): NotificationDao
    abstract fun productDao(): ProductDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "school_portal_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialData(getDatabase(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            // Prepopulate Schools
            val school1 = SchoolEntity(
                id = "SCH-1001",
                name = "Delhi Public Academy",
                registrationId = "REG-DPA-2026",
                address = "Sector 4, Central Enclave"
            )
            val school2 = SchoolEntity(
                id = "SCH-1002",
                name = "St. Xavier's Model School",
                registrationId = "REG-SXMS-2026",
                address = "Park Street Boulevard"
            )
            db.schoolDao().insertSchool(school1)
            db.schoolDao().insertSchool(school2)

            // Prepopulate Teachers
            val teacher1 = TeacherEntity(
                id = "TCH-501",
                schoolId = "SCH-1001",
                schoolName = "Delhi Public Academy",
                name = "Mrs. Sarah Jenkins",
                email = "sarah.jenkins@school.edu",
                mobile = "+1 98765 43210",
                password = "teacher123",
                subject = "Mathematics & Science",
                assignedClass = "Class 10-A"
            )
            val teacher2 = TeacherEntity(
                id = "TCH-502",
                schoolId = "SCH-1001",
                schoolName = "Delhi Public Academy",
                name = "Mr. Robert Vance",
                email = "robert.vance@school.edu",
                mobile = "+1 98765 43211",
                password = "teacher123",
                subject = "English Literature",
                assignedClass = "Class 10-B"
            )
            db.teacherDao().insertTeacher(teacher1)
            db.teacherDao().insertTeacher(teacher2)

            // Prepopulate Students
            val student1 = StudentEntity(
                id = "STU-10-A-01",
                schoolId = "SCH-1001",
                schoolName = "Delhi Public Academy",
                name = "Aarav Sharma",
                className = "10",
                section = "A",
                rollNumber = "01",
                fatherName = "Rajesh Sharma",
                motherName = "Sunita Sharma",
                parentMobile = "+1 91234 56780",
                studentMobile = "+1 91234 56781",
                password = "pass123",
                gender = "Male",
                casteCategory = "General",
                bloodGroup = "O+",
                photoUri = "",
                barcodeData = "SCH-1001:10:A:01:STU-10-A-01"
            )
            val student2 = StudentEntity(
                id = "STU-10-A-02",
                schoolId = "SCH-1001",
                schoolName = "Delhi Public Academy",
                name = "Priya Patel",
                className = "10",
                section = "A",
                rollNumber = "02",
                fatherName = "Vikram Patel",
                motherName = "Anita Patel",
                parentMobile = "+1 91234 56782",
                studentMobile = "+1 91234 56783",
                password = "pass123",
                gender = "Female",
                casteCategory = "OBC",
                bloodGroup = "B+",
                photoUri = "",
                barcodeData = "SCH-1001:10:A:02:STU-10-A-02"
            )
            val student3 = StudentEntity(
                id = "STU-10-B-01",
                schoolId = "SCH-1001",
                schoolName = "Delhi Public Academy",
                name = "Rohan Das",
                className = "10",
                section = "B",
                rollNumber = "01",
                fatherName = "Debashis Das",
                motherName = "Mita Das",
                parentMobile = "+1 91234 56784",
                studentMobile = "+1 91234 56785",
                password = "pass123",
                gender = "Male",
                casteCategory = "SC",
                bloodGroup = "A+",
                photoUri = "",
                barcodeData = "SCH-1001:10:B:01:STU-10-B-01"
            )
            db.studentDao().insertStudent(student1)
            db.studentDao().insertStudent(student2)
            db.studentDao().insertStudent(student3)

            // Prepopulate Attendance
            val todayStr = "2026-09-17"
            val yesterdayStr = "2026-09-16"
            val twoDaysAgoStr = "2026-09-15"

            db.attendanceDao().insertAttendance(
                AttendanceEntity(
                    studentId = student1.id,
                    studentName = student1.name,
                    schoolId = student1.schoolId,
                    className = student1.className,
                    section = student1.section,
                    rollNumber = student1.rollNumber,
                    type = "ENTRY",
                    timestamp = System.currentTimeMillis() - 28800000L,
                    dateString = todayStr,
                    timeString = "08:15 AM",
                    recordedByTeacherId = "TCH-501"
                )
            )
            db.attendanceDao().insertAttendance(
                AttendanceEntity(
                    studentId = student1.id,
                    studentName = student1.name,
                    schoolId = student1.schoolId,
                    className = student1.className,
                    section = student1.section,
                    rollNumber = student1.rollNumber,
                    type = "EXIT",
                    timestamp = System.currentTimeMillis() - 7200000L,
                    dateString = todayStr,
                    timeString = "02:45 PM",
                    recordedByTeacherId = "TCH-501"
                )
            )
            db.attendanceDao().insertAttendance(
                AttendanceEntity(
                    studentId = student1.id,
                    studentName = student1.name,
                    schoolId = student1.schoolId,
                    className = student1.className,
                    section = student1.section,
                    rollNumber = student1.rollNumber,
                    type = "ENTRY",
                    timestamp = System.currentTimeMillis() - 86400000L,
                    dateString = yesterdayStr,
                    timeString = "08:10 AM",
                    recordedByTeacherId = "TCH-501"
                )
            )
            db.attendanceDao().insertAttendance(
                AttendanceEntity(
                    studentId = student1.id,
                    studentName = student1.name,
                    schoolId = student1.schoolId,
                    className = student1.className,
                    section = student1.section,
                    rollNumber = student1.rollNumber,
                    type = "EXIT",
                    timestamp = System.currentTimeMillis() - 64800000L,
                    dateString = yesterdayStr,
                    timeString = "02:50 PM",
                    recordedByTeacherId = "TCH-501"
                )
            )

            // Prepopulate Exam Results
            val results = listOf(
                ExamResultEntity(
                    studentId = student1.id,
                    schoolId = student1.schoolId,
                    examType = "Monthly Exam - August 2026",
                    examMonthYear = "2026-08",
                    subject = "Mathematics",
                    marksObtained = 94,
                    maxMarks = 100,
                    grade = "A+",
                    remarks = "Excellent problem solving ability"
                ),
                ExamResultEntity(
                    studentId = student1.id,
                    schoolId = student1.schoolId,
                    examType = "Monthly Exam - August 2026",
                    examMonthYear = "2026-08",
                    subject = "Science",
                    marksObtained = 88,
                    maxMarks = 100,
                    grade = "A",
                    remarks = "Good conceptual clarity"
                ),
                ExamResultEntity(
                    studentId = student1.id,
                    schoolId = student1.schoolId,
                    examType = "Monthly Exam - August 2026",
                    examMonthYear = "2026-08",
                    subject = "English",
                    marksObtained = 91,
                    maxMarks = 100,
                    grade = "A+",
                    remarks = "Very good expression"
                ),
                ExamResultEntity(
                    studentId = student1.id,
                    schoolId = student1.schoolId,
                    examType = "Annual Exam - 2026",
                    examMonthYear = "2026-03",
                    subject = "Mathematics",
                    marksObtained = 96,
                    maxMarks = 100,
                    grade = "A+",
                    remarks = "Outstanding performance"
                ),
                ExamResultEntity(
                    studentId = student1.id,
                    schoolId = student1.schoolId,
                    examType = "Annual Exam - 2026",
                    examMonthYear = "2026-03",
                    subject = "Science",
                    marksObtained = 90,
                    maxMarks = 100,
                    grade = "A+",
                    remarks = "Consistent dedication"
                ),
                ExamResultEntity(
                    studentId = student1.id,
                    schoolId = student1.schoolId,
                    examType = "Annual Exam - 2026",
                    examMonthYear = "2026-03",
                    subject = "Social Studies",
                    marksObtained = 85,
                    maxMarks = 100,
                    grade = "A",
                    remarks = "Good historical context understanding"
                )
            )
            db.examResultDao().insertExamResults(results)

            // Prepopulate Notifications
            db.notificationDao().insertNotification(
                NotificationEntity(
                    schoolId = "SCH-1001",
                    senderTeacherId = "TCH-501",
                    senderName = "Mrs. Sarah Jenkins",
                    targetType = "ALL_SCHOOL",
                    title = "Annual Sports Day Notice",
                    message = "Annual Sports Day will be held on Oct 5th. All students are invited to register with house captains.",
                    imageUrl = null,
                    isPaymentAlert = false,
                    timestamp = System.currentTimeMillis() - 172800000L
                )
            )
            db.notificationDao().insertNotification(
                NotificationEntity(
                    schoolId = "SCH-1001",
                    senderTeacherId = "TCH-501",
                    senderName = "Mrs. Sarah Jenkins",
                    targetType = "CLASS",
                    targetClassName = "10",
                    targetSection = "A",
                    title = "Math Mid-Term Syllabus & Formula Sheet",
                    message = "Please revise Chapters 3, 4 and 6 for the upcoming test this Friday. Bring geometry instruments.",
                    imageUrl = null,
                    isPaymentAlert = false,
                    timestamp = System.currentTimeMillis() - 86400000L
                )
            )
            db.notificationDao().insertNotification(
                NotificationEntity(
                    schoolId = "SCH-1001",
                    senderTeacherId = "TCH-501",
                    senderName = "Mrs. Sarah Jenkins",
                    targetType = "STUDENT",
                    targetStudentId = student1.id,
                    title = "Term 2 Lab Fee Due",
                    message = "Reminder: Laboratory and science kit renewal fees are pending for this quarter.",
                    imageUrl = null,
                    isPaymentAlert = true,
                    amountDue = 45.0,
                    timestamp = System.currentTimeMillis() - 3600000L
                )
            )

            // Prepopulate Online Shopping Products
            val products = listOf(
                ProductEntity(
                    title = "Oxford Complete Student Atlas & Compass Set",
                    description = "Comprehensive world geography atlas paired with a high-precision metal geometric compass set for secondary classes.",
                    imageUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500&auto=format&fit=crop&q=60",
                    purchaseUrl = "https://www.amazon.com/s?k=student+school+atlas+compass+set",
                    price = 19.99,
                    category = "Books",
                    inStock = true,
                    targetAudience = "ALL"
                ),
                ProductEntity(
                    title = "Official School Blazer & Tie Uniform Pack",
                    description = "Premium breathable wool-blend navy blazer tailored with official academy crest and coordinated necktie.",
                    imageUrl = "https://images.unsplash.com/photo-1594938298603-c8148c4dae35?w=500&auto=format&fit=crop&q=60",
                    purchaseUrl = "https://www.amazon.com/s?k=navy+school+blazer+uniform",
                    price = 49.50,
                    category = "Uniform",
                    inStock = true,
                    targetAudience = "STUDENT"
                ),
                ProductEntity(
                    title = "Casio FX-991EX Scientific Calculator",
                    description = "High-resolution natural textbook display with 552 functions, matrix calculation, and QR code visualization for STEM students.",
                    imageUrl = "https://images.unsplash.com/photo-1611125832047-1d7ad1e8e48f?w=500&auto=format&fit=crop&q=60",
                    purchaseUrl = "https://www.amazon.com/s?k=casio+scientific+calculator",
                    price = 24.95,
                    category = "Electronics",
                    inStock = true,
                    targetAudience = "ALL"
                ),
                ProductEntity(
                    title = "Ergonomic Hardbound Teacher Planner & Gradebook",
                    description = "All-in-one academic planner with attendance matrices, weekly lesson plans, and tear-out sticky note flags.",
                    imageUrl = "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=500&auto=format&fit=crop&q=60",
                    purchaseUrl = "https://www.amazon.com/s?k=teacher+lesson+planner+gradebook",
                    price = 15.00,
                    category = "Stationery",
                    inStock = true,
                    targetAudience = "TEACHER"
                )
            )
            products.forEach { db.productDao().insertProduct(it) }

            // Prepopulate App Settings
            db.appSettingsDao().saveSettings(
                AppSettingsEntity(
                    configKey = "GLOBAL_CONFIG",
                    appName = "School Portal",
                    logoUrl = ""
                )
            )
        }
    }
}
