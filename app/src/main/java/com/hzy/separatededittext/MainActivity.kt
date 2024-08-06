package com.hzy.separatededittext

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.hzy.separated.SeparatedEditText
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    var showContent = false
    var showCursor = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCursor.setOnClickListener(this)
        btnContent.setOnClickListener(this)

        edit_solid.setTextChangedListener(object : SeparatedEditText.TextChangedListener {
            override fun textChanged(changeText: CharSequence?) {
            }

            override fun textCompleted(text: CharSequence?) {
                edit_solid.showError()
            }

        })
        edit_underline.setTextChangedListener(object : SeparatedEditText.TextChangedListener {
            override fun textChanged(changeText: CharSequence?) {
            }

            override fun textCompleted(text: CharSequence?) {
                edit_underline.showError()
            }

        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnCursor -> {
                handleCursor()
            }
            R.id.btnContent -> {
                handleContent()
            }
        }
    }

    private fun handleCursor() {
        edit_solid.setShowCursor(!showCursor)
        edit_hollow.setShowCursor(!showCursor)
        edit_underline.setShowCursor(!showCursor)
        showCursor = !showCursor
    }

    private fun handleContent() {
        edit_solid.setPassword(!showContent)
        edit_hollow.setPassword(!showContent)
        edit_underline.setPassword(!showContent)
        showContent = !showContent
    }
}