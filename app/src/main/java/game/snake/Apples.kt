package game.snake

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.AnimatedVectorDrawable
import kotlin.random.Random

class Apples(val gameView: GameView) {

    private val apples = mutableListOf<Apple>()

    private var applesAmount = 0

    /**
     * assumes 0 < [applesAmount] <= 5
     */
    fun initialize(applesAmount : Int) {
        this.applesAmount = applesAmount
        apples.clear()
        for (i in 0 until applesAmount) {
            apples.add(Apple(i + 1))
            apples[i].initialize()
        }
    }

    fun getApplePositions() : MutableList<Point> {
        val positions = mutableListOf<Point>()
        for (a : Apple in apples)
            positions.add(Point(a.x, a.y))
        return positions
    }

    /**
     * this function should be called from the snake after finding an apple in [getApplePositions]
     * that has the snakes head [x] and [y] positions
     */
    fun changeAppleLocation(x : Int, y : Int) {
        for (a : Apple in apples)
            if (a.x == x && a.y == y) {
                a.changeLocation()
                return
            }
    }

    fun draw(canvas: Canvas) {
        for (a : Apple in apples) {
            a.drawable.draw(canvas)
        }
    }

    private fun removeApple(apple: Apple) {
        apples.remove(apple)
        applesAmount--
        if (applesAmount == 0)
            gameView.gameOver(true)
    }

    private inner class Apple(private val pos: Int) {

        var x = 0; private set
        var y = 0; private set
        var drawable = Drawables.get(R.drawable.game_donut_animation) as AnimatedVectorDrawable; private set

        private val u : Int = gameView.unitSize

        fun initialize() {
            drawable.start()
            val widthInSquares = this@Apples.gameView.width / u
            val initY = (this@Apples.gameView.height / 2) / u * u
            when(pos) {
                1 -> {
                    x = (widthInSquares - 5) * u
                    y = initY
                }
                2 -> {
                    x = (widthInSquares - 3) * u
                    y = initY + 2 * u
                }
                3 -> {
                    x = (widthInSquares - 3) * u
                    y = initY - 2 * u
                }
                4 -> {
                    x = (widthInSquares - 7) * u
                    y = initY + 2 * u
                }
                5 -> {
                    x = (widthInSquares - 7) * u
                    y = initY - 2 * u
                }
            }
            drawable.setBounds((x - u / 4F).toInt(), (y - u / 4F).toInt(), (x + 1.25F * u).toInt(), (y + 1.25F * u).toInt())
        }

        fun changeLocation() {
            val emptySquares = gameView.emptySquares
            if (emptySquares.size > 0) {
                val i = Random.nextInt(emptySquares.size)
                x = emptySquares[i].x
                y = emptySquares[i].y
                drawable.reset()
                drawable.start()
                drawable.setBounds((x - u / 4F).toInt(), (y - u / 4F).toInt(), (x + 1.25F * u).toInt(), (y + 1.25F * u).toInt())
                return
            }
            this@Apples.removeApple(this)
        }
    }
}