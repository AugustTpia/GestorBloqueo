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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        setContentView(R.layout.activity_lock)

        // REGISTRO SEGURO (SOLUCIÓN AL ERROR)
        val filter = IntentFilter("CERRAR_PANTALLA_BLOQUEO")
        // Usamos la bandera EXPORTED para que el servicio de Firebase pueda avisarle a esta pantalla
        ContextCompat.registerReceiver(this, cerrarReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
    }

    override fun onPause() {
        super.onPause()
        // 1. Si ya tenemos permiso de cerrar, no hacemos nada
        if (permitirCerrar) return

        Handler(Looper.getMainLooper()).postDelayed({
            // 2. Usar el contexto protegido para checar el disco REAL
            val safeContext = applicationContext.createDeviceProtectedStorageContext()
            val stillLocked = safeContext.getSharedPreferences("CONFIG", Context.MODE_PRIVATE)
                .getBoolean("dispositivo_bloqueado", true) // Por seguridad, default true

            // 3. Si en el disco ya dice 'false', forzamos el cierre aquí mismo
            if (!stillLocked) {
                permitirCerrar = true
                finish()
                return@postDelayed
            }

            // 4. Si sigue bloqueado, relanzamos
            if (!permitirCerrar) {
                val intent = Intent(this, LockActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }
        }, 1000) // Aumentamos a 1000ms para dar estabilidad en el arranque
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(cerrarReceiver)
        } catch (e: Exception) {}
    }
}