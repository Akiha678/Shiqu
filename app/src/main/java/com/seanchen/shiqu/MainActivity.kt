package com.seanchen.shiqu

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var avatarView: ShapeableImageView
    private lateinit var maleButton: TextView
    private lateinit var femaleButton: TextView
    private lateinit var birthdayView: TextView

    private val birthdayCalendar = Calendar.getInstance()
    private val birthdayFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    // 相册启动器
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val imageUri = result.data?.data
        if (result.resultCode == RESULT_OK && imageUri != null) {
            avatarView.setImageURI(imageUri)
        }
    }

    // 相机启动器
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK){
            @Suppress("DEPRECATION")
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                avatarView.setImageBitmap(bitmap)
            }
        }
    }

    // 获取相册权限
    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openGallery()
        } else {
            Toast.makeText(this, "需要相册权限才能选择头像", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openCamera()
        } else {
            Toast.makeText(this, "需要相机权限才能开启相机", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_information)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editInformationRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        avatarView = findViewById(R.id.ivAvatar)
        maleButton = findViewById(R.id.btnMale)
        femaleButton = findViewById(R.id.btnFemale)
        birthdayView = findViewById(R.id.tvBirthday)
    }

    private fun setupGenderSelector(){
        maleButton.setOnClickListener { selectGender(isMale = true) }
        femaleButton.setOnClickListener { selectGender(isMale = false) }
        selectGender(isMale = true)
    }

    private fun selectGender(isMale: Boolean) {
        maleButton.setBackgroundResource(
            if (isMale){
                R.drawable.bg_gender_selected
            } else {
                R.drawable.bg_gender_unselected
            }
        )

        femaleButton.setBackgroundResource(
            if (isMale){
                R.drawable.bg_gender_unselected
            } else {
                R.drawable.bg_gender_selected
            }
        )
    }

    private fun setupBirthdayPicker() {
        birthdayView.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    birthdayCalendar.set(year, month, dayOfMonth)
                    birthdayView.text = birthdayFormatter.format(birthdayCalendar.time)
                },
                birthdayCalendar.get(Calendar.YEAR),
                birthdayCalendar.get(Calendar.MONTH),
                birthdayCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupAvatarPicker() {
        avatarView.setOnClickListener { showAvatarOptions() }
        findViewById<TextView>(R.id.tvChangeAvatar).setOnClickListener { showAvatarOptions() }
    }

    private fun showAvatarOptions(){
        MaterialAlertDialogBuilder(this)
            .setItems(arrayOf("从相册选择", "拍照")){ _, which ->
                when(which) {
                    0 -> requestGalleryPermission()
                    1 -> requestCameraPermission()
                }
            }
            .show()
    }

    private fun requestGalleryPermission(){
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            galleryPermissionLauncher.launch(permission)
        }
    }


    private fun requestCameraPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openGallery(){
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    // 开启相机
    @SuppressLint("QueryPermissionsNeeded")
    private fun openCamera(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null){
            cameraLauncher.launch(intent)
        } else {
            Toast.makeText(this, "未找到可用相机", Toast.LENGTH_SHORT).show()
        }
    }
}