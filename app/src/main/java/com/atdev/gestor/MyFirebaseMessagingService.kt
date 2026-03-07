package com.atdev.gestor

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val action = remoteMessage.data["action"]
        val safeContext = applicationContext.createDeviceProtectedStorageContext()
        val prefs = safeContext.getSharedPreferences("CONFIG", Context.MODE_PRIVATE)

        when (action) {
            "LOCK_DEVICE" -> {
                prefs.edit().putBoolean("dispositivo_bloqueado", true).apply()

                // Bloqueo físico
                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val admin = ComponentName(this, MyDeviceAdminReceiver::class.java)
                if (dpm.isAdminActive(admin)) dpm.lockNow()

                // Lanzar pantalla
                val intent = Intent(this, LockActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }

            // En tu MyFirebaseMessagingService
            "UNLOCK_DEVICE" -> {
                val safeContext = applicationContext.createDeviceProtectedStorageContext()
                safeContext.getSharedPreferences("CONFIG", Context.MODE_PRIVATE)
                    .edit().putBoolean("dispositivo_bloqueado", false).commit() // COMMIT es clave aquí

                val intent = Intent("CERRAR_PANTALLA_BLOQUEO")
                intent.setPackage(packageName)
                sendBroadcast(intent)
            }
        }
    }
}