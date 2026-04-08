package com.atdev.gestor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Accedemos al almacenamiento que funciona ANTES del PIN
        val safeContext = context.createDeviceProtectedStorageContext()
        val prefs = safeContext.getSharedPreferences("CONFIG", Context.MODE_PRIVATE)
        val estaBloqueado = prefs.getBoolean("dispositivo_bloqueado", false)

        if (estaBloqueado) {
            val lockIntent = Intent(context, LockActivity::class.java)
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(lockIntent)
        }
    }
}