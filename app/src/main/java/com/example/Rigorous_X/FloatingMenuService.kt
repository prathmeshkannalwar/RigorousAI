package com.example.Rigorous_X

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import kotlin.math.abs

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter

class FloatingMenuService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingWidget: View
    private lateinit var params: WindowManager.LayoutParams

    private lateinit var collapsedView: View
    private lateinit var expandedView: LinearLayout
    // The loadingIndicator and serviceStateReceiver properties have been removed.

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        val contextWithTheme = ContextThemeWrapper(this, R.style.Theme_Rigorous_X)
        floatingWidget = LayoutInflater.from(contextWithTheme).inflate(R.layout.floating_widget_layout, null)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val buttonWidthApproximation = (150 * metrics.density).toInt()
        params.x = screenWidth - buttonWidthApproximation
        params.y = (screenHeight / 2) - buttonWidthApproximation

        windowManager.addView(floatingWidget, params)

        collapsedView = floatingWidget.findViewById(R.id.btn_collapsed_menu)
        expandedView = floatingWidget.findViewById(R.id.layout_expanded_menu)
        // findViewById for loading_indicator is removed.

        // Broadcast receiver registration is removed.
        setupTouchListener()
        setupMenuClickListeners()
    }

    private fun setupMenuClickListeners() {
        // Define the color you want for the icons
        val iconColor = Color.parseColor("#000000") // Black

        // --- NEW CHAT BUTTON ---
        val newChatButton = floatingWidget.findViewById<ImageButton>(R.id.btn_new_chat)
        newChatButton.drawable.colorFilter =
            PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        newChatButton.setOnClickListener {
            sendBroadcast(Intent(MainActivity.ACTION_NEW_CHAT).setPackage(packageName))
            collapseMenu()
        }

        // --- COPY RESPONSE BUTTON ---
        val copyButton = floatingWidget.findViewById<ImageButton>(R.id.btn_copy_response)
        copyButton.drawable.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        copyButton.setOnClickListener {
            sendBroadcast(Intent(MainActivity.ACTION_COPY_RESPONSE).setPackage(packageName))
            collapseMenu()
        }

        // --- SHARE RESPONSE BUTTON ---
        val shareButton = floatingWidget.findViewById<ImageButton>(R.id.btn_share_response)
        shareButton.drawable.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        shareButton.setOnClickListener {
            sendBroadcast(Intent(MainActivity.ACTION_SHARE_RESPONSE).setPackage(packageName))
            collapseMenu()
        }

        // --- SETTINGS BUTTON ---
        val settingsButton = floatingWidget.findViewById<ImageButton>(R.id.btn_settings)
        settingsButton.drawable.colorFilter =
            PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        settingsButton.setOnClickListener {
            sendBroadcast(Intent(MainActivity.ACTION_OPEN_SETTINGS).setPackage(packageName))
            collapseMenu()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f
        floatingWidget.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && expandedView.visibility == View.VISIBLE) {
                collapseMenu()
                return@setOnTouchListener true
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { initialX = params.x; initialY = params.y; initialTouchX = event.rawX; initialTouchY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> { params.x = initialX + (event.rawX - initialTouchX).toInt(); params.y = initialY + (event.rawY - initialTouchY).toInt(); windowManager.updateViewLayout(floatingWidget, params); true }
                MotionEvent.ACTION_UP -> {
                    if (abs(event.rawX - initialTouchX) < 10 && abs(event.rawY - initialTouchY) < 10) expandMenu()
                    else {
                        val screenWidth = resources.displayMetrics.widthPixels
                        val finalX = if (params.x > screenWidth / 2) screenWidth - floatingWidget.width else 0
                        ValueAnimator.ofInt(params.x, finalX).apply {
                            duration = 200
                            addUpdateListener { params.x = it.animatedValue as Int; windowManager.updateViewLayout(floatingWidget, params) }
                            start()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun collapseMenu() {
        if (expandedView.visibility == View.VISIBLE) {
            expandedView.visibility = View.GONE
            collapsedView.visibility = View.VISIBLE
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            windowManager.updateViewLayout(floatingWidget, params)
        }
    }

    private fun expandMenu() {
        if (expandedView.visibility != View.VISIBLE) {
            collapsedView.visibility = View.GONE
            expandedView.visibility = View.VISIBLE
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            windowManager.updateViewLayout(floatingWidget, params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingWidget.isInitialized) windowManager.removeView(floatingWidget)
    }
}