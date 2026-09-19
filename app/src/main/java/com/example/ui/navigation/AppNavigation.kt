package com.example.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.viewmodel.SchoolViewModel

object AppDestinations {
    const val HOME = "home"
    const val TEACHER_LOGIN = "teacher_login"
    const val STUDENT_LOGIN = "student_login"
    const val ADMIN_LOGIN = "admin_login"
    const val TEACHER_DASHBOARD = "teacher_dashboard"
    const val STUDENT_DASHBOARD = "student_dashboard"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val SHOPPING = "shopping"
}

@Composable
fun AppNavigation(
    viewModel: SchoolViewModel,
    snackbarHostState: SnackbarHostState,
    navController: NavHostController = rememberNavController()
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val allSchools by viewModel.allSchools.collectAsStateWithLifecycle()
    val allTeachers by viewModel.allTeachers.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

    // Student specific states
    val currentStudent by viewModel.currentStudent.collectAsStateWithLifecycle()
    val studentAttendance by viewModel.studentAttendance.collectAsStateWithLifecycle()
    val studentExamResults by viewModel.studentExamResults.collectAsStateWithLifecycle()
    val studentNotifications by viewModel.studentNotifications.collectAsStateWithLifecycle()

    // Teacher specific states
    val teacherStudents by viewModel.teacherSchoolStudents.collectAsStateWithLifecycle()
    val teacherAttendance by viewModel.teacherSchoolAttendance.collectAsStateWithLifecycle()
    val teacherNotifications by viewModel.teacherSchoolNotifications.collectAsStateWithLifecycle()

    // Cloud Persistent Storage synchronization state
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    // Listen to user feedback messages and display in snackbar
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestinations.HOME
    ) {
        // Home Screen
        composable(AppDestinations.HOME) {
            HomeScreen(
                appSettings = appSettings,
                currentSession = session,
                onNavigateToTeacherLogin = {
                    navController.navigate(AppDestinations.TEACHER_LOGIN)
                },
                onNavigateToStudentLogin = {
                    navController.navigate(AppDestinations.STUDENT_LOGIN)
                },
                onNavigateToAdminLogin = {
                    navController.navigate(AppDestinations.ADMIN_LOGIN)
                },
                onNavigateToDashboard = { role ->
                    when (role) {
                        "TEACHER" -> navController.navigate(AppDestinations.TEACHER_DASHBOARD)
                        "STUDENT" -> navController.navigate(AppDestinations.STUDENT_DASHBOARD)
                        "ADMIN" -> navController.navigate(AppDestinations.ADMIN_DASHBOARD)
                    }
                },
                onLogout = {
                    viewModel.logout()
                }
            )
        }

        // Teacher Login Screen
        composable(AppDestinations.TEACHER_LOGIN) {
            TeacherLoginScreen(
                availableSchools = allSchools,
                availableTeachers = allTeachers,
                onLoginSuccess = {
                    navController.navigate(AppDestinations.TEACHER_DASHBOARD) {
                        popUpTo(AppDestinations.HOME)
                    }
                },
                onBackClick = { navController.popBackStack() },
                onLoginAction = { school, teacherId, pass ->
                    viewModel.loginTeacher(school, teacherId, pass)
                }
            )
        }

        // Student Login Screen
        composable(AppDestinations.STUDENT_LOGIN) {
            StudentLoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppDestinations.STUDENT_DASHBOARD) {
                        popUpTo(AppDestinations.HOME)
                    }
                },
                onBackClick = { navController.popBackStack() },
                onLoginByClassRoll = { school, cls, sec, roll, pass ->
                    viewModel.loginStudent(school, cls, sec, roll, pass)
                },
                onLoginByBarcodeId = { barcodeId, pass ->
                    viewModel.loginStudentByBarcode(barcodeId, pass)
                }
            )
        }

        // Admin Login Screen
        composable(AppDestinations.ADMIN_LOGIN) {
            AdminLoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppDestinations.ADMIN_DASHBOARD) {
                        popUpTo(AppDestinations.HOME)
                    }
                },
                onBackClick = { navController.popBackStack() },
                onAdminLoginAction = { id, pass ->
                    viewModel.loginAdmin(id, pass)
                }
            )
        }

        // Teacher Dashboard Screen
        composable(AppDestinations.TEACHER_DASHBOARD) {
            val effectiveTeacherStudents = if (teacherStudents.isNotEmpty()) {
                teacherStudents
            } else {
                val filtered = allStudents.filter { it.schoolId == session.schoolId }
                if (filtered.isNotEmpty()) filtered else allStudents
            }

            TeacherDashboardScreen(
                currentSession = session,
                students = effectiveTeacherStudents,
                attendanceRecords = teacherAttendance,
                notifications = teacherNotifications,
                syncState = syncState,
                onSyncRoomToFirestore = { viewModel.syncRoomToFirestore() },
                onSyncFirestoreToRoom = { viewModel.syncFirestoreToRoom() },
                onRecordAttendance = { studentId, type ->
                    viewModel.recordAttendance(studentId, type)
                },
                onSaveStudent = { student ->
                    viewModel.saveStudent(student)
                },
                onUpdateStudentCredentials = { studentId, newPhone, newPass ->
                    viewModel.updateStudentPasswordAndPhone(studentId, newPhone, newPass)
                },
                onUpdateStudentPhoto = { studentId, photoUri ->
                    viewModel.updateStudentPhoto(studentId, photoUri, session.userId)
                },
                onSaveExamResult = { result ->
                    viewModel.saveExamResult(result)
                },
                onSendNotification = { type, stId, cls, sec, title, msg, img, isPay, amt ->
                    viewModel.sendNotification(type, stId, cls, sec, title, msg, img, isPay, amt)
                },
                onChangeTeacherPassword = { newPass ->
                    viewModel.changeTeacherPassword(newPass)
                },
                onEditStudentProfile = { oldId, updatedStudent ->
                    viewModel.editStudentProfile(oldId, updatedStudent, session.userId)
                },
                onRemoveStudent = { student ->
                    viewModel.deleteStudentByTeacher(student, session.userId)
                },
                onUpdateTeacherProfile = { updatedTeacher ->
                    viewModel.updateTeacherProfile(updatedTeacher)
                },
                onDeleteTeacherProfile = {
                    viewModel.deleteTeacherSelf(session.userId) {
                        navController.navigate(AppDestinations.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                teacherProfile = allTeachers.find { it.id == session.userId },
                onOpenShopping = {
                    navController.navigate(AppDestinations.SHOPPING)
                },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(AppDestinations.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Student Dashboard Screen
        composable(AppDestinations.STUDENT_DASHBOARD) {
            StudentDashboardScreen(
                student = currentStudent,
                attendanceList = studentAttendance,
                examResults = studentExamResults,
                notifications = studentNotifications,
                onOpenShopping = {
                    navController.navigate(AppDestinations.SHOPPING)
                },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(AppDestinations.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Admin Dashboard Screen
        composable(AppDestinations.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                schools = allSchools,
                teachers = allTeachers,
                students = allStudents,
                attendanceRecords = teacherAttendance.ifEmpty { allSchools.flatMap { listOf() } },
                products = allProducts,
                appSettings = appSettings,
                recentSearches = recentSearches,
                adminId = if (session.userId.isNotBlank()) session.userId else "ADM-001",
                syncState = syncState,
                onSyncRoomToFirestore = { viewModel.syncRoomToFirestore() },
                onSyncFirestoreToRoom = { viewModel.syncFirestoreToRoom() },
                onDeleteTeacher = { teacher ->
                    viewModel.deleteTeacherByAdmin(teacher, if (session.userId.isNotBlank()) session.userId else "ADM-001")
                },
                onRegisterSchoolAndTeacher = { sName, addr, tName, tEmail, tMob, tPass, cls ->
                    viewModel.createSchoolAndTeacher(sName, addr, tName, tEmail, tMob, tPass, cls)
                },
                onAddProduct = { title, desc, img, url, price, cat, aud ->
                    viewModel.addProduct(title, desc, img, url, price, cat, aud)
                },
                onUpdateProduct = { prod ->
                    viewModel.updateProduct(prod)
                },
                onDeleteProduct = { prod ->
                    viewModel.deleteProduct(prod)
                },
                onUpdateAppSettings = { name, logo ->
                    viewModel.updateAppSettings(name, logo)
                },
                onRecordAttendance = { studentId, type ->
                    viewModel.recordAttendance(studentId, type)
                },
                onSaveSearchQuery = { query, type ->
                    viewModel.saveSearchQuery(query, type)
                },
                onDeleteSearchHistory = { id ->
                    viewModel.deleteSearchHistoryItem(id)
                },
                onClearSearchHistory = {
                    viewModel.clearAllSearchHistory()
                },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(AppDestinations.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Online Shopping Catalog Screen
        composable(AppDestinations.SHOPPING) {
            ShoppingScreen(
                userRole = if (session.role.isNotBlank() && session.role != "NONE") session.role else "STUDENT",
                products = allProducts,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
