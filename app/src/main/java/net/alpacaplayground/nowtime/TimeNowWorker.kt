package net.alpacaplayground.nowtime

import android.app.AlarmManager
import android.app.PendingIntent
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.speech.tts.TextToSpeech
import android.support.v4.app.AlarmManagerCompat
import android.util.Log
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

private const val WhatTimeNow = 1
private const val WhatReleaseTts = 2

class TimeNowWorker(val context: Context) : Handler.Callback {
    private var nextAlert = 0L
    private var tts: TextToSpeech? = null
    private var ttsPrepare = TextToSpeech.ERROR

    private val timerHandler: Handler = {
        val th = HandlerThread("TimeNow")
        th.start()
        Handler(th.looper, this)
    }()

    private val dateFormat by lazy { SimpleDateFormat("HH:mm:ss") }

    val liveMsg = MutableLiveData<String>()

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            WhatTimeNow -> {
                timerHandler.removeMessages(1)//清除语音消息
                timerHandler.removeMessages(2)//清除释放tts消息
                val tts = this.tts
                if (tts == null) {
                    ttsPrepare = TextToSpeech.ERROR
                    this.tts = TextToSpeech(context) {
                        ttsPrepare = it
                        timerHandler.sendEmptyMessageDelayed(1, 1000)
                    }
                } else if (TextToSpeech.SUCCESS != ttsPrepare) {
                    timerHandler.sendEmptyMessageDelayed(1, 1000)
                } else {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = System.currentTimeMillis()
                    val hour = calendar[Calendar.HOUR_OF_DAY]
                    val minute = calendar[Calendar.MINUTE]
                    val second = calendar[Calendar.SECOND]

                    val nextDelay = ((if (minute <= 1) {
                        speech(tts, "现在时间,${hour}点整")
                        30
                    } else if (minute >= 59) {
                        speech(tts, "现在时间,${hour + 1}点整")
                        30
                    } else {
                        speech(tts, "现在时间,${hour}点${minute}分")
                        if (minute < 30) {
                            30 - minute
                        } else {
                            60 - minute
                        }
                    }) * 60 - second) * 1000


                    // = (2 * 60 - second) * 1000

                    //释放tts
                    timerHandler.sendEmptyMessageDelayed(2, 10 * 1000)
                    //下次播报时间
                    calendar.timeInMillis = calendar.timeInMillis + nextDelay
                    val nextReportMsg = "下次播报时间：${dateFormat.format(calendar.time)}"
                    val nextAlert = calendar.timeInMillis
                    if (nextAlert != this.nextAlert) {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val intent = Intent(context, TimeNowService::class.java)

                        AlarmManagerCompat.setAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, nextAlert, PendingIntent.getService(context, 1, intent, PendingIntent.FLAG_ONE_SHOT))
                    }
                    Log.i("TimeNow", nextReportMsg)
                    liveMsg.postValue(nextReportMsg)
                }
            }
            WhatReleaseTts -> {
                val tts = this.tts
                if (tts != null) {
                    tts.shutdown()
                    this.tts = null
                    ttsPrepare = TextToSpeech.ERROR
                    Log.i("NowTime", "释放TTS")
                }
            }
        }
        return true
    }

    fun timeNow() {
        timerHandler.sendEmptyMessage(WhatTimeNow)
    }

    private fun speech(tts: TextToSpeech, s: String) {
        if (Build.VERSION.SDK_INT >= 21) {
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "time")
        } else {
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    fun onDestroy() {
        //释放tts
        val tts = this.tts
        if (tts != null) {
            tts.shutdown()
            this.tts = null
        }
        //取消闹钟
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimeNowService::class.java)
        val pi = PendingIntent.getService(context, 1, intent, PendingIntent.FLAG_ONE_SHOT)
        alarmManager.cancel(pi)
        //移除消息并关闭
        timerHandler.removeCallbacksAndMessages(null)
        try {
            if (SDK_INT >= 18) {
                timerHandler.looper.quitSafely()
            } else {
                timerHandler.looper.quit()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}