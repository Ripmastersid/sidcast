package com.sidcast.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var isStreaming = false
    private lateinit var tvStatus: TextView
    private lateinit var btnStartStop: Button

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenService.resultCode = result.resultCode
            ScreenService.resultData = result.data
            val serviceIntent = Intent(this, ScreenService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
            isStreaming = true
            btnStartStop.text = "PARAR LIVE"
            tvStatus.text = "Status: Transmitindo ao vivo..."
        } else {
            Toast.makeText(this, "Permissão de tela negada", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickGifLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            ScreenService().setGifOverlay(this, it)
            Toast.makeText(this, "GIF carregado no overlay!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etRtmpUrl = findViewById<EditText>(R.id.etRtmpUrl)
        val etStreamKey = findViewById<EditText>(R.id.etStreamKey)
        val btnSelectGif = findViewById<Button>(R.id.btnSelectGif)
        val btnToggleGif = findViewById<Button>(R.id.btnToggleGif)
        btnStartStop = findViewById(R.id.btnStartStop)
        tvStatus = findViewById(R.id.tvStatus)

        requestPermissions()

        btnSelectGif.setOnClickListener {
            pickGifLauncher.launch("image/gif")
        }

        btnToggleGif.setOnClickListener {
            ScreenService().toggleGif()
            tvStatus.text = if (ScreenService.isGifVisible) "Status: GIF visível" else "Status: GIF oculto"
        }

        btnStartStop.setOnClickListener {
            if (!isStreaming) {
                val url = etRtmpUrl.text.toString().trim()
                val key = etStreamKey.text.toString().trim()
                if (url.isEmpty() || key.isEmpty()) {
                    Toast.makeText(this, "Preencha a URL e a Chave RTMP", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                ScreenService.endpointUrl = if (url.endsWith("/")) "$url$key" else "$url/$key"
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
            } else {
                stopService(Intent(this, ScreenService::class.java))
                isStreaming = false
                btnStartStop.text = "INICIAR LIVE"
                tvStatus.text = "Status: Transmissão encerrada"
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
        }
    }
}
