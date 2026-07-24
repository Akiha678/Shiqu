package com.seanchen.shiqu

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {
    private lateinit var avatarView: ShapeableImageView
    private lateinit var maleButton: TextView
    private lateinit var femaleButton: TextView
    private lateinit var birthdayView: TextView
    private lateinit var nicknameEditText: EditText

    // 记录用户当前选择的状态，点击“完成”时统一保存。
    private var selectedGender = GENDER_MALE
    private var selectedAvatarUri: Uri? = null
    private var pendingCameraUri: Uri? = null

    private val birthdayCalendar = Calendar.getInstance()
    private val birthdayFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    // 相册选择
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { imageUri ->
        if (imageUri != null) {
            contentResolver.takePersistableUriPermission(
                imageUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedAvatarUri = imageUri
            avatarView.setImageURI(imageUri)
        }
    }

    // 相机启动器
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        if (saved) {
            selectedAvatarUri = pendingCameraUri
            avatarView.setImageURI(pendingCameraUri)
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

    // 获取相机权限
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

        // 绑定XML点击事件
        avatarView = findViewById(R.id.ivAvatar)
        maleButton = findViewById(R.id.btnMale)
        femaleButton = findViewById(R.id.btnFemale)
        birthdayView = findViewById(R.id.tvBirthday)
        nicknameEditText = findViewById(R.id.etNickname)

        loadSavedInformation()
        setupGenderSelector()
        setupBirthdayPicker()
        setupAvatarPicker()
        setupBackButton()
        setupDoneButton()
    }

    // 性别选择
    private fun setupGenderSelector() {
        maleButton.setOnClickListener { selectGender(isMale = true) }
        femaleButton.setOnClickListener { selectGender(isMale = false) }
    }

    private fun selectGender(isMale: Boolean) {
        selectedGender = if (isMale) GENDER_MALE else GENDER_FEMALE
        maleButton.setBackgroundResource(
            if (isMale) {
                R.drawable.bg_gender_selected
            } else {
                R.drawable.bg_gender_unselected
            }
        )

        femaleButton.setBackgroundResource(
            if (isMale) {
                R.drawable.bg_gender_unselected
            } else {
                R.drawable.bg_gender_selected
            }
        )
    }

    // 生日选择
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

    // 头像区域
    private fun setupAvatarPicker() {
        avatarView.setOnClickListener { showAvatarOptions() }
        findViewById<TextView>(R.id.tvChangeAvatar).setOnClickListener { showAvatarOptions() }
    }

    // 头像来源弹窗
    private fun showAvatarOptions() {
        MaterialAlertDialogBuilder(this)
            .setItems(arrayOf("从相册选择", "拍照")) { _, which ->
                when (which) {
                    0 -> requestGalleryPermission()
                    1 -> requestCameraPermission()
                }
            }
            .show()
    }

    // Android 13 及以上使用 READ_MEDIA_IMAGES，低版本使用 READ_EXTERNAL_STORAGE。
    private fun requestGalleryPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    // 拍照前检查 CAMERA 权限，没有权限就触发系统授权弹窗。
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 打开系统文件选择器
    private fun openGallery() {
        galleryLauncher.launch(arrayOf("image/*"))
    }

    private fun openCamera() {
        val imageUri = createCameraImageUri()
        if (imageUri != null) {
            pendingCameraUri = imageUri
            cameraLauncher.launch(imageUri)
        } else {
            Toast.makeText(this, "创建照片文件失败", Toast.LENGTH_SHORT).show()
        }
    }

    // FileProvider
    private fun createCameraImageUri(): Uri? {
        val imageDir = File(cacheDir, "images").apply { mkdirs() }
        val imageFile = File.createTempFile("avatar_", ".jpg", imageDir)
        return FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            imageFile
        )
    }

    // 返回按钮
    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    // 完成按钮
    private fun setupDoneButton() {
        findViewById<TextView>(R.id.btnDone).setOnClickListener {
            saveInformation()
        }
    }

    // 保存前先校验昵称
    private fun saveInformation() {
        val nickname = nicknameEditText.text.toString().trim()
        if (nickname.isEmpty()) {
            Toast.makeText(this, "请输入昵称", Toast.LENGTH_SHORT).show()
            return
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit {
                putString(KEY_NICKNAME, nickname)
                    .putString(KEY_GENDER, selectedGender)
                    .putString(KEY_BIRTHDAY, birthdayView.text.toString())
                    .putString(KEY_AVATAR_URI, selectedAvatarUri?.toString())
            }

        Toast.makeText(this, "信息已保存", Toast.LENGTH_SHORT).show()
    }

    // 本地化存储
    private fun loadSavedInformation() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        nicknameEditText.setText(prefs.getString(KEY_NICKNAME, nicknameEditText.text.toString()))
        birthdayView.text = prefs.getString(KEY_BIRTHDAY, birthdayView.text.toString())

        // 同步 Calendar 的时间，确保下一次打开日期选择器时默认日期正确。
        runCatching {
            birthdayCalendar.time = birthdayFormatter.parse(birthdayView.text.toString()) ?: birthdayCalendar.time
        }

        val savedAvatarUri = prefs.getString(KEY_AVATAR_URI, null)
        if (!savedAvatarUri.isNullOrEmpty()) {
            selectedAvatarUri = Uri.parse(savedAvatarUri)
            avatarView.setImageURI(selectedAvatarUri)
        }

        selectGender(prefs.getString(KEY_GENDER, GENDER_MALE) == GENDER_MALE)
    }

    companion object {
        private const val PREFS_NAME = "edit_information"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_GENDER = "gender"
        private const val KEY_BIRTHDAY = "birthday"
        private const val KEY_AVATAR_URI = "avatar_uri"
        private const val GENDER_MALE = "male"
        private const val GENDER_FEMALE = "female"
    }
}
