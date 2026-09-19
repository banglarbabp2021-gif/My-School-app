package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthResult
import com.example.data.auth.FirebaseAuthService
import com.example.data.firestore.FirestoreRepository
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.SchoolRepository
import com.example.data.session.SessionManager
import com.example.data.session.UserSessionState
import com.example.data.sync.FirestoreSyncService
import com.example.data.sync.SyncResult
import com.example.data.sync.SyncState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SchoolViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = SchoolRepository(database)
    val sessionManager = SessionManager(application)

    // Firebase Authentication Service (Admin, Teacher, Student)
    val firebaseAuthService = FirebaseAuthService(application, database)

    // Direct Real-time Firestore Cloud Repository
    val firestoreRepository = FirestoreRepository(application, database)

    // Room-to-Firestore Data Synchronization Service
    val firestoreSyncService = FirestoreSyncService(application, database)
    val syncState: StateFlow<SyncState> = firestoreSyncService.syncState

    val session: StateFlow<UserSessionState> = sessionManager.sessionFlow

    val appSettings: StateFlow<AppSettingsEntity?> = repository.appSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Admin & General: Real-time schools from Firestore (with Room fallback)
    val allSchools: StateFlow<List<SchoolEntity>> = firestoreRepository.getAllSchoolsRealtime()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTeachers: StateFlow<List<TeacherEntity>> = repository.allTeachers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStudents: StateFlow<List<StudentEntity>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<SearchHistoryEntity>> = repository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schoolCount: StateFlow<Int> = repository.schoolCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val teacherCount: StateFlow<Int> = repository.teacherCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val studentCount: StateFlow<Int> = repository.studentCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Current Student Profile Flow - Strictly scoped to student's own ID
    val currentStudent: StateFlow<StudentEntity?> = session.flatMapLatest { sess ->
        if (sess.role == "STUDENT" && sess.userId.isNotBlank()) {
            firestoreRepository.getStudentProfileRealtime(sess.userId)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current Student Attendance History - Strictly scoped to student's own ID
    val studentAttendance: StateFlow<List<AttendanceEntity>> = session.flatMapLatest { sess ->
        if (sess.role == "STUDENT" && sess.userId.isNotBlank()) {
            firestoreRepository.getStudentAttendanceRealtime(sess.userId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Student Exam Results - Strictly scoped to student's own ID
    val studentExamResults: StateFlow<List<ExamResultEntity>> = session.flatMapLatest { sess ->
        if (sess.role == "STUDENT" && sess.userId.isNotBlank()) {
            firestoreRepository.getStudentExamResultsRealtime(sess.userId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Student Notifications - Strictly scoped to student's school and targeted to student/class
    val studentNotifications: StateFlow<List<NotificationEntity>> = session.flatMapLatest { sess ->
        if (sess.role == "STUDENT" && sess.userId.isNotBlank()) {
            firestoreRepository.getStudentNoticesRealtime(
                schoolId = sess.schoolId,
                studentId = sess.userId,
                className = sess.userClass,
                section = sess.userSection
            )
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Teacher's School Students - Strictly scoped to Teacher's School
    val teacherSchoolStudents: StateFlow<List<StudentEntity>> = session.flatMapLatest { sess ->
        if (sess.role == "TEACHER" && sess.schoolId.isNotBlank()) {
            firestoreRepository.getTeacherStudentsRealtime(sess.schoolId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Teacher's School Attendance Records - Strictly scoped to Teacher's School
    val teacherSchoolAttendance: StateFlow<List<AttendanceEntity>> = session.flatMapLatest { sess ->
        if (sess.schoolId.isNotBlank()) {
            firestoreRepository.getTeacherAttendanceRealtime(sess.schoolId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Teacher's School Notifications - Strictly scoped to Teacher's School
    val teacherSchoolNotifications: StateFlow<List<NotificationEntity>> = session.flatMapLatest { sess ->
        if (sess.schoolId.isNotBlank()) {
            firestoreRepository.getTeacherNotificationsRealtime(sess.schoolId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Teacher's School Exam Results - Strictly scoped to Teacher's School
    val teacherSchoolExamResults: StateFlow<List<ExamResultEntity>> = session.flatMapLatest { sess ->
        if (sess.schoolId.isNotBlank()) {
            firestoreRepository.getTeacherExamResultsRealtime(sess.schoolId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feedback message for snackbars/toasts
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        // Automatically sync & populate baseline schools/teachers/students into Firestore
        viewModelScope.launch {
            firestoreSyncService.syncRoomToFirestore()
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // --- Authentication Actions using Firebase Auth ---

    suspend fun loginAdmin(adminId: String, pass: String): Boolean {
        return when (val result = firebaseAuthService.authenticateAdmin(adminId, pass)) {
            is AuthResult.Success -> {
                sessionManager.saveSession(result.session)
                _userMessage.value = "Welcome Admin ${result.session.userName}!"
                // Ensure Firestore has latest data
                viewModelScope.launch { firestoreSyncService.syncRoomToFirestore() }
                true
            }
            is AuthResult.Error -> {
                _userMessage.value = result.message
                false
            }
        }
    }

    suspend fun loginTeacher(schoolNameOrId: String, teacherIdOrName: String, pass: String): Boolean {
        return when (val result = firebaseAuthService.authenticateTeacher(schoolNameOrId, teacherIdOrName, pass)) {
            is AuthResult.Success -> {
                sessionManager.saveSession(result.session)
                _userMessage.value = "Welcome, ${result.session.userName}!"
                true
            }
            is AuthResult.Error -> {
                _userMessage.value = result.message
                false
            }
        }
    }

    suspend fun loginStudent(
        schoolQuery: String,
        className: String,
        section: String,
        rollNumber: String,
        pass: String
    ): Boolean {
        return when (val result = firebaseAuthService.authenticateStudentByRoll(schoolQuery, className, section, rollNumber, pass)) {
            is AuthResult.Success -> {
                sessionManager.saveSession(result.session)
                _userMessage.value = "Welcome back, ${result.session.userName}!"
                true
            }
            is AuthResult.Error -> {
                _userMessage.value = result.message
                false
            }
        }
    }

    suspend fun loginStudentByBarcode(barcodeId: String, pass: String): Boolean {
        return when (val result = firebaseAuthService.authenticateStudentByBarcode(barcodeId, pass)) {
            is AuthResult.Success -> {
                sessionManager.saveSession(result.session)
                _userMessage.value = "Welcome, ${result.session.userName}!"
                true
            }
            is AuthResult.Error -> {
                _userMessage.value = result.message
                false
            }
        }
    }

    fun logout() {
        firebaseAuthService.signOut()
        sessionManager.clearSession()
        _userMessage.value = "Logged out successfully."
    }

    fun updateTeacherProfile(updatedTeacher: TeacherEntity) {
        viewModelScope.launch {
            repository.updateTeacher(updatedTeacher)
            firestoreSyncService.replicateTeacherToCloud(updatedTeacher)
            if (session.value.userId == updatedTeacher.id) {
                sessionManager.saveSession(
                    session.value.copy(
                        userName = updatedTeacher.name
                    )
                )
            }
            _userMessage.value = "Teacher profile for ID ${updatedTeacher.id} updated successfully."
        }
    }

    fun deleteTeacherSelf(teacherId: String, onLoggedOut: () -> Unit = {}) {
        viewModelScope.launch {
            val teacher = repository.getTeacherById(teacherId)
            if (teacher != null) {
                repository.deleteTeacher(teacher)
                firestoreSyncService.removeTeacherFromCloud(teacher.id)
                sessionManager.clearSession()
                _userMessage.value = "Tutender profile for Teacher ID $teacherId was deleted."
                onLoggedOut()
            }
        }
    }

    // --- Cloud Synchronization Operations ---

    fun syncRoomToFirestore(onComplete: (SyncResult) -> Unit = {}) {
        viewModelScope.launch {
            when (val result = firestoreSyncService.syncRoomToFirestore()) {
                is SyncResult.Success -> {
                    _userMessage.value = "Cloud Backup: ${result.itemsCount} records saved to Firestore."
                    onComplete(result)
                }
                is SyncResult.Error -> {
                    _userMessage.value = "Cloud Sync Notice: ${result.message}"
                    onComplete(result)
                }
                SyncResult.Offline -> {
                    _userMessage.value = "Cloud Sync: Offline local mode active."
                    onComplete(result)
                }
            }
        }
    }

    fun syncFirestoreToRoom(onComplete: (SyncResult) -> Unit = {}) {
        viewModelScope.launch {
            when (val result = firestoreSyncService.syncFirestoreToRoom()) {
                is SyncResult.Success -> {
                    _userMessage.value = "Cloud Restore: ${result.itemsCount} records restored to Room."
                    onComplete(result)
                }
                is SyncResult.Error -> {
                    _userMessage.value = "Cloud Restore Notice: ${result.message}"
                    onComplete(result)
                }
                SyncResult.Offline -> {
                    _userMessage.value = "Cloud Restore: Offline local mode active."
                    onComplete(result)
                }
            }
        }
    }

    // --- Attendance Operations ---

    fun recordAttendance(studentId: String, type: String) {
        viewModelScope.launch {
            val operatorId = session.value.userId.ifBlank { "TEACHER" }
            val record = repository.recordAttendance(studentId, type, operatorId)
            if (record != null) {
                firestoreRepository.recordAttendanceToFirestore(record)
                firestoreSyncService.replicateAttendanceToCloud(record)
                _userMessage.value = "Logged $type for ${record.studentName} at ${record.timeString} (Firestore synced)"
            } else {
                _userMessage.value = "Student with ID $studentId not found."
            }
        }
    }

    // --- Teacher Student Operations ---

    fun saveStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.insertStudent(student)
            firestoreRepository.saveStudentToFirestore(student)
            firestoreSyncService.replicateStudentToCloud(student)
            _userMessage.value = "Student ${student.name} profile saved to Firestore!"
        }
    }

    fun updateStudentPasswordAndPhone(studentId: String, newPhone: String, newPass: String) {
        viewModelScope.launch {
            val existing = repository.getStudentById(studentId)
            if (existing != null) {
                val updated = existing.copy(
                    studentMobile = newPhone,
                    password = newPass
                )
                repository.updateStudent(updated)
                firestoreRepository.saveStudentToFirestore(updated)
                firestoreSyncService.replicateStudentToCloud(updated)
                _userMessage.value = "Updated mobile and password for ${existing.name} in Firestore."
            }
        }
    }

    fun updateStudentPhoto(studentId: String, photoUri: String, teacherId: String? = null) {
        viewModelScope.launch {
            val existing = repository.getStudentById(studentId)
            if (existing != null) {
                val updated = existing.copy(photoUri = photoUri)
                repository.updateStudent(updated)
                firestoreRepository.saveStudentToFirestore(updated)
                firestoreSyncService.replicateStudentToCloud(updated)
                val teacherTag = if (!teacherId.isNullOrBlank()) " (Teacher: $teacherId)" else ""
                _userMessage.value = "Photo updated for ${existing.name}$teacherTag."
            }
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            firestoreRepository.deleteStudentFromFirestore(student.id)
            firestoreSyncService.removeStudentFromCloud(student.id)
            _userMessage.value = "Student ${student.name} removed from Firestore."
        }
    }

    fun deleteStudentByTeacher(student: StudentEntity, teacherId: String? = null) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            firestoreRepository.deleteStudentFromFirestore(student.id)
            firestoreSyncService.removeStudentFromCloud(student.id)
            val teacherTag = if (!teacherId.isNullOrBlank()) " by Teacher ID ($teacherId)" else ""
            _userMessage.value = "Student ID ${student.id} (${student.name}) removed$teacherTag."
        }
    }

    fun editStudentProfile(oldId: String, updatedStudent: StudentEntity, teacherId: String? = null) {
        viewModelScope.launch {
            if (oldId != updatedStudent.id) {
                val old = repository.getStudentById(oldId)
                if (old != null) {
                    repository.deleteStudent(old)
                }
                firestoreRepository.deleteStudentFromFirestore(oldId)
                firestoreSyncService.removeStudentFromCloud(oldId)
                repository.insertStudent(updatedStudent)
            } else {
                repository.updateStudent(updatedStudent)
            }
            firestoreRepository.saveStudentToFirestore(updatedStudent)
            firestoreSyncService.replicateStudentToCloud(updatedStudent)
            val teacherTag = if (!teacherId.isNullOrBlank()) " by Teacher ID ($teacherId)" else ""
            _userMessage.value = "Student ID ${updatedStudent.id} (${updatedStudent.name}) updated in Firestore$teacherTag."
        }
    }

    fun deleteTeacherByAdmin(teacher: TeacherEntity, adminId: String? = null) {
        viewModelScope.launch {
            repository.deleteTeacher(teacher)
            firestoreRepository.deleteTeacherFromFirestore(teacher.id)
            firestoreSyncService.removeTeacherFromCloud(teacher.id)
            val adminTag = if (!adminId.isNullOrBlank()) " by Admin ID ($adminId)" else ""
            _userMessage.value = "Teacher ID ${teacher.id} (${teacher.name}) removed$adminTag."
        }
    }

    // --- Exam Results Operations ---

    fun saveExamResult(result: ExamResultEntity) {
        viewModelScope.launch {
            val insertedId = repository.insertExamResult(result)
            val toSave = if (result.id == 0L) result.copy(id = insertedId) else result
            val firestoreResult = firestoreRepository.saveExamResultToFirestore(toSave)
            firestoreSyncService.replicateExamResultToCloud(toSave)
            if (firestoreResult.isSuccess) {
                _userMessage.value = "Exam marks saved to Cloud Firestore for ${result.subject} (Score: ${result.marksObtained}/${result.maxMarks}, Grade: ${result.grade})."
            } else {
                val err = firestoreResult.exceptionOrNull()?.localizedMessage ?: "Network/Permission error"
                _userMessage.value = "Saved locally. Cloud Firestore sync status: $err"
            }
        }
    }

    // --- Notification Operations ---

    fun sendNotification(
        targetType: String, // "STUDENT", "CLASS", "ALL_SCHOOL"
        targetStudentId: String? = null,
        targetClassName: String? = null,
        targetSection: String? = null,
        title: String,
        message: String,
        imageUrl: String? = null,
        isPaymentAlert: Boolean = false,
        amountDue: Double? = null
    ) {
        viewModelScope.launch {
            val currentSchoolId = session.value.schoolId.ifBlank { "SCH-1001" }
            val currentTeacherId = session.value.userId.ifBlank { "TCH-501" }
            val senderName = session.value.userName.ifBlank { "Teacher" }

            val notification = NotificationEntity(
                schoolId = currentSchoolId,
                senderTeacherId = currentTeacherId,
                senderName = senderName,
                targetType = targetType,
                targetStudentId = targetStudentId,
                targetClassName = targetClassName,
                targetSection = targetSection,
                title = title,
                message = message,
                imageUrl = imageUrl,
                isPaymentAlert = isPaymentAlert,
                amountDue = amountDue
            )
            repository.sendNotification(notification)
            firestoreRepository.sendNotificationToFirestore(notification)
            firestoreSyncService.replicateNotificationToCloud(notification)
            _userMessage.value = "Notice published to Firestore successfully!"
        }
    }

    // --- Teacher Password Change ---

    fun changeTeacherPassword(newPass: String) {
        viewModelScope.launch {
            val teacherId = session.value.userId
            if (teacherId.isNotBlank()) {
                repository.updateTeacherPassword(teacherId, newPass)
                val existing = repository.getTeacherById(teacherId)
                if (existing != null) {
                    firestoreRepository.saveTeacherToFirestore(existing.copy(password = newPass))
                }
                _userMessage.value = "Password updated in Firestore!"
            }
        }
    }

    // --- Admin Operations ---

    fun createSchoolAndTeacher(
        schoolName: String,
        schoolAddress: String,
        teacherName: String,
        teacherEmail: String,
        teacherMobile: String,
        teacherPassword: String,
        assignedClass: String
    ) {
        viewModelScope.launch {
            val newSchoolId = "SCH-" + (1000 + (allSchools.value.size + 1))
            val newRegId = "REG-" + (10000 + (allSchools.value.size + 1))
            val newTeacherId = "TCH-" + (500 + (allTeachers.value.size + 1))

            val school = SchoolEntity(
                id = newSchoolId,
                name = schoolName,
                registrationId = newRegId,
                address = schoolAddress
            )
            val teacher = TeacherEntity(
                id = newTeacherId,
                schoolId = newSchoolId,
                schoolName = schoolName,
                name = teacherName,
                email = teacherEmail,
                mobile = teacherMobile,
                password = teacherPassword,
                assignedClass = assignedClass
            )
            repository.insertSchool(school)
            repository.insertTeacher(teacher)
            firestoreRepository.saveSchoolToFirestore(school)
            firestoreRepository.saveTeacherToFirestore(teacher)
            firestoreSyncService.replicateTeacherToCloud(teacher)
            _userMessage.value = "Registered School & Teacher in Firestore: $schoolName (ID: $newSchoolId, Teacher: $newTeacherId)"
        }
    }

    fun addProduct(
        title: String,
        description: String,
        imageUrl: String,
        purchaseUrl: String,
        price: Double,
        category: String,
        targetAudience: String
    ) {
        viewModelScope.launch {
            val product = ProductEntity(
                title = title,
                description = description,
                imageUrl = imageUrl,
                purchaseUrl = purchaseUrl,
                price = price,
                category = category,
                targetAudience = targetAudience,
                inStock = true
            )
            repository.insertProduct(product)
            firestoreRepository.saveProductToFirestore(product)
            _userMessage.value = "Product added to Online Store & Firestore!"
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product)
            firestoreRepository.saveProductToFirestore(product)
            _userMessage.value = "Product updated in Firestore."
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            firestoreRepository.deleteProductFromFirestore(product.id)
            _userMessage.value = "Product removed from store & Firestore."
        }
    }

    fun updateAppSettings(appName: String, logoUrl: String) {
        viewModelScope.launch {
            repository.saveAppSettings(
                AppSettingsEntity(
                    configKey = "GLOBAL_CONFIG",
                    appName = appName,
                    logoUrl = logoUrl
                )
            )
            _userMessage.value = "App name & branding updated!"
        }
    }

    fun saveSearchQuery(query: String, searchType: String = "ALL") {
        viewModelScope.launch {
            repository.saveSearchQuery(query, searchType)
        }
    }

    fun deleteSearchHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteSearchById(id)
        }
    }

    fun clearAllSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
            _userMessage.value = "Search history cleared."
        }
    }
}
