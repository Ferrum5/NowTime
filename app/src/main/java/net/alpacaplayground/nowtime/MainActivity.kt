package net.alpacaplayground.nowtime

import android.Manifest
import android.arch.lifecycle.Observer
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.buttonReport).setOnClickListener { startService(Intent(this, MainService::class.java)) }
        findViewById<View>(R.id.buttonExit).setOnClickListener {
            stopService(Intent(this, MainService::class.java))
            stopService(Intent(this, TimeNowService::class.java))
            finish()
        }
        val textMessage: TextView = findViewById(R.id.textView)
        liveMsg.observe(this, Observer {
            textMessage.text = it
        })
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SET_ALARM) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SET_ALARM), 1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, MainService::class.java))
    }
}