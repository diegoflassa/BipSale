package dev.diegoflassa.bipsale.ui.backup

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.diegoflassa.bipsale.core.domain.backup.BackupMetadata
import dev.diegoflassa.bipsale.core.domain.backup.BackupRepository
import dev.diegoflassa.bipsale.core.domain.backup.BackupSummary
import dev.diegoflassa.bipsale.core.domain.backup.StagedBackup
import dev.diegoflassa.bipsale.core.domain.usecase.CreateBackupUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.InspectBackupUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.RestoreBackupUseCase
import dev.diegoflassa.bipsale.core.domain.usecase.ShareBackupUseCase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class BackupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val summary = BackupSummary(
        products = 12,
        sales = 34,
        saleItems = 56,
        images = 7,
        createdAt = 1_700_000_000_000
    )

    private val metadata = BackupMetadata(
        products = 12,
        sales = 34,
        saleItems = 56,
        images = 7,
        createdAt = 1_700_000_000_000,
        appVersionName = "0.0.2-alpha",
        formatVersion = 1,
        schemaVersion = 2
    )

    private class FakeBackupRepository(
        private val failWith: Throwable? = null
    ) : BackupRepository {
        var restoredFrom: String? = null
        var createdAt: String? = null
        var restoreCount = 0

        var summary = BackupSummary(0, 0, 0, 0, 0)
        var metadata: BackupMetadata? = null

        override suspend fun createBackup(destinationUri: String): BackupSummary {
            failWith?.let { throw it }
            createdAt = destinationUri
            return summary
        }

        override suspend fun inspectBackup(sourceUri: String): BackupMetadata {
            failWith?.let { throw it }
            return metadata ?: error("no metadata configured")
        }

        override suspend fun restoreBackup(sourceUri: String): BackupSummary {
            failWith?.let { throw it }
            restoredFrom = sourceUri
            restoreCount++
            return summary
        }

        override suspend fun stageBackupForSharing(): StagedBackup {
            failWith?.let { throw it }
            return StagedBackup("content://staged/backup.zip", "bipsale-backup.zip", summary)
        }

        override fun suggestedFileName(): String = "bipsale-backup-20260822-101500.zip"
    }

    private fun viewModel(repository: FakeBackupRepository) = BackupViewModel(
        createBackup = CreateBackupUseCase(repository),
        inspectBackup = InspectBackupUseCase(repository),
        restoreBackup = RestoreBackupUseCase(repository),
        shareBackup = ShareBackupUseCase(repository),
        backupRepository = repository
    )

    @Test
    fun `creating a backup reports what it wrote and stops the spinner`() = runTest {
        val repository = FakeBackupRepository().apply { summary = this@BackupViewModelTest.summary }
        val vm = viewModel(repository)

        vm.effect.test {
            vm.onIntent(BackupContract.Intent.CreateBackup("content://out/backup.zip"))
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(BackupContract.Effect.ShowSnackbar::class.java)
        }
        assertThat(repository.createdAt).isEqualTo("content://out/backup.zip")
        assertThat(vm.uiState.value.isBusy).isFalse()
        assertThat(vm.uiState.value.lastSummary).isEqualTo(summary)
    }

    @Test
    fun `a failed write reports it and stops the spinner`() = runTest {
        val vm = viewModel(FakeBackupRepository(IllegalStateException("no space left")))

        vm.effect.test {
            vm.onIntent(BackupContract.Intent.CreateBackup("content://out/backup.zip"))
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(BackupContract.Effect.ShowSnackbar::class.java)
        }
        assertThat(vm.uiState.value.isBusy).isFalse()
    }

    @Test
    fun `sharing hands the staged file out`() = runTest {
        val vm = viewModel(FakeBackupRepository())

        vm.effect.test {
            vm.onIntent(BackupContract.Intent.ShareBackup)
            advanceUntilIdle()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(BackupContract.Effect.ShareFile::class.java)
            assertThat((effect as BackupContract.Effect.ShareFile).fileName)
                .isEqualTo("bipsale-backup.zip")
        }
    }

    @Test
    fun `choosing a file asks for confirmation instead of restoring straight away`() = runTest {
        // Restore replaces every product, sale and image; it must never happen on file choice alone.
        val repository = FakeBackupRepository().apply { metadata = this@BackupViewModelTest.metadata }
        val vm = viewModel(repository)

        vm.onIntent(BackupContract.Intent.RestoreRequested("content://in/backup.zip"))
        advanceUntilIdle()

        assertThat(vm.uiState.value.pendingRestore?.sourceUri).isEqualTo("content://in/backup.zip")
        assertThat(vm.uiState.value.pendingRestore?.metadata).isEqualTo(metadata)
        assertThat(repository.restoreCount).isEqualTo(0)
    }

    @Test
    fun `confirming restores from the file that was inspected`() = runTest {
        val repository = FakeBackupRepository().apply {
            metadata = this@BackupViewModelTest.metadata
            summary = this@BackupViewModelTest.summary
        }
        val vm = viewModel(repository)
        vm.onIntent(BackupContract.Intent.RestoreRequested("content://in/backup.zip"))
        advanceUntilIdle()

        vm.onIntent(BackupContract.Intent.RestoreConfirmed)
        advanceUntilIdle()

        assertThat(repository.restoredFrom).isEqualTo("content://in/backup.zip")
        assertThat(vm.uiState.value.pendingRestore).isNull()
        assertThat(vm.uiState.value.lastSummary).isEqualTo(summary)
    }

    @Test
    fun `cancelling drops the pending restore and touches nothing`() = runTest {
        val repository = FakeBackupRepository().apply { metadata = this@BackupViewModelTest.metadata }
        val vm = viewModel(repository)
        vm.onIntent(BackupContract.Intent.RestoreRequested("content://in/backup.zip"))
        advanceUntilIdle()

        vm.onIntent(BackupContract.Intent.RestoreCancelled)
        advanceUntilIdle()

        assertThat(vm.uiState.value.pendingRestore).isNull()
        assertThat(repository.restoreCount).isEqualTo(0)
    }

    @Test
    fun `confirming with nothing chosen restores nothing`() = runTest {
        val repository = FakeBackupRepository()
        val vm = viewModel(repository)

        vm.onIntent(BackupContract.Intent.RestoreConfirmed)
        advanceUntilIdle()

        assertThat(repository.restoreCount).isEqualTo(0)
    }

    @Test
    fun `an unreadable archive is refused at inspection, before anything is replaced`() = runTest {
        val repository = FakeBackupRepository(IllegalArgumentException("not a BipSale backup"))
        val vm = viewModel(repository)

        vm.effect.test {
            vm.onIntent(BackupContract.Intent.RestoreRequested("content://in/photo.jpg"))
            advanceUntilIdle()

            assertThat(awaitItem()).isInstanceOf(BackupContract.Effect.ShowSnackbar::class.java)
        }
        assertThat(vm.uiState.value.pendingRestore).isNull()
        assertThat(repository.restoreCount).isEqualTo(0)
    }

    @Test
    fun `the suggested file name comes from the repository`() = runTest {
        val vm = viewModel(FakeBackupRepository())

        assertThat(vm.suggestedFileName()).isEqualTo("bipsale-backup-20260822-101500.zip")
    }
}
