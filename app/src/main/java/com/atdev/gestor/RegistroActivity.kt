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
        setContentView(R.layout.activity_registro)

        // 1. Inicializar Vistas
        etNombreCliente = findViewById(R.id.etNombreCliente)
        btnVerContrato = findViewById(R.id.btnVerContrato)
        btnEnviar = findViewById(R.id.btnEnviarTodo)

        // 2. Verificar permisos críticos de administración al entrar
        verificarPermisosEspeciales()

        // 3. Configurar clics
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

        btnEnviar.isEnabled = false
        btnEnviar.text = "REGISTRANDO..."

        // Obtener el token de Firebase y enviar a Laravel
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
                // Guardar que el dispositivo ya no está bloqueado tras el registro exitoso
                val safeContext = applicationContext.createDeviceProtectedStorageContext()
                safeContext.getSharedPreferences("CONFIG", MODE_PRIVATE)
                    .edit().putBoolean("dispositivo_bloqueado", false).apply()

                Toast.makeText(this, "✅ Registro exitoso", Toast.LENGTH_SHORT).show()

                // Ir a la pantalla principal
                startActivity(Intent(this, MainActivity::class.java))
                finish()
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

        // 30 segundos de timeout para el registro
        request.retryPolicy = DefaultRetryPolicy(30000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        queue.add(request)
    }

    private fun verificarPermisosEspeciales() {
        // Permiso para mostrarse sobre otras apps
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        // Permiso de Administrador de Dispositivo
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Es necesario activar la protección para continuar.")
            startActivity(intent)
        }
    }
}