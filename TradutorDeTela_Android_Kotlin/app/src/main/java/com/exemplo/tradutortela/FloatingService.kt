package com.exemplo.tradutortela

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingBubbleView: View
    private var popupView: View? = null
    
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    // Google ML Kit: Reconhecedor de Texto (OCR On-Device)
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Google ML Kit: Tradutor Offline (Inglês -> Português do Brasil)
    private lateinit var translator: Translator
    private var isModelDownloaded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        criarCanalNotificacao()
        inicializarTradutorMLKit()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val dataIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("DATA_INTENT", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("DATA_INTENT")
        }

        if (resultCode == Activity.RESULT_OK && dataIntent != null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, dataIntent)
            configurarBolinhaFlutuante()
        }
        return START_NOT_STICKY
    }

    private fun inicializarTradutorMLKit() {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.PORTUGUESE)
            .build()
        translator = Translation.getClient(options)

        // Baixa o dicionário offline silenciosamente na primeira inicialização
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                isModelDownloaded = true
            }
            .addOnFailureListener {
                isModelDownloaded = false
            }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configurarBolinhaFlutuante() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingBubbleView = inflater.inflate(R.layout.layout_bolinha, null)

        // Lógica de arrastar a bolinha e detectar clique simples
        floatingBubbleView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = true

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (Math.hypot(dx.toDouble(), dy.toDouble()) > 15) {
                            isClick = false
                        }
                        params.x = (initialX + dx).toInt()
                        params.y = (initialY + dy).toInt()
                        windowManager.updateViewLayout(floatingBubbleView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            darFeedbackTatil()
                            capturarTelaETraduzir()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingBubbleView, params)
    }

    private fun darFeedbackTatil() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(40)
        }
    }

    private fun capturarTelaETraduzir() {
        mostrarPopup("Escaneando a tela...")

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "TradutorTelaCapture",
            width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                // Libera os recursos de captura de tela imediatamente
                virtualDisplay?.release()
                virtualDisplay = null

                // Processa o bitmap com OCR do ML Kit
                processarOCR(bitmap)
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun processarOCR(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                if (fullText.isBlank()) {
                    atualizarPopup("Nenhum texto detectado na tela atual.")
                    return@addOnSuccessListener
                }

                // Identifica se é fala normal ou balão de pensamento
                val formatado = classificarFalasEPensamentos(visionText)
                traduzirTexto(formatado)
            }
            .addOnFailureListener {
                atualizarPopup("Erro ao ler letras da tela: ${it.localizedMessage}")
            }
    }

    private fun classificarFalasEPensamentos(visionText: com.google.mlkit.vision.text.Text): String {
        val sb = StringBuilder()
        for (block in visionText.textBlocks) {
            val text = block.text.trim()
            if (text.startsWith("(") && text.endsWith(")")) {
                sb.append("💭 [PENSAMENTO]: ").append(text).append("

")
            } else {
                sb.append("💬 [FALA]: ").append(text).append("

")
            }
        }
        return sb.toString().trim()
    }

    private fun traduzirTexto(textoOriginal: String) {
        atualizarPopup("Traduzindo texto identificado...")
        translator.translate(textoOriginal)
            .addOnSuccessListener { textoTraduzido ->
                atualizarPopup(textoTraduzido)
            }
            .addOnFailureListener {
                atualizarPopup("Erro ao traduzir. Baixando pacote offline de Português...")
                translator.downloadModelIfNeeded()
            }
    }

    private fun mostrarPopup(textoInicial: String) {
        if (popupView != null) {
            atualizarPopup(textoInicial)
            return
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            y = 100
        }

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        popupView = inflater.inflate(R.layout.layout_traducao, null)
        popupView?.findViewById<TextView>(R.id.tvTraduzido)?.text = textoInicial

        popupView?.findViewById<View>(R.id.btnFechar)?.setOnClickListener {
            removerPopup()
        }

        windowManager.addView(popupView, params)
    }

    private fun atualizarPopup(texto: String) {
        popupView?.findViewById<TextView>(R.id.tvTraduzido)?.text = texto
    }

    private fun removerPopup() {
        if (popupView != null) {
            windowManager.removeView(popupView)
            popupView = null
        }
    }

    private fun criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                "tradutor_screen_service",
                "Tradutor de Tela Flutuante",
                NotificationManager.IMPORTANCE_LOW
            )
            canal.description = "Mantém a bolinha tradutora ativa sobre os jogos"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)

            val notificacao = NotificationCompat.Builder(this, "tradutor_screen_service")
                .setContentTitle("Tradutor de Tela Ativo")
                .setContentText("Toque na bolinha flutuante para traduzir")
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .setOngoing(true)
                .build()

            startForeground(101, notificacao)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingBubbleView.isInitialized) {
            windowManager.removeView(floatingBubbleView)
        }
        removerPopup()
        mediaProjection?.stop()
        translator.close()
    }
}
