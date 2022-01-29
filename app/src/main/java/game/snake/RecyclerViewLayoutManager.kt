package game.snake

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class RecyclerViewLayoutManager(ctx : Context, private val recyclerView: RecyclerView) : LinearLayoutManager(ctx) {

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            val recyclerViewCenterX = (recyclerView.right - recyclerView.left) / 2 + recyclerView.left
            var minDistance = recyclerView.width
            var position = -1
            for (i in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(i)
                val childCenterX = getDecoratedLeft(child) + (getDecoratedRight(child) - getDecoratedLeft(child)) / 2
                val childDistanceFromCenter = abs(childCenterX - recyclerViewCenterX)
                if (childDistanceFromCenter < minDistance) {
                    minDistance = childDistanceFromCenter
                    position = recyclerView.getChildLayoutPosition(child)
                }
            }
            if (recyclerView is OptionsRecyclerView)
                recyclerView.itemSelected(position)
        }
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        if (orientation == RecyclerView.HORIZONTAL) {
            val scrolled = super.scrollHorizontallyBy(dx, recycler, state)
            changeAlpha()
            return scrolled
        }
        return 0
    }

    private fun changeAlpha() {
        val mid = width / 2F
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child != null && child.x > mid - child.width && child.x < mid)
                child.alpha = 1F
            else
                child?.alpha = 0.6F
        }
    }
}