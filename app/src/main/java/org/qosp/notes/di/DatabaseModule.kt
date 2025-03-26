package org.qosp.notes.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.qosp.notes.data.AppDatabase
import org.qosp.notes.data.DatabaseVersionChecker
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private var databaseInstance: AppDatabase? = null

    @Provides
    @Singleton
    fun provideRoomDatabase(
        @ApplicationContext context: Context,
        databaseVersionChecker: DatabaseVersionChecker
    ): AppDatabase {
        // Return existing instance if available
        databaseInstance?.let { return it }

        // Check if we need to backup the database before applying migrations
        // or restore from a backup if the disk version is higher than the app version
        val migrationResult = databaseVersionChecker.checkAndHandleDatabaseMigration()
        if (!migrationResult) {
            // If migration/restoration failed, log a warning
            Log.w("DatabaseModule", "Database migration or restoration failed")
        }

        // Create a new instance
        val builder = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DB_NAME)
        // we don't want to silently wipe user data in case DB migration fails,
        // rather let the app crash

        // Always add migrations
        builder.addMigrations(AppDatabase.MIGRATION_1_2)
        builder.addMigrations(AppDatabase.MIGRATION_2_3)

        return builder.build().also { databaseInstance = it }
    }
}
