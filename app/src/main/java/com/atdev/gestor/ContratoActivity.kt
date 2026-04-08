package com.atdev.gestor

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ContratoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contrato)

        // URL del contrato en tu servidor
        val urlContrato = "https://gestor.atinychef.com.mx/contrato.pdf"

        val webView = findViewById<WebView>(R.id.webViewContrato)
        val fabDownload = findViewById<FloatingActionButton>(R.id.fabDownload)

        // Configuración del WebView
        webView.settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Opcional: ocultar un progress bar aquí si decides poner uno
            }
        }

        // Cargamos el PDF usando el visor de Google Docs (es la forma más compatible)
        val googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=$urlContrato"
        webView.loadUrl(googleDocsUrl)

        // Configuración del botón de descarga
        fabDownload.setOnClickListener {
            descargarPDF(urlContrato)
        }
    }

    private fun descargarPDF(url: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setTitle("Contrato_Gestor.pdf")
            request.setDescription("Descargando archivo...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // Guardar en la carpeta de descargas pública
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Contrato_Gestor.pdf")

            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)

            Toast.makeText(this, "Iniciando descarga...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al descargar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}