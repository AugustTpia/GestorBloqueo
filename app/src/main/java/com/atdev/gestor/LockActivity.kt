package com.atdev.gestor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class LockActivity : AppCompatActivity() {

    private var permitirCerrar = false

    private val cerrarReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "CERRAR_PANTALLA_BLOQUEO") {
                permitirCerrar = true
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. REGISTRAR EL RECEPTOR (Esto te faltaba)
        // 1. Definir el filtro
        val filter = IntentFilter("CERRAR_PANTALLA_BLOQUEO")

// 2. Usar ContextCompat para registrar el receptor
// Esto elimina el error de Android 14 y funciona en versiones viejas también
        ContextCompat.registerReceiver(
            this,
            cerrarReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED // <--- Aquí le decimos al sistema que Firebase puede hablar con la Actividad
        )

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setFinishOnTouchOutside(false)
        setContentView(R.layout.activity_lock)

        // Verificación inicial por si ya se desbloqueó mientras la actividad nacía
        verificarEstadoBloqueo()
    }

    override fun onResume() {
        super.onResume()
        verificarEstadoBloqueo()
    }

    // 2. CONSULTA DIRECTA AL ALMACENAMIENTO PROTEGIDO
    private fun verificarEstadoBloqueo() {
        val safeContext = applicationContext.createDeviceProtectedStorageContext()
        val estaBloqueado = safeContext.getSharedPreferences("CONFIG", Context.MODE_PRIVATE)
            .getBoolean("dispositivo_bloqueado", false)

        if (!estaBloqueado) {
            permitirCerrar = true
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !permitirCerrar) {
            val intent = Intent(this, LockActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        if (permitirCerrar) return

        Handler(Looper.getMainLooper()).postDelayed({
            // 3. ANTES DE REINICIAR EL BUCLE, CHECAMOS DISCO
            verificarEstadoBloqueo()

            if (!permitirCerrar) {
                val intent = Intent(this, LockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        }, 100)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(cerrarReceiver)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}