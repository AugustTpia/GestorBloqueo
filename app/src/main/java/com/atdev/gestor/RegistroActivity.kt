package com.atdev.gestor

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.accessibilityservice.AccessibilityService
import android.text.TextUtils
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.messaging.FirebaseMessaging

class RegistroActivity : AppCompatActivity() {

    private lateinit var etNombreCliente: TextInputEditText
    private lateinit var btnVerContrato: MaterialButton
    private lateinit var btnEnviar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val safeContext = applicationContext.createDeviceProtectedStorageContext()
        val prefs = safeContext.getSharedPreferences("CONFIG", MODE_PRIVATE)
        val yaRegistrado = prefs.getBoolean("registro_completado", false)

        if (yaRegistrado) {
            finish()
            return
        }

        setContentView(R.layout.activity_registro)

        etNombreCliente = findViewById(R.id.etNombreCliente)
        btnVerContrato = findViewById(R.id.btnVerContrato)
        btnEnviar = findViewById(R.id.btnEnviarTodo)

        verificarPermisosEspeciales()

        btnVerContrato.setOnClickListener {
            val intent = Intent(this, ContratoActivity::class.java)
            startActivity(intent)
        }

        btnEnviar.setOnClickListener {
            validarYEnviar()
        }
    }

    private fun validarYEnviar() {
        val nombre = etNombreCliente.text.toString().trim()

        if (nombre.isEmpty()) {
            etNombreCliente.error = "Por favor, ingresa el nombre del cliente"
            return
        }

        // --- VALIDACIÓN EXTRA DE SEGURIDAD ---
        // Si el usuario no activó los permisos, no lo dejamos enviar el registro
        if (!Settings.canDrawOverlays(this) || !isAccessibilityServiceEnabled(this, DetectorDesinstalacion::class.java)) {
            Toast.makeText(this, "⚠️ Debe activar todos los permisos de protección primero", Toast.LENGTH_LONG).show()
            verificarPermisosEspeciales()
            return
        }

        btnEnviar.isEnabled = false
        btnEnviar.text = "REGISTRANDO..."

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result ?: ""
                enviarALaravel(token, nombre)
            } else {
                btnEnviar.isEnabled = true
                btnEnviar.text = "REINTENTAR"
                Toast.makeText(this, "Error: No se pudo generar el token FCM", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enviarALaravel(token: String, nombre: String) {
        val url = "https://gestor.atinychef.com.mx/api/registrar-dispositivo"
        val queue = Volley.newRequestQueue(this)

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                val safeContext = applicationContext.createDeviceProtectedStorageContext()
                safeContext.getSharedPreferences("CONFIG", MODE_PRIVATE)
                    .edit()
                    .putBoolean("registro_completado", true)
                    .putBoolean("dispositivo_bloqueado", false)
                    .apply()

                Toast.makeText(this, "✅ Registro exitoso. Protección activada.", Toast.LENGTH_SHORT).show()
                finishAffinity()
            },
            { error ->
                btnEnviar.isEnabled = true
                btnEnviar.text = "CONFIRMAR Y ENVIAR"
                val errorMsg = if (error.networkResponse != null) "Error: ${error.networkResponse.statusCode}" else "Error de conexión"
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["fcm_token"] = token
                params["client_name"] = nombre
                params["brand"] = android.os.Build.MANUFACTURER
                params["model"] = android.os.Build.MODEL
                params["os_version"] = android.os.Build.VERSION.RELEASE
                return params
            }
        }

        request.retryPolicy = DefaultRetryPolicy(30000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        queue.add(request)
    }

    private fun verificarPermisosEspeciales() {
        // 1. Superposición (Overlay)
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return // Detenemos para que el usuario procese un permiso a la vez
        }

        // 2. Administrador de Dispositivo
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Es necesario activar la protección para continuar.")
            startActivity(intent)
            return
        }

        // 3. Accesibilidad (Para detectar intentos de desinstalación)
        if (!isAccessibilityServiceEnabled(this, DetectorDesinstalacion::class.java)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Busca 'ATLink' y activa la Accesibilidad", Toast.LENGTH_LONG).show()
        }
    }

    // Función auxiliar para verificar si el servicio está corriendo
    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val expectedId = ComponentName(context, service).flattenToString() // Usamos flattenToString para el ID completo
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

        if (enabledServices.isNullOrEmpty()) return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            // Comparamos ignorando mayúsculas/minúsculas
            if (componentName.equals(expectedId, ignoreCase = true)) {
                return true
            }
        }

        // Verificación secundaria por si el ID se guardó de forma corta
        val shortId = ComponentName(context, service).flattenToShortString()
        return enabledServices.contains(shortId, ignoreCase = true)
    }
}