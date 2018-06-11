package net.alpacaplayground.nowtime

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity(), Handler.Callback {

    private val calendar = Calendar.getInstance()
    private var tts: TextToSpeech? = null
    private var ttsPrepare = TextToSpeech.ERROR

    private val timerHandler = Handler(this)

    private lateinit var textView: TextView

    private val dateFormat by lazy { SimpleDateFormat("HH:mm:ss") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)

        findViewById<View>(R.id.button).setOnClickListener {
            timerHandler.sendEmptyMessage(1)
        }
        timerHandler.sendEmptyMessage(1)
    }


    override fun onDestroy() {
        super.onDestroy()
        val tts = this.tts
        if (tts != null) {
            tts.shutdown()
            this.tts = null
        }
        timerHandler.removeCallbacksAndMessages(null)
    }

    override fun handleMessage(msg: Message): Boolean {
        if (msg.what == 2) { //释放tts
            val tts = this.tts
            if (tts != null) {
                tts.shutdown()
                this.tts = null
                ttsPrepare = TextToSpeech.ERROR
                Log.i("NowTime", "释放TTS")
            }
            return true
        }
        timerHandler.removeMessages(1)//清除语音消息
        timerHandler.removeMessages(2)//清除释放tts消息
        val tts = this.tts
        if (tts == null) {
            ttsPrepare = TextToSpeech.ERROR
            this.tts = TextToSpeech(this) {
                ttsPrepare = it
                timerHandler.sendEmptyMessageDelayed(1, 1000)
            }
            return true
        } else if (TextToSpeech.SUCCESS != ttsPrepare) {
            timerHandler.sendEmptyMessageDelayed(1, 1000)
            return true
        }
        calendar.timeInMillis = System.currentTimeMillis()
        val hour = calendar[Calendar.HOUR_OF_DAY]
        val minute = calendar[Calendar.MINUTE]
        val second = calendar[Calendar.SECOND]
        val nextDelay = (when (minute) {
            in 0..1 -> {
                speech(tts, "现在时间,${hour}点整")
                30
            }
            !in 0 until 59 -> {
                speech(tts, "现在时间,${hour + 1}点整")
                30
            }
            else -> {
                speech(tts, "现在时间,${hour}点${minute}分")
                if (minute < 30) {
                    30 - minute
                } else {
                    60 - minute
                }
            }
        } * 60 - second) * 1000
        timerHandler.sendEmptyMessageDelayed(2, 10 * 1000)
        calendar.timeInMillis = calendar.timeInMillis + nextDelay

        textView.text = "下次播报时间：${dateFormat.format(calendar.time)}"

        timerHandler.sendEmptyMessageDelayed(1, nextDelay.toLong())
        return true
    }


    private fun speech(tts: TextToSpeech, s: String) {
        if (Build.VERSION.SDK_INT >= 21) {
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "time")
        } else {
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null)
        }
    }
}