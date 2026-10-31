package com.exemplo.tradutortela

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager

    // Launcher para o diálogo nativo do Android de captura de tela
    private val capturePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Inicia o ForegroundService com os dados de projeção de tela
            val serviceIntent = Intent(this, FloatingService::class.java).apply {
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("DATA_INTENT", result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "Bolinha ativada! Abra seu jogo.", Toast.LENGTH_SHORT).show()
            finish() // Minimiza para o usuário ir direto ao jogo
        } else {
            Toast.makeText(this, "Permissão de gravação de tela recusada.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val btnAtivar = findViewById<Button>(R.id.btnAtivar)
        btnAtivar.setOnClickListener {
            solicitarPermissoes()
        }
    }

    private fun solicitarPermissoes() {
        // 1. Verifica se já tem permissão para desenhar por cima de outros apps
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Por favor, autorize 'Sobrepor a outros apps'", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        // 2. Solicita autorização de captura de tela nativa (MediaProjection)
        capturePermissionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}
