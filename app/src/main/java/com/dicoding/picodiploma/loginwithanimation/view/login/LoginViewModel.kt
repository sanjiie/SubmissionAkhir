package com.dicoding.picodiploma.loginwithanimation.view.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.picodiploma.loginwithanimation.data.UserRepository
import com.dicoding.picodiploma.loginwithanimation.api.ErrorResponse
import com.dicoding.picodiploma.loginwithanimation.data.pref.UserModel
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginViewModel(private val repository: UserRepository) : ViewModel() {

    fun saveSession(user: UserModel) {
        viewModelScope.launch {
            try {
                repository.saveSession(user)
                Log.d("LoginViewModel", "Session saved: ${user.email}")
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Failed to save session: ${e.message}")
            }
        }
    }

    fun login(email: String, password: String, onSuccess: (UserModel) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.login(email, password)
                if (response.error == false && response.loginResult != null) {
                    val user = UserModel(response.loginResult.name!!, response.loginResult.token!!, true)
                    saveSession(user)
                    onSuccess(user)
                } else {
                    onError(response.message ?: "Login gagal")
                }
            } catch (e: HttpException) {
                val errorMessage = try {
                    val jsonInString = e.response()?.errorBody()?.string()
                    val errorBody = Gson().fromJson(jsonInString, ErrorResponse::class.java)
                    errorBody.message
                } catch (jsonException: Exception) {
                    "Login gagal"
                }
                onError(errorMessage)
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan pada login")
            }
        }
    }
}
