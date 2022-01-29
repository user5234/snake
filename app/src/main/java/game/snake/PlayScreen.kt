package game.snake

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.core.view.doOnLayout

class PlayScreen : ConstraintLayout {

    private lateinit var optionsMenu : View

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : super(ctx, attrs, defStyleAttr)

    init {
        doOnLayout {
            optionsMenu = LayoutInflater.from(context).inflate(R.layout.options_menu, findViewById(R.id.playView), false)
            //buttons in the play screen
            findViewById<ThemeButton>(R.id.playButton).runnable = Runnable { remove() }
            findViewById<ThemeButton>(R.id.settingsButton).runnable = Runnable { addOptionsMenu() }
            //recycler views in the options menu
            optionsMenu.findViewById<OptionsRecyclerView>(R.id.speedRecycleView).items = listOf(
                OptionsRecyclerView.Item(R.drawable.options_speed_slow, GameView.Speed.SLOW),
                OptionsRecyclerView.Item(R.drawable.options_speed_normal, GameView.Speed.NORMAL),
                OptionsRecyclerView.Item(R.drawable.options_speed_fast, GameView.Speed.FAST)
            )
            optionsMenu.findViewById<OptionsRecyclerView>(R.id.mapSizeRecyclerView).items = listOf(
                OptionsRecyclerView.Item(R.drawable.options_map_size_small, GameView.MapSize.SMALL),
                OptionsRecyclerView.Item(R.drawable.options_map_size_normal, GameView.MapSize.NORMAL),
                OptionsRecyclerView.Item(R.drawable.options_map_size_large, GameView.MapSize.LARGE)
            )
            optionsMenu.findViewById<OptionsRecyclerView>(R.id.applesAmountRecyclerView).items = listOf(
                OptionsRecyclerView.Item(R.drawable.options_1_donut, GameView.ApplesAmount.ONE),
                OptionsRecyclerView.Item(R.drawable.options_3_donuts, GameView.ApplesAmount.THREE),
                OptionsRecyclerView.Item(R.drawable.options_5_donuts, GameView.ApplesAmount.FIVE)
            )
            //button in the options menu
            optionsMenu.findViewById<ThemeButton>(R.id.applySettingsButton).runnable = Runnable { removeOptionsMenu() }
        }
    }

    fun add() {
        postDelayed( {
            findViewById<ThemeButton>(R.id.playButton).text = "PLAY  AGAIN"
            (context as MainActivity).findViewById<ViewGroup>(R.id.mainLayout).addView(this)
            val alphaAnimation = AlphaAnimation(0F, 1F)
            val scaleAnimation = ScaleAnimation(0F, 1F, 0F, 1F, Animation.RELATIVE_TO_SELF, 0.5F, Animation.RELATIVE_TO_SELF, 0.5F)
            scaleAnimation.interpolator = PathInterpolatorCompat.create(0.725F, 0F, 0.195F, 1.460F)
            alphaAnimation.fillAfter = true
            alphaAnimation.duration = 500
            scaleAnimation.fillAfter = true
            scaleAnimation.duration = 500
            startAnimation(alphaAnimation)
            findViewById<View>(R.id.playView).startAnimation(scaleAnimation)
        } , 1000)
    }

    private fun remove() {
        post {
            val alphaAnimation = AlphaAnimation(1F, 0F)
            alphaAnimation.duration = 500
            alphaAnimation.startOffset = 300
            alphaAnimation.fillAfter = true
            val scaleAnimation = ScaleAnimation(1F, 0F, 1F, 0F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
            scaleAnimation.duration = 500
            scaleAnimation.startOffset = 300
            scaleAnimation.fillAfter = true
            startAnimation(alphaAnimation)
            findViewById<View>(R.id.playView).startAnimation(scaleAnimation)
            (parent as ViewGroup).removeView(this)
        }
    }

    private fun addOptionsMenu() {
        findViewById<ViewGroup>(R.id.playView).addView(optionsMenu)
        val alphaAnimation = AlphaAnimation(0F, 1F);
        alphaAnimation.startOffset = 100;
        alphaAnimation.duration = 100;
        alphaAnimation.fillAfter = true;
        optionsMenu.startAnimation(alphaAnimation);
    }

    private fun removeOptionsMenu() {
        val alphaAnimation = AlphaAnimation(1F, 0F)
        alphaAnimation.startOffset = 170
        alphaAnimation.duration = 100
        alphaAnimation.fillAfter = true
        optionsMenu.startAnimation(alphaAnimation)
        (context as MainActivity).findViewById<GameView>(R.id.gameView).newGame(
            findViewById<OptionsRecyclerView>(R.id.mapSizeRecyclerView).getSelectedItem() as GameView.MapSize,
            findViewById<OptionsRecyclerView>(R.id.speedRecycleView).getSelectedItem() as GameView.Speed,
            findViewById<OptionsRecyclerView>(R.id.applesAmountRecyclerView).getSelectedItem() as GameView.ApplesAmount
        )
        findViewById<ViewGroup>(R.id.playView).removeView(optionsMenu)
    }
}