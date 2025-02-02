package com.hzy.separated

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat

class PasteDialog(context: Context, private val view: View) : Dialog(context) {


    var onPasteClick: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_paste)
    }

    override fun onStart() {
        super.onStart()
        val location = IntArray(2)
        view.getLocationInWindow(location) //获取在当前窗口内的绝对坐标

        window?.apply {
            setDimAmount(0.2f)
            attributes = attributes.apply {
                x = location[0]
                y = location[1] + (view.height * 0.65).toInt()
            }
            setGravity(Gravity.TOP or Gravity.START)
            decorView.apply {
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                setPadding(0, 0, 0, 0)
            }
        }

        findViewById<TextView>(R.id.text).setOnClickListener {
            onPasteClick?.invoke()
            dismiss()
        }
    }
}