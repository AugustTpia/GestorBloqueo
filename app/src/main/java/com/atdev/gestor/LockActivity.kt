package com.atdev.gestor

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

class LockActivity : AppCompatActivity() {

    private var permitirCerrar = false

    private val cerrarReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            permitirCerrar = true // <--- Esto detiene el Handler del onPause
            finish() // <--- Esto cierra la pantalla
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hacer que la actividad sea persistente sobre cualquier cosa
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Esto es vital para Android 10+ para que la app no sea minimizada
        setFinishOnTouchOutside(false)

        setContentView(R.layout.activity_lock)
    }

    // Evita que usen el botón de "Recientes" para cerrar la app
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !permitirCerrar) {
            // Si pierde el foco (porque abrieron notificaciones o ajustes),
            // forzamos el regreso inmediato.
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

        // Un delay muy corto para que no dé tiempo de reaccionar
        Handler(Looper.getMainLooper()).postDelayed({
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
        super.onDestroy()
        try {
            unregisterReceiver(cerrarReceiver)
        } catch (e: Exception) {}
    }
}