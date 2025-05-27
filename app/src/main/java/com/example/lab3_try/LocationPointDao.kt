package com.example.lab3_try

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocationPointDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPoint(point: LocationPoint)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllPoints(points: List<LocationPoint>)

    @Query("SELECT * FROM location_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getPointsForTrack(trackId: Long): List<LocationPoint>

    @Query("SELECT * FROM location_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getPointsForTrackLiveData(trackId: Long): LiveData<List<LocationPoint>>
}