package com.example.mypomodorotimer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.mypomodorotimer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var timerRunning = false
    private var timeLeftInMillis = 0L
    private lateinit var countDownTimer: CountDownTimer
    private var isWorkTime = true

    private var workTimeMinutes = 25 // 기본값
    private var breakTimeMinutes = 5  // 기본값

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        checkNotificationPermission()  // 권한 체크 추가
        timeLeftInMillis = workTimeMinutes * 60 * 1000L
        setupTimer()
        setupButtons()
        setupTimeInputs()
    }

    private fun setupTimer() {
        updateTimerUI()
    }

    private fun setupTimeInputs() {
        // 작업 시간 설정
        binding.workTimeInput.setText(workTimeMinutes.toString())
        binding.workTimeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.toIntOrNull()?.let { minutes ->
                    if (minutes > 0) {
                        workTimeMinutes = minutes
                        if (isWorkTime && !timerRunning) {
                            timeLeftInMillis = workTimeMinutes * 60 * 1000L
                            updateTimerUI()
                        }
                    }
                }
            }
        })

        // 휴식 시간 설정
        binding.breakTimeInput.setText(breakTimeMinutes.toString())
        binding.breakTimeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.toIntOrNull()?.let { minutes ->
                    if (minutes > 0) {
                        breakTimeMinutes = minutes
                        if (!isWorkTime && !timerRunning) {
                            timeLeftInMillis = breakTimeMinutes * 60 * 1000L
                            updateTimerUI()
                        }
                    }
                }
            }
        })
    }

    private fun setupButtons() {
        binding.startButton.setOnClickListener {
            if (timerRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        binding.resetButton.setOnClickListener {
            resetTimer()
        }

        binding.switchButton.setOnClickListener {
            switchTimerMode()
        }
    }

    private fun switchTimerMode() {
        if (!timerRunning) {
            isWorkTime = !isWorkTime
            timeLeftInMillis = if (isWorkTime) {
                workTimeMinutes * 60 * 1000L
            } else {
                breakTimeMinutes * 60 * 1000L
            }
            updateTimerUI()
        }
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerUI()
            }

            override fun onFinish() {
                if (isWorkTime) {
                    timeLeftInMillis = breakTimeMinutes * 60 * 1000L
                    isWorkTime = false
                    showNotification("작업 시간 종료!", "휴식 시간입니다.")
                } else {
                    timeLeftInMillis = workTimeMinutes * 60 * 1000L
                    isWorkTime = true
                    showNotification("휴식 시간 종료!", "작업을 시작하세요.")
                }
                updateTimerUI()
                startTimer()
            }
        }.start()

        timerRunning = true
        updateButtons()
        disableTimeInputs(true)
    }

    private fun pauseTimer() {
        countDownTimer.cancel()
        timerRunning = false
        updateButtons()
        disableTimeInputs(false)
    }

    private fun resetTimer() {
        if (this::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
        timeLeftInMillis = if (isWorkTime) {
            workTimeMinutes * 60 * 1000L
        } else {
            breakTimeMinutes * 60 * 1000L
        }
        timerRunning = false
        updateTimerUI()
        updateButtons()
        disableTimeInputs(false)
    }

    private fun disableTimeInputs(disable: Boolean) {
        binding.workTimeInput.isEnabled = !disable
        binding.breakTimeInput.isEnabled = !disable
    }

    private fun updateTimerUI() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60

        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        binding.timerText.text = timeFormatted

        binding.statusText.text = if (isWorkTime) "작업 시간" else "휴식 시간"
    }

    private fun updateButtons() {
        binding.startButton.text = if (timerRunning) "일시정지" else "시작"
    }

    // 권한 체크 함수 추가
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }



    // showNotification 함수 수정
    private fun showNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 없으면 알림을 보내지 않음
            return
        }

        val channelId = "POMODORO_CHANNEL"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "뽀모도로 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        try {
            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            NotificationManagerCompat.from(this).notify(1, notification)
        } catch (e: SecurityException) {
            // 권한이 거부된 경우 처리
            e.printStackTrace()
        }
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    // 권한이 승인된 경우
                } else {
                    // 권한이 거부된 경우
                    // 필요하다면 사용자에게 알림 권한이 없다는 메시지를 표시
                }
            }
        }
    }
}