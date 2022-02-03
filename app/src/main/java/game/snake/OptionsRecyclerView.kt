package game.snake

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

class OptionsRecyclerView : RecyclerView {

    var items = listOf<Item>()
        set(value) {
            field = value
            LinearSnapHelper().attachToRecyclerView(this)
            val recyclerViewAdapter = RecyclerViewAdapter()
            recyclerViewAdapter.items = value
            this.adapter = recyclerViewAdapter

            val recyclerViewLayoutManager = RecyclerViewLayoutManager(context, this)
            recyclerViewLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
            this.layoutManager = recyclerViewLayoutManager

            scrollToInitOptions()
        }

    private var position = 0

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : super(ctx, attrs, defStyleAttr)

    init {
        doOnLayout {
            val parent = parent
            if (parent is View)
                setPadding(parent.width / 2 - height / 2, 0, parent.width / 2 - height / 2, 0)
        }
    }

    fun viewClicked(v : View) { this.smoothScrollToPosition(this.getChildLayoutPosition(v)); }

    fun itemSelected(position : Int) { this.position = position; }

    fun getSelectedItem(): GameView.GameOption = items[position].option

    private fun scrollToInitOptions() {
        for (i in items.indices) {
            GameView.GameOption::class.sealedSubclasses.forEach {
                if (items[i].option == MainActivity.instance.getLast(it)) smoothScrollToPosition(i)
            }
        }
    }

    class Item (@DrawableRes val imageRes : Int, val option: GameView.GameOption)
}