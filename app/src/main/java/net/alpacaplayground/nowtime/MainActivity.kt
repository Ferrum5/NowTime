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
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat

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
            textMessage.text = """
                |距下次播报时间
                |${(time + ((it ?: 0) * 60 * 1000)).format("HH:mm")}
                |还有${it}分钟
                """.trimMargin()
            updateTime()
        })
    }

    override fun onBackPressed() {
        TimeNowWorker.timeNow()
    }


    private fun updateTime() {
        time.updateToNow()
        textTime.text = time.format("HH:mm")
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