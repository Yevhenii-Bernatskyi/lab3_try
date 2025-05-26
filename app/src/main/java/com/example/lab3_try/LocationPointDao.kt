package com.example.lab3_try

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocationPointDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Можна IGNORE, якщо точка з таким часом для треку вже є
    suspend fun insertPoint(point: LocationPoint)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllPoints(points: List<LocationPoint>)

    @Query("SELECT * FROM location_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getPointsForTrack(trackId: Long): List<LocationPoint>

    // Ця версія з LiveData може бути корисною, якщо ти хочеш,
    // щоб карта оновлювала точки поточного треку в реальному часі,
    // отримуючи їх прямо з БД. Але для простоти можна почати без неї.
    @Query("SELECT * FROM location_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getPointsForTrackLiveData(trackId: Long): LiveData<List<LocationPoint>>
}