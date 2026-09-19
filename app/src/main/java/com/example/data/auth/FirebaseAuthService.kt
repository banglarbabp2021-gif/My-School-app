package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.StudentEntity
import com.example.data.model.TeacherEntity
import com.example.data.session.UserSessionState
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(val session: UserSessionState, val firebaseUser: FirebaseUser?) : AuthResult()
    data class Error(val message: String, val cause: Throwable? = null) : AuthResult()
}

/**
 * Firebase Authentication service providing separate authentication flows for:
 *  - Admin (System Administrator)
 *  - Teacher (School-scoped Educator)
 *  - Student (Individual Scholar)
 */
class FirebaseAuthService(
    private val context: Context,
    private val database: AppDatabase
) {
    private val TAG = "FirebaseAuthService"

    private fun getFirebaseAuth(): FirebaseAuth? {
        return try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth unavailable: ${e.message}")
            null
        }
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore unavailable: ${e.message}")
            null
        }
    }

    val currentUser: FirebaseUser?
        get() = getFirebaseAuth()?.currentUser

    // =========================================================================
    // 1. ADMIN AUTHENTICATION
    // =========================================================================
    suspend fun authenticateAdmin(adminId: String, pass: String): AuthResult = withContext(Dispatchers.IO) {
        val trimmedId = adminId.trim()
        val validDefaultAdmin = (trimmedId.equals("Sourav1997", ignoreCase = true) && pass == "Sourav#1997Soma@1998")

        val authEmail = if (trimmedId.contains("@")) {
            trimmedId.lowercase()
        } else {
            "${trimmedId.lowercase()}@schoolportal.admin"
        }

        val auth = getFirebaseAuth()
        val firestore = getFirestore()
        var firebaseUser: FirebaseUser? = null

        if (auth != null) {
            try {
                // Try signing in
                val signInResult = try {
                    auth.signInWithEmailAndPassword(authEmail, pass).await()
                } catch (e: Exception) {
                    // If user doesn't exist yet and credentials match default admin, create account
                    if (validDefaultAdmin) {
                        try {
                            auth.createUserWithEmailAndPassword(authEmail, pass).await()
                        } catch (createErr: Exception) {
                            Log.w(TAG, "Could not create Firebase Admin account: ${createErr.message}")
                            null
                        }
                    } else {
                        null
                    }
                }
                firebaseUser = signInResult?.user ?: auth.currentUser

                // Record / verify admin document in Firestore
                if (firestore != null && (validDefaultAdmin || firebaseUser != null)) {
                    val adminDoc = firestore.collection("admins").document(trimmedId)
                    adminDoc.set(
                        hashMapOf(
                            "adminId" to trimmedId,
                            "email" to authEmail,
                            "role" to "ADMIN",
                            "name" to "Administrator Sourav",
                            "lastLoginAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    ).await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Admin Auth network step failed: ${e.message}")
            }
        }

        // Validate admin credentials
        if (validDefaultAdmin || firebaseUser != null) {
            val session = UserSessionState(
                role = "ADMIN",
                userId = trimmedId,
                userName = "Administrator Sourav"
            )
            AuthResult.Success(session, firebaseUser)
        } else {
            AuthResult.Error("Invalid Admin ID or Password.")
        }
    }

    // =========================================================================
    // 2. TEACHER AUTHENTICATION (Restricted to Teacher's School)
    // =========================================================================
    suspend fun authenticateTeacher(
        schoolQuery: String,
        teacherIdOrName: String,
        pass: String
    ): AuthResult = withContext(Dispatchers.IO) {
        val trimmedSchool = schoolQuery.trim()
        val trimmedIdOrName = teacherIdOrName.trim()
        val firestore = getFirestore()
        val auth = getFirebaseAuth()

        var teacher: TeacherEntity? = null

        // 1. Query Firestore first for teacher record
        if (firestore != null) {
            try {
                val teachersRef = firestore.collection("teachers")
                val querySnapshot = teachersRef
                    .whereEqualTo("password", pass)
                    .get()
                    .await()

                for (doc in querySnapshot.documents) {
                    val docId = doc.getString("id") ?: doc.id
                    val docSchoolId = doc.getString("schoolId") ?: ""
                    val docSchoolName = doc.getString("schoolName") ?: ""
                    val docName = doc.getString("name") ?: ""

                    val matchesSchool = docSchoolId.equals(trimmedSchool, ignoreCase = true) ||
                            docSchoolName.equals(trimmedSchool, ignoreCase = true)
                    val matchesTeacher = docId.equals(trimmedIdOrName, ignoreCase = true) ||
                            docName.equals(trimmedIdOrName, ignoreCase = true)

                    if (matchesSchool && matchesTeacher) {
                        teacher = TeacherEntity(
                            id = docId,
                            schoolId = docSchoolId,
                            schoolName = docSchoolName,
                            name = docName,
                            email = doc.getString("email") ?: "",
                            mobile = doc.getString("mobile") ?: "",
                            password = pass,
                            subject = doc.getString("subject") ?: "General",
                            assignedClass = doc.getString("assignedClass") ?: "Class 10"
                        )
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore teacher lookup failed: ${e.message}")
            }
        }

        // 2. Fallback to local Room database if Firestore is offline or didn't return
        if (teacher == null) {
            teacher = database.teacherDao().loginTeacher(trimmedSchool, trimmedIdOrName, pass)
        }

        if (teacher == null) {
            return@withContext AuthResult.Error("Teacher login failed. Check School, Teacher ID, and Password.")
        }

        // 3. Authenticate with Firebase Auth
        var firebaseUser: FirebaseUser? = null
        if (auth != null) {
            val teacherEmail = if (teacher.email.isNotBlank() && teacher.email.contains("@")) {
                teacher.email.lowercase()
            } else {
                "${teacher.id.lowercase()}@schoolportal.teacher"
            }

            try {
                val signInResult = try {
                    auth.signInWithEmailAndPassword(teacherEmail, pass).await()
                } catch (e: Exception) {
                    try {
                        auth.createUserWithEmailAndPassword(teacherEmail, pass).await()
                    } catch (ce: Exception) {
                        null
                    }
                }
                firebaseUser = signInResult?.user ?: auth.currentUser

                // Update Firestore teacher document with login timestamp
                firestore?.collection("teachers")?.document(teacher.id)?.set(
                    hashMapOf(
                        "id" to teacher.id,
                        "schoolId" to teacher.schoolId,
                        "schoolName" to teacher.schoolName,
                        "name" to teacher.name,
                        "email" to teacherEmail,
                        "subject" to teacher.subject,
                        "assignedClass" to teacher.assignedClass,
                        "lastLoginAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Auth for teacher notice: ${e.message}")
            }
        }

        val session = UserSessionState(
            role = "TEACHER",
            userId = teacher.id,
            schoolId = teacher.schoolId,
            schoolName = teacher.schoolName,
            userName = teacher.name,
            userClass = teacher.assignedClass
        )
        AuthResult.Success(session, firebaseUser)
    }

    // =========================================================================
    // 3. STUDENT AUTHENTICATION (By Class & Roll or Barcode ID)
    // =========================================================================
    suspend fun authenticateStudentByRoll(
        schoolQuery: String,
        className: String,
        section: String,
        rollNumber: String,
        pass: String
    ): AuthResult = withContext(Dispatchers.IO) {
        val trimmedSchool = schoolQuery.trim()
        val trimmedClass = className.trim()
        val trimmedSection = section.trim()
        val trimmedRoll = rollNumber.trim()
        val firestore = getFirestore()

        var student: StudentEntity? = null

        // 1. Check Firestore first
        if (firestore != null) {
            try {
                val snapshot = firestore.collection("students")
                    .whereEqualTo("className", trimmedClass)
                    .whereEqualTo("section", trimmedSection)
                    .whereEqualTo("rollNumber", trimmedRoll)
                    .whereEqualTo("password", pass)
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    val docSchoolId = doc.getString("schoolId") ?: ""
                    val docSchoolName = doc.getString("schoolName") ?: ""
                    val matchesSchool = docSchoolId.equals(trimmedSchool, ignoreCase = true) ||
                            docSchoolName.equals(trimmedSchool, ignoreCase = true)

                    if (matchesSchool) {
                        student = parseStudentDoc(doc, pass)
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore student by roll query failed: ${e.message}")
            }
        }

        // 2. Fallback to local Room database
        if (student == null) {
            student = database.studentDao().loginStudent(
                schoolQuery = trimmedSchool,
                className = trimmedClass,
                section = trimmedSection,
                rollNumber = trimmedRoll,
                password = pass
            )
        }

        if (student == null) {
            return@withContext AuthResult.Error("Student login failed. Check class, section, roll number, and password.")
        }

        finishStudentAuth(student, pass)
    }

    suspend fun authenticateStudentByBarcode(
        barcodeId: String,
        pass: String
    ): AuthResult = withContext(Dispatchers.IO) {
        val trimmedId = barcodeId.trim()
        val firestore = getFirestore()

        var student: StudentEntity? = null

        // 1. Check Firestore
        if (firestore != null) {
            try {
                val doc = firestore.collection("students").document(trimmedId).get().await()
                if (doc.exists() && doc.getString("password") == pass) {
                    student = parseStudentDoc(doc, pass)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore student by barcode query failed: ${e.message}")
            }
        }

        // 2. Fallback to local Room database
        if (student == null) {
            student = database.studentDao().loginStudentByBarcodeId(trimmedId, pass)
        }

        if (student == null) {
            return@withContext AuthResult.Error("Invalid Barcode ID or Password.")
        }

        finishStudentAuth(student, pass)
    }

    private suspend fun finishStudentAuth(student: StudentEntity, pass: String): AuthResult {
        val auth = getFirebaseAuth()
        val firestore = getFirestore()
        var firebaseUser: FirebaseUser? = null

        if (auth != null) {
            val studentEmail = "${student.id.lowercase().replace("-", "_")}@schoolportal.student"
            try {
                val signInResult = try {
                    auth.signInWithEmailAndPassword(studentEmail, pass).await()
                } catch (e: Exception) {
                    try {
                        auth.createUserWithEmailAndPassword(studentEmail, pass).await()
                    } catch (ce: Exception) {
                        null
                    }
                }
                firebaseUser = signInResult?.user ?: auth.currentUser

                firestore?.collection("students")?.document(student.id)?.set(
                    hashMapOf(
                        "lastLoginAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )?.await()
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Auth student step notice: ${e.message}")
            }
        }

        val session = UserSessionState(
            role = "STUDENT",
            userId = student.id,
            schoolId = student.schoolId,
            schoolName = student.schoolName,
            userName = student.name,
            userClass = student.className,
            userSection = student.section,
            userRoll = student.rollNumber
        )
        return AuthResult.Success(session, firebaseUser)
    }

    private fun parseStudentDoc(doc: com.google.firebase.firestore.DocumentSnapshot, pass: String): StudentEntity {
        val id = doc.getString("id") ?: doc.id
        return StudentEntity(
            id = id,
            schoolId = doc.getString("schoolId") ?: "",
            schoolName = doc.getString("schoolName") ?: "",
            name = doc.getString("name") ?: "",
            className = doc.getString("className") ?: "",
            section = doc.getString("section") ?: "",
            rollNumber = doc.getString("rollNumber") ?: "",
            fatherName = doc.getString("fatherName") ?: "",
            motherName = doc.getString("motherName") ?: "",
            parentMobile = doc.getString("parentMobile") ?: "",
            studentMobile = doc.getString("studentMobile") ?: "",
            password = doc.getString("password") ?: pass,
            gender = doc.getString("gender") ?: "General",
            casteCategory = doc.getString("casteCategory") ?: "General",
            bloodGroup = doc.getString("bloodGroup") ?: "O+",
            photoUri = doc.getString("photoUri") ?: "",
            barcodeData = doc.getString("barcodeData") ?: id,
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    fun signOut() {
        try {
            getFirebaseAuth()?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error signing out from FirebaseAuth: ${e.message}")
        }
    }
}
