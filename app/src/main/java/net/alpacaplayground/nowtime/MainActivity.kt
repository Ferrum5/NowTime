package net.alpacaplayground.nowtime

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import android.widget.*

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

        setBeginTime()
        setEndTime()
        setReportType()

        val textMessage: TextView = findViewById(R.id.textNext)

        liveMsg.observe(this, Observer {
            textMessage.text = it
            updateTime()
        })
    }

    private fun setBeginTime(){
        //开始时间
        val textBegin: TextView = findViewById(R.id.textTimeBegin)
        val seekBegin: SeekBar = findViewById(R.id.seekTimeBegin)
        seekBegin.max = 23
        seekBegin.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                NowTime.beginTime = progress
                textBegin.text = "开始时间${progress}点"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
        seekBegin.progress = NowTime.beginTime
    }



    private fun setEndTime(){
        //结束时间时间
        val textEnd: TextView = findViewById(R.id.textTimeEnd)
        val seekEnd: SeekBar = findViewById(R.id.seekTimeEnd)
        seekEnd.max = 23
        seekEnd.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                NowTime.endTime = progress
                textEnd.text = "开始时间${progress}点"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
        seekEnd.progress = NowTime.endTime
    }

    private fun setReportType(){
        val check: Switch = findViewById(R.id.reportType)
        val checkChangeFun = {
            NowTime.reportType = if(check.isChecked) NowTime.ReportTypeEvery15 else NowTime.ReportTypeEvery30
            check.text = if(NowTime.reportType == NowTime.ReportTypeEvery15) "每15分钟报时" else "每30分钟报时"
        }
        check.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                checkChangeFun()
            }
        })
        checkChangeFun()


    }

    override fun onBackPressed() {

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