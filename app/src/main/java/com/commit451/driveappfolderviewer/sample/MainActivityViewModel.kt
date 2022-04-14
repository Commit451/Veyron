package com.commit451.driveappfolderviewer.sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.commit451.veyron.Veyron
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivityViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        const val PATH_FAVORITE_DOGS = "favorite-dogs"
    }

    private var _uiState = MutableLiveData(
        MainActivityState()
    )

    val uiState: LiveData<MainActivityState> = _uiState

    fun loadFavorites(veyron: Veyron) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = veyron.files(PATH_FAVORITE_DOGS)
                val dogs = files.map { file ->
                    val dog = Dog()
                    dog.name = file.name
                    //dog.created = Date(file.createdTime.value)
                    dog
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value?.copy(favorites = dogs)
                }
            } catch (e: Exception) {
                error(e)
            }
        }
    }
}