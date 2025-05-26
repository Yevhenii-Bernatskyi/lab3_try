package com.example.lab3_try

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Track::class, LocationPoint::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun locationPointDao(): LocationPointDao

    companion object {
        @Volatile // Значення цієї змінної ніколи не кешується, всі записи і читання йдуть з/в головну пам'ять
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Якщо INSTANCE не null, повертаємо його.
            // Якщо null, створюємо базу даних у synchronized блоці.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "location_tracker_db" // Назва файлу бази даних
                )
                    // .fallbackToDestructiveMigration() // Для простоти під час розробки, якщо змінюєш схему.
                    // У продакшені потрібно реалізовувати міграції.
                    .build()
                INSTANCE = instance
                // Повертаємо instance
                instance
            }
        }
    }
}