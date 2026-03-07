package com.atdev.gestor

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AdminReceiver : DeviceAdminReceiver() {

    // Esto se ejecuta cuando el QR logra activar la app como jefa
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Gestor: Control activado", Toast.LENGTH_SHORT).show()
    }

    // Esto avisa si alguien intentara quitarle el permiso (aunque por QR es casi imposible)
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Gestor: Control desactivado", Toast.LENGTH_SHORT).show()
    }
}