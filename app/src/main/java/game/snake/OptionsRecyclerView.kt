package game.snake

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView

class OptionsRecyclerView : RecyclerView {

    private val Items = mutableListOf<Item>()

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : super(ctx, attrs, defStyleAttr)

    init {

    }

    private class Item (@DrawableRes private val imageRes : Int, private val value : Int) {}
}