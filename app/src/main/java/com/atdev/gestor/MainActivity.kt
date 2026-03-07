package com.atdev.gestor

import android.os.Bundle
import android.widget.TextView
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Un ScrollView por si el token es muy largo y no cabe
        val scroll = ScrollView(this)
        val tv = TextView(this)

        tv.textSize = 16f
        tv.setPadding(50, 50, 50, 50)
        tv.setTextIsSelectable(true) // Esta es la forma correcta en algunas versiones

        scroll.addView(tv)
        setContentView(scroll)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                tv.text = "TOKEN PARA TU BASE DE DATOS:\n\n$token"

                // Esto sale en la pestaña "RUN"
                println("------------------------------------------")
                println("TOKEN: $token")
                println("------------------------------------------")
            } else {
                tv.text = "Error: No se pudo obtener el token. ¿Tienes WiFi activo?"
            }
        }
    }
}