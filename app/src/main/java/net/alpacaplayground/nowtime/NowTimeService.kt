package net.alpacaplayground.nowtime

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.speech.tts.TextToSpeech
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class NowTimeService : Service(), Handler.Callback {

    private val calendar = Calendar.getInstance()
    private var tts: TextToSpeech? = null
    private var ttsPrepare = TextToSpeech.ERROR

    private var floatView: TextView? = null

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
        sendNotification(SimpleDateFormat("HH:mm:ss").format(calendar.time))
        timerHandler.sendEmptyMessageDelayed(1, nextDelay.toLong())

        return true
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
        if (floatView == null) {
            val floatView = TextView(this)
            floatView.setTextColor(Color.WHITE)
            floatView.setBackgroundColor(0xCC000000.toInt())
            floatView.gravity = Gravity.CENTER
            val padding = dip(5)
            floatView.setPadding(padding, padding, padding, padding)
            floatView.setOnClickListener {
                val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                manager.removeView(floatView)
                this.floatView = null
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams()
            params.width = WRAP_CONTENT
            params.height = WRAP_CONTENT
            params.gravity = Gravity.BOTTOM or Gravity.RIGHT
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            manager.addView(floatView, params)
            this.floatView = floatView
        }
        timerHandler.sendEmptyMessage(1)
        return START_STICKY
    }


    override fun onDestroy() {
        val tts = this.tts
        if (tts != null) {
            tts.shutdown()
            this.tts = null
        }
        val floatView = this.floatView
        if (floatView != null) {
            val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            manager.removeView(floatView)
            this.floatView = null
        }
        timerHandler.removeCallbacksAndMessages(null)
    }

    private fun sendNotification(s: String) {
        floatView?.text = s
        Log.i("NowTime", "通知内容$s")
        val manager = NotificationManagerCompat.from(this)
        if (manager.areNotificationsEnabled()) {
            val notification = NotificationCompat.Builder(this, "1")
                    .setContentTitle("整点报时")
                    .setContentText("下次播报时间：$s")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .build()
            manager.notify(1, notification)
        }
    }
}