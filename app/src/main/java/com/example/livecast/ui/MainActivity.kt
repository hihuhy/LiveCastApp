package com.example.livecast.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.livecast.databinding.ActivityMainBinding
import com.example.livecast.model.StreamProfile
import com.example.livecast.service.StreamService
import com.google.android.material.chip.Chip

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var selectedVideoUri: Uri? = null
    private var isLive = false

    private val pickVideoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onVideoPicked(it) }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNeededPermissions()
        setupPlatformChips()

        binding.btnPickVideo.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }

        binding.btnStartStop.setOnClickListener {
            if (!isLive) startGoLive() else stopGoLive()
        }
    }

    private fun requestNeededPermissions() {
        val perms = mutableListOf(Manifest.permission.INTERNET)
        perms += if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS

        val notGranted = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    /** Hiển thị 4 lựa chọn nhanh: Facebook / Youtube / Twitch / Tùy chỉnh, tự điền server RTMP mặc định */
    private fun setupPlatformChips() {
        val platforms = StreamProfile.Platform.entries
        platforms.forEach { platform ->
            val chip = Chip(this).apply {
                text = platform.label
                isCheckable = true
                setOnClickListener {
                    binding.etRtmpServer.setText(platform.defaultServer)
                }
            }
            binding.chipGroupPlatform.addView(chip)
        }
    }

    private fun onVideoPicked(uri: Uri) {
        // Giữ quyền truy cập lâu dài vào file này (content:// Uri) để dùng lại được
        // ngay cả sau khi app bị hệ thống thu hồi bộ nhớ / khởi động lại Service.
        try {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Một số nguồn (vd Google Photos) không hỗ trợ persistable permission, bỏ qua
        }
        selectedVideoUri = uri
        binding.tvSelectedVideo.text = queryFileName(uri)
    }

    /** Lấy tên file để hiển thị cho người dùng */
    private fun queryFileName(uri: Uri): String {
        var name = "video_da_chon"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
        }
        return name
    }

    private fun startGoLive() {
        val server = binding.etRtmpServer.text.toString().trim()
        val key = binding.etStreamKey.text.toString().trim()
        val videoUri = selectedVideoUri

        if (videoUri == null) {
            binding.tvStatus.text = "Vui lòng chọn video trước"
            return
        }
        if (server.isBlank()) {
            binding.tvStatus.text = "Vui lòng nhập địa chỉ RTMP server"
            return
        }

        val fullUrl = if (key.isBlank()) server else "${server.trimEnd('/')}/$key"

        val intent = Intent(this, StreamService::class.java).apply {
            putExtra(StreamService.EXTRA_VIDEO_URI, videoUri)
            putExtra(StreamService.EXTRA_RTMP_URL, fullUrl)
            putExtra(StreamService.EXTRA_LOOP, binding.switchLoop.isChecked)
        }
        ContextCompat.startForegroundService(this, intent)

        isLive = true
        binding.btnStartStop.text = "Dừng phát trực tiếp"
        binding.tvStatus.text = "Đang bắt đầu phát trực tiếp..."
    }

    private fun stopGoLive() {
        val intent = Intent(this, StreamService::class.java).apply {
            action = StreamService.ACTION_STOP
        }
        startService(intent)

        isLive = false
        binding.btnStartStop.text = "Bắt đầu phát trực tiếp"
        binding.tvStatus.text = "Đã dừng"
    }
}
