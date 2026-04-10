package com.atdev.gestor

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class LockActivity : AppCompatActivity() {

    private var permitirCerrar = false

    private val cerrarReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "CERRAR_PANTALLA_BLOQUEO") {
                permitirCerrar = true
                try {
                    stopLockTask() // MUY IMPORTANTE: Liberar el anclaje antes de cerrar
                } catch (e: Exception) {}
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Registrar el receiver con el método seguro de ContextCompat
        val filter = IntentFilter("CERRAR_PANTALLA_BLOQUEO")
        ContextCompat.registerReceiver(this, cerrarReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        // Bloqueo de interacción con el sistema
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        // Ocultar barras (Método compatible)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        setContentView(R.layout.activity_lock)
    }

    override fun onResume() {
        super.onResume()

        // Forzamos el anclaje de pantalla (Screen Pinning)
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                    startLockTask()
                }
            } else {
                startLockTask()
            }
        } catch (e: Exception) {
            Log.e("LOCK", "Error al anclar pantalla: ${e.message}")
        }

        verificarEstadoBloqueo()
    }

    private fun verificarEstadoBloqueo() {
        val safeContext = applicationContext.createDeviceProtectedStorageContext()
        val estaBloqueado = safeContext.getSharedPreferences("CONFIG", Context.MODE_PRIVATE)
            .getBoolean("dispositivo_bloqueado", false)

        if (!estaBloqueado) {
            permitirCerrar = true
            try { stopLockTask() } catch (e: Exception) {}
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
            if (!permitirCerrar) {
                val intent = Intent(this, LockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }
                startActivity(intent)
            }
        }, 10)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(cerrarReceiver)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}