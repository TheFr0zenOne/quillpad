package org.qosp.notes.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to check the version of the database on disk and perform backups if needed.
 */
@Singleton
class DatabaseVersionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DatabaseVersionChecker"
        private const val BACKUP_FOLDER = "database_backups"
    }

    /**
     * Checks if the database exists and returns its version.
     * Returns -1 if the database doesn't exist.
     */
    fun getDatabaseVersion(): Int {
        val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
        if (!dbFile.exists()) {
            Log.d(TAG, "Database file doesn't exist")
            return -1
        }

        return try {
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                val version = db.version
                Log.d(TAG, "Database version: $version")
                version
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking database version", e)
            -1
        }
    }

    /**
     * Checks if the database needs a backup (if the app's database version is newer than the one on disk).
     */
    fun needsDatabaseBackup(): Boolean {
        val diskVersion = getDatabaseVersion()
        if (diskVersion == -1) return false // No database yet, no need for backup

        val appVersion = TARGET_DB_VERSION

        Log.d(TAG, "App database version: $appVersion, Disk database version: $diskVersion")
        return appVersion > diskVersion
    }

    /**
     * Creates a backup of the database.
     * Returns true if the backup was successful, false otherwise.
     */
    fun backupDatabase(): Boolean {
        val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
        if (!dbFile.exists()) {
            Log.d(TAG, "Database file doesn't exist, nothing to backup")
            return false
        }

        val backupDir = File(context.filesDir, BACKUP_FOLDER)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val diskVersion = getDatabaseVersion()
        val appVersion = TARGET_DB_VERSION
        val timestamp = System.currentTimeMillis()
        val backupFile = File(backupDir, "${AppDatabase.DB_NAME}_v${diskVersion}_to_v${appVersion}_$timestamp.db")

        return try {
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Database backup created at ${backupFile.absolutePath}")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error creating database backup", e)
            false
        }
    }

    /**
     * Checks if a database migration is needed, takes a backup if necessary.
     */
    fun checkAndHandleDatabaseMigration() {
        if (needsDatabaseBackup()) {
            Log.d(TAG, "Database migration needed, taking backup")
            val backupSuccess = backupDatabase()
            Log.d(TAG, "Database backup result: $backupSuccess")
        } else {
            Log.d(TAG, "No database migration needed")
        }
    }
}
