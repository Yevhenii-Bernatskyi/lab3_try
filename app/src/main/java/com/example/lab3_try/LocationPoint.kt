package com.example.lab3_try

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_points",
    foreignKeys = [ForeignKey(
        entity = Track::class,
        parentColumns = ["id"],
        childColumns = ["trackId"],
        onDelete = ForeignKey.CASCADE // Якщо трек видаляється, видаляються і всі його точки
    )],
    indices = [Index(value = ["trackId"])] // Індекс для швидшого пошуку точок за trackId
)
data class LocationPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: Long, // Зовнішній ключ до таблиці tracks
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long // Час отримання цієї точки у мілісекундах
)
