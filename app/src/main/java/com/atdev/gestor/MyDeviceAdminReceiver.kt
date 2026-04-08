package com.atdev.gestor

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // CORRECCIÓN: Usamos Context.DEVICE_POLICY_SERVICE para evitar el error de tipo
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

        // 1. Apagamos la pantalla inmediatamente para desorientar al usuario
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
        }

        // 2. Lanzamos la pantalla de bloqueo con un pequeño retraso (0.5 seg)
        // para asegurar que se ejecute después de que el sistema procese el bloqueo
        Handler(Looper.getMainLooper()).postDelayed({
            val lockIntent = Intent(context, LockActivity::class.java)
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context.startActivity(lockIntent)
        }, 500)

        // 3. Este mensaje aparecerá en el diálogo de confirmación de Android
        return "ATLink Security: ¡AVISO DE SEGURIDAD! Desactivar esta función bloqueará el dispositivo y notificará al servidor."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}