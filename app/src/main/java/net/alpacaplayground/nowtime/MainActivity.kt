package net.alpacaplayground.nowtime

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View

class MainActivity : Activity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    public fun start(view: View){
        startService(Intent(this,NowTimeService::class.java))
    }

    public fun Shutdown(view: View){
        stopService(Intent(this,NowTimeService::class.java))
    }
}