package com.airemore.fieldapp.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airemore.fieldapp.AiremoreApp
import com.airemore.fieldapp.data.repository.AuthResult
import com.airemore.fieldapp.ui.theme.AiremoreAccent
import com.airemore.fieldapp.ui.theme.AiremoreBad
import com.airemore.fieldapp.ui.theme.AiremoreBorder
import com.airemore.fieldapp.ui.theme.AiremoreFieldBg
import com.airemore.fieldapp.ui.theme.AiremoreNavy
import com.airemore.fieldapp.ui.theme.AiremorePanel
import com.airemore.fieldapp.ui.theme.AiremoreSteel
import com.airemore.fieldapp.ui.theme.AiremoreTextMuted
import kotlinx.coroutines.launch

/**
 * Mirrors the web login page 1:1 (login.php + .login-wrap / .login-card in
 * assets/css/style.css):
 *  - full-bleed diagonal navy -> steel gradient behind the card
 *  - white rounded card, "aire" (navy) + "more" (accent) wordmark
 *  - uppercase muted tagline
 *  - uppercase, letter-spaced field labels above each input
 *  - full-width black uppercase "LOG IN" button
 */
@Composable
fun LoginScreen(app: AiremoreApp, onLoggedIn: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // background: linear-gradient(160deg, navy 0%, #16405F 60%, steel 100%)
    val backgroundBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to AiremoreNavy,
            0.6f to Color(0xFF16405F),
            1.0f to AiremoreSteel,
        ),
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        // .login-card { background:#fff; width:380px; padding:40px 36px; border-radius:8px; shadow }
        Column(
            Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AiremorePanel)
                .padding(horizontal = 36.dp, vertical = 40.dp),
        ) {
            // .login-card .brand — "aire" (navy) + "more" (accent), flush together on one line
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = AiremoreNavy)) { append("aire") }
                    withStyle(SpanStyle(color = AiremoreAccent)) { append("more") }
                },
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            // .login-card .tagline
            Text(
                "AIRCON SERVICE MANAGEMENT",
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = AiremoreTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 26.dp),
            )

            if (error != null) {
                // .error-msg
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFDECEA))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(error!!, color = AiremoreBad, fontSize = 13.sp)
                }
                Spacer(Modifier.height(16.dp))
            }

            LabeledField(label = "Username") {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; error = null },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = airemoreFieldColors(),
                )
            }
            Spacer(Modifier.height(16.dp))
            LabeledField(label = "Password") {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = AiremoreTextMuted,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = airemoreFieldColors(),
                )
            }

            Spacer(Modifier.height(24.dp))
            // .btn.btn-primary — full width, uppercase, black
            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        error = "Please enter both username and password."
                        return@Button
                    }
                    loading = true
                    error = null
                    scope.launch {
                        when (val result = app.authRepository.login(username, password)) {
                            is AuthResult.Success -> {
                                app.lookupRepository.refreshFromServer()
                                loading = false
                                onLoggedIn()
                            }
                            is AuthResult.Error -> {
                                loading = false
                                error = result.message
                            }
                        }
                    }
                },
                enabled = !loading,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AiremoreAccent,
                    contentColor = Color.White,
                    disabledContainerColor = AiremoreAccent.copy(alpha = 0.6f),
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("LOG IN", fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Tip: mag-login habang may signal para ma-download ang company list at iba pang data. Pagkatapos nun, gumagana na ang app kahit walang internet.",
                fontSize = 12.sp,
                color = AiremoreTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column {
        // .field label — uppercase, 12px, weight 600, letter-spacing, muted
        Text(
            label.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            color = AiremoreTextMuted,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        content()
    }
}

@Composable
private fun airemoreFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AiremoreFieldBg,
    unfocusedContainerColor = AiremoreFieldBg,
    disabledContainerColor = AiremoreFieldBg,
    focusedBorderColor = AiremoreSteel,
    unfocusedBorderColor = AiremoreBorder,
    cursorColor = AiremoreSteel,
)
