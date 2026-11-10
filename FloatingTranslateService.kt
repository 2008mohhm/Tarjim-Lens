package app.lenstranslatorpro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/** فقاعة عائمة فوق التطبيقات: اضغطها لعرض آخر ترجمة ثم انسخها أو استمع لها. */
class FloatingTranslateService : Service() {
    private var wm: WindowManager? = null
    private var root: LinearLayout? = null
    private var speaker: Speaker? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        speaker = Speaker(this)
        scope.launch {
            val s = SettingsRepository(this@FloatingTranslateService).settings.first()
            buildBubble(s)
        }
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel("overlay", "الفقاعة العائمة", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val n = NotificationCompat.Builder(this, "overlay")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("LensTranslatorPro")
            .setContentText("الفقاعة العائمة تعمل")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun buildBubble(s: AppSettings) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val size = dp(s.overlaySize)

        val bubble = TextView(this).apply {
            text = "T"
            gravity = Gravity.CENTER
            textSize = 20f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF6750A4.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(size, size)
        }
        val result = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            maxLines = 8
            maxWidth = dp(260)
        }

        fun actionButton(label: String, onClick: () -> Unit) = Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(actionButton("نسخ") {
                val cm = getSystemService(ClipboardManager::class.java)
                cm.setPrimaryClip(ClipData.newPlainText("translation", LastTranslation.translated))
            })
            addView(actionButton("استمع") {
                speaker?.speak(LastTranslation.translated, LastTranslation.targetLang, s.speechRate, s.pitch)
            })
            addView(actionButton("فتح") {
                startActivity(Intent(this@FloatingTranslateService, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            })
            addView(actionButton("إغلاق") { stopSelf() })
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(12), dp(12), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(0xEE1C1B1F.toInt())
                cornerRadius = dp(16).toFloat()
            }
            addView(result)
            addView(buttons)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            alpha = s.overlayOpacity
            addView(bubble)
            addView(panel)
        }

        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(200)
        }

        fun toggle() {
            if (panel.visibility == View.VISIBLE) {
                panel.visibility = View.GONE
            } else {
                result.text = LastTranslation.translated.ifBlank { "لا توجد ترجمة بعد" }
                panel.visibility = View.VISIBLE
            }
            wm?.updateViewLayout(container, lp)
        }

        // سحب الفقاعة، والضغط القصير يفتح/يغلق اللوحة
        bubble.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0; var startY = 0
            var touchX = 0f; var touchY = 0f
            var moved = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = lp.x; startY = lp.y
                        touchX = e.rawX; touchY = e.rawY
                        moved = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - touchX).toInt()
                        val dy = (e.rawY - touchY).toInt()
                        if (abs(dx) > 10 || abs(dy) > 10) moved = true
                        if (moved) {
                            lp.x = startX + dx
                            lp.y = startY + dy
                            wm?.updateViewLayout(container, lp)
                        }
                    }
                    MotionEvent.ACTION_UP -> if (!moved) toggle()
                }
                return true
            }
        })

        root = container
        wm?.addView(container, lp)
    }

    override fun onDestroy() {
        scope.cancel()
        root?.let { try { wm?.removeView(it) } catch (_: Exception) {} }
        speaker?.shutdown()
        super.onDestroy()
    }
}
