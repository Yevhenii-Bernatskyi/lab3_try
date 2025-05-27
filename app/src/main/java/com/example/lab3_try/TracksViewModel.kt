package com.example.lab3_try

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lab3_try.AppDatabase
import com.example.lab3_try.LocationPoint
import com.example.lab3_try.Track
import kotlinx.coroutines.launch

class TracksViewModel(application: Application) : AndroidViewModel(application) {

    private val trackDao = AppDatabase.getDatabase(application).trackDao()
    private val locationPointDao = AppDatabase.getDatabase(application).locationPointDao()

    val allTracks: LiveData<List<Track>> = trackDao.getAllTracks()


    private val _selectedTrackPoints = MutableLiveData<List<LocationPoint>>()
    val selectedTrackPoints: LiveData<List<LocationPoint>> get() = _selectedTrackPoints


    private val _selectedTrack = MutableLiveData<Track?>()
    val selectedTrack: LiveData<Track?> get() = _selectedTrack

    fun loadTrackPoints(trackId: Long) {
        viewModelScope.launch {
            _selectedTrack.postValue(trackDao.getTrackById(trackId))
            _selectedTrackPoints.postValue(locationPointDao.getPointsForTrack(trackId))
        }
    }

    fun clearSelectedTrack() {
        _selectedTrack.postValue(null)
        _selectedTrackPoints.postValue(emptyList())
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {

            trackDao.deleteTrackById(track.id)
        }
    }
}