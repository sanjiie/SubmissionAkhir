package com.dicoding.picodiploma.loginwithanimation.view.maps

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.dicoding.picodiploma.loginwithanimation.data.UserRepository
import com.dicoding.picodiploma.loginwithanimation.story.StoryResponse
import retrofit2.HttpException
import java.io.IOException

class LocationViewModel(private val repository: UserRepository) : ViewModel() {

    fun getStoriesWithLocation(): LiveData<StoryResponse?> = liveData {
        try {
            val response = repository.getStoriesWithLocation()
            emit(response)
        } catch (e: HttpException) {
            emit(null)
        } catch (e: IOException) {
            emit(null)
        }
    }
}
