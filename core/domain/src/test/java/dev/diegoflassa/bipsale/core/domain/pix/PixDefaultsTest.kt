package dev.diegoflassa.bipsale.core.domain.pix

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.UUID

class PixDefaultsTest {

    @Test
    fun `the fixed PIX key is a valid UUID`() {
        assertThat(runCatching { UUID.fromString(PixDefaults.KEY) }.isSuccess).isTrue()
    }
}
