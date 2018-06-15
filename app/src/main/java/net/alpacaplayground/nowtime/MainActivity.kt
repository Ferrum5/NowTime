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
import android.view.WindowManager
import android.widget.TextView
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var textTime: TextView
    private val dateFormat by lazy { SimpleDateFormat("HH:mm") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textTime = findViewById(R.id.textTime)
        updateTime()
        findViewById<View>(R.id.buttonReport).setOnClickListener { startService(Intent(this, MainService::class.java)) }
        findViewById<View>(R.id.buttonExit).setOnClickListener {
            stopService(Intent(this, MainService::class.java))
            stopService(Intent(this, TimeNowService::class.java))
            finish()
        }
        val textMessage: TextView = findViewById(R.id.textView)
        liveMsg.observe(this, Observer {
            textMessage.text = it
            updateTime()
        })
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SET_ALARM) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SET_ALARM), 1)
        }
    }


    private fun updateTime() {
        textTime.text = dateFormat.format(System.currentTimeMillis())
    }

    override fun onStart() {
        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, MainService::class.java))
    }
}