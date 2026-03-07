package com.atdev.gestor

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.messaging.FirebaseMessaging
import java.io.File

class RegistroActivity : AppCompatActivity() {

    private lateinit var etNombreCliente: TextInputEditText
    private lateinit var btnFoto: MaterialButton
    private lateinit var btnVideo: MaterialButton
    private lateinit var btnEnviar: MaterialButton

    private var fotoLista = false
    private var videoListo = false

    // Variables para archivos
    private var uriFoto: Uri? = null
    private var uriVideo: Uri? = null
    private var fotoFile: File? = null
    private var videoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // 1. Vincular vistas
        etNombreCliente = findViewById(R.id.etNombreCliente)
        btnFoto = findViewById(R.id.btnFotoID)
        btnVideo = findViewById(R.id.btnVideoAceptacion)
        btnEnviar = findViewById(R.id.btnEnviarTodo)

        val tvTerminos = findViewById<TextView>(R.id.tvTerminos)
        tvTerminos.movementMethod = android.text.method.ScrollingMovementMethod()

        // 2. Pedir permisos especiales de bloqueo al iniciar
        verificarPermisosEspeciales()

        // 3. Configurar clics
        btnFoto.setOnClickListener {
            if (revisarPermisos()) tomarFotoID()
        }

        btnVideo.setOnClickListener {
            if (revisarPermisos()) grabarVideoAceptacion()
        }

        btnEnviar.setOnClickListener {
            validarYEnviar()
        }
    }

    // --- FUNCIONES DE CAPTURA (CORREGIDAS) ---

    private fun tomarFotoID() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            fotoFile = File(getExternalFilesDir(null), "foto_id_temp.jpg")

            // El authority debe ser igual al del Manifest
            uriFoto = FileProvider.getUriForFile(this, "${packageName}.fileprovider", fotoFile!!)

            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriFoto)
            startActivityForResult(intent, 1)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun grabarVideoAceptacion() {
        try {
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 15) // 15 segundos máximo
            videoFile = File(getExternalFilesDir(null), "video_temp.mp4")

            uriVideo = FileProvider.getUriForFile(this, "${packageName}.fileprovider", videoFile!!)

            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriVideo)
            startActivityForResult(intent, 2)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al grabar video", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val colorSuccess = getColor(R.color.success) // Asegúrate de tenerlo en colors.xml
            val colorWhite = getColor(R.color.white)

            when (requestCode) {
                1 -> {
                    fotoLista = true
                    actualizarEstiloBoton(btnFoto, "✅ IDENTIFICACIÓN LISTA", colorSuccess, colorWhite)
                }
                2 -> {
                    videoListo = true
                    actualizarEstiloBoton(btnVideo, "✅ VIDEO GRABADO", colorSuccess, colorWhite)
                }
            }

            if (fotoLista && videoListo) {
                btnEnviar.isEnabled = true
                btnEnviar.alpha = 1.0f
            }
        }
    }

    // --- PERMISOS Y ENVÍO ---

    private fun verificarPermisosEspeciales() {
        // Permiso Overlay (Mostrar sobre otras apps)
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        // Permiso Admin (Para bloquear pantalla)
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Protección obligatoria del dispositivo.")
            startActivity(intent)
        }
    }

    private fun validarYEnviar() {
        val nombre = etNombreCliente.text.toString().trim()
        if (nombre.isEmpty()) {
            etNombreCliente.error = "Ingresa el nombre"
            return
        }

        btnEnviar.isEnabled = false
        btnEnviar.text = "ENVIANDO..."

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                enviarALaravel(task.result, nombre)
            } else {
                btnEnviar.isEnabled = true
                Toast.makeText(this, "Error de Token", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enviarALaravel(token: String, nombre: String) {
        val url = "https://gestor.atinychef.com.mx/api/registrar-dispositivo"
        val queue = Volley.newRequestQueue(this)

        val multipartRequest = object : VolleyMultipartRequest(
            Method.POST, url,
            { response ->
                // Guardar estado inicial seguro
                val safeContext = applicationContext.createDeviceProtectedStorageContext()
                safeContext.getSharedPreferences("CONFIG", MODE_PRIVATE)
                    .edit().putBoolean("dispositivo_bloqueado", false).apply()

                Toast.makeText(this, "✅ Registro exitoso", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            },
            { error ->
                btnEnviar.isEnabled = true
                btnEnviar.text = "REINTENTAR ENVIAR"
                Log.e("API_ERROR", "Error: ${error.networkResponse?.statusCode}")
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["fcm_token"] = token
                params["client_name"] = nombre
                params["brand"] = android.os.Build.MANUFACTURER
                params["model"] = android.os.Build.MODEL
                params["status"] = "active"
                return params
            }
            override fun getByteData(): Map<String, DataPart> = HashMap()
        }
        multipartRequest.retryPolicy = DefaultRetryPolicy(60000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        queue.add(multipartRequest)
    }

    private fun revisarPermisos(): Boolean {
        val permisos = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
        val faltantes = permisos.filter { checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (faltantes.isNotEmpty()) {
            requestPermissions(faltantes.toTypedArray(), 100)
            return false
        }
        return true
    }

    private fun actualizarEstiloBoton(boton: MaterialButton, texto: String, fondo: Int, textoColor: Int) {
        boton.text = texto
        boton.backgroundTintList = ColorStateList.valueOf(fondo)
        boton.setTextColor(textoColor)
    }
}