package game.snake

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnLayout

class ThemeButton : View {

    var text = ""
    var drawablePrimary : Drawable? = null
    var drawableSecondary : Drawable? = null

    var foregroundColorPrimary = Color.BLACK
    var foregroundColorSecondary = Color.WHITE
    var highlightColor = Color.WHITE
    var mainColor = Color.rgb(255, 161, 43)
    var shadowColor = Color.rgb(145, 81, 0)
    var baseColor = Color.rgb(43, 24, 0)

    var runnable : Runnable? = null

    private val paint = Paint()

    private var buttonTop = 0F
    private var buttonTopHeight = 0F
    private var textHeight = 0F

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs) { init(attrs, 0) }

    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : super(ctx, attrs, defStyleAttr) { init(attrs, defStyleAttr) }

    init {
        doOnLayout {
            buttonTopHeight = 4F * height / 5 - 1
            buttonTop = 0F
            //text
            paint.typeface = ResourcesCompat.getFont(context, R.font.arcade_font)
            paint.textSize = 0.4666667f * height
            val metrics = paint.fontMetrics
            textHeight = metrics.ascent + metrics.descent + metrics.leading
            //drawables
            if (drawablePrimary != null) {
                //cloning the drawable to the second drawable that is going to be behind the primary drawable
                drawableSecondary = drawablePrimary?.constantState?.newDrawable()?.mutate()
                //initializing the bounds and color of the primary drawable
                drawablePrimary?.setBounds((width / 8F).toInt(), (buttonTop + buttonTopHeight / 8F).toInt(), (7 * width / 8F).toInt(), (buttonTop + 7 * buttonTopHeight / 8F).toInt())
                drawablePrimary?.colorFilter = PorterDuffColorFilter(foregroundColorPrimary, PorterDuff.Mode.SRC_ATOP)
                //initializing the bounds and color of the secondary drawable
                drawableSecondary?.setBounds((width / 8F).toInt(), (buttonTop + buttonTopHeight / 8F + height / 90F).toInt(), (7 * width / 8F).toInt(), (buttonTop + 7 * buttonTopHeight / 8F + height / 90F).toInt())
                drawableSecondary?.colorFilter = PorterDuffColorFilter(foregroundColorSecondary, PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

    private fun init(attrs: AttributeSet, defStyleAttr: Int) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ThemeButton, defStyleAttr, 0)
        val n = ta.indexCount
        for (i in 0 until n) {
            when (val attr = ta.getIndex(i)) {
                R.styleable.ThemeButton_android_text -> {
                    text = ta.getText(attr) as String
                }
                R.styleable.ThemeButton_android_drawable -> {
                    drawablePrimary = ta.getDrawable(attr)
                }
                R.styleable.ThemeButton_foregroundColorPrimary -> {
                    foregroundColorPrimary = ta.getColor(attr, foregroundColorPrimary)
                }
                R.styleable.ThemeButton_foregroundColorSecondary -> {
                    foregroundColorSecondary = ta.getColor(attr, foregroundColorSecondary)
                }
                R.styleable.ThemeButton_highlightColor -> {
                    highlightColor = ta.getColor(attr, highlightColor)
                }
                R.styleable.ThemeButton_mainColor -> {
                    mainColor = ta.getColor(attr, mainColor)
                }
                R.styleable.ThemeButton_shadowColor -> {
                    shadowColor = ta.getColor(attr, shadowColor)
                }
                R.styleable.ThemeButton_baseColor -> {
                    baseColor = ta.getColor(attr, baseColor)
                }
            }
        }
        ta.recycle()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) return
        paint.color = baseColor
        canvas.drawRoundRect(0F, height / 15f, width - 1F, height - 1F, height / 15f, height / 15f, paint)
        paint.color = shadowColor
        canvas.drawRoundRect(width / 75F, height / 2F, 74 * width / 75F, 19 * height / 20F - 1, height / 15F, height / 15F, paint)
        paint.color = highlightColor
        canvas.drawRoundRect(width / 75F, buttonTop, 74 * width / 75F, buttonTopHeight + buttonTop, height / 15F, height / 15F, paint)
        paint.color = mainColor
        canvas.drawRoundRect(width / 75F, buttonTop + height / 50F, 74 * width / 75F, buttonTopHeight + buttonTop, height / 15F, height / 15F, paint)
        paint.color = foregroundColorSecondary
        canvas.drawText(text, (width - paint.measureText(text)) / 2F, buttonTop + (buttonTopHeight - textHeight) / 2F + height / 90F, paint)
        paint.color = foregroundColorPrimary
        canvas.drawText(text, (width - paint.measureText(text)) / 2F, buttonTop + (buttonTopHeight - textHeight) / 2F, paint)
        drawableSecondary?.draw(canvas)
        drawablePrimary?.draw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        if (e == null) return true
        when(e.action) {
            MotionEvent.ACTION_DOWN -> {
                buttonTop = height / 8F
                drawablePrimary?.setBounds((width / 8F).toInt(), (buttonTop + buttonTopHeight / 8F).toInt(), (7 * width / 8F).toInt(), (buttonTop + 7 * buttonTopHeight / 8F).toInt())
                drawableSecondary?.setBounds((width / 8F).toInt(), (buttonTop + buttonTopHeight / 8F + height / 90F).toInt(), (7 * width / 8F).toInt(), (buttonTop + 7 * buttonTopHeight / 8F + height / 90F).toInt())
            }
            MotionEvent.ACTION_UP -> {
                buttonTop = 0F
                drawablePrimary?.setBounds((width / 8F).toInt(), (buttonTop + buttonTopHeight / 8F).toInt(), (7 * width / 8F).toInt(), (buttonTop + 7 * buttonTopHeight / 8F).toInt())
                drawableSecondary?.setBounds((width / 8F).toInt(), (buttonTop + buttonTopHeight / 8F + height / 90F).toInt(), (7 * width / 8F).toInt(), (buttonTop + 7 * buttonTopHeight / 8F + height / 90F).toInt())
            }
        }
        postInvalidate()
        return true
    }
}