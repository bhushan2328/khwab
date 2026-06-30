package com.toblad.khwab.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.toblad.khwab.R

class FloatingWindow(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var floatingView: View? = null

    fun show() {

        if (floatingView != null) return

        floatingView = LayoutInflater.from(context)
            .inflate(R.layout.floating_mic, null)

        val params = WindowManager.LayoutParams(

            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,

            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,

            PixelFormat.TRANSLUCENT

        )

        params.gravity = Gravity.TOP or Gravity.START

        params.x = 100
        params.y = 300

        windowManager.addView(floatingView, params)

    }

    fun hide() {

        floatingView?.let {

            windowManager.removeView(it)

            floatingView = null

        }

    }

}