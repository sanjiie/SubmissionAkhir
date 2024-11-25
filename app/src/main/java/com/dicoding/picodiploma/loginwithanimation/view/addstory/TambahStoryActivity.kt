package com.dicoding.picodiploma.loginwithanimation.view.addstory

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.dicoding.picodiploma.loginwithanimation.api.ApiConfig
import com.dicoding.picodiploma.loginwithanimation.api.reduceFileImage
import com.dicoding.picodiploma.loginwithanimation.api.uriToFile
import com.dicoding.picodiploma.loginwithanimation.data.pref.UserPreference
import com.dicoding.picodiploma.loginwithanimation.data.pref.dataStore
import com.dicoding.picodiploma.loginwithanimation.databinding.ActivityTambahStoryBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class TambahStoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahStoryBinding
    private lateinit var userPreference: UserPreference
    private var photoFile: File? = null
    private var selectedImageUri: Uri? = null

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Izin kamera diperlukan untuk menggunakan fitur ini.", Toast.LENGTH_SHORT).show()
            }
        }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                photoFile?.let {
                    val photoUri = Uri.fromFile(it)
                    binding.ivPreview.setImageURI(photoUri)
                    selectedImageUri = photoUri
                    Log.d("TambahStoryActivity", "Photo taken and selectedImageUri set: $selectedImageUri")
                } ?: run {
                    Log.e("TambahStoryActivity", "Photo file is null after taking photo.")
                }
            } else {
                Log.e("TambahStoryActivity", "Result not OK, failed to take photo.")
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                binding.ivPreview.setImageURI(selectedImageUri)
                Log.d("TambahStoryActivity", "Image picked from gallery: $selectedImageUri")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userPreference = UserPreference.getInstance(dataStore)

        binding.ivBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnTakePhoto.setOnClickListener {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        binding.btnSubmit.setOnClickListener {
            val description = binding.etDescription.text.toString()
            if (description.isNotEmpty() && selectedImageUri != null) {
                runBlocking {
                    val userModel = userPreference.getSession().first() // Mengambil token secara sinkron
                    val token = userModel.token
                    Log.d("TambahStoryActivity", "Uploading image with description: $description, token: $token")
                    uploadImage(description, token)
                }
            } else {
                Toast.makeText(this, "Isi deskripsi dan pilih gambar!", Toast.LENGTH_SHORT).show()
                Log.e("TambahStoryActivity", "Description or selectedImageUri is null.")
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            try {
                photoFile = createImageFile()
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.dicoding.picodiploma.loginwithanimation.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePhotoLauncher.launch(takePictureIntent)
                    Log.d("TambahStoryActivity", "Camera opened with URI: $photoURI")
                }
            } catch (ex: IOException) {
                Toast.makeText(this, "Gagal membuat file gambar.", Toast.LENGTH_SHORT).show()
                Log.e("TambahStoryActivity", "IOException when creating file: ${ex.message}")
            }
        } else {
            Toast.makeText(this, "Kamera tidak tersedia.", Toast.LENGTH_SHORT).show()
            Log.e("TambahStoryActivity", "Camera is not available.")
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = externalCacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            photoFile = this
            Log.d("TambahStoryActivity", "Temporary image file created: $absolutePath")
        }
    }

    private fun uploadImage(description: String, token: String) {
        selectedImageUri?.let { uri ->
            Log.d("TambahStoryActivity", "Setting progress bar visible")
            binding.progressBar.visibility = View.VISIBLE // ProgressBar visible
            var imageFile = uriToFile(uri, this)
            imageFile = imageFile.reduceFileImage()

            val requestBody = description.toRequestBody("text/plain".toMediaType())
            val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaType())
            val multipartBody = MultipartBody.Part.createFormData(
                "photo",
                imageFile.name,
                requestImageFile
            )

            lifecycleScope.launch {
                try {
                    val apiService = ApiConfig.getApiService(token)
                    val successResponse = apiService.uploadImage(multipartBody, requestBody)
                    delay(2000) // Simulasi delay untuk debugging
                    binding.progressBar.visibility = View.GONE // ProgressBar gone
                    Log.d("TambahStoryActivity", "Progress bar set to GONE after success")
                    Toast.makeText(this@TambahStoryActivity, successResponse.message, Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    Log.e("TambahStoryActivity", "Error uploading image: ${e.message}")
                    Toast.makeText(this@TambahStoryActivity, "Gagal mengupload gambar", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            Log.e("TambahStoryActivity", "No image selected")
            Toast.makeText(this, "Pilih gambar terlebih dahulu", Toast.LENGTH_SHORT).show()
        }
    }
}
