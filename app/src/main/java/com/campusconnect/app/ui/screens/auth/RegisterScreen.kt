package com.campusconnect.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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

// ── Password strength helpers ─────────────────────────────────────────────────

private fun passwordStrength(pw: String): Int {
    if (pw.length < 6) return 0
    var score = 1
    if (pw.length >= 10) score++
    if (pw.any { it.isUpperCase() } && pw.any { it.isLowerCase() }) score++
    if (pw.any { !it.isLetterOrDigit() } || pw.any { it.isDigit() }) score++
    return score.coerceIn(0, 4)
}

private val strengthColors = listOf(
    Color(0xFFEF4444),
    Color(0xFFF97316),
    Color(0xFFF59E0B),
    Color(0xFF10B981)
)
private val strengthLabels = listOf("Weak", "Fair", "Good", "Strong")

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun RegisterScreen(
    onRegisterSuccess: (isAdmin: Boolean) -> Unit,
    onNavigateToLogin: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state   by vm.uiState.collectAsStateWithLifecycle()
    val isAdmin by vm.isAdmin.collectAsStateWithLifecycle()

    var name                    by remember { mutableStateOf("") }
    var username                by remember { mutableStateOf("") }
    var email                   by remember { mutableStateOf("") }
    var password                by remember { mutableStateOf("") }
    var confirmPassword         by remember { mutableStateOf("") }
    var passwordVisible         by remember { mutableStateOf(false) }
    var confirmPasswordVisible  by remember { mutableStateOf(false) }

    val context      = LocalContext.current
    val focusManager = LocalFocusManager.current
    val strength     = remember(password) { passwordStrength(password) }

    LaunchedEffect(state.success) {
        if (state.success) onRegisterSuccess(isAdmin)
    }

    fun submit() {
        focusManager.clearFocus()
        vm.signUp(username, email, password, name)
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
            Spacer(Modifier.height(56.dp))

            Text(
                text       = "Campus Connect",
                style      = MaterialTheme.typography.headlineMedium,
                color      = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Create your student account",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f)
            )

            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(28.dp)
            ) {
                Column(Modifier.padding(24.dp)) {

                    Text(
                        text       = "Sign Up",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Full Name ─────────────────────────────────────────────
                    OutlinedTextField(
                        value         = name,
                        onValueChange = { name = it },
                        label         = { Text("Full Name") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        shape         = RoundedCornerShape(14.dp),
                        leadingIcon   = { Icon(Icons.Default.Person, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            imeAction      = ImeAction.Next,
                            capitalization = KeyboardCapitalization.Words
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(Modifier.height(12.dp))

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

                    // ── Email ─────────────────────────────────────────────────
                    OutlinedTextField(
                        value         = email,
                        onValueChange = { email = it.trim() },
                        label         = { Text("Email") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        shape         = RoundedCornerShape(14.dp),
                        leadingIcon   = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            imeAction    = ImeAction.Next,
                            keyboardType = KeyboardType.Email
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
                            imeAction    = ImeAction.Next,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
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

                    // ── Password strength bar ─────────────────────────────────
                    AnimatedVisibility(
                        visible = password.isNotEmpty(),
                        enter   = fadeIn() + expandVertically(),
                        exit    = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(4) { index ->
                                    val filled   = strength > 0 && index < strength
                                    val segColor = if (filled) strengthColors[strength - 1]
                                    else MaterialTheme.colorScheme.surfaceVariant
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(segColor)
                                    )
                                }
                            }
                            if (strength > 0) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text       = strengthLabels[strength - 1],
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = strengthColors[strength - 1],
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Confirm Password ──────────────────────────────────────
                    val mismatch = confirmPassword.isNotEmpty() && confirmPassword != password

                    OutlinedTextField(
                        value         = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label         = { Text("Confirm Password") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        shape         = RoundedCornerShape(14.dp),
                        leadingIcon   = { Icon(Icons.Default.Lock, contentDescription = null) },
                        isError       = mismatch,
                        supportingText = if (mismatch) {
                            { Text("Passwords do not match") }
                        } else null,
                        visualTransformation = if (confirmPasswordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            imeAction    = ImeAction.Done,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Hide password"
                                    else "Show password"
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(18.dp))

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

                    // ── Create Account button ─────────────────────────────────
                    val canSubmit = name.isNotBlank() && username.isNotBlank() &&
                            email.contains("@") && email.contains(".") && password.length >= 6 &&
                            confirmPassword == password

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
                                text       = "Create Account",
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

                    // ── Navigate to Login ─────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "Already have an account? ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text       = "Sign In",
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.primary,
                            modifier   = Modifier.clickable { onNavigateToLogin() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
