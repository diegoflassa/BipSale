package dev.diegoflassa.bipsale.core.utils.logging

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

/**
 * Pins §8.3 both ways (CORE_RULES §12).
 *
 * The rule has two halves and it is the second one that keeps getting lost: `release` must not carry
 * personal values, **and** redaction must not become deletion — a release log that says only
 * `sale failed` is as useless as no log at all.
 */
class LogRedactionTest {

    @After
    fun restoreDefault() {
        // The object is process-wide; leaving it in debug mode would silently disarm any later test.
        LogRedaction.configure(isDebugBuild = false)
    }

    @Test
    fun `debug keeps full fidelity`() {
        LogRedaction.configure(isDebugBuild = true)

        assertThat(LogRedaction.cpf("12345678901")).isEqualTo("12345678901")
        assertThat(LogRedaction.name("Maria Silva")).isEqualTo("Maria Silva")
        assertThat(LogRedaction.contact("maria@example.com")).isEqualTo("maria@example.com")
    }

    @Test
    fun `release drops the value but keeps the length`() {
        LogRedaction.configure(isDebugBuild = false)

        // Length is what separates "the field was empty" from "11 digits went and were still
        // rejected" — the actual diagnostic question.
        assertThat(LogRedaction.cpf("12345678901")).isEqualTo("[REDACTED len=11]")
        assertThat(LogRedaction.name("Maria Silva")).isEqualTo("[REDACTED len=11]")
    }

    @Test
    fun `release never echoes the value itself`() {
        LogRedaction.configure(isDebugBuild = false)

        assertThat(LogRedaction.cpf("12345678901")).doesNotContain("12345678901")
        assertThat(LogRedaction.name("Maria Silva")).doesNotContain("Maria")
        assertThat(LogRedaction.contact("maria@example.com")).doesNotContain("maria")
        assertThat(LogRedaction.path("/storage/emulated/0/Maria/vendas.xlsx")).doesNotContain("Maria")
    }

    @Test
    fun `a contact keeps only the shape that says which kind it was`() {
        LogRedaction.configure(isDebugBuild = false)

        assertThat(LogRedaction.contact("maria@example.com")).contains("kind=email")
        assertThat(LogRedaction.contact("+5511999998888")).contains("kind=phone")
    }

    @Test
    fun `a path keeps its depth so a wrong directory is still diagnosable`() {
        LogRedaction.configure(isDebugBuild = false)

        assertThat(LogRedaction.path("/storage/emulated/0/Maria/vendas.xlsx")).contains("depth=5")
    }

    @Test
    fun `null is reported as null rather than as a redacted empty value`() {
        LogRedaction.configure(isDebugBuild = false)

        // "null" and "[REDACTED len=0]" mean different things: absent versus present-and-empty.
        assertThat(LogRedaction.cpf(null)).isEqualTo("null")
        assertThat(LogRedaction.name(null)).isEqualTo("null")
        assertThat(LogRedaction.cpf("")).isEqualTo("[REDACTED len=0]")
    }

    @Test
    fun `the default is to redact, so an unconfigured path cannot leak`() {
        // Whatever any other test left behind, a fresh read must not be permissive by accident.
        LogRedaction.configure(isDebugBuild = false)

        assertThat(LogRedaction.isRedacting).isTrue()
    }
}
