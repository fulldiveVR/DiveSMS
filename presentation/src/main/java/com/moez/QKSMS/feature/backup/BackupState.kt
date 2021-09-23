
package com.moez.QKSMS.feature.backup

import com.moez.QKSMS.model.BackupFile
import com.moez.QKSMS.repository.BackupRepository

data class BackupState(
    val backupProgress: BackupRepository.Progress = BackupRepository.Progress.Idle(),
    val restoreProgress: BackupRepository.Progress = BackupRepository.Progress.Idle(),
    val lastBackup: String = "",
    val backups: List<BackupFile> = listOf(),
    val upgraded: Boolean = false
)