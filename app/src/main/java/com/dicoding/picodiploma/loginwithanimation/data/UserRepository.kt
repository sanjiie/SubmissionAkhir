package com.dicoding.picodiploma.loginwithanimation.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.dicoding.picodiploma.loginwithanimation.api.ApiService
import com.dicoding.picodiploma.loginwithanimation.data.pref.UserModel
import com.dicoding.picodiploma.loginwithanimation.data.pref.UserPreference
import com.dicoding.picodiploma.loginwithanimation.story.ListStoryItem
import com.dicoding.picodiploma.loginwithanimation.story.StoryPagingSource
import com.dicoding.picodiploma.loginwithanimation.story.StoryResponse
import com.dicoding.picodiploma.loginwithanimation.view.login.LoginResponse
import com.dicoding.picodiploma.loginwithanimation.view.signup.RegisterResponse
import kotlinx.coroutines.flow.Flow

class UserRepository private constructor(
    private val userPreference: UserPreference,
    private val apiService: ApiService
) {
    suspend fun register(name: String, email: String, password: String): RegisterResponse {
        return apiService.register(name, email, password)
    }

    suspend fun login(email: String, password: String): LoginResponse {
        return apiService.login(email, password)
    }

    suspend fun saveSession(user: UserModel) {
        userPreference.saveSession(user)
    }

    fun getSession(): Flow<UserModel> {
        return userPreference.getSession()
    }

    suspend fun logout() {
        userPreference.logout()
    }

    suspend fun getStories(): StoryResponse {
        return apiService.getStories()
    }

    suspend fun getStoriesWithLocation(): StoryResponse {
        return apiService.getStoriesWithLocation()
    }


    companion object {
        fun getInstance(
            apiService: ApiService,
            userPreference: UserPreference
        ): UserRepository {
            return UserRepository(userPreference, apiService)
        }
    }

    fun getStoriesPaging(token: String): Flow<PagingData<ListStoryItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { StoryPagingSource(apiService, token) }
        ).flow
    }
}
