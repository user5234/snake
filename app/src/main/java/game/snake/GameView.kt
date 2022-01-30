package game.snake

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.view.doOnLayout
import java.util.*
import kotlin.concurrent.thread

private const val FPS = 30F

/**
 * note: because everything is contained in this view, all the points are relative to this view and
 * NOT to the actual screen of the device
 *
 * that means that Point(0, 0) is the top left of THIS view, and not the devices view
 */
class GameView : View {

    /**
     * interface that represents an option / parameter for a new game
     */
    interface GameOption

    enum class MapSize : GameOption {
        SMALL {
            override val squaresAmount: Int
                get() = 120
        },
        NORMAL {
            override val squaresAmount: Int
                get() = 252
        },
        LARGE {
            override val squaresAmount: Int
                get() = 350
        };

        abstract val squaresAmount : Int
    }

    enum class Speed : GameOption {
        SLOW {
            override val moveTime: Long
                get() = 220L
        },
        NORMAL {
            override val moveTime: Long
                get() = 180L
        },
        FAST {
            override val moveTime: Long
                get() = 140L
        };

        abstract val moveTime : Long
    }

    enum class ApplesAmount : GameOption {
        ONE {
            override val amount: Int
                get() = 1
        },
        THREE {
            override val amount: Int
                get() = 3
        },
        FIVE{
            override val amount: Int
                get() = 5
        };

        abstract val amount : Int
    }

    var unitSize = 0; private set
    var framesPerMove = 0; private set
    var emptySquares = mutableListOf<Point>(); private set

    private val apples = Apples(this)
    private val snake = Snake(this, apples)

    private var timer = Timer()
    private var frameNumber = 0
    private var score = 0
    private var moveTime = 0L
    private var changedDirection = false //to have only one direction change per move
    private var gameOver = false
    private var won = false

    private lateinit var bgBitmap : Bitmap
    private lateinit var allSquares : Array<Point>

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : super(ctx, attrs, defStyleAttr)

    init {
        doOnLayout {
            newGame(MainActivity.instance.getLast(), MainActivity.instance.getLast(), MainActivity.instance.getLast())
        }
    }

    fun newGame(mapSize: MapSize, speed: Speed, applesAmount: ApplesAmount) {
        fun findUnitSize(targetSquaresAmount: Int): Int {
            var unitSize: Int
            var y: Float
            var x = 2F
            //loop while incrementing x(width) and y(height) while keeping their proportions, until going over the target squares amount
            do {
                x++
                unitSize = (width / x).toInt()
                y = (height - unitSize).toFloat() / unitSize
            } while (y * (x - 2) <= targetSquaresAmount)
            //check if you should go one step backwards (if it's closer to the target squares amount)
            if (y.toInt() * (x - 2) - targetSquaresAmount > targetSquaresAmount - ((height - width / (x - 1)).toInt() / (width / (x - 1))) * (x - 3))
                unitSize = (width / (x - 1)).toInt()
            return unitSize
        }
        moveTime = speed.moveTime
        unitSize = findUnitSize(mapSize.squaresAmount)
        framesPerMove = (FPS * moveTime / 1000F).toInt()
        bgBitmap = Background.get(width, height, unitSize)
        allSquares = Array((width / unitSize - 2) * (height / unitSize - 1)) {
            i ->
            val widthInSquares = width / unitSize - 2
            Point((i % widthInSquares + 1) * unitSize, i / widthInSquares * unitSize)
        }
        apples.initialize(applesAmount.amount)
        snake.initialize(speed)
        gameOver = false
        frameNumber = 0
        score = 0
        postInvalidate()
    }

    fun gameOver(won: Boolean) {
        timer.cancel()
        this.won = won
        this.gameOver = true
        if (won) return
        frameNumber = framesPerMove
        timer = Timer()
        timer.scheduleAtFixedRate( object : TimerTask() {
            override fun run() {
                frameNumber--
                if (frameNumber == -1) {
                    frameNumber++
                }
                postInvalidate()
            }
        }, moveTime / framesPerMove, moveTime / framesPerMove)
    }

    fun incrementScore() {
        score++
        post { (parent as View).findViewById<TextView>(R.id.scoreText).text = "$score" }
    }

    fun pause() { if (!gameOver) timer.cancel() }

    fun resume() { if (!gameOver) timer.cancel() }

    private fun startTimer() {
        frameNumber = 0
        thread {
            timer = Timer()
            timer.scheduleAtFixedRate( object : TimerTask() {
                override fun run() {
                    frameNumber++
                    if (frameNumber == framesPerMove) {
                        frameNumber = 0
                        changedDirection = false
                        emptySquares = allSquares.toMutableList()
                        snake.move()
                    }
                    postInvalidate()
                }
            }, moveTime / framesPerMove, moveTime / framesPerMove)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas != null) {
            canvas.drawBitmap(bgBitmap, 0F, 0F, null)
            snake.draw(canvas, frameNumber)
            apples.draw(canvas)
        }
    }

    private var touchX = 0F
    private var touchY = 0F
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        if (e == null || gameOver)
            return true
        fun changeDirection(direction : Snake.Direction) {
            if (!changedDirection && snake.setDirection(direction, frameNumber)) {
                changedDirection = true
            }
            touchX = e.x
            touchY = e.y
        }
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = e.x
                touchY = e.y
            }
            MotionEvent.ACTION_MOVE -> {
                when {
                    //swipe left
                    touchX - e.x > width / 15F -> changeDirection(Snake.Direction.LEFT)
                    //swipe up
                    touchY - e.y > width / 15F -> changeDirection(Snake.Direction.UP)
                    //swipe right
                    e.x - touchX > width / 15F -> {
                        if (!gameOver) {
                            startTimer()
                        }
                        changeDirection(Snake.Direction.RIGHT)
                    }
                    //swipe down
                    e.y - touchY > width / 15F -> changeDirection(Snake.Direction.DOWN)
                }
            }
        }
        return true
    }
}