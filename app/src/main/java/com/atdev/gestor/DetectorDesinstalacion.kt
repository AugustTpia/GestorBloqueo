package com.atdev.gestor

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class DetectorDesinstalacion : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 1. Verificamos si el registro ya se hizo (esto es correcto)
        val safeContext = applicationContext.createDeviceProtectedStorageContext()
        val prefs = safeContext.getSharedPreferences("CONFIG", MODE_PRIVATE)
        if (!prefs.getBoolean("registro_completado", false)) return

        // 2. IMPORTANTE: Solo actuar si el dispositivo NO está bloqueado por falta de pago.
        // Si el dispositivo está bloqueado general, la LockActivity se encarga.
        // Aquí solo queremos evitar la desinstalación.
        val bloqueadoPorPago = prefs.getBoolean("dispositivo_bloqueado", false)
        if (bloqueadoPorPago) return

        val rootNode = rootInActiveWindow ?: return

        // 3. Buscamos específicamente si el usuario está viendo los detalles de NUESTRA app
        if (analizarPantallaRecursivo(rootNode)) {
            // En lugar de lanzar la pantalla negra eterna, solo lo expulsamos al escritorio
            sacarAlEscritorio()
        }

        rootNode.recycle()
    }

    private fun sacarAlEscritorio() {
        // Esta es la acción que "patea" al usuario fuera de Ajustes
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun analizarPantallaRecursivo(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val texto = node.text?.toString() ?: ""
        val paquete = node.packageName?.toString() ?: ""

        // ¿Estamos en Ajustes y el nodo dice nuestro nombre?
        if (paquete.contains("settings") && texto.contains("ATLink", ignoreCase = true)) {
            // Si además el sistema muestra botones típicos de gestión...
            val root = rootInActiveWindow
            val esVentanaPeligrosa = root?.findAccessibilityNodeInfosByText("Desinstalar")?.isNotEmpty() == true ||
                    root?.findAccessibilityNodeInfosByText("Uninstall")?.isNotEmpty() == true ||
                    root?.findAccessibilityNodeInfosByText("Forzar detención")?.isNotEmpty() == true

            if (esVentanaPeligrosa) return true
        }

        for (i in 0 until node.childCount) {
            if (analizarPantallaRecursivo(node.getChild(i))) return true
        }
        return false
    }

    private fun bloquearAcceso() {
        performGlobalAction(GLOBAL_ACTION_HOME) // Lo manda al escritorio

        val intent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}