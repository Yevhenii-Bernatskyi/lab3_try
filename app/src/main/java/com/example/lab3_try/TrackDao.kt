package com.example.lab3_try

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track): Long // Повертає id вставленого треку

    @Query("UPDATE tracks SET endTime = :endTime WHERE id = :trackId")
    suspend fun updateTrackEndTime(trackId: Long, endTime: Long)

    @Query("SELECT * FROM tracks ORDER BY startTime DESC")
    fun getAllTracks(): LiveData<List<Track>> // LiveData для автоматичного оновлення UI

    @Query("SELECT * FROM tracks WHERE id = :trackId")
    suspend fun getTrackById(trackId: Long): Track?

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: Long)

    @Query("SELECT * FROM tracks ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastTrack(): Track? // Може бути корисним для отримання поточного треку
}