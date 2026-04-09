package com.atdev.gestor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            // Usamos el contexto protegido para leer el estado de bloqueo
            val safeContext = context.createDeviceProtectedStorageContext()
            val prefs = safeContext.getSharedPreferences("CONFIG", Context.MODE_PRIVATE)
            val estaBloqueado = prefs.getBoolean("dispositivo_bloqueado", false)

            if (estaBloqueado) {
                val lockIntent = Intent(context, LockActivity::class.java)
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                context.startActivity(lockIntent)
            }
        }
    }
}