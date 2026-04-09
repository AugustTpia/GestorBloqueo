package com.atdev.gestor

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        // Opcional: Podrías notificar a tu servidor Laravel que el admin fue activado
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)

        // 1. Bloqueo inmediato de hardware
        try {
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
            }
        } catch (e: Exception) {
            // Fallback si algo falla con el DPM
        }

        // 2. Lanzamiento agresivo de la LockActivity
        // Usamos un intervalo más corto para ganar la carrera contra la ventana de Android
        Handler(Looper.getMainLooper()).postDelayed({
            val lockIntent = Intent(context, LockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                // Flag extra para asegurar que se muestre sobre el sistema
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(lockIntent)
        }, 100)

        // 3. El mensaje disuasorio (aparece en la ventana del sistema)
        return "ADVERTENCIA: Esta acción infringe el contrato de ATLink. El dispositivo quedará inhabilitado permanentemente hasta que un administrador lo libere."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        // Si el usuario logra desactivarlo (muy difícil con tu LockActivity corriendo),
        // aquí es donde mandas la alerta final al servidor Laravel.
        super.onDisabled(context, intent)
    }
}