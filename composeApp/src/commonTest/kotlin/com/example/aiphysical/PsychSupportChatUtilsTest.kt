package com.example.aiphysical

import com.example.aiphysical.data.model.buildPsychChatId
import com.example.aiphysical.data.model.isAllowedPsychChatPair
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PsychSupportChatUtilsTest {

    @Test
    fun build_psych_chat_id_is_deterministic() {
        val first = buildPsychChatId("org-1", "user-10", "psych-22")
        val second = buildPsychChatId("org-1", "user-10", "psych-22")

        assertEquals(first, second)
    }

    @Test
    fun build_psych_chat_id_does_not_depend_on_participant_order() {
        val first = buildPsychChatId("org-1", "user-10", "psych-22")
        val second = buildPsychChatId("org-1", "psych-22", "user-10")

        assertEquals(first, second)
    }

    @Test
    fun psychologist_can_chat_with_user_and_teacher() {
        assertTrue(isAllowedPsychChatPair("psychologist", "user"))
        assertTrue(isAllowedPsychChatPair("teacher", "psychologist"))
    }

    @Test
    fun unsupported_role_pairs_are_rejected() {
        assertFalse(isAllowedPsychChatPair("user", "teacher"))
        assertFalse(isAllowedPsychChatPair("psychologist", "psychologist"))
        assertFalse(isAllowedPsychChatPair("director", "psychologist"))
    }
}

