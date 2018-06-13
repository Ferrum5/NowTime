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
import kotlin.math.min

private const val WhatTimeNow = 1
private const val WhatReleaseTts = 2
private const val WhatTimeNowForce = 3

class TimeNowWorker(val context: Context) : Handler.Callback {
    private var speakFlag = 0
    private var tts: TextToSpeech? = null
    private var ttsPrepare = TextToSpeech.ERROR
    private val calendar by lazy { Calendar.getInstance() }

    private val timerHandler: Handler = {
        val th = HandlerThread("TimeNow")
        th.start()
        Handler(th.looper, this)
    }()

    private val dateFormat by lazy { SimpleDateFormat("HH:mm") }

    val liveMsg = MutableLiveData<String>()

    override fun handleMessage(msg: Message): Boolean {
        val what = msg.what
        when (what) {
            WhatTimeNow, WhatTimeNowForce -> {
                timerHandler.removeMessages(WhatTimeNow)//清除语音消息
                timerHandler.removeMessages(WhatReleaseTts)//清除释放tts消息
                timerHandler.removeMessages(WhatTimeNowForce)//清除强制语音消息
                val tts = this.tts
                if (tts == null) {
                    ttsPrepare = TextToSpeech.ERROR
                    this.tts = TextToSpeech(context) {
                        ttsPrepare = it
                        timerHandler.sendEmptyMessageDelayed(what, 1000)
                    }
                } else if (TextToSpeech.SUCCESS != ttsPrepare) {
                    timerHandler.sendEmptyMessageDelayed(what, 1000)
                } else {
                    calendar.timeInMillis = System.currentTimeMillis()
                    val hour = calendar[Calendar.HOUR_OF_DAY]
                    val minute = calendar[Calendar.MINUTE]
                    val second = calendar[Calendar.SECOND]

                    var speech = false

                    val time2Next = if (speakFlag == 0) { //下次整点报时
                        if (minute <= 30) { //改为半点
                            speakFlag = speakFlag xor 1
                            speech = true
                            30 - minute
                        } else {
                            60 - minute
                        }
                    } else { //下次半点报时
                        if (minute >= 30) { //改为整点报时
                            speakFlag = speakFlag xor 1
                            speech = true
                            60 - minute
                        } else {
                            30 - minute
                        }
                    }

                    speech = speech || (WhatTimeNowForce == what)

                    if (speech) {
                        if (minute <= 1) {
                            speech(tts, "现在时间,${hour}点整")
                        } else if (minute >= 59) {
                            speech(tts, "现在时间,${hour + 1}点整")
                        } else {
                            speech(tts, "现在时间,${hour}点${if (minute < 10) "0" else ""}${minute}分")
                        }
                    }
//                    val nextDelay = ((if (minute <= 1) {
//                        speech(tts, "现在时间,${hour}点整")
//                        30
//                    } else if (minute >= 59) {
//                        speech(tts, "现在时间,${hour + 1}点整")
//                        30
//                    } else {
//                        speech(tts, "现在时间,${hour}点${minute}分")
//                        if (minute < 30) {
//                            30 - minute
//                        } else {
//                            60 - minute
//                        }
//                    }) * 60 - second) * 1000


                    // = (2 * 60 - second) * 1000

                    //释放tts
                    timerHandler.sendEmptyMessageDelayed(2, 10000)
                    //下次播报时间
                    calendar.timeInMillis = calendar.timeInMillis + (time2Next * 60 * 1000)
                    val nextReportMsg = "下次播报时间：${dateFormat.format(calendar.time)}\n还有${time2Next}分钟"
//                    val nextAlert = calendar.timeInMillis
//                    if (nextAlert != this.nextAlert) {
//                        this.nextAlert = nextAlert
//                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//                        val intent = Intent(context, TimeNowService::class.java)
//
//                        AlarmManagerCompat.setAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, nextAlert, PendingIntent.getService(context, 1, intent, PendingIntent.FLAG_ONE_SHOT))
//                    }
                    Log.i("TimeNow", nextReportMsg)
                    liveMsg.postValue(nextReportMsg)
                    timerHandler.sendEmptyMessageDelayed(1, (60 - second) * 1000L)
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
        timerHandler.sendEmptyMessage(WhatTimeNowForce)
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