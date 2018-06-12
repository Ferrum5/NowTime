package net.alpacaplayground.nowtime

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.buttonReport).setOnClickListener { startService(Intent(this, MainService::class.java)) }
        findViewById<View>(R.id.buttonExit).setOnClickListener { stopService(Intent(this, MainService::class.java)) }
        val textMessage: TextView = findViewById(R.id.textView)
        liveMsg.observe(this, Observer {
            textMessage.text = it
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this,MainService::class.java))
    }
}