package com.atdev.gestor

import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import java.io.*

open class VolleyMultipartRequest(
    method: Int,
    url: String,
    private val listener: Response.Listener<NetworkResponse>,
    private val errorListener: Response.ErrorListener
) : Request<NetworkResponse>(method, url, errorListener) {

    private val boundary = "apiclient-" + System.currentTimeMillis()

    override fun getBodyContentType(): String = "multipart/form-data;boundary=$boundary"

    @Throws(AuthFailureError::class)
    override fun getBody(): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        try {
            // 1. Escribir parámetros de texto (nombre, token, etc)
            val params = params
            if (params != null && params.isNotEmpty()) {
                params.forEach { (name, value) ->
                    buildTextPart(dos, name, value)
                }
            }

            // 2. Escribir datos de archivos (fotos, videos)
            val data = getByteData()
            if (data != null && data.isNotEmpty()) {
                data.forEach { (name, dataPart) ->
                    buildDataPart(dos, name, dataPart)
                }
            }

            // 3. Escribir el cierre final del boundary
            dos.writeBytes("--$boundary--\r\n")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bos.toByteArray()
    }

    private fun buildTextPart(dos: DataOutputStream, parameterName: String, parameterValue: String) {
        dos.writeBytes("--$boundary\r\n")
        dos.writeBytes("Content-Disposition: form-data; name=\"$parameterName\"\r\n\r\n")
        // Usamos write para asegurar que caracteres especiales se codifiquen bien
        dos.write(parameterValue.toByteArray(Charsets.UTF_8))
        dos.writeBytes("\r\n")
    }

    private fun buildDataPart(dos: DataOutputStream, parameterName: String, dataFile: DataPart) {
        dos.writeBytes("--$boundary\r\n")
        dos.writeBytes("Content-Disposition: form-data; name=\"$parameterName\"; filename=\"${dataFile.fileName}\"\r\n")
        dos.writeBytes("Content-Type: ${dataFile.type}\r\n\r\n")
        dos.write(dataFile.content)
        dos.writeBytes("\r\n")
    }

    open fun getByteData(): Map<String, DataPart>? = null

    override fun parseNetworkResponse(response: NetworkResponse): Response<NetworkResponse> {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
    }

    override fun deliverResponse(response: NetworkResponse) {
        listener.onResponse(response)
    }

    class DataPart(val fileName: String, val content: ByteArray, val type: String)
}