package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentLoginScreen(
    onLoginSuccess: () -> Unit,
    onBackClick: () -> Unit,
    onLoginByClassRoll: suspend (school: String, className: String, section: String, roll: String, pass: String) -> Boolean,
    onLoginByBarcodeId: suspend (barcodeId: String, pass: String) -> Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    var loginMethodTab by remember { mutableStateOf(0) } // 0: Class & Roll, 1: Barcode ID

    // Fields for Class & Roll mode
    var schoolInput by remember { mutableStateOf("Delhi Public Academy") }
    var classInput by remember { mutableStateOf("10") }
    var sectionInput by remember { mutableStateOf("A") }
    var rollNumberInput by remember { mutableStateOf("01") }

    // Fields for Barcode ID mode
    var barcodeIdInput by remember { mutableStateOf("STU-10-A-01") }

    // Shared password field
    var passwordInput by remember { mutableStateOf("pass123") }
    var passwordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Login", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("btn_back_student_login")) {
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
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Student Portal Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "View your attendance, digital ID card, exam report card, and teacher announcements.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Method Selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = loginMethodTab == 0,
                    onClick = { loginMethodTab = 0; errorMessage = null },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("tab_login_class_roll")
                ) {
                    Text("Class & Roll No.")
                }
                SegmentedButton(
                    selected = loginMethodTab == 1,
                    onClick = { loginMethodTab = 1; errorMessage = null },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("tab_login_barcode_id")
                ) {
                    Text("Barcode ID")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (loginMethodTab == 0) {
                // School field
                OutlinedTextField(
                    value = schoolInput,
                    onValueChange = { schoolInput = it; errorMessage = null },
                    label = { Text("School Name") },
                    leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_student_school"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Class, Section, Roll in a row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = classInput,
                        onValueChange = { classInput = it; errorMessage = null },
                        label = { Text("Class") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_student_class"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = sectionInput,
                        onValueChange = { sectionInput = it; errorMessage = null },
                        label = { Text("Section") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_student_section"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = rollNumberInput,
                        onValueChange = { rollNumberInput = it; errorMessage = null },
                        label = { Text("Roll No.") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_student_roll"),
                        singleLine = true
                    )
                }
            } else {
                // Barcode ID field
                OutlinedTextField(
                    value = barcodeIdInput,
                    onValueChange = { barcodeIdInput = it; errorMessage = null },
                    label = { Text("Student Barcode ID") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                    placeholder = { Text("e.g. STU-10-A-01") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_student_barcode_id"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Password field
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it; errorMessage = null },
                label = { Text("Student Password") },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_student_password"),
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
                    coroutineScope.launch {
                        isLoading = true
                        val success = if (loginMethodTab == 0) {
                            onLoginByClassRoll(schoolInput, classInput, sectionInput, rollNumberInput, passwordInput)
                        } else {
                            onLoginByBarcodeId(barcodeIdInput, passwordInput)
                        }
                        isLoading = false
                        if (success) {
                            onLoginSuccess()
                        } else {
                            errorMessage = "Credentials did not match our school records."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_submit_student_login"),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Login to Student Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick demo buttons
            Text(
                text = "Demo Student Profiles:",
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
                        loginMethodTab = 0
                        schoolInput = "Delhi Public Academy"
                        classInput = "10"
                        sectionInput = "A"
                        rollNumberInput = "01"
                        passwordInput = "pass123"
                    },
                    label = { Text("Aarav (Class 10-A, #01)") },
                    modifier = Modifier.weight(1f).testTag("chip_quick_student_aarav")
                )
                SuggestionChip(
                    onClick = {
                        loginMethodTab = 1
                        barcodeIdInput = "STU-10-A-02"
                        passwordInput = "pass123"
                    },
                    label = { Text("Priya (ID: STU-10-A-02)") },
                    modifier = Modifier.weight(1f).testTag("chip_quick_student_priya")
                )
            }
        }
    }
}
