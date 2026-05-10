package com.campusconnect.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campusconnect.app.BuildConfig
import com.campusconnect.app.R
import com.campusconnect.app.viewmodel.AuthViewModel
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

@Composable
fun LoginScreen(
    onLoginSuccess: (isAdmin: Boolean) -> Unit,
    onNavigateToRegister: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state   by vm.uiState.collectAsStateWithLifecycle()
    val isAdmin by vm.isAdmin.collectAsStateWithLifecycle()

    var username        by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotDialog   by remember { mutableStateOf(false) }
    var forgotEmail        by remember { mutableStateOf("") }
    var forgotSentMessage  by remember { mutableStateOf<String?>(null) }

    val context      = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.success) {
        if (state.success) onLoginSuccess(isAdmin)
    }

    fun submit() {
        focusManager.clearFocus()
        vm.signIn(username, password)
    }

    val credentialManager = remember { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF060D2E),
                        Color(0xFF0D1B4B),
                        Color(0xFF152260),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            Text(
                text       = "Campus Connect",
                style      = MaterialTheme.typography.headlineMedium,
                color      = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Welcome back",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f)
            )

            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(28.dp)
            ) {
                Column(Modifier.padding(24.dp)) {

                    Text(
                        text       = "Sign In",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Username ──────────────────────────────────────────────
                    OutlinedTextField(
                        value         = username,
                        onValueChange = { username = it.lowercase().replace(" ", "") },
                        label         = { Text("Username") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        shape         = RoundedCornerShape(14.dp),
                        leadingIcon   = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            imeAction    = ImeAction.Next,
                            keyboardType = KeyboardType.Ascii
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // ── Password ──────────────────────────────────────────────
                    OutlinedTextField(
                        value         = password,
                        onValueChange = { password = it },
                        label         = { Text("Password") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        shape         = RoundedCornerShape(14.dp),
                        leadingIcon   = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            imeAction    = ImeAction.Done,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password"
                                    else "Show password"
                                )
                            }
                        }
                    )

                    // ── Forgot Password ───────────────────────────────────────
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = {
                            forgotEmail       = ""
                            forgotSentMessage = null
                            showForgotDialog  = true
                        }) {
                            Text(
                                "Forgot password?",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF93A8F4)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Error message ─────────────────────────────────────────
                    AnimatedVisibility(
                        visible = state.error != null,
                        enter   = fadeIn() + expandVertically(),
                        exit    = fadeOut() + shrinkVertically()
                    ) {
                        state.error?.let { errorMsg ->
                            Surface(
                                color    = MaterialTheme.colorScheme.errorContainer,
                                shape    = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint               = MaterialTheme.colorScheme.error,
                                        modifier           = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text  = errorMsg,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    // ── Sign In button ────────────────────────────────────────
                    val canSubmit = username.isNotBlank() && password.isNotBlank()

                    Button(
                        onClick  = ::submit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !state.isLoading && canSubmit,
                        shape   = RoundedCornerShape(14.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color       = Color.White
                            )
                        } else {
                            Text(
                                text       = "Sign In",
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 16.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(Modifier.weight(1f))
                        Text(
                            text  = "  or  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Google sign-in ────────────────────────────────────────
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                        .build()
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()
                                    val result = credentialManager.getCredential(context, request)
                                    val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)
                                    vm.signInWithGoogle(googleCred.idToken)
                                } catch (_: GetCredentialException) { }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape   = RoundedCornerShape(14.dp),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Signing in…")
                        } else {
                            Image(
                                painter            = painterResource(R.drawable.ic_google),
                                contentDescription = "Google",
                                modifier           = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Continue with Google")
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Navigate to Register ──────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "Don't have an account? ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text      = "Register",
                            style     = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color     = MaterialTheme.colorScheme.primary,
                            modifier  = Modifier.clickable { onNavigateToRegister() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Forgot Password Dialog ────────────────────────────────────────────────
    if (showForgotDialog) {
        var sendingReset by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!sendingReset) showForgotDialog = false },
            title   = { Text("Reset Password") },
            text    = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (forgotSentMessage != null) {
                        val isError = forgotSentMessage!!.startsWith("No account") ||
                                      forgotSentMessage!!.startsWith("Could not") ||
                                      forgotSentMessage!!.startsWith("Please enter") ||
                                      forgotSentMessage!!.startsWith("Failed")
                        Text(
                            forgotSentMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isError) MaterialTheme.colorScheme.error
                                    else         MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            "Enter the email linked to your account and we'll send a reset link.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value         = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            label         = { Text("Email address") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            enabled       = !sendingReset,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                    }
                }
            },
            confirmButton = {
                if (forgotSentMessage != null && !forgotSentMessage!!.startsWith("No account") &&
                    !forgotSentMessage!!.startsWith("Could not") &&
                    !forgotSentMessage!!.startsWith("Please enter") &&
                    !forgotSentMessage!!.startsWith("Failed")) {
                    TextButton(onClick = { showForgotDialog = false }) { Text("Done") }
                } else {
                    TextButton(
                        onClick = {
                            sendingReset = true
                            forgotSentMessage = null
                            scope.launch {
                                forgotSentMessage = vm.sendPasswordReset(forgotEmail.trim())
                                sendingReset = false
                            }
                        },
                        enabled = forgotEmail.contains("@") && !sendingReset
                    ) {
                        if (sendingReset) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Send Reset Link")
                        }
                    }
                }
            },
            dismissButton = {
                if (!sendingReset) {
                    TextButton(onClick = { showForgotDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}
