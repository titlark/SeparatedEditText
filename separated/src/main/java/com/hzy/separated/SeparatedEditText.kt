package com.hzy.separated

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.text.InputFilter
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.IntDef
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import java.util.*

/**
 * 分离的EditText
 */
class SeparatedEditText @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    AppCompatEditText(context, attrs, defStyleAttr) {
    private lateinit var mBorderPaint: Paint //边界画笔
    private lateinit var mBlockPaint: Paint//实心块画笔
    private lateinit var mTextPaint: Paint
    private lateinit var mCursorPaint: Paint
    private lateinit var mBorderRectF: RectF
    private lateinit var mBoxRectF: RectF//小方块、小矩形

    private var mWidth = 0 //可绘制宽度 = 0
    private var mHeight = 0 //可绘制高度 = 0
    private var mBoxWidth = 0 //方块宽度 = 0
    private var mBoxHeight = 0 //方块高度 = 0
    private var mSpacing: Int//方块之间间隙

    private var mCorner: Int//圆角

    private var mMaxLength: Int//最大位数

    private var mBorderWidth: Int//边界粗细

    private var mPassword: Boolean//是否是密码类型

    private var mShowCursor: Boolean //显示光标

    private var mCursorDuration: Int//光标闪动间隔

    private var mCursorWidth: Int//光标宽度

    private var mCursorColor: Int//光标颜色

    private var mType: Int//实心方式、空心方式

    private var mHighLightEnable: Boolean // 是否显示框框高亮
    private var mHighLightStyle: Int // 高亮样式，仅支持 solid

    private var mShowKeyboard: Boolean

    private var mBorderColor: Int
    private var mBlockColor: Int
    private var mTextColor: Int
    private var mTextSize: Int // 字体大小
    private var mHighLightColor: Int // 框框高亮颜色
    private var mErrorColor: Int // 框框错误颜色

    private var mHighLightBefore = false // 待输入之前的一并高亮
    private var mIsCursorShowing = false
    private var mContentText: CharSequence = ""
    private var mTextChangedListener: TextChangedListener? = null
    private lateinit var mTimer: Timer
    private lateinit var mTimerTask: TimerTask

    private var showError = false

    fun setSpacing(spacing: Int) {
        this.mSpacing = spacing
        postInvalidate()
    }

    fun setCorner(corner: Int) {
        this.mCorner = corner
        postInvalidate()
    }

    fun setMaxLength(maxLength: Int) {
        this.mMaxLength = maxLength
        this.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
        initBox()
        clearText()
    }

    fun setBorderWidth(borderWidth: Int) {
        this.mBorderWidth = borderWidth
        postInvalidate()
    }

    fun setPassword(password: Boolean) {
        this.mPassword = password
        postInvalidate()
    }

    fun setShowCursor(showCursor: Boolean) {
        this.mShowCursor = showCursor
        postInvalidate()
    }

    fun setHighLightEnable(enable: Boolean) {
        this.mHighLightEnable = enable
        postInvalidate()
    }

    fun setCursorDuration(cursorDuration: Int) {
        this.mCursorDuration = cursorDuration
        postInvalidate()
    }

    fun setCursorWidth(cursorWidth: Int) {
        this.mCursorWidth = cursorWidth
        postInvalidate()
    }

    fun setCursorColor(cursorColor: Int) {
        this.mCursorColor = cursorColor
        postInvalidate()
    }

    fun setType(@TypeDef type: Int) {
        this.mType = type
        postInvalidate()
    }

    fun setBorderColor(borderColor: Int) {
        this.mBorderColor = borderColor
        postInvalidate()
    }

    fun setBlockColor(blockColor: Int) {
        this.mBlockColor = blockColor
        postInvalidate()
    }

    override fun setTextColor(textColor: Int) {
        this.mTextColor = textColor
        postInvalidate()
    }

    fun setTextSize(textSize: Int) {
        this.mTextSize = textSize
    }

    fun setHighLightColor(color: Int) {
        this.mHighLightColor = color
        postInvalidate()
    }

    fun setErrorColor(color: Int) {
        this.mErrorColor = color
        postInvalidate()
    }

    fun showError() {
        if (this.mType in listOf(TYPE_SOLID, TYPE_UNDERLINE)) {
            showError = true
            postInvalidate()
        }
    }

    fun setHighlightStyle(@StyleDef style: Int) {
        this.mHighLightStyle = style
        postInvalidate()
    }

    private fun init() {
        this.isFocusableInTouchMode = true
        this.isFocusable = true
        this.requestFocus()
        this.isCursorVisible = false
        this.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(mMaxLength))
        if (mShowKeyboard) {
            Handler().postDelayed({
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED)
            }, 500)
        }

        mBlockPaint = Paint().apply {
            isAntiAlias = true
            color = mBlockColor
            style = Paint.Style.FILL
            strokeWidth = 1f
        }

        mTextPaint = Paint().apply {
            isAntiAlias = true
            color = mTextColor
            textSize = textSize
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 1f
        }

        mBorderPaint = Paint().apply {
            isAntiAlias = true
            color = mBorderColor
            style = Paint.Style.STROKE
            strokeWidth = mBorderWidth.toFloat()
        }

        mCursorPaint = Paint().apply {
            isAntiAlias = true
            color = mCursorColor
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = mCursorWidth.toFloat()
        }

        mBorderRectF = RectF()
        mBoxRectF = RectF()

        if (mType == TYPE_HOLLOW) mSpacing = 0

        mTimerTask = object : TimerTask() {
            override fun run() {
                mIsCursorShowing = !mIsCursorShowing
                postInvalidate()
            }
        }
        mTimer = Timer()

        setOnLongClickListener {
            handlePaste(it)
            return@setOnLongClickListener true
        }
    }

    private fun handlePaste(view: View) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip ?: return

            if (clip.itemCount > 0) {
                val pasteText = clip.getItemAt(0).text
                PasteDialog(context, view).apply {
                    onPasteClick = { setText(pasteText) }
                    show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        initBox()
    }

    private fun initBox() {
        mBoxWidth = (mWidth - mSpacing * (mMaxLength - 1)) / mMaxLength
        mBoxHeight = mHeight
        mBorderRectF.set(0f, 0f, mWidth.toFloat(), mHeight.toFloat())
//        textPaint.textSize = boxWidth / 2.0f
        mTextPaint.textSize = if (mTextSize == 0) mBoxWidth / 2.0f else spToPx(mTextSize)
    }

    private fun spToPx(dp: Int): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dp.toFloat(), context.resources.displayMetrics)
    }

    override fun onDraw(canvas: Canvas) {
        drawRect(canvas)
        drawText(canvas, mContentText)
        drawCursor(canvas)
    }

    /**
     * 绘制光标
     *
     * @param canvas
     */
    private fun drawCursor(canvas: Canvas) {
        if (!mIsCursorShowing && mShowCursor && mContentText.length < mMaxLength && hasFocus()) {
            val cursorPosition = mContentText.length + 1
            val startX =
                mSpacing * (cursorPosition - 1) + mBoxWidth * (cursorPosition - 1) + mBoxWidth / 2
            val startY = mBoxHeight / 4
            val endY = mBoxHeight - mBoxHeight / 4
            canvas.drawLine(
                startX.toFloat(),
                startY.toFloat(),
                startX.toFloat(),
                endY.toFloat(),
                mCursorPaint
            )
        }
    }

    private fun drawRect(canvas: Canvas) {
        val currentPos = mContentText.length
        loop@ for (i in 0 until mMaxLength) {
            mBoxRectF[mSpacing * i + mBoxWidth * i.toFloat(), 0f, mSpacing * i + mBoxWidth * i + mBoxWidth.toFloat()] =
                mBoxHeight.toFloat()
            val light = mHighLightBefore.matchValue(currentPos >= i, currentPos == i)
            when (mType) {
                TYPE_SOLID -> {
                    if (showError) {
                        if (mHighLightStyle == STYLE_SOLID) {
                            canvas.drawRoundRect(
                                mBoxRectF,
                                mCorner.toFloat(),
                                mCorner.toFloat(),
                                mBlockPaint.apply { color = mErrorColor })
                        } else {
                            canvas.drawRoundRect(
                                mBoxRectF,
                                mCorner.toFloat(),
                                mCorner.toFloat(),
                                mBlockPaint.apply { color = mBlockColor })
                            val tempRect = RectF(
                                mBoxRectF.left + mBorderWidth / 2,
                                mBoxRectF.top + mBorderWidth / 2,
                                mBoxRectF.right - mBorderWidth / 2,
                                mBoxRectF.bottom - mBorderWidth / 2
                            )
                            canvas.drawRoundRect(
                                tempRect,
                                mCorner.toFloat(),
                                mCorner.toFloat(),
                                mBorderPaint.apply { color = mErrorColor })
                        }
                        continue@loop
                    }
                    if (mHighLightEnable && hasFocus() && light) {
                        if (mHighLightStyle == STYLE_SOLID) {
                            canvas.drawRoundRect(
                                mBoxRectF,
                                mCorner.toFloat(),
                                mCorner.toFloat(),
                                mBlockPaint.apply { color = mHighLightColor })
                        } else {
                            canvas.drawRoundRect(
                                mBoxRectF,
                                mCorner.toFloat(),
                                mCorner.toFloat(),
                                mBlockPaint.apply { color = mBlockColor })
                            val tempRect = RectF(
                                mBoxRectF.left + mBorderWidth / 2,
                                mBoxRectF.top + mBorderWidth / 2,
                                mBoxRectF.right - mBorderWidth / 2,
                                mBoxRectF.bottom - mBorderWidth / 2
                            )
                            canvas.drawRoundRect(
                                tempRect,
                                mCorner.toFloat(),
                                mCorner.toFloat(),
                                mBorderPaint.apply { color = mHighLightColor })
                        }
                    } else {
                        canvas.drawRoundRect(
                            mBoxRectF,
                            mCorner.toFloat(),
                            mCorner.toFloat(),
                            mBlockPaint.apply { color = mBlockColor })
                        if (mHighLightStyle == STYLE_BORDER) {
                            val tempRect = RectF(
                                mBoxRectF.left + mBorderWidth / 2,
                                mBoxRectF.top + mBorderWidth / 2,
                                mBoxRectF.right - mBorderWidth / 2,
                                mBoxRectF.bottom - mBorderWidth / 2
                            )
                            canvas.drawRoundRect(
                                tempRect,
                                mCorner.toFloat(),
                                mCorner.toFloat(),
                                mBorderPaint.apply { color = mBorderColor })
                        }
                    }
//
                }
                TYPE_UNDERLINE -> {
                    if (showError) {
                        canvas.drawLine(
                            mBoxRectF.left,
                            mBoxRectF.bottom,
                            mBoxRectF.right,
                            mBoxRectF.bottom,
                            mBorderPaint.apply { color = mErrorColor })
                        continue@loop
                    }
                    canvas.drawLine(
                        mBoxRectF.left,
                        mBoxRectF.bottom,
                        mBoxRectF.right,
                        mBoxRectF.bottom,
                        mBorderPaint.apply {
                            color = (mHighLightEnable && hasFocus() && light).matchValue(
                                mHighLightColor,
                                mBorderColor
                            )
                        })

                }
                TYPE_HOLLOW -> {
                    if (i == 0 || i == mMaxLength) continue@loop
                    canvas.drawLine(
                        mBoxRectF.left,
                        mBoxRectF.top,
                        mBoxRectF.left,
                        mBoxRectF.bottom,
                        mBorderPaint.apply { color = mBorderColor })
                }

            }
        }
        if (mType == TYPE_HOLLOW) canvas.drawRoundRect(
            mBorderRectF,
            mCorner.toFloat(),
            mCorner.toFloat(),
            mBorderPaint
        )
    }

    override fun onTextChanged(
        text: CharSequence,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        showError = false
        mContentText = text
        invalidate()
        mTextChangedListener?.also {
            if (text.length == mMaxLength)
                it.textCompleted(text)
            else
                it.textChanged(text)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        //cursorFlashTime为光标闪动的间隔时间
        mTimer.scheduleAtFixedRate(mTimerTask, 0, mCursorDuration.toLong())
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mTimer.cancel()
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        return true
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        val text: CharSequence? = text
        if (text != null) {
            if (selStart != text.length || selEnd != text.length) {
                setSelection(text.length, text.length)
                return
            }
        }
        super.onSelectionChanged(selStart, selEnd)
    }

    private fun drawText(canvas: Canvas, charSequence: CharSequence) {
        for (i in charSequence.indices) {
            val startX = mSpacing * i + mBoxWidth * i
            val startY = 0
            val baseX =
                (startX + mBoxWidth / 2 - mTextPaint.measureText(charSequence[i].toString()) / 2).toInt()
            val baseY =
                (startY + mBoxHeight / 2 - (mTextPaint.descent() + mTextPaint.ascent()) / 2).toInt()
            val centerX = startX + mBoxWidth / 2
            val centerY = startY + mBoxHeight / 2
            val radius = Math.min(mBoxWidth, mBoxHeight) / 6
            if (mPassword) canvas.drawCircle(
                centerX.toFloat(),
                centerY.toFloat(),
                radius.toFloat(),
                mTextPaint
            ) else canvas.drawText(
                charSequence[i].toString(),
                baseX.toFloat(),
                baseY.toFloat(),
                mTextPaint
            )
        }
    }

    fun setTextChangedListener(listener: TextChangedListener?) {
        mTextChangedListener = listener
    }

    fun clearText() {
        setText("")
    }

    /**
     * 密码监听者
     */
    interface TextChangedListener {
        /**
         * 输入/删除监听
         *
         * @param changeText 输入/删除的字符
         */
        fun textChanged(changeText: CharSequence?)

        /**
         * 输入完成
         */
        fun textCompleted(text: CharSequence?)
    }

    companion object {
        private const val TYPE_HOLLOW = 1 //空心
        private const val TYPE_SOLID = 2 //实心
        private const val TYPE_UNDERLINE = 3 //下划线

        private const val STYLE_SOLID = 1 //实心
        private const val STYLE_BORDER = 2 // 边界

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(STYLE_SOLID, STYLE_BORDER)
        annotation class StyleDef

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(TYPE_HOLLOW, TYPE_SOLID, TYPE_UNDERLINE)
        annotation class TypeDef
    }

    init {
        setTextIsSelectable(false)
        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
                return false
            }

            override fun onDestroyActionMode(actionMode: ActionMode) {}
        }
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SeparatedEditText)
        mPassword = ta.getBoolean(R.styleable.SeparatedEditText_password, false)
        mShowCursor = ta.getBoolean(R.styleable.SeparatedEditText_showCursor, true)
        mHighLightEnable = ta.getBoolean(R.styleable.SeparatedEditText_highLightEnable, false)
        mBorderColor = ta.getColor(
            R.styleable.SeparatedEditText_borderColor,
            ContextCompat.getColor(getContext(), R.color.lightGrey)
        )
        mBlockColor = ta.getColor(
            R.styleable.SeparatedEditText_blockColor,
            ContextCompat.getColor(getContext(), R.color.purple_500)
        )
        mTextColor = ta.getColor(
            R.styleable.SeparatedEditText_textColor,
            ContextCompat.getColor(getContext(), R.color.lightGrey)
        )
        mTextSize = ta.getInt(R.styleable.SeparatedEditText_textSize, 0)
        mHighLightColor = ta.getColor(
            R.styleable.SeparatedEditText_highlightColor,
            ContextCompat.getColor(getContext(), R.color.lightGrey)
        )
        mHighLightBefore = ta.getBoolean(R.styleable.SeparatedEditText_highLightBefore, false)
        mCursorColor = ta.getColor(
            R.styleable.SeparatedEditText_cursorColor,
            ContextCompat.getColor(getContext(), R.color.lightGrey)
        )
        mCorner = ta.getDimension(R.styleable.SeparatedEditText_corner, 0f).toInt()
        mSpacing = ta.getDimension(R.styleable.SeparatedEditText_blockSpacing, 0f).toInt()
        mType = ta.getInt(R.styleable.SeparatedEditText_separateType, TYPE_HOLLOW)
        mHighLightStyle = ta.getInt(R.styleable.SeparatedEditText_highlightStyle, STYLE_SOLID)
        mMaxLength = ta.getInt(R.styleable.SeparatedEditText_maxLength, 6)
        mCursorDuration = ta.getInt(R.styleable.SeparatedEditText_cursorDuration, 500)
        mCursorWidth = ta.getDimension(R.styleable.SeparatedEditText_cursorWidth, 2f).toInt()
        mBorderWidth = ta.getDimension(R.styleable.SeparatedEditText_borderWidth, 5f).toInt()
        mShowKeyboard = ta.getBoolean(R.styleable.SeparatedEditText_showKeyboard, false)
        mErrorColor = ta.getColor(
            R.styleable.SeparatedEditText_errorColor,
            ContextCompat.getColor(getContext(), R.color.errorColor)
        )
        ta.recycle()
        init()
    }
}