package com.atdev.gestor

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ContratoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contrato)

        val urlContrato = "https://gestor.atinychef.com.mx/contrato.pdf"
        val webView = findViewById<WebView>(R.id.webViewContrato)
        val fabDownload = findViewById<FloatingActionButton>(R.id.fabDownload)

        // Configurar WebView para ver PDF (usando Google Docs Viewer como motor)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://docs.google.com/gview?embedded=true&url=$urlContrato")

        // Botón para descargar
        fabDownload.setOnClickListener {
            descargarPDF(urlContrato)
        }
    }

    private fun descargarPDF(url: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setTitle("Contrato_Gestor.pdf")
            request.setDescription("Descargando contrato de aceptación...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Contrato_Gestor.pdf")

            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(this, "Iniciando descarga...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al descargar", Toast.LENGTH_SHORT).show()
        }
    }
}