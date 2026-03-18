@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aiphysical.ui.screens.director

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.Organization
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.presentation.director.*
import com.example.aiphysical.ui.components.*
import com.example.aiphysical.ui.theme.*

@Composable
fun ManagementTab(
    state: DirectorDashboardState,
    vm: DirectorDashboardViewModel,
    modifier: Modifier = Modifier,
) {
    val strings = getStrings(state.currentLanguage)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(NeonViolet.copy(0.12f), CyanAccent.copy(0.06f))))
                        .border(1.dp, Brush.horizontalGradient(listOf(NeonViolet.copy(0.4f), CyanAccent.copy(0.2f))), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        strings.tabManagement,
                        style = TextStyle(brush = Brush.horizontalGradient(listOf(NeonViolet, CyanAccent)), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    )
                    Text(strings.userManagement, color = Color.White.copy(0.4f), fontSize = 11.sp)
                }
            }

            // ── Invite User Button ────────────────────────────────────────────
            item {
                InviteUserHighEndButton(
                    label = strings.inviteUser,
                    onClick = { vm.onEvent(DirectorEvent.OpenInviteSheet) }
                )
            }

            // ── Search bar ────────────────────────────────────────────────────
            item {
                GlassSearchBar(
                    query = state.searchQuery,
                    hint = strings.searchHint,
                    onQueryChange = { vm.onEvent(DirectorEvent.SearchMembers(it)) }
                )
            }

            // ── Stats row ─────────────────────────────────────────────────────
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniStatChip(
                        label = "Всего", value = "${state.members.size}",
                        color = CyanAccent, modifier = Modifier.weight(1f)
                    )
                    MiniStatChip(
                        label = "Критично", value = "${state.criticalMembers.size}",
                        color = StatusCritical, modifier = Modifier.weight(1f)
                    )
                    MiniStatChip(
                        label = "Психологов", value = "${state.psychologists.size}",
                        color = NeonViolet, modifier = Modifier.weight(1f)
                    )
                }
            }

            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) {
                        CircularProgressIndicator(color = NeonViolet, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    }
                }
                return@LazyColumn
            }

            if (state.filteredMembers.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(0.04f)).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(24.dp)).padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("👥", fontSize = 40.sp)
                        Text(
                            if (state.searchQuery.isBlank()) strings.noMembers else strings.noMatchingMembers,
                            color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                items(state.filteredMembers, key = { it.uid }) { member ->
                    ManagementMemberCard(
                        member = member, strings = strings,
                        onViewDetails = { vm.onEvent(DirectorEvent.SelectMember(member)) },
                        onChangeRole = { vm.onEvent(DirectorEvent.OpenRoleChangeSheet(member)) },
                        onToggleBlock = { vm.onEvent(DirectorEvent.ToggleUserBlock(member.uid)) }
                    )
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    // ── Invite Bottom Sheet ───────────────────────────────────────────────────
    if (state.showInviteSheet) {
        InviteCodeBottomSheet(
            organization = state.organization,
            strings = strings,
            onDismiss = { vm.onEvent(DirectorEvent.DismissInviteSheet) },
            onCopyStudent = { vm.onEvent(DirectorEvent.CopyStudentCode) },
            onCopyPsych = { vm.onEvent(DirectorEvent.CopyPsychCode) },
            onShareStudent = { vm.onEvent(DirectorEvent.ShareStudentCode) },
            onSharePsych = { vm.onEvent(DirectorEvent.SharePsychCode) },
        )
    }

    // ── Role Change Bottom Sheet ──────────────────────────────────────────────
    if (state.showRoleChangeSheet && state.roleChangeTarget != null) {
        RoleChangeBottomSheet(
            member = state.roleChangeTarget!!,
            strings = strings,
            onDismiss = { vm.onEvent(DirectorEvent.DismissRoleChangeSheet) },
            onChangeRole = { role -> vm.onEvent(DirectorEvent.ChangeUserRole(state.roleChangeTarget!!.uid, role)) }
        )
    }
}

// ─── Invite User High-End Button ──────────────────────────────────────────────

@Composable
private fun InviteUserHighEndButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(NeonViolet.copy(0.25f), CyanAccent.copy(0.15f))),
                    cornerRadius = CornerRadius(22.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(NeonViolet.copy(0.2f), CyanAccent.copy(0.12f))))
            .border(2.dp, Brush.horizontalGradient(listOf(NeonViolet.copy(0.9f), CyanAccent.copy(0.8f))), RoundedCornerShape(22.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).background(NeonViolet.copy(0.5f), CircleShape).border(1.dp, NeonViolet, CircleShape),
                Alignment.Center
            ) { Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Column {
                Text(label, style = TextStyle(brush = Brush.horizontalGradient(listOf(Color.White, CyanAccent.copy(0.9f))), fontSize = 15.sp, fontWeight = FontWeight.Bold))
                Text("Психолог, студент или преподаватель", color = Color.White.copy(0.4f), fontSize = 11.sp)
            }
        }
    }
}

// ─── Mini Stat Chip ───────────────────────────────────────────────────────────

@Composable
private fun MiniStatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(0.08f))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = color.copy(0.7f), fontSize = 10.sp)
    }
}

// ─── Management Member Card ───────────────────────────────────────────────────

@Composable
private fun ManagementMemberCard(
    member: UserProfile,
    strings: Strings,
    onViewDetails: () -> Unit,
    onChangeRole: () -> Unit,
    onToggleBlock: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val isBlocked = member.isBlocked
    val roleColor = when (member.role) {
        "director" -> NeonViolet
        "psychologist" -> CyanAccent
        "teacher" -> AlertOrange
        else -> TextSecondary
    }
    val borderBrush = when {
        isBlocked -> Brush.horizontalGradient(listOf(ErrorColor.copy(0.5f), ErrorColor.copy(0.2f)))
        member.latestAiStatus == "critical" -> Brush.horizontalGradient(listOf(StatusCritical.copy(0.5f), StatusCritical.copy(0.2f)))
        else -> Brush.horizontalGradient(listOf(Color.White.copy(0.14f), Color.White.copy(0.04f)))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isBlocked) ErrorColor.copy(0.06f) else Color.White.copy(0.04f)
            )
            .border(1.dp, borderBrush, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            Modifier.size(44.dp)
                .background(
                    Brush.radialGradient(listOf(if (isBlocked) ErrorColor.copy(0.4f) else NeonViolet.copy(0.5f), CardSurface)),
                    CircleShape
                )
                .border(1.5.dp, if (isBlocked) ErrorColor.copy(0.6f) else NeonViolet.copy(0.5f), CircleShape),
            Alignment.Center
        ) {
            Text(member.fullName.take(1).uppercase(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    member.fullName,
                    color = if (isBlocked) TextSecondary else TextPrimary,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isBlocked) Text("🔒", fontSize = 10.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.background(roleColor.copy(0.15f), RoundedCornerShape(6.dp)).border(1.dp, roleColor.copy(0.3f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(roleLabel(member.role, strings), color = roleColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                if (member.ageGroup.isNotBlank()) Text("· ${member.ageGroup}", color = TextHint, fontSize = 10.sp)
            }
        }

        StatusBadge(status = member.latestAiStatus, label = statusLabel(member.latestAiStatus, strings, member.role))

        Box {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(0.06f))
                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(10.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Text("⋮", color = TextSecondary, fontSize = 18.sp)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .background(CardSurfaceLight)
                    .border(1.dp, Brush.verticalGradient(listOf(NeonViolet.copy(0.3f), GlassBorder)), RoundedCornerShape(16.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("👁 ${strings.viewDetails}", color = TextPrimary, fontSize = 13.sp) },
                    onClick = { showMenu = false; onViewDetails() }
                )
                DropdownMenuItem(
                    text = { Text("🎭 ${strings.changeRole}", color = CyanAccent, fontSize = 13.sp) },
                    onClick = { showMenu = false; onChangeRole() }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (isBlocked) "🔓 ${strings.unblockUser}" else "🔒 ${strings.blockUser}",
                            color = if (isBlocked) SuccessColor else ErrorColor, fontSize = 13.sp
                        )
                    },
                    onClick = { showMenu = false; onToggleBlock() }
                )
            }
        }
    }
}

private fun roleLabel(role: String, strings: Strings): String = when (role) {
    "director"     -> strings.roleDirectorShort
    "psychologist" -> strings.rolePsychShort
    "teacher"      -> strings.roleTeacherShort
    else           -> strings.roleStudentShort
}

// ─── Invite Code Bottom Sheet ─────────────────────────────────────────────────

@Composable
fun InviteCodeBottomSheet(
    organization: Organization?,
    strings: Strings,
    onDismiss: () -> Unit,
    onCopyStudent: () -> Unit,
    onCopyPsych: () -> Unit,
    onShareStudent: () -> Unit,
    onSharePsych: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F0F28),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.width(48.dp).height(4.dp)
                        .background(Brush.horizontalGradient(listOf(NeonViolet.copy(0.6f), CyanAccent.copy(0.5f))), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Sheet header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    strings.inviteSheetTitle,
                    style = TextStyle(brush = Brush.horizontalGradient(listOf(NeonViolet, CyanAccent)), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    organization?.name ?: strings.orgHealthTitle,
                    color = Color.White.copy(0.35f), fontSize = 12.sp
                )
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(NeonViolet.copy(0.5f), CyanAccent.copy(0.3f), Color.Transparent))))

            // Psychologist Card
            InviteTypeCard(
                emoji = "🧠",
                title = strings.psychAccess,
                subtitle = strings.psychAccessDesc,
                code = organization?.inviteCodePsych ?: "——",
                accentColor = NeonViolet,
                copyLabel = strings.copyCode,
                shareLabel = strings.shareCode,
                onCopy = onCopyPsych,
                onShare = onSharePsych
            )

            // Student Card
            InviteTypeCard(
                emoji = "🎓",
                title = strings.studentAccess,
                subtitle = strings.studentAccessDesc,
                code = organization?.inviteCodeStudent ?: "——",
                accentColor = CyanAccent,
                copyLabel = strings.copyCode,
                shareLabel = strings.shareCode,
                onCopy = onCopyStudent,
                onShare = onShareStudent
            )
        }
    }
}

@Composable
private fun InviteTypeCard(
    emoji: String, title: String, subtitle: String, code: String,
    accentColor: Color, copyLabel: String, shareLabel: String,
    onCopy: () -> Unit, onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(accentColor.copy(0.18f), accentColor.copy(0.06f))))
            .border(1.5.dp, Brush.verticalGradient(listOf(accentColor.copy(0.6f), accentColor.copy(0.2f))), RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title row
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp)
                    .background(accentColor.copy(0.3f), RoundedCornerShape(14.dp))
                    .border(1.dp, accentColor.copy(0.6f), RoundedCornerShape(14.dp)),
                Alignment.Center
            ) { Text(emoji, fontSize = 20.sp) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = accentColor.copy(0.8f), fontSize = 11.sp)
            }
        }

        // Code display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(0.4f))
                .border(1.dp, accentColor.copy(0.4f), RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = code.ifBlank { "——" },
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(accentColor, accentColor.copy(0.7f))),
                    fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace, letterSpacing = 5.sp
                )
            )
        }

        // Action buttons
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CodeButton(emoji = "📋", label = copyLabel, color = accentColor, onClick = onCopy, modifier = Modifier.weight(1f))
            CodeButton(emoji = "📤", label = shareLabel, color = accentColor, onClick = onShare, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CodeButton(emoji: String, label: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .background(color.copy(0.15f), RoundedCornerShape(14.dp))
            .border(1.dp, color.copy(0.4f), RoundedCornerShape(14.dp))
    ) {
        Text("$emoji $label", color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Role Change Bottom Sheet ─────────────────────────────────────────────────

@Composable
private fun RoleChangeBottomSheet(
    member: UserProfile,
    strings: Strings,
    onDismiss: () -> Unit,
    onChangeRole: (String) -> Unit,
) {
    val roles = listOf(
        Triple("user",         "🎓", strings.roleStudentShort),
        Triple("teacher",      "🧑‍🏫", strings.roleTeacherShort),
        Triple("psychologist", "🧠", strings.rolePsychShort),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F0F28),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.width(48.dp).height(4.dp).background(Brush.horizontalGradient(listOf(NeonViolet.copy(0.6f), CyanAccent.copy(0.5f))), RoundedCornerShape(2.dp)))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    strings.changeRole,
                    style = TextStyle(brush = Brush.horizontalGradient(listOf(NeonViolet, CyanAccent)), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                )
                // Member badge
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(NeonViolet.copy(0.1f)).border(1.dp, NeonViolet.copy(0.3f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(24.dp).background(Brush.radialGradient(listOf(NeonViolet.copy(0.5f), CardSurface)), CircleShape).border(1.dp, NeonViolet.copy(0.5f), CircleShape),
                        Alignment.Center
                    ) { Text(member.fullName.take(1).uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    Text(member.fullName, color = NeonViolet, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(NeonViolet.copy(0.4f), Color.Transparent))))

            roles.forEach { (roleKey, roleEmoji, roleLabel) ->
                val isCurrentRole = member.role == roleKey
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isCurrentRole) NeonViolet.copy(0.18f) else Color.White.copy(0.04f))
                        .border(
                            if (isCurrentRole) 1.5.dp else 1.dp,
                            if (isCurrentRole) Brush.horizontalGradient(listOf(NeonViolet.copy(0.8f), CyanAccent.copy(0.5f))) else Brush.horizontalGradient(listOf(Color.White.copy(0.12f), Color.White.copy(0.04f))),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !isCurrentRole) { onChangeRole(roleKey) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(36.dp)
                                .background(if (isCurrentRole) NeonViolet.copy(0.3f) else Color.White.copy(0.06f), RoundedCornerShape(12.dp))
                                .border(1.dp, if (isCurrentRole) NeonViolet.copy(0.6f) else Color.White.copy(0.1f), RoundedCornerShape(12.dp)),
                            Alignment.Center
                        ) { Text(roleEmoji, fontSize = 18.sp) }
                        Column {
                            Text(roleLabel, color = if (isCurrentRole) NeonViolet else TextPrimary, fontSize = 14.sp, fontWeight = if (isCurrentRole) FontWeight.Bold else FontWeight.Medium)
                            if (isCurrentRole) Text("Текущая роль", color = NeonViolet.copy(0.6f), fontSize = 11.sp)
                        }
                    }
                    if (isCurrentRole) {
                        Box(
                            Modifier.size(24.dp).background(NeonViolet.copy(0.2f), CircleShape).border(1.dp, NeonViolet.copy(0.5f), CircleShape),
                            Alignment.Center
                        ) { Text("✓", color = NeonViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
