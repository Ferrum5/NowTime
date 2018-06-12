package net.alpacaplayground.nowtime

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class TimeNowWorker(val context: Context) : Handler.Callback {
    private val calendar = Calendar.getInstance()
    private var tts: TextToSpeech? = null
    private var ttsPrepare = TextToSpeech.ERROR

    private val timerHandler: Handler = {
        val th = HandlerThread("TimeNow")
        th.start()
        Handler(th.looper, this)
    }()

    private val dateFormat by lazy { SimpleDateFormat("HH:mm:ss") }

    val liveMsg = MutableLiveData<String>()

    fun timeNow() {
        timerHandler.sendEmptyMessage(1)
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
        } else if (msg.what == 1) {
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
                calendar.timeInMillis = System.currentTimeMillis()
                val hour = calendar[Calendar.HOUR_OF_DAY]
                val minute = calendar[Calendar.MINUTE]
                val second = calendar[Calendar.SECOND]
                val nextDelay = (
                        (if (minute <= 1) {
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
                /*
                val nextDelay = (if (second < 19) {
                            20 - second
                        } else if (second < 39) {
                            40 - second
                        } else {
                            60 - second
                        }) * 1000
                        */

                //释放tts
                timerHandler.sendEmptyMessageDelayed(2, 10 * 1000)
                //下次播报时间
                calendar.timeInMillis = calendar.timeInMillis + nextDelay
                val nextReportMsg = "下次播报时间：${dateFormat.format(calendar.time)}"
                Log.i("TimeNow", nextReportMsg)
                liveMsg.postValue(nextReportMsg)
                timerHandler.sendEmptyMessageDelayed(1, nextDelay.toLong())
            }
        }
        return true
    }

    private fun speech(tts: TextToSpeech, s: String) {
        if (Build.VERSION.SDK_INT >= 21) {
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "time")
        } else {
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    fun onDestroy() {
        val tts = this.tts
        if (tts != null) {
            tts.shutdown()
            this.tts = null
        }
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