package com.example.playersampleapp.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.playersampleapp.R
import com.example.playersampleapp.server.ConnectController
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val connectController : ConnectController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val ip:TextView = findViewById(R.id.ip_value)
        val portValue:TextView = findViewById(R.id.port_value)

        connectController.start(this)

        connectController.serverAddress.observe(this) { (host, port) ->
            ip.text = host
            portValue.text = port
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectController.stop()
    }
}