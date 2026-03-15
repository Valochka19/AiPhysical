@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aiphysical.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.presentation.auth.*
import com.example.aiphysical.ui.components.*
import com.example.aiphysical.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    role: UserRole,
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
) {
    val strings = getStrings(uiState.currentLanguage)

    AnimatedBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            // ── Top Bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onEvent(AuthEvent.NavigateToRoleSelection) }) {
                    Text("← ${strings.back}", color = TextSecondary, fontSize = 14.sp)
                }
                LanguageSwitcher(
                    currentLanguage = uiState.currentLanguage,
                    onLanguageChange = { onEvent(AuthEvent.ChangeLanguage(it)) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Role badge ───────────────────────────────────────────────────
            val (roleEmoji, roleTitle, gradientStart, gradientEnd) = when (role) {
                UserRole.DIRECTOR -> RoleInfo("🏢", strings.roleDirector, Color(0xFF7C3AED), Color(0xFF4C1D95))
                UserRole.PSYCHOLOGIST -> RoleInfo("🧠", strings.rolePsychologist, Color(0xFF0891B2), Color(0xFF164E63))
                UserRole.STUDENT -> RoleInfo("👤", strings.roleStudent, Color(0xFFBE185D), Color(0xFF7C1D45))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(gradientStart.copy(0.25f), gradientEnd.copy(0.15f))),
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, gradientStart.copy(0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(roleEmoji, fontSize = 28.sp)
                Column {
                    Text(
                        text = strings.register,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = roleTitle,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Error banner ─────────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it }
            ) {
                Column {
                    uiState.errorMessage?.let {
                        ErrorBanner(message = it, onDismiss = { onEvent(AuthEvent.DismissError) })
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Form Card ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24.dp, bgAlpha = 0.10f, borderAlpha = 0.22f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (role) {
                    UserRole.DIRECTOR -> DirectorForm(strings, uiState.isLoading, onEvent)
                    UserRole.PSYCHOLOGIST -> PsychologistForm(strings, uiState.isLoading, onEvent)
                    UserRole.STUDENT -> StudentForm(strings, uiState.isLoading, uiState.currentLanguage, onEvent)
                }
            }

            Spacer(Modifier.height(24.dp))

            TextButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = { onEvent(AuthEvent.NavigateToLogin) }
            ) {
                Text(strings.haveAccount, color = VioletGlow, fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Director Form ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectorForm(
    strings: Strings,
    isLoading: Boolean,
    onEvent: (AuthEvent) -> Unit,
) {
    var orgName by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    FormTitle("🏢", strings.roleDirector)

    GlassTextField(
        value = orgName,
        onValueChange = { orgName = it },
        label = strings.orgName,
        leadingEmoji = "🏛️"
    )
    GlassTextField(
        value = fullName,
        onValueChange = { fullName = it },
        label = strings.fullName,
        leadingEmoji = "👤"
    )
    GlassTextField(
        value = email,
        onValueChange = { email = it },
        label = strings.email,
        keyboardType = KeyboardType.Email,
        leadingEmoji = "✉️"
    )
    GlassTextField(
        value = password,
        onValueChange = { password = it },
        label = strings.password,
        isPassword = true,
        leadingEmoji = "🔒"
    )

    GlassPrimaryButton(
        text = strings.createOrg,
        isLoading = isLoading,
        onClick = {
            onEvent(
                AuthEvent.RegisterDirector(
                    orgName = orgName.trim(),
                    fullName = fullName.trim(),
                    email = email.trim(),
                    password = password
                )
            )
        }
    )
}

// ─── Psychologist Form ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PsychologistForm(
    strings: Strings,
    isLoading: Boolean,
    onEvent: (AuthEvent) -> Unit,
) {
    var fullName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    FormTitle("🧠", strings.rolePsychologist)

    GlassTextField(
        value = fullName,
        onValueChange = { fullName = it },
        label = strings.fullName,
        leadingEmoji = "👤"
    )
    GlassTextField(
        value = inviteCode,
        onValueChange = { inviteCode = it },
        label = strings.specialCode,
        leadingEmoji = "🔑"
    )
    GlassTextField(
        value = email,
        onValueChange = { email = it },
        label = strings.email,
        keyboardType = KeyboardType.Email,
        leadingEmoji = "✉️"
    )
    GlassTextField(
        value = password,
        onValueChange = { password = it },
        label = strings.password,
        isPassword = true,
        leadingEmoji = "🔒"
    )

    GlassPrimaryButton(
        text = strings.register,
        isLoading = isLoading,
        onClick = {
            onEvent(
                AuthEvent.RegisterPsychologist(
                    fullName = fullName.trim(),
                    inviteCode = inviteCode.trim().uppercase(),
                    email = email.trim(),
                    password = password
                )
            )
        }
    )
}

// ─── Student Form ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentForm(
    strings: Strings,
    isLoading: Boolean,
    language: AppLanguage,
    onEvent: (AuthEvent) -> Unit,
) {
    var fullName by remember { mutableStateOf("") }
    var orgCode by remember { mutableStateOf("") }
    var selectedAgeGroup by remember { mutableStateOf(AgeGroup.JUNIOR) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    FormTitle("👤", strings.roleStudent)

    GlassTextField(
        value = fullName,
        onValueChange = { fullName = it },
        label = strings.fullName,
        leadingEmoji = "👤"
    )
    GlassTextField(
        value = orgCode,
        onValueChange = { orgCode = it },
        label = strings.orgCode,
        leadingEmoji = "🏛️"
    )

    // ── Age Group Picker (stable DropdownMenu, no experimental API) ───────────
    AgeGroupPicker(
        selected = selectedAgeGroup,
        onSelected = { selectedAgeGroup = it },
        label = strings.ageGroupLabel,
        language = language
    )

    GlassTextField(
        value = email,
        onValueChange = { email = it },
        label = strings.email,
        keyboardType = KeyboardType.Email,
        leadingEmoji = "✉️"
    )
    GlassTextField(
        value = password,
        onValueChange = { password = it },
        label = strings.password,
        isPassword = true,
        leadingEmoji = "🔒"
    )

    GlassPrimaryButton(
        text = strings.register,
        isLoading = isLoading,
        onClick = {
            onEvent(
                AuthEvent.RegisterStudent(
                    fullName = fullName.trim(),
                    orgCode = orgCode.trim().uppercase(),
                    ageGroup = selectedAgeGroup,
                    email = email.trim(),
                    password = password
                )
            )
        }
    )
}

// ─── Helper Composable ────────────────────────────────────────────────────────

@Composable
private fun FormTitle(emoji: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 26.sp
        )
    }
    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
}

@Composable
private fun AgeGroupPicker(
    selected: AgeGroup,
    onSelected: (AgeGroup) -> Unit,
    label: String,
    language: AppLanguage,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .background(GlassBg, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selected.display(language), color = TextPrimary, fontSize = 15.sp)
                    Text(if (expanded) "▲" else "▼", color = TextSecondary, fontSize = 11.sp)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(SurfaceDeep)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            ) {
                AgeGroup.entries.forEach { ageGroup ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (ageGroup == selected) "●" else "○",
                                    color = if (ageGroup == selected) VioletGlow else TextHint,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = ageGroup.display(language),
                                    color = if (ageGroup == selected) VioletGlow else TextPrimary,
                                    fontWeight = if (ageGroup == selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        onClick = { onSelected(ageGroup); expanded = false }
                    )
                }
            }
        }
    }
}

private data class RoleInfo(
    val emoji: String,
    val title: String,
    val gradientStart: Color,
    val gradientEnd: Color,
)

