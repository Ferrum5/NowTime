package net.alpacaplayground.nowtime

import android.app.Service
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.speech.tts.TextToSpeech
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class NowTimeService : Service(), Handler.Callback {

    private val calendar = Calendar.getInstance()
    private var tts: TextToSpeech? = null
    private var ttsPrepare = TextToSpeech.ERROR

    override fun handleMessage(msg: Message): Boolean {
        if (msg.what == 2) { //释放tts
            val tts = this.tts
            if (tts != null) {
                tts.shutdown()
                this.tts = null
                ttsPrepare = TextToSpeech.ERROR
                Log.i("NowTime","释放TTS")
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
        sendNotification("下次播报时间：${SimpleDateFormat("HH:mm:ss").format(calendar.time)}")
        timerHandler.sendEmptyMessageDelayed(1, nextDelay.toLong())

        return true
    }

    override fun onCreate() {
        super.onCreate()
    }

    private fun speech(tts: TextToSpeech, s: String) {
        if (SDK_INT >= 21) {
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "time")
        } else {
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    private val timerHandler = Handler(this)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        timerHandler.sendEmptyMessage(1)
        return START_STICKY
    }

    override fun onDestroy() {
        val tts = this.tts
        if(tts!=null){
            tts.shutdown()
            this.tts = null
        }
        timerHandler.removeCallbacksAndMessages(null)
    }

    fun sendNotification(s: String) {
        Log.i("NowTime", "通知内容$s")
        val manager = NotificationManagerCompat.from(this)
        if (manager.areNotificationsEnabled()) {
            val notification = NotificationCompat.Builder(this, "1")
                    .setContentTitle("整点报时")
                    .setContentText(s)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .build()
            manager.notify(1, notification)
        }
    }
}