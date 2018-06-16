package net.alpacaplayground.nowtime

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var textTime: TextView
    private val time = Time()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TimeNowWorker.context = this

        setContentView(R.layout.activity_main)
        textTime = findViewById(R.id.textTime)

        updateTime()
        //报时
        val button: Button = findViewById(R.id.buttonReport)
        button.setOnClickListener { TimeNowWorker.timeNow() }


        //tts状态
        liveTtsStatus.observe(this, Observer {
            button.text = it ?: "REPORT"
        })

        val textMessage: TextView = findViewById(R.id.textNext)

        liveMsg.observe(this, Observer {
            textMessage.text = it
            updateTime()
        })
    }

    override fun onBackPressed() {
        TimeNowWorker.timeNow()
    }


    private fun updateTime() {
        time.updateToNow()
        textTime.text = time.hourMinute("%02d:%02d")
    }

    override fun onStart() {
        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        super.onStop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}