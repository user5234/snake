package game.snake

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View

class ThemeButton : View {

    var text = ""
    var drawable : Drawable? = null

    var textColor = 0
    var mainColor = 0
    var shadowColor = 0
    var highlightColor = 0
    var baseColor = 0

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : super(ctx, attrs, defStyleAttr)

    private fun init(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) {
//        val ta = ctx.obtainStyledAttributes(attrs, R.styleable.ThemeButton, defStyleAttr)
    }
}