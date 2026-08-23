package dev.diegoflassa.bipsale.core.ui.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UiTextTest {

    private val someResId = 0x7f0f0001
    private val otherResId = 0x7f0f0002

    @Test
    fun `two string resources built from the same id are equal`() {
        // A UiText lives inside UI state. When equality fell back to array identity, every state
        // copy holding one compared unequal and the screen recomposed on every emission.
        assertThat(UiText.StringResource(someResId))
            .isEqualTo(UiText.StringResource(someResId))
    }

    @Test
    fun `equal string resources hash alike`() {
        assertThat(UiText.StringResource(someResId).hashCode())
            .isEqualTo(UiText.StringResource(someResId).hashCode())
    }

    @Test
    fun `arguments participate in equality`() {
        assertThat(UiText.StringResource(someResId, "Ana"))
            .isEqualTo(UiText.StringResource(someResId, "Ana"))
        assertThat(UiText.StringResource(someResId, "Ana"))
            .isNotEqualTo(UiText.StringResource(someResId, "Bruno"))
    }

    @Test
    fun `a different id is a different text`() {
        assertThat(UiText.StringResource(someResId))
            .isNotEqualTo(UiText.StringResource(otherResId))
    }

    @Test
    fun `an argument count difference is a difference`() {
        assertThat(UiText.StringResource(someResId))
            .isNotEqualTo(UiText.StringResource(someResId, "Ana"))
    }

    @Test
    fun `a dynamic string is never equal to a string resource`() {
        assertThat(UiText.DynamicString("Ana")).isNotEqualTo(UiText.StringResource(someResId))
    }

    @Test
    fun `dynamic strings compare by value`() {
        assertThat(UiText.DynamicString("Ana")).isEqualTo(UiText.DynamicString("Ana"))
        assertThat(UiText.DynamicString("Ana")).isNotEqualTo(UiText.DynamicString("Bruno"))
    }

    @Test
    fun `toString reports the id and the arguments`() {
        val text = UiText.StringResource(someResId, "Ana", 2).toString()

        assertThat(text).contains(someResId.toString())
        assertThat(text).contains("Ana")
    }
}
