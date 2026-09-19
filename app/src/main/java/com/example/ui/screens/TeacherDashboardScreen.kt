package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.session.UserSessionState
import com.example.data.sync.SyncState
import com.example.ui.components.BarcodeGeneratorCard
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.AttendanceTrendLineChart
import com.example.ui.components.CloudSyncDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    currentSession: UserSessionState,
    students: List<StudentEntity>,
    attendanceRecords: List<AttendanceEntity>,
    notifications: List<NotificationEntity>,
    syncState: SyncState = SyncState(),
    onSyncRoomToFirestore: () -> Unit = {},
    onSyncFirestoreToRoom: () -> Unit = {},
    onRecordAttendance: (studentId: String, type: String) -> Unit,
    onSaveStudent: (StudentEntity) -> Unit,
    onUpdateStudentCredentials: (studentId: String, newPhone: String, newPass: String) -> Unit,
    onUpdateStudentPhoto: (studentId: String, photoUri: String) -> Unit = { _, _ -> },
    onEditStudentProfile: (oldId: String, updated: StudentEntity) -> Unit = { _, _ -> },
    onRemoveStudent: (StudentEntity) -> Unit = {},
    onUpdateTeacherProfile: (TeacherEntity) -> Unit = {},
    onDeleteTeacherProfile: () -> Unit = {},
    teacherProfile: TeacherEntity? = null,
    onSaveExamResult: (ExamResultEntity) -> Unit,
    onSendNotification: (
        targetType: String,
        targetStudentId: String?,
        targetClassName: String?,
        targetSection: String?,
        title: String,
        message: String,
        imageUrl: String?,
        isPaymentAlert: Boolean,
        amountDue: Double?
    ) -> Unit,
    onChangeTeacherPassword: (newPass: String) -> Unit,
    onOpenShopping: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Students, 1: Attendance, 2: Exam Marks, 3: Send Notices
    var showScannerDialog by remember { mutableStateOf(false) }
    var showMarksScannerDialog by remember { mutableStateOf(false) }
    var marksScannedStudentId by remember { mutableStateOf<String?>(null) }
    var showNoticeScannerDialog by remember { mutableStateOf(false) }
    var noticeScannedStudentId by remember { mutableStateOf<String?>(null) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var selectedStudentForDetails by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToEditCredentials by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToFullEdit by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToRemove by remember { mutableStateOf<StudentEntity?>(null) }
    var studentForPhotoUpload by remember { mutableStateOf<StudentEntity?>(null) }
    var showTeacherProfileDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showPasswordChangeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Student Search Query
    var studentSearchQuery by remember { mutableStateOf("") }
    val filteredStudents = remember(students, studentSearchQuery) {
        if (studentSearchQuery.isBlank()) students
        else {
            students.filter {
                it.name.contains(studentSearchQuery, ignoreCase = true) ||
                it.className.contains(studentSearchQuery, ignoreCase = true) ||
                it.rollNumber.contains(studentSearchQuery, ignoreCase = true) ||
                it.id.contains(studentSearchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentSession.userName.ifBlank { "Teacher Dashboard" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(currentSession.schoolName.ifBlank { "School Portal" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    // Cloud Sync action
                    IconButton(
                        onClick = { showCloudSyncDialog = true },
                        modifier = Modifier.testTag("btn_teacher_cloud_sync")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Cloud Storage Sync",
                            tint = if (syncState.isCloudConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Attendance Barcode Scanner Quick Launch
                    IconButton(
                        onClick = { showScannerDialog = true },
                        modifier = Modifier.testTag("btn_teacher_open_scanner")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Attendance", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Online Shopping
                    IconButton(
                        onClick = onOpenShopping,
                        modifier = Modifier.testTag("btn_teacher_shopping")
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Online Store")
                    }

                    // Tutender Teacher Profile Management
                    IconButton(
                        onClick = { showTeacherProfileDialog = true },
                        modifier = Modifier.testTag("btn_teacher_profile")
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "My Profile", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Change Password
                    IconButton(
                        onClick = { showPasswordChangeDialog = true },
                        modifier = Modifier.testTag("btn_teacher_change_password")
                    ) {
                        Icon(Icons.Default.Key, contentDescription = "Change Password")
                    }

                    // Logout
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.testTag("btn_teacher_logout")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddStudentDialog = true },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("New Student") },
                    modifier = Modifier.testTag("fab_add_student")
                )
            } else if (selectedTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showScannerDialog = true },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    text = { Text("Scan Barcode") },
                    modifier = Modifier.testTag("fab_scan_attendance")
                )
            } else if (selectedTab == 2) {
                ExtendedFloatingActionButton(
                    onClick = { showMarksScannerDialog = true },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    text = { Text("Scan Student") },
                    modifier = Modifier.testTag("fab_scan_marks_student")
                )
            } else if (selectedTab == 3) {
                ExtendedFloatingActionButton(
                    onClick = { showNoticeScannerDialog = true },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    text = { Text("Scan Student") },
                    modifier = Modifier.testTag("fab_scan_notice_student")
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("Students") },
                    modifier = Modifier.testTag("tab_nav_students")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.HowToReg, contentDescription = null) },
                    label = { Text("Attendance") },
                    modifier = Modifier.testTag("tab_nav_attendance")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null) },
                    label = { Text("Marks") },
                    modifier = Modifier.testTag("tab_nav_marks")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                    label = { Text("Notices") },
                    modifier = Modifier.testTag("tab_nav_notices")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    // Students Directory & Profiles
                    TeacherStudentsView(
                        students = filteredStudents,
                        searchQuery = studentSearchQuery,
                        onSearchChange = { studentSearchQuery = it },
                        onSelectStudent = { selectedStudentForDetails = it },
                        onEditCredentials = { studentToEditCredentials = it },
                        onEditStudent = { studentToFullEdit = it },
                        onDeleteStudent = { studentToRemove = it },
                        onUploadPhoto = { studentForPhotoUpload = it },
                        onScanClick = { showScannerDialog = true }
                    )
                }

                1 -> {
                    // Attendance Records & Scanning Hub
                    TeacherAttendanceView(
                        records = attendanceRecords,
                        students = students,
                        onLaunchScanner = { showScannerDialog = true },
                        onManualRecord = onRecordAttendance
                    )
                }

                2 -> {
                    // Exam Marks Entry
                    TeacherExamMarksView(
                        students = students,
                        scannedStudentIdToSelect = marksScannedStudentId,
                        onClearScannedStudentId = { marksScannedStudentId = null },
                        onLaunchBarcodeScanner = { showMarksScannerDialog = true },
                        onSaveMarks = onSaveExamResult
                    )
                }

                3 -> {
                    // Notifications Dispatcher (Specific Student, Class, or All School)
                    TeacherNotificationsView(
                        students = students,
                        sentNotifications = notifications,
                        scannedStudentIdToSelect = noticeScannedStudentId,
                        onClearScannedStudentId = { noticeScannedStudentId = null },
                        onLaunchBarcodeScanner = { showNoticeScannerDialog = true },
                        onSendNotice = onSendNotification
                    )
                }
            }
        }
    }

    // Camera Barcode Scanner Dialog for Attendance
    CameraScannerDialog(
        isOpen = showScannerDialog,
        onDismiss = { showScannerDialog = false },
        onBarcodeScanned = { studentId, type ->
            onRecordAttendance(studentId, type)
        },
        availableStudents = students,
        operatorRole = "TEACHER"
    )

    // Camera Barcode Scanner Dialog for Marks Student Selection
    CameraScannerDialog(
        isOpen = showMarksScannerDialog,
        onDismiss = { showMarksScannerDialog = false },
        onBarcodeScanned = { scannedId, _ ->
            marksScannedStudentId = scannedId
            showMarksScannerDialog = false
        },
        availableStudents = students,
        operatorRole = "TEACHER"
    )

    // Camera Barcode Scanner Dialog for Notices Student Selection
    CameraScannerDialog(
        isOpen = showNoticeScannerDialog,
        onDismiss = { showNoticeScannerDialog = false },
        onBarcodeScanned = { scannedId, _ ->
            noticeScannedStudentId = scannedId
            showNoticeScannerDialog = false
        },
        availableStudents = students,
        operatorRole = "TEACHER"
    )

    // Add Student Dialog
    if (showAddStudentDialog) {
        AddStudentDialog(
            schoolId = currentSession.schoolId.ifBlank { "SCH-1001" },
            schoolName = currentSession.schoolName.ifBlank { "Delhi Public Academy" },
            onDismiss = { showAddStudentDialog = false },
            onSave = { newStudent ->
                onSaveStudent(newStudent)
                showAddStudentDialog = false
            }
        )
    }

    // Student ID Card & Full Profile Dialog
    if (selectedStudentForDetails != null) {
        Dialog(onDismissRequest = { selectedStudentForDetails = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    BarcodeGeneratorCard(student = selectedStudentForDetails!!)

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            studentForPhotoUpload = selectedStudentForDetails
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_dialog_upload_student_photo"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedStudentForDetails!!.photoUri.isNotBlank()) "Change Student Photo" else "Upload Student Photo")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    StudentProfileDetailsCard(student = selectedStudentForDetails!!)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val s = selectedStudentForDetails
                                selectedStudentForDetails = null
                                studentToFullEdit = s
                            },
                            modifier = Modifier.weight(1f).testTag("btn_dialog_edit_student_id")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit ID/Info")
                        }

                        OutlinedButton(
                            onClick = {
                                val s = selectedStudentForDetails
                                selectedStudentForDetails = null
                                studentToRemove = s
                            },
                            modifier = Modifier.weight(1f).testTag("btn_dialog_remove_student_id"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove ID")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { selectedStudentForDetails = null },
                        modifier = Modifier.fillMaxWidth().testTag("btn_close_student_card_dialog")
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    // Upload Student Photo Dialog (Teacher Authorized)
    if (studentForPhotoUpload != null) {
        UploadStudentPhotoDialog(
            student = studentForPhotoUpload!!,
            teacherId = currentSession.userId,
            teacherName = currentSession.userName,
            onDismiss = { studentForPhotoUpload = null },
            onSavePhoto = { newPhotoUri ->
                onUpdateStudentPhoto(studentForPhotoUpload!!.id, newPhotoUri)
                if (selectedStudentForDetails?.id == studentForPhotoUpload?.id) {
                    selectedStudentForDetails = selectedStudentForDetails?.copy(photoUri = newPhotoUri)
                }
                studentForPhotoUpload = null
            }
        )
    }

    // Edit Student Credentials Dialog
    if (studentToEditCredentials != null) {
        EditStudentCredentialsDialog(
            student = studentToEditCredentials!!,
            onDismiss = { studentToEditCredentials = null },
            onSave = { newPhone, newPass ->
                onUpdateStudentCredentials(studentToEditCredentials!!.id, newPhone, newPass)
                studentToEditCredentials = null
            }
        )
    }

    // Edit Full Student ID & Profile (Authorized by Teacher ID)
    if (studentToFullEdit != null) {
        EditStudentIdAndProfileDialog(
            student = studentToFullEdit!!,
            teacherId = currentSession.userId.ifBlank { "TCH-501" },
            teacherName = currentSession.userName.ifBlank { "Authorized Faculty" },
            onDismiss = { studentToFullEdit = null },
            onSave = { oldId, updatedStudent ->
                onEditStudentProfile(oldId, updatedStudent)
                studentToFullEdit = null
            }
        )
    }

    // Confirm Remove Student ID (Authorized by Teacher ID)
    if (studentToRemove != null) {
        ConfirmRemoveStudentDialog(
            student = studentToRemove!!,
            teacherId = currentSession.userId.ifBlank { "TCH-501" },
            onDismiss = { studentToRemove = null },
            onConfirm = {
                onRemoveStudent(studentToRemove!!)
                studentToRemove = null
            }
        )
    }

    // Teacher Tutender Profile Management Dialog
    if (showTeacherProfileDialog) {
        TeacherProfileDialog(
            teacher = teacherProfile,
            currentSession = currentSession,
            onDismiss = { showTeacherProfileDialog = false },
            onSaveProfile = { updated ->
                onUpdateTeacherProfile(updated)
                showTeacherProfileDialog = false
            },
            onDeleteProfile = {
                showTeacherProfileDialog = false
                onDeleteTeacherProfile()
            }
        )
    }

    // Change Teacher Password Dialog
    if (showPasswordChangeDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordChangeDialog = false },
            onConfirm = { newPass ->
                onChangeTeacherPassword(newPass)
                showPasswordChangeDialog = false
            }
        )
    }

    // Logout Confirmation
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Teacher Logout") },
            text = { Text("Are you sure you want to log out of your teacher account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    modifier = Modifier.testTag("btn_confirm_teacher_logout")
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Cloud Persistent Storage Synchronization Dialog
    CloudSyncDialog(
        isOpen = showCloudSyncDialog,
        syncState = syncState,
        onDismiss = { showCloudSyncDialog = false },
        onSyncRoomToFirestore = onSyncRoomToFirestore,
        onSyncFirestoreToRoom = onSyncFirestoreToRoom
    )
}

@Composable
fun TeacherStudentsView(
    students: List<StudentEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectStudent: (StudentEntity) -> Unit,
    onEditCredentials: (StudentEntity) -> Unit,
    onEditStudent: (StudentEntity) -> Unit = {},
    onDeleteStudent: (StudentEntity) -> Unit = {},
    onUploadPhoto: (StudentEntity) -> Unit,
    onScanClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Quick Barcode Scanning Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onScanClick)
                .testTag("banner_quick_barcode_scan"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scan Student Barcode / QR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Record school entry or exit instantly with camera",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by name, class, roll, or ID...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_search_student_directory"),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Registered Students (${students.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap for ID / Barcode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No students match your search.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(students) { student ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_card_${student.id}")
                            .clickable { onSelectStudent(student) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Student Profile Photo Avatar with click to upload
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .clickable { onUploadPhoto(student) }
                                    .testTag("avatar_student_${student.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (student.photoUri.isNotBlank()) {
                                    AsyncImage(
                                        model = student.photoUri,
                                        contentDescription = "Photo of ${student.name}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = student.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(18.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Upload Photo",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Class ${student.className}-${student.section}  |  Roll: ${student.rollNumber}  |  ID: ${student.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Mobile: ${student.studentMobile.ifBlank { "None" }}  |  Blood: ${student.bloodGroup}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onUploadPhoto(student) },
                                    modifier = Modifier.testTag("btn_upload_photo_${student.id}")
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Upload Photo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onEditCredentials(student) },
                                    modifier = Modifier.testTag("btn_edit_creds_${student.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Password,
                                        contentDescription = "Edit Phone & Password",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onEditStudent(student) },
                                    modifier = Modifier.testTag("btn_edit_student_${student.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Student Profile & ID",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteStudent(student) },
                                    modifier = Modifier.testTag("btn_remove_student_${student.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove Student ID",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onSelectStudent(student) },
                                    modifier = Modifier.testTag("btn_view_barcode_${student.id}")
                                ) {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = "View Barcode",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherAttendanceView(
    records: List<AttendanceEntity>,
    students: List<StudentEntity>,
    onLaunchScanner: () -> Unit,
    onManualRecord: (studentId: String, type: String) -> Unit
) {
    var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
    var selectedType by remember { mutableStateOf("ENTRY") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Attendance Trend Line Chart (Last Month Consistency)
        item {
            AttendanceTrendLineChart(
                attendanceRecords = records,
                students = students,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_attendance_trends")
            )
        }

        // Quick Scan CTA
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_attendance_scanner_cta"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Barcode Attendance Scanner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Scan student cards using camera to auto-record Entry or Exit timestamp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onLaunchScanner,
                        modifier = Modifier.fillMaxWidth().testTag("btn_launch_camera_scanner")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Camera Barcode Scanner")
                    }
                }
            }
        }

        // Manual Marking Option
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Manual Attendance Log",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (students.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedType == "ENTRY",
                                onClick = { selectedType = "ENTRY" },
                                label = { Text("Entry (IN)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedType == "EXIT",
                                onClick = { selectedType = "EXIT" },
                                label = { Text("Exit (OUT)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (selectedStudentId.isNotBlank()) {
                                    onManualRecord(selectedStudentId, selectedType)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("btn_quick_log_selected_student")
                        ) {
                            Text("Mark $selectedType for First / Selected Student")
                        }
                    }
                }
            }
        }

        // Recent Logs List
        item {
            Text(
                text = "Recent Attendance Logs (${records.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (records.isEmpty()) {
            item {
                Text(
                    text = "No attendance logs recorded today yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(records) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(record.studentName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Class ${record.className}-${record.section} • ID: ${record.studentId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (record.type == "ENTRY") Color(0xFF16A34A) else Color(0xFFDC2626)
                            ) {
                                Text(
                                    text = record.type,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "${record.dateString} ${record.timeString}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherExamMarksView(
    students: List<StudentEntity>,
    scannedStudentIdToSelect: String? = null,
    onClearScannedStudentId: () -> Unit = {},
    onLaunchBarcodeScanner: () -> Unit = {},
    onSaveMarks: (ExamResultEntity) -> Unit
) {
    var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
    var examType by remember { mutableStateOf("Monthly Exam - Sept 2026") }
    var marksInput by remember { mutableStateOf("85") }
    var maxMarksInput by remember { mutableStateOf("100") }
    var remarksInput by remember { mutableStateOf("Good performance") }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(students) {
        if (selectedStudent == null && students.isNotEmpty()) {
            selectedStudent = students.firstOrNull()
        }
    }

    // Student Search & Barcode Selection State
    var searchQuery by remember { mutableStateOf("") }

    // Seven Subjects with In-place Editing
    val subjectList = remember {
        mutableStateListOf(
            "Mathematics",
            "Science",
            "English",
            "Social Studies",
            "Computer Science",
            "Physical Education",
            "Art & Design"
        )
    }
    var selectedSubjectIndex by remember { mutableStateOf(0) }
    var subjectToEditIndex by remember { mutableStateOf<Int?>(null) }
    var subjectEditTempText by remember { mutableStateOf("") }

    val currentSubject = subjectList.getOrElse(selectedSubjectIndex) { subjectList.firstOrNull() ?: "Mathematics" }

    // Handle incoming scanned barcode from camera scanner
    LaunchedEffect(scannedStudentIdToSelect) {
        if (!scannedStudentIdToSelect.isNullOrBlank()) {
            val parsedId = com.example.util.BarcodeUtil.parseStudentIdFromScan(scannedStudentIdToSelect)
            val matched = students.find {
                it.id.equals(parsedId, ignoreCase = true) ||
                it.id.equals(scannedStudentIdToSelect, ignoreCase = true) ||
                scannedStudentIdToSelect.contains(it.id, ignoreCase = true)
            }
            if (matched != null) {
                selectedStudent = matched
                searchQuery = matched.name
            } else {
                searchQuery = scannedStudentIdToSelect
            }
            onClearScannedStudentId()
        }
    }

    // Filter students by Name or Barcode / Student ID
    val searchFilteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else {
            val q = searchQuery.trim()
            students.filter { student ->
                student.name.contains(q, ignoreCase = true) ||
                student.id.contains(q, ignoreCase = true) ||
                student.rollNumber.contains(q, ignoreCase = true) ||
                student.className.contains(q, ignoreCase = true)
            }
        }
    }

    val examOptions = listOf("Monthly Exam - Sept 2026", "Monthly Exam - Oct 2026", "Annual Exam - 2026")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Record Student Exam Marks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Search or scan student barcode, select from 7 subjects, and save marks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(
                    onClick = onLaunchBarcodeScanner,
                    modifier = Modifier.testTag("btn_marks_open_scanner")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Student Barcode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (saveSuccessMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = saveSuccessMessage ?: "",
                            color = Color(0xFF166534),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { saveSuccessMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFF166534),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // SECTION 1: SEARCH & SELECT STUDENT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Student Selection",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (selectedStudent != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Selected: ${selectedStudent!!.name}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search input by Name or Barcode
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            // If typed query exactly matches a barcode or name, select it automatically
                            val exactMatch = students.find {
                                it.id.equals(query.trim(), ignoreCase = true) ||
                                it.name.equals(query.trim(), ignoreCase = true)
                            }
                            if (exactMatch != null) {
                                selectedStudent = exactMatch
                            }
                        },
                        label = { Text("Search by Name or Barcode / ID") },
                        placeholder = { Text("e.g. Rahul Sharma or STU-10-A-01") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.testTag("btn_clear_marks_search")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                                IconButton(
                                    onClick = onLaunchBarcodeScanner,
                                    modifier = Modifier.testTag("btn_scan_student_barcode_field")
                                ) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan Barcode",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_marks_student_search")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Selected student preview card
                    if (selectedStudent != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_selected_student"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = selectedStudent!!.name.take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedStudent!!.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Class ${selectedStudent!!.className}-${selectedStudent!!.section} • Roll ${selectedStudent!!.rollNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.QrCode,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = selectedStudent!!.id,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filtered students selection chips or list
                    Text(
                        text = if (searchQuery.isBlank()) "Matching Students (Tap to select):" else "Search Results (${searchFilteredStudents.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (searchFilteredStudents.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No student found matching \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("btn_reset_marks_student_filter")
                                ) {
                                    Text("Show All Students")
                                }
                            }
                        }
                    } else {
                        // Scrollable student choice list
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            searchFilteredStudents.forEach { st ->
                                val isSelected = selectedStudent?.id == st.id
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedStudent = st
                                            searchQuery = st.name
                                        }
                                        .testTag("chip_student_${st.id}"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                selectedStudent = st
                                                searchQuery = st.name
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = st.name,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Barcode / ID: ${st.id} • Class ${st.className}-${st.section}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // SECTION 2: EXAM TYPE
                    Text("2. Exam Type", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        examOptions.forEach { opt ->
                            FilterChip(
                                selected = examType == opt,
                                onClick = { examType = opt },
                                label = { Text(if (opt.contains("Annual")) "Annual" else if (opt.contains("Oct")) "Monthly Oct" else "Monthly Sept") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // SECTION 3: SEVEN SUBJECTS WITH EDITABLE NAMES
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "3. Subject (7 Subjects Available)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Tap subject to select, or tap pencil icon to edit subject name",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Render the 7 subjects as interactive cards / chips with edit action
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        subjectList.forEachIndexed { index, subName ->
                            val isSelected = selectedSubjectIndex == index
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSubjectIndex = index }
                                    .testTag("subject_item_$index"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = subName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Active",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }

                                    // Pencil Button to edit subject name
                                    IconButton(
                                        onClick = {
                                            subjectToEditIndex = index
                                            subjectEditTempText = subName
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("btn_edit_subject_$index")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit $subName",
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // SECTION 4: MARKS & REMARKS
                    Text(
                        text = "4. Enter Marks for $currentSubject",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = marksInput,
                            onValueChange = { marksInput = it },
                            label = { Text("Marks Obtained") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_marks_obtained"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = maxMarksInput,
                            onValueChange = { maxMarksInput = it },
                            label = { Text("Max Marks") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_max_marks"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = remarksInput,
                        onValueChange = { remarksInput = it },
                        label = { Text("Teacher Remarks") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_exam_remarks"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (selectedStudent != null && marksInput.isNotBlank()) {
                                val marks = marksInput.toIntOrNull() ?: 0
                                val max = maxMarksInput.toIntOrNull() ?: 100
                                val pct = if (max > 0) (marks.toDouble() / max.toDouble()) * 100.0 else 0.0
                                val grade = when {
                                    pct >= 90 -> "A+"
                                    pct >= 80 -> "A"
                                    pct >= 70 -> "B+"
                                    pct >= 60 -> "B"
                                    pct >= 50 -> "C"
                                    else -> "D"
                                }
                                val entity = ExamResultEntity(
                                    studentId = selectedStudent!!.id,
                                    schoolId = selectedStudent!!.schoolId.ifBlank { "SCH-1001" },
                                    examType = examType,
                                    examMonthYear = "2026-09",
                                    subject = currentSubject,
                                    marksObtained = marks,
                                    maxMarks = max,
                                    grade = grade,
                                    remarks = remarksInput
                                )
                                onSaveMarks(entity)
                                saveSuccessMessage = "Recorded $currentSubject marks ($marks/$max, Grade: $grade) for ${selectedStudent!!.name}. Saved to Cloud Firestore."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_save_exam_marks"),
                        enabled = selectedStudent != null
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Exam Result to Report Card")
                    }
                }
            }
        }
    }

    // Dialog for Editing Subject Name
    if (subjectToEditIndex != null) {
        val targetIndex = subjectToEditIndex!!
        AlertDialog(
            onDismissRequest = { subjectToEditIndex = null },
            title = {
                Text("Edit Subject ${targetIndex + 1}")
            },
            text = {
                Column {
                    Text(
                        text = "Customize the name of this subject. It will be reflected in the marks selection and saved on the report card.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = subjectEditTempText,
                        onValueChange = { subjectEditTempText = it },
                        label = { Text("Subject Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_edit_subject_name")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = subjectEditTempText.trim()
                        if (trimmed.isNotEmpty() && targetIndex in subjectList.indices) {
                            subjectList[targetIndex] = trimmed
                        }
                        subjectToEditIndex = null
                    },
                    enabled = subjectEditTempText.isNotBlank(),
                    modifier = Modifier.testTag("btn_confirm_edit_subject")
                ) {
                    Text("Save Subject Name")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { subjectToEditIndex = null },
                    modifier = Modifier.testTag("btn_cancel_edit_subject")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TeacherNotificationsView(
    students: List<StudentEntity>,
    sentNotifications: List<NotificationEntity>,
    scannedStudentIdToSelect: String? = null,
    onClearScannedStudentId: () -> Unit = {},
    onLaunchBarcodeScanner: () -> Unit = {},
    onSendNotice: (
        targetType: String,
        targetStudentId: String?,
        targetClassName: String?,
        targetSection: String?,
        title: String,
        message: String,
        imageUrl: String?,
        isPaymentAlert: Boolean,
        amountDue: Double?
    ) -> Unit
) {
    var targetMode by remember { mutableStateOf(0) } // 0: Specific Student, 1: Specific Class, 2: Entire School
    var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
    var selectedClass by remember { mutableStateOf("10") }
    var selectedSection by remember { mutableStateOf("A") }

    var titleInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("") }
    var isPaymentAlert by remember { mutableStateOf(false) }
    var amountDueInput by remember { mutableStateOf("50.0") }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Student Search & Barcode Selection State
    var searchQuery by remember { mutableStateOf("") }

    // Handle incoming scanned barcode from camera scanner
    LaunchedEffect(scannedStudentIdToSelect) {
        if (!scannedStudentIdToSelect.isNullOrBlank()) {
            targetMode = 0 // Switch to 1 Student mode
            val parsedId = com.example.util.BarcodeUtil.parseStudentIdFromScan(scannedStudentIdToSelect)
            val matched = students.find {
                it.id.equals(parsedId, ignoreCase = true) ||
                it.id.equals(scannedStudentIdToSelect, ignoreCase = true) ||
                scannedStudentIdToSelect.contains(it.id, ignoreCase = true)
            }
            if (matched != null) {
                selectedStudentId = matched.id
                searchQuery = matched.name
            } else {
                searchQuery = scannedStudentIdToSelect
            }
            onClearScannedStudentId()
        }
    }

    val selectedStudent = students.find { it.id == selectedStudentId } ?: students.firstOrNull()

    // Filter students by Name, Barcode / Student ID, Class, or Roll Number
    val searchFilteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else {
            val q = searchQuery.trim()
            students.filter { student ->
                student.name.contains(q, ignoreCase = true) ||
                student.id.contains(q, ignoreCase = true) ||
                student.rollNumber.contains(q, ignoreCase = true) ||
                student.className.contains(q, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Send Student Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Search or scan student barcode to select a student, or send to class or whole school.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(
                    onClick = onLaunchBarcodeScanner,
                    modifier = Modifier.testTag("btn_notice_open_scanner")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Student Barcode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (saveSuccessMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = saveSuccessMessage ?: "",
                            color = Color(0xFF166534),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { saveSuccessMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFF166534),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Target Audience", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = targetMode == 0,
                            onClick = { targetMode = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            modifier = Modifier.testTag("seg_target_student")
                        ) {
                            Text("1 Student", fontSize = 11.sp)
                        }
                        SegmentedButton(
                            selected = targetMode == 1,
                            onClick = { targetMode = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            modifier = Modifier.testTag("seg_target_class")
                        ) {
                            Text("Class", fontSize = 11.sp)
                        }
                        SegmentedButton(
                            selected = targetMode == 2,
                            onClick = { targetMode = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            modifier = Modifier.testTag("seg_target_school")
                        ) {
                            Text("All School", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (targetMode == 0) {
                        // Student Search & Barcode Selection Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select Student:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            if (selectedStudent != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Selected: ${selectedStudent.name}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Search input by Name or Barcode / ID
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                searchQuery = query
                                val exactMatch = students.find {
                                    it.id.equals(query.trim(), ignoreCase = true) ||
                                    it.name.equals(query.trim(), ignoreCase = true)
                                }
                                if (exactMatch != null) {
                                    selectedStudentId = exactMatch.id
                                }
                            },
                            label = { Text("Search by Name or Barcode / ID") },
                            placeholder = { Text("e.g. Rahul Sharma or STU-10-A-01") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { searchQuery = "" },
                                            modifier = Modifier.testTag("btn_clear_notice_student_search")
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                    IconButton(
                                        onClick = onLaunchBarcodeScanner,
                                        modifier = Modifier.testTag("btn_scan_notice_student_barcode")
                                    ) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = "Scan Barcode",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_notice_student_search")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Selected student preview card
                        if (selectedStudent != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_selected_notice_student"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = selectedStudent.name.take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedStudent.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Class ${selectedStudent.className}-${selectedStudent.section} • Roll ${selectedStudent.rollNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.QrCode,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = selectedStudent.id,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filtered students selection choices
                        Text(
                            text = if (searchQuery.isBlank()) "Matching Students (Tap to select):" else "Search Results (${searchFilteredStudents.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (searchFilteredStudents.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No student found matching \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.testTag("btn_reset_notice_student_filter")
                                    ) {
                                        Text("Show All Students")
                                    }
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                searchFilteredStudents.forEach { st ->
                                    val isSelected = selectedStudentId == st.id
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedStudentId = st.id
                                                searchQuery = st.name
                                            }
                                            .testTag("chip_notice_student_${st.id}"),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedStudentId = st.id
                                                    searchQuery = st.name
                                                },
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = st.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = "Barcode / ID: ${st.id} • Class ${st.className}-${st.section} • Roll ${st.rollNumber}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isPaymentAlert,
                                onCheckedChange = { isPaymentAlert = it },
                                modifier = Modifier.testTag("chk_payment_alert")
                            )
                            Text("Payment / Fee Alert Notice", style = MaterialTheme.typography.bodyMedium)
                        }

                        if (isPaymentAlert) {
                            OutlinedTextField(
                                value = amountDueInput,
                                onValueChange = { amountDueInput = it },
                                label = { Text("Outstanding Amount (₹)") },
                                modifier = Modifier.fillMaxWidth().testTag("input_amount_due"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else if (targetMode == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedClass,
                                onValueChange = { selectedClass = it },
                                label = { Text("Class (e.g. 10)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = selectedSection,
                                onValueChange = { selectedSection = it },
                                label = { Text("Section (e.g. A)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Notification Title") },
                        placeholder = { Text("e.g. Homework Assignment / Exam Schedule") },
                        modifier = Modifier.fillMaxWidth().testTag("input_notice_title"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = { Text("Notification Message") },
                        placeholder = { Text("Enter detailed announcement text...") },
                        modifier = Modifier.fillMaxWidth().testTag("input_notice_message"),
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = imageUrlInput,
                        onValueChange = { imageUrlInput = it },
                        label = { Text("Optional Image URL Attachment") },
                        placeholder = { Text("https://example.com/circular.jpg") },
                        modifier = Modifier.fillMaxWidth().testTag("input_notice_image"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (titleInput.isNotBlank() && messageInput.isNotBlank()) {
                                val type = when (targetMode) {
                                    0 -> "STUDENT"
                                    1 -> "CLASS"
                                    else -> "ALL_SCHOOL"
                                }
                                val amt = if (isPaymentAlert) amountDueInput.toDoubleOrNull() else null
                                onSendNotice(
                                    type,
                                    if (type == "STUDENT") selectedStudentId else null,
                                    if (type == "CLASS") selectedClass else null,
                                    if (type == "CLASS") selectedSection else null,
                                    titleInput,
                                    messageInput,
                                    imageUrlInput.ifBlank { null },
                                    isPaymentAlert,
                                    amt
                                )
                                val targetName = if (type == "STUDENT") (selectedStudent?.name ?: "Student") else type
                                saveSuccessMessage = "Notice successfully sent to $targetName."
                                titleInput = ""
                                messageInput = ""
                                imageUrlInput = ""
                                isPaymentAlert = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_dispatch_notification"),
                        enabled = titleInput.isNotBlank() && messageInput.isNotBlank() && (targetMode != 0 || selectedStudentId.isNotBlank())
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Notice to Student(s)")
                    }
                }
            }
        }

        item {
            Text(
                text = "Sent Notices (${sentNotifications.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(sentNotifications) { notice ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(notice.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = notice.targetType,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(notice.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun AddStudentDialog(
    schoolId: String,
    schoolName: String,
    onDismiss: () -> Unit,
    onSave: (StudentEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("10") }
    var section by remember { mutableStateOf("A") }
    var rollNumber by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var parentMobile by remember { mutableStateOf("") }
    var studentMobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("pass123") }
    var gender by remember { mutableStateOf("Male") }
    var casteCategory by remember { mutableStateOf("General") }
    var bloodGroup by remember { mutableStateOf("O+") }
    var photoUri by remember { mutableStateOf("") }

    val addStudentPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                photoUri = uri.toString()
            }
        }
    )

    val genderOptions = listOf("Male", "Female", "Other")
    val bloodOptions = listOf("A+", "B+", "O+", "AB+", "O-")
    val casteOptions = listOf("General", "OBC", "SC", "ST")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Create Student Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A unique ZXing Barcode will be auto-generated for this student.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("input_new_student_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text("Class *") },
                        modifier = Modifier.weight(1f).testTag("input_new_student_class"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Section *") },
                        modifier = Modifier.weight(1f).testTag("input_new_student_section"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rollNumber,
                        onValueChange = { rollNumber = it },
                        label = { Text("Roll No. *") },
                        modifier = Modifier.weight(1f).testTag("input_new_student_roll"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("Father's Name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_new_student_father"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = motherName,
                    onValueChange = { motherName = it },
                    label = { Text("Mother's Name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_new_student_mother"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = parentMobile,
                    onValueChange = { parentMobile = it },
                    label = { Text("Parent's Mobile Number") },
                    modifier = Modifier.fillMaxWidth().testTag("input_new_student_parent_phone"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = studentMobile,
                    onValueChange = { studentMobile = it },
                    label = { Text("Student Mobile Number") },
                    modifier = Modifier.fillMaxWidth().testTag("input_new_student_phone"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Student Password *") },
                    modifier = Modifier.fillMaxWidth().testTag("input_new_student_password"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Gender:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    genderOptions.forEach { g ->
                        FilterChip(
                            selected = gender == g,
                            onClick = { gender = g },
                            label = { Text(g) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Blood Group:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    bloodOptions.forEach { b ->
                        FilterChip(
                            selected = bloodGroup == b,
                            onClick = { bloodGroup = b },
                            label = { Text(b) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Caste Category:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    casteOptions.forEach { c ->
                        FilterChip(
                            selected = casteCategory == c,
                            onClick = { casteCategory = c },
                            label = { Text(c) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Student Profile Photo:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = photoUri,
                        onValueChange = { photoUri = it },
                        label = { Text("Photo URL / Device Pick") },
                        placeholder = { Text("Pick or paste URL") },
                        modifier = Modifier.weight(1f).testTag("input_new_student_photo"),
                        singleLine = true
                    )
                    FilledTonalButton(
                        onClick = {
                            addStudentPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("btn_add_student_pick_photo")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Pick Photo", modifier = Modifier.size(18.dp))
                    }
                }

                if (photoUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Photo Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Photo Selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { photoUri = "" }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).testTag("btn_cancel_add_student")
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && className.isNotBlank() && rollNumber.isNotBlank()) {
                                val generatedStudentId = "STU-$className-$section-$rollNumber"
                                val barcodePayload = "$schoolId:$className:$section:$rollNumber:$generatedStudentId"
                                val student = StudentEntity(
                                    id = generatedStudentId,
                                    schoolId = schoolId,
                                    schoolName = schoolName,
                                    name = name,
                                    className = className,
                                    section = section,
                                    rollNumber = rollNumber,
                                    fatherName = fatherName,
                                    motherName = motherName,
                                    parentMobile = parentMobile,
                                    studentMobile = studentMobile,
                                    password = password,
                                    gender = gender,
                                    casteCategory = casteCategory,
                                    bloodGroup = bloodGroup,
                                    photoUri = photoUri,
                                    barcodeData = barcodePayload
                                )
                                onSave(student)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("btn_save_new_student"),
                        enabled = name.isNotBlank() && className.isNotBlank() && rollNumber.isNotBlank()
                    ) {
                        Text("Create Profile")
                    }
                }
            }
        }
    }
}

@Composable
fun EditStudentCredentialsDialog(
    student: StudentEntity,
    onDismiss: () -> Unit,
    onSave: (phone: String, pass: String) -> Unit
) {
    var phone by remember { mutableStateOf(student.studentMobile) }
    var password by remember { mutableStateOf(student.password) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Student Credentials") },
        text = {
            Column {
                Text(
                    text = "Update login phone number and password for ${student.name} (${student.id}):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Student Mobile Number") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_student_phone"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Student Password") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_student_password"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(phone, password) },
                modifier = Modifier.testTag("btn_save_student_creds")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (newPass: String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Teacher Password") },
        text = {
            Column {
                Text("Enter a new password for your teacher account:")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; error = null },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth().testTag("input_new_teacher_pass"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    label = { Text("Confirm New Password") },
                    modifier = Modifier.fillMaxWidth().testTag("input_confirm_teacher_pass"),
                    singleLine = true
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.isBlank()) {
                        error = "Password cannot be blank"
                    } else if (newPassword != confirmPassword) {
                        error = "Passwords do not match"
                    } else {
                        onConfirm(newPassword)
                    }
                },
                modifier = Modifier.testTag("btn_confirm_change_password")
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UploadStudentPhotoDialog(
    student: StudentEntity,
    teacherId: String,
    teacherName: String,
    onDismiss: () -> Unit,
    onSavePhoto: (photoUri: String) -> Unit
) {
    var photoUriInput by remember { mutableStateOf(student.photoUri) }
    var previewUri by remember { mutableStateOf(student.photoUri) }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val uriStr = uri.toString()
                photoUriInput = uriStr
                previewUri = uriStr
            }
        }
    )

    val sampleAvatars = listOf(
        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400&q=80",
        "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&q=80",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&q=80",
        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=400&q=80",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&q=80"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Upload Student Photo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${student.name} • Class ${student.className}-${student.section} (Roll: ${student.rollNumber})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Teacher ID authorization badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Teacher ID: ${teacherId.ifBlank { "TCH-001" }} (${teacherName.ifBlank { "Authorized Faculty" }})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Photo Preview
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                        .testTag("preview_student_photo"),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewUri.isNotBlank()) {
                        AsyncImage(
                            model = previewUri,
                            contentDescription = "Selected Student Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "No Photo Set",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (previewUri.isNotBlank()) {
                    TextButton(
                        onClick = {
                            previewUri = ""
                            photoUriInput = ""
                        },
                        modifier = Modifier.testTag("btn_clear_student_photo")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove Photo", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Option 1: Gallery Photo Picker
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_pick_photo_gallery"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose from Device Gallery")
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Option 2: Sample Student Portraits
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Or choose sample student portrait:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sampleAvatars) { avatarUrl ->
                            val isSelected = previewUri == avatarUrl
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        previewUri = avatarUrl
                                        photoUriInput = avatarUrl
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Sample Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Option 3: Manual URL entry
                OutlinedTextField(
                    value = photoUriInput,
                    onValueChange = {
                        photoUriInput = it
                        previewUri = it.trim()
                    },
                    label = { Text("Or Paste Photo URL") },
                    placeholder = { Text("https://example.com/photo.jpg") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_student_photo_url"),
                    singleLine = true,
                    trailingIcon = {
                        if (photoUriInput.isNotBlank()) {
                            IconButton(onClick = { photoUriInput = ""; previewUri = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_cancel_upload_photo")
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onSavePhoto(previewUri.trim())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_student_photo")
                    ) {
                        Text("Save Photo")
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherProfileDialog(
    teacher: TeacherEntity?,
    currentSession: UserSessionState,
    onDismiss: () -> Unit,
    onSaveProfile: (TeacherEntity) -> Unit,
    onDeleteProfile: () -> Unit
) {
    val effectiveTeacher = teacher ?: TeacherEntity(
        id = currentSession.userId.ifBlank { "TCH-501" },
        schoolId = currentSession.schoolId.ifBlank { "SCH-1001" },
        schoolName = currentSession.schoolName.ifBlank { "Delhi Public Academy" },
        name = currentSession.userName.ifBlank { "Teacher" },
        email = "faculty@school.edu",
        mobile = "+91 98765 43210",
        assignedClass = "10",
        subject = "Mathematics & Science",
        password = "password123"
    )

    var name by remember { mutableStateOf(effectiveTeacher.name) }
    var email by remember { mutableStateOf(effectiveTeacher.email) }
    var mobile by remember { mutableStateOf(effectiveTeacher.mobile) }
    var assignedClass by remember { mutableStateOf(effectiveTeacher.assignedClass) }
    var subject by remember { mutableStateOf(effectiveTeacher.subject) }
    var password by remember { mutableStateOf(effectiveTeacher.password) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Teacher Tutender Profile")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Teacher ID: ${effectiveTeacher.id}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "School: ${effectiveTeacher.schoolName} (${effectiveTeacher.schoolId})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Teacher Full Name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_teacher_profile_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth().testTag("input_teacher_profile_email"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number") },
                    modifier = Modifier.fillMaxWidth().testTag("input_teacher_profile_mobile"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = assignedClass,
                        onValueChange = { assignedClass = it },
                        label = { Text("Class") },
                        modifier = Modifier.weight(1f).testTag("input_teacher_profile_class"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.weight(1f).testTag("input_teacher_profile_subject"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Account Password") },
                    modifier = Modifier.fillMaxWidth().testTag("input_teacher_profile_password"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Danger Zone: Delete Profile
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Delete Tutender Profile",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Permanently remove your teacher profile (ID: ${effectiveTeacher.id}) from the system.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().testTag("btn_delete_teacher_profile_trigger")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete My Profile (ID: ${effectiveTeacher.id})")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveProfile(
                        effectiveTeacher.copy(
                            name = name.ifBlank { effectiveTeacher.name },
                            email = email.ifBlank { effectiveTeacher.email },
                            mobile = mobile.ifBlank { effectiveTeacher.mobile },
                            assignedClass = assignedClass.ifBlank { effectiveTeacher.assignedClass },
                            subject = subject.ifBlank { effectiveTeacher.subject },
                            password = password.ifBlank { effectiveTeacher.password }
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("btn_save_teacher_profile")
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Profile Deletion") },
            text = {
                Text("Are you sure you want to permanently delete your Tutender teacher profile (${effectiveTeacher.name}, ID: ${effectiveTeacher.id})?\n\nThis will permanently delete your teacher credentials and log you out immediately.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteProfile()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_teacher_self")
                ) {
                    Text("Delete Forever & Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditStudentIdAndProfileDialog(
    student: StudentEntity,
    teacherId: String,
    teacherName: String,
    onDismiss: () -> Unit,
    onSave: (oldId: String, updated: StudentEntity) -> Unit
) {
    var studentId by remember { mutableStateOf(student.id) }
    var name by remember { mutableStateOf(student.name) }
    var className by remember { mutableStateOf(student.className) }
    var section by remember { mutableStateOf(student.section) }
    var rollNumber by remember { mutableStateOf(student.rollNumber) }
    var fatherName by remember { mutableStateOf(student.fatherName) }
    var motherName by remember { mutableStateOf(student.motherName) }
    var studentMobile by remember { mutableStateOf(student.studentMobile) }
    var parentMobile by remember { mutableStateOf(student.parentMobile) }
    var gender by remember { mutableStateOf(student.gender) }
    var casteCategory by remember { mutableStateOf(student.casteCategory) }
    var bloodGroup by remember { mutableStateOf(student.bloodGroup) }
    var password by remember { mutableStateOf(student.password) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Student ID & Profile")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Authorized under Teacher ID: $teacherId ($teacherName)",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text("Student ID (Unique)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_student_id"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Student Full Name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_student_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text("Class") },
                        modifier = Modifier.weight(1f).testTag("input_edit_student_class"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Section") },
                        modifier = Modifier.weight(1f).testTag("input_edit_student_section"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rollNumber,
                        onValueChange = { rollNumber = it },
                        label = { Text("Roll #") },
                        modifier = Modifier.weight(1f).testTag("input_edit_student_roll"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fatherName,
                        onValueChange = { fatherName = it },
                        label = { Text("Father Name") },
                        modifier = Modifier.weight(1f).testTag("input_edit_student_father"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = motherName,
                        onValueChange = { motherName = it },
                        label = { Text("Mother Name") },
                        modifier = Modifier.weight(1f).testTag("input_edit_student_mother"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = studentMobile,
                        onValueChange = { studentMobile = it },
                        label = { Text("Student Phone") },
                        modifier = Modifier.weight(1f).testTag("input_edit_student_phone"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = { bloodGroup = it },
                        label = { Text("Blood Group") },
                        modifier = Modifier.weight(1f).testTag("input_edit_student_blood"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = parentMobile,
                        onValueChange = { parentMobile = it },
                        label = { Text("Parent Mobile") },
                        modifier = Modifier.weight(1f).testTag("input_edit_student_parent_phone"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = gender,
                        onValueChange = { gender = it },
                        label = { Text("Gender") },
                        modifier = Modifier.weight(1f).testTag("input_edit_student_gender"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = casteCategory,
                    onValueChange = { casteCategory = it },
                    label = { Text("Category (General/OBC/SC/ST)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Student Password") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_student_pwd"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanId = studentId.trim().ifBlank { student.id }
                    val updated = student.copy(
                        id = cleanId,
                        name = name.ifBlank { student.name },
                        className = className.ifBlank { student.className },
                        section = section.ifBlank { student.section },
                        rollNumber = rollNumber.ifBlank { student.rollNumber },
                        fatherName = fatherName.ifBlank { student.fatherName },
                        motherName = motherName.ifBlank { student.motherName },
                        parentMobile = parentMobile.ifBlank { student.parentMobile },
                        studentMobile = studentMobile,
                        gender = gender.ifBlank { student.gender },
                        casteCategory = casteCategory.ifBlank { student.casteCategory },
                        bloodGroup = bloodGroup.ifBlank { student.bloodGroup },
                        password = password.ifBlank { student.password },
                        barcodeData = "${student.schoolId}:$className:$section:$rollNumber:$cleanId"
                    )
                    onSave(student.id, updated)
                    onDismiss()
                },
                modifier = Modifier.testTag("btn_save_edit_student_id")
            ) {
                Text("Save ID & Details")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmRemoveStudentDialog(
    student: StudentEntity,
    teacherId: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Remove Student ID") },
        text = {
            Column {
                Text("Are you sure you want to remove Student ID '${student.id}' (${student.name}) from Class ${student.className}-${student.section}?")
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Authorized by Teacher ID: $teacherId\nThis permanently removes the student's records and profile.",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("btn_confirm_remove_student_${student.id}")
            ) {
                Text("Remove Student ID")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
