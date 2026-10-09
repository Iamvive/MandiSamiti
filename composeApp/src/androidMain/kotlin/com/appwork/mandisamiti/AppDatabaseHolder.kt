package com.appwork.mandisamiti

import android.content.Context
import com.appwork.mandisamiti.database.AppDatabase
import com.appwork.mandisamiti.database.DriverFactory
import com.appwork.mandisamiti.database.createDatabase

/** One database per process: the UI and the sync worker share it, so pulled rows reach open screens and no connections leak. */
object AppDatabaseHolder {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: createDatabase(DriverFactory(context.applicationContext)).also { instance = it }
        }
}
