package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.SchoolEntity
import com.example.data.model.TeacherEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherLoginScreen(
    availableSchools: List<SchoolEntity>,
    availableTeachers: List<TeacherEntity>,
    onLoginSuccess: () -> Unit,
    onBackClick: () -> Unit,
    onLoginAction: suspend (school: String, teacherId: String, pass: String) -> Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    var schoolInput by remember { mutableStateOf("Delhi Public Academy") }
    var teacherIdInput by remember { mutableStateOf("TCH-501") }
    var passwordInput by remember { mutableStateOf("teacher123") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Login", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("btn_back_teacher_login")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Teacher Portal Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter your school name, teacher ID, and password assigned by Administrator.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // School Name or School ID
            OutlinedTextField(
                value = schoolInput,
                onValueChange = {
                    schoolInput = it
                    errorMessage = null
                },
                label = { Text("School Name or School ID") },
                leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null) },
                placeholder = { Text("e.g. Delhi Public Academy or SCH-1001") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_teacher_school"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Teacher ID / Email / Name
            OutlinedTextField(
                value = teacherIdInput,
                onValueChange = {
                    teacherIdInput = it
                    errorMessage = null
                },
                label = { Text("Teacher ID or Email") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                placeholder = { Text("e.g. TCH-501 or sarah.jenkins@school.edu") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_teacher_id"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password
            OutlinedTextField(
                value = passwordInput,
                onValueChange = {
                    passwordInput = it
                    errorMessage = null
                },
                label = { Text("Teacher Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (schoolInput.isNotBlank() && teacherIdInput.isNotBlank() && passwordInput.isNotBlank()) {
                        coroutineScope.launch {
                            isLoading = true
                            val success = onLoginAction(schoolInput, teacherIdInput, passwordInput)
                            isLoading = false
                            if (success) {
                                onLoginSuccess()
                            } else {
                                errorMessage = "Incorrect school, teacher ID, or password."
                            }
                        }
                    }
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_teacher_password"),
                singleLine = true
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (schoolInput.isBlank() || teacherIdInput.isBlank() || passwordInput.isBlank()) {
                        errorMessage = "Please fill in all fields"
                        return@Button
                    }
                    coroutineScope.launch {
                        isLoading = true
                        val success = onLoginAction(schoolInput, teacherIdInput, passwordInput)
                        isLoading = false
                        if (success) {
                            onLoginSuccess()
                        } else {
                            errorMessage = "Incorrect school, teacher ID, or password."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_submit_teacher_login"),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Login as Teacher", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick demo logins
            Text(
                text = "Quick Demo Teacher Accounts:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = {
                        schoolInput = "Delhi Public Academy"
                        teacherIdInput = "TCH-501"
                        passwordInput = "teacher123"
                    },
                    label = { Text("Mrs. Jenkins (TCH-501)") },
                    modifier = Modifier.weight(1f).testTag("chip_quick_teacher_jenkins")
                )
                SuggestionChip(
                    onClick = {
                        schoolInput = "Delhi Public Academy"
                        teacherIdInput = "TCH-502"
                        passwordInput = "teacher123"
                    },
                    label = { Text("Mr. Vance (TCH-502)") },
                    modifier = Modifier.weight(1f).testTag("chip_quick_teacher_vance")
                )
            }
        }
    }
}
