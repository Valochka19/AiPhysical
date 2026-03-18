package com.example.aiphysical

import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.presentation.auth.AgeGroup
import com.example.aiphysical.presentation.auth.AuthScreen
import com.example.aiphysical.presentation.auth.persistedRole
import com.example.aiphysical.presentation.auth.routeByRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ComposeAppCommonTest {

    @Test
    fun teacher_age_group_maps_to_teacher_role() {
        assertEquals("teacher", AgeGroup.TEACHER.persistedRole())
    }

    @Test
    fun non_teacher_age_groups_map_to_user_role() {
        AgeGroup.entries
            .filter { it != AgeGroup.TEACHER }
            .forEach { group ->
                assertEquals("user", group.persistedRole())
            }
    }

    @Test
    fun teacher_role_routes_to_teacher_dashboard() {
        val screen = routeByRole(
            uid = "teacher-uid",
            profile = UserProfile(uid = "teacher-uid", fullName = "Teacher", role = "teacher", orgId = "org-1")
        )

        assertIs<AuthScreen.TeacherDashboard>(screen)
        assertEquals("teacher-uid", screen.uid)
        assertEquals("org-1", screen.orgId)
    }

    @Test
    fun user_role_routes_to_student_dashboard() {
        val screen = routeByRole(
            uid = "user-uid",
            profile = UserProfile(uid = "user-uid", fullName = "Student", role = "user", orgId = "org-2")
        )

        assertIs<AuthScreen.StudentDashboard>(screen)
        assertEquals("user-uid", screen.uid)
        assertEquals("org-2", screen.orgId)
    }
}