package game.snake

import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.RotateDrawable
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

//private val TAIL_COLOR = Color.rgb(85, 104, 201)
//private val HEAD_COLOR = Color.rgb(194, 68, 189)

private const val HEAD_COLOR = Color.BLACK
private const val TAIL_COLOR = Color.WHITE

private val KNOCKED_OUT_GIF = Drawables.get(R.drawable.knocked_out_gif) as AnimationDrawable
private val NORMAL_HEAD = Drawables.get(R.drawable.snake_head_normal_rotate) as RotateDrawable
private val ATE_APPLE_HEAD = Drawables.get(R.drawable.snake_head_on_apple_rotate) as RotateDrawable
private val FAILED_HEAD = Drawables.get(R.drawable.snake_head_on_fail_rotate) as RotateDrawable


class Snake(private val gameView: GameView, private val apples: Apples) {

    enum class Direction {
        LEFT {
            override val opposite: Direction
                get() = RIGHT
        },
        UP {
            override val opposite: Direction
                get() = DOWN
        },
        RIGHT {
            override val opposite: Direction
                get() = LEFT
        },
        DOWN {
            override val opposite: Direction
                get() = UP
        };

        abstract val opposite: Direction
    }

    private val x = mutableListOf<Int>()
    private val y = mutableListOf<Int>()
    private val turns = mutableListOf<Point>()
    private val path = Path()
    private val bgPaint = Paint()
    private val sweepGradients = mutableListOf<Paint>()
    private val linearGradients = mutableListOf<Paint>()

    private var direction = Direction.RIGHT
    private var prevDirection = Direction.RIGHT // only used to rotate the head after failing
    private var bodyParts = 0
    private var turnsAmount = 0
    private var textTime = 0
    private var moveTime = 0L
    private var ateApple = true
    private var u = gameView.unitSize
    private var distancePerFrame = 0F
    private var degreesPerFrame = 0F

    /**
     * initializes all the snakes parameters in order to prepare it for
     * a new game
     *
     * this method should always be called at the start of a new game,
     * after initializing all the variables needed in the GameView
     */
    fun initialize(moveTime : Long) {
        //set the size variables to match the current Map Size
        u = gameView.unitSize
        distancePerFrame = u.toFloat() / gameView.framesPerMove
        degreesPerFrame = 90F / gameView.framesPerMove
        //clear the lists
        x.clear()
        y.clear()
        turns.clear()
        sweepGradients.clear()
        linearGradients.clear()
        //initialize the basic variables
        this.moveTime = moveTime
        direction = Direction.RIGHT
        prevDirection = Direction.RIGHT
        bodyParts = 4
        textTime = 0
        ateApple = true
        NORMAL_HEAD.fromDegrees = 90F
        NORMAL_HEAD.toDegrees = 90F
        NORMAL_HEAD.level = 1
        //snake bitmap, shader with the background bitmap and canvas
        bgPaint.shader = (BitmapShader(
            Background.get(gameView.width, gameView.height, u),
            Shader.TileMode.CLAMP,
            Shader.TileMode.CLAMP
        ))
        //set initial position
        val initX = (gameView.width / 3) / u * u + u
        val initY = (gameView.height / 2) / u * u
        for (i in 0..bodyParts + 1) {
            x.add(initX - i * u)
            y.add(initY)
        }
        //add the turns to the turns list after initializing the position
        addTurns()
    }

    /**
     * moves the snake
     *
     * removes all the non empty points from the [GameView.emptySquares]
     *
     * checks if failed (snake collided with itself or the walls) and if so calls [GameView.gameOver]
     *
     * checks if an apple has been eaten and if so moves it
     */
    fun move() {
        ateApple = false
        textTime--
        val nonEmptyPoints = mutableListOf<Point>()
        //always have 2 'invisible' body parts behind
        for (i in bodyParts + 1 downTo 1) {
            x[i] = x[i - 1]
            y[i] = y[i - 1]
            //add the visible body parts to the list
            if (i < bodyParts) nonEmptyPoints.add(Point(x[i], y[i]))
        }
        //move the head in the current direction
        when (direction) {
            Direction.LEFT -> x[0] = x[0] - u
            Direction.UP -> y[0] = y[0] - u
            Direction.RIGHT -> x[0] = x[0] + u
            Direction.DOWN -> y[0] = y[0] + u
        }
        //add the head and apples to the list
        nonEmptyPoints.add(Point(x[0], y[0]))
        val applePositions = apples.getApplePositions()
        nonEmptyPoints.addAll(applePositions)
        //remove all the non empty point from the empty points list in GameView
        gameView.emptySquares.removeAll(nonEmptyPoints)
        //check if the snake collided with itself or the walls
        if (failed()) {
            //call gameOver and return right away so that changeAppleLocation wont cause a game over event as a win,
            //and so that addTurns function is never called so the drawing of the snake after fail
            //will stay in place
            gameView.post { setDirection(prevDirection, 0) }
            gameView.gameOver(false)
            return
        }
        //set the previous direction to direction only AFTER checking if failed, so that if this is the first move
        //after changing directions and we failed, the value of prevDirection is the direction before changing
        prevDirection = direction
        //check if the snake head is on an apple and if so, change the apples location and adds to body parts
        for (p: Point in applePositions)
            if (p.x == x[0] && p.y == y[0]) {
                apples.changeAppleLocation(x[0], y[0])
                ateApple = true
                bodyParts++
                textTime = 6
                gameView.incrementScore()
                x.add(0); y.add(0)
            }
        //add the turns to the turns list after moving
        addTurns()
    }

    fun setDirection(direction: Direction, frameNumber: Int): Boolean {
        if (direction.opposite != this.direction && direction != this.direction) {
            rotateHead(direction, frameNumber)
            prevDirection = this.direction
            this.direction = direction
            return true
        }
        return false
    }

    /**
     * basically wizardry
     */
    fun draw(canvas: Canvas, frameNumber: Int) {
        if (frameNumber % 2 == 0) addGradients(frameNumber)
        //bounds for the head drawable
        val imgDstRect = Rect()
        //width at the head
        val headWidth = 3 * u / 4F
        //width at the tail (minimum is headWidth - 6u/15, at 60 or more body parts)
        val tailWidth = headWidth - min(bodyParts, 60) * u / 150
        val headDistance = (frameNumber + 1) * distancePerFrame
        var distance = frameNumber * distancePerFrame
        var degrees = frameNumber * degreesPerFrame
        var width = tailWidth //grows up to head width at the end of the drawing cycle

        //
        //----------------------------------------------------------------------helper methods to reduce duplicate code!------------------------------------------------
        fun pathRight(i: Int) {
            width += (headWidth - tailWidth) / bodyParts
            path.reset()
            path.moveTo(turns[i].x.toFloat() + u, turns[i].y + (u - width) / 2)
            path.lineTo(turns[i].x.toFloat() + u, turns[i].y + u - (u - width) / 2)
            width += (headWidth - tailWidth) / bodyParts * (turns[i + 1].x - turns[i].x - u) / u
            path.lineTo(turns[i + 1].x.toFloat(), turns[i + 1].y + u - (u - width) / 2)
            path.lineTo(turns[i + 1].x.toFloat(), turns[i + 1].y + (u - width) / 2)
            path.close()
        }

        fun pathLeft(i: Int) {
            width += (headWidth - tailWidth) / bodyParts
            path.reset()
            path.moveTo(turns[i].x.toFloat(), turns[i].y + (u - width) / 2)
            path.lineTo(turns[i].x.toFloat(), turns[i].y + u - (u - width) / 2)
            width += (headWidth - tailWidth) / bodyParts * (turns[i].x - turns[i + 1].x - u) / u
            path.lineTo(turns[i + 1].x.toFloat() + u, turns[i + 1].y + u - (u - width) / 2)
            path.lineTo(turns[i + 1].x.toFloat() + u, turns[i + 1].y + (u - width) / 2)
            path.close()
        }

        fun pathDown(i: Int) {
            width += (headWidth - tailWidth) / bodyParts
            path.reset()
            path.moveTo(turns[i].x + (u - width) / 2, turns[i].y.toFloat() + u)
            path.lineTo(turns[i].x + u - (u - width) / 2, turns[i].y.toFloat() + u)
            width += (headWidth - tailWidth) / bodyParts * (turns[i + 1].y - turns[i].y - u) / u
            path.lineTo(turns[i + 1].x + u - (u - width) / 2, turns[i + 1].y.toFloat())
            path.lineTo(turns[i + 1].x + (u - width) / 2, turns[i + 1].y.toFloat())
            path.close()
        }

        fun pathUp(i: Int) {
            width += (headWidth - tailWidth) / bodyParts
            path.reset()
            path.moveTo(turns[i].x + (u - width) / 2, turns[i].y.toFloat())
            path.lineTo(turns[i].x + u - (u - width) / 2, turns[i].y.toFloat())
            width += (headWidth - tailWidth) / bodyParts * (turns[i].y - turns[i + 1].y - u) / u
            path.lineTo(turns[i + 1].x + u - (u - width) / 2, turns[i + 1].y.toFloat() + u)
            path.lineTo(turns[i + 1].x + (u - width) / 2, turns[i + 1].y.toFloat() + u)
            path.close()
        }

        fun drawTurn(centerX: Float, centerY: Float, startAngle: Float, clockwise: Boolean, stayInPlace: Boolean = true, paint: Paint) {
            if (stayInPlace)
                canvas.drawArc(centerX - width - (u - width) / 2, centerY - width - (u - width) / 2, centerX + width + (u - width) / 2, centerY + width + (u - width) / 2, startAngle, 90F, true, paint)
            else {
                if (clockwise)
                    canvas.drawArc(centerX - width - (u - width) / 2, centerY - width - (u - width) / 2, centerX + width + (u - width) / 2, centerY + width + (u - width) / 2, startAngle + degrees, 90 - degrees, true, paint)
                else
                    canvas.drawArc(centerX - width - (u - width) / 2, centerY - width - (u - width) / 2, centerX + width + (u - width) / 2, centerY + width + (u - width) / 2, startAngle, 90 - degrees, true, paint)
            }
            canvas.drawArc(centerX - (u - width) / 2, centerY - (u - width) / 2, centerX + (u - width) / 2, centerY + (u - width) / 2, startAngle, 90F, true, bgPaint)
        }
        //-----------------------------------------------------------------------------------actual drawing-------------------------------------------------------------
        //
        //note: the for loop starts at 0 and ends at turnsAmount - 1, so the tail will be draw first, and the head last
        for (i in 0 until turnsAmount) {
            when (i) {
                //--------------------------------------------------------------------------------head------------------------------------------------------------------
                turnsAmount - 1 -> {
                    if (turns[i - 1].y == turns[i].y) {
                        //going right
                        if (turns[i - 1].x < turns[i].x) {
                            canvas.drawRect(turns[i].x.toFloat(), turns[i].y + (u - width) / 2, turns[i].x + distance, turns[i].y + u - (u - width) / 2, linearGradients[turnsAmount - 2])
                            imgDstRect.set((turns[i].x - u + headDistance).toInt(), turns[i].y, (turns[i].x + headDistance).toInt(), turns[i].y + u)
                        }
                        //going left
                        else {
                            canvas.drawRect(turns[i].x + u - distance, turns[i].y + (u - width) / 2, turns[i].x.toFloat() + u, turns[i].y + u - (u - width) / 2, linearGradients[turnsAmount - 2])
                            imgDstRect.set((turns[i].x + u - headDistance).toInt(), turns[i].y, (turns[i].x + 2 * u - headDistance).toInt(), turns[i].y + u)
                        }
                    } else {
                        //going down
                        if (turns[i - 1].y < turns[i].y) {
                            canvas.drawRect(turns[i].x + (u - width) / 2, turns[i].y.toFloat(), turns[i].x + u - (u - width) / 2, turns[i].y + distance, linearGradients[turnsAmount - 2])
                            imgDstRect.set(turns[i].x, (turns[i].y - u + headDistance).toInt(), turns[i].x + u, (turns[i].y + headDistance).toInt())
                        }
                        //going up
                        else {
                            canvas.drawRect(turns[i].x + (u - width) / 2, turns[i].y + u - distance, turns[i].x + u - (u - width) / 2, turns[i].y.toFloat() + u, linearGradients[turnsAmount - 2])
                            imgDstRect.set(turns[i].x, (turns[i].y + u - headDistance).toInt(), turns[i].x + u, (turns[i].y + 2 * u - headDistance).toInt())
                        }
                    }
                    NORMAL_HEAD.bounds = imgDstRect
                    NORMAL_HEAD.draw(canvas)
                }
                //--------------------------------------------------------------------------------tail------------------------------------------------------------------
                0 -> {
                    val tempDistance = distance
                    val tempDegrees = degrees
                    //if the apple was eaten we want the tail to appear stationary,
                    // so we set the distance and degrees like below, and store the regular distance in this temporary
                    // val, so in later iterations we can use distance as usual
                    if (ateApple) {
                        distance = gameView.framesPerMove * distancePerFrame
                        degrees = gameView.framesPerMove * degreesPerFrame
                    }
                    //the tail is very annoying, we have 2 different types of cases, straight lines and turns
                    //-----------------------------------------------------------------------straight(tail)-------------------------------------------------------------
                    when {
                        turns[i + 1].y == y[bodyParts] -> {
                            //going right
                            if (turns[i].x < turns[i + 1].x) {
                                canvas.drawRect(turns[i].x + distance, turns[i].y + (u - width) / 2, turns[i].x.toFloat() + u, turns[i].y + u - (u - width) / 2, linearGradients[i])
                                canvas.drawArc(turns[i].x - width / 2 + distance, turns[i].y + (u - width) / 2, turns[i].x + width / 2 + distance, turns[i].y + u - (u - width) / 2, 90F, 180F, true, linearGradients[i])
                                pathRight(i)
                            }
                            //going left
                            else {
                                canvas.drawRect(turns[i].x.toFloat(), turns[i].y + (u - width) / 2, turns[i].x + u - distance, turns[i].y + u - (u - width) / 2, linearGradients[i])
                                canvas.drawArc(turns[i].x + u - width / 2 - distance, turns[i].y + (u - width) / 2, turns[i].x + u + width / 2 - distance, turns[i].y + u - (u - width) / 2, 270f, 180f, true, linearGradients[i])
                                pathLeft(i)
                            }
                        }
                        turns[i + 1].x == x[bodyParts] -> {
                            //going down
                            if (turns[i].y < turns[i + 1].y) {
                                canvas.drawRect(turns[i].x + (u - width) / 2, turns[i].y + distance, turns[i].x + u - (u - width) / 2, turns[i].y.toFloat() + u, linearGradients[i])
                                canvas.drawArc(turns[i].x + (u - width) / 2, turns[i].y - width / 2 + distance, turns[i].x + u - (u - width) / 2, turns[i].y + width / 2 + distance, 180f, 180f, true, linearGradients[i])
                                pathDown(i)
                            }
                            //going up
                            else {
                                canvas.drawRect(turns[i].x + (u - width) / 2, turns[i].y.toFloat(), turns[i].x + u - (u - width) / 2, turns[i].y + u - distance, linearGradients[i])
                                canvas.drawArc(turns[i].x + (u - width) / 2, turns[i].y + u - width / 2 - distance, turns[i].x + u - (u - width) / 2, turns[i].y + u + width / 2 - distance, 0f, 180f, true, linearGradients[i])
                                pathUp(i)
                            }
                        }
                        //-------------------------------------------------------------------------turn(tail)---------------------------------------------------------------
                        //
                        //now for some real nasty code with lots of trigonometry and if blocks
                        turns[i + 1].y < turns[i].y -> {
                            //counterclockwise bottom right
                            if (x[bodyParts] < turns[i].x) {
                                val b = Math.toRadians(90.0 - degrees)
                                val cos = turns[i].x + u / 2F * cos(b).toFloat()
                                val sin = turns[i].y + u / 2F * sin(b).toFloat()
                                canvas.drawArc(cos - width / 2, sin - width / 2, cos + width / 2, sin + width / 2, 90 - degrees, 180F, true, linearGradients[i])
                                drawTurn(turns[i].x.toFloat(), turns[i].y.toFloat(), 0F, clockwise = false, stayInPlace = false, linearGradients[i])
                            }
                            //clockwise bottom left
                            else {
                                val b = Math.toRadians(degrees.toDouble())
                                val cos = turns[i].y + u / 2F * cos(b).toFloat()
                                val sin = turns[i].x - u / 2F * sin(b).toFloat() + u
                                canvas.drawArc(sin - width / 2, cos - width / 2, sin + width / 2, cos + width / 2, 270 + degrees, 180F, true, linearGradients[i])
                                drawTurn(turns[i].x.toFloat() + u, turns[i].y.toFloat(), 90F, clockwise = true, stayInPlace = false, linearGradients[i])
                            }
                            pathUp(i)
                        }
                        turns[i].y < turns[i + 1].y -> {
                            //clockwise top right
                            if (x[bodyParts] < turns[i].x) {
                                val b = Math.toRadians(degrees.toDouble())
                                val cos = turns[i].y - u / 2F * cos(b).toFloat() + u
                                val sin = turns[i].x + u / 2F * sin(b).toFloat()
                                canvas.drawArc(sin - width / 2, cos - width / 2, sin + width / 2, cos + width / 2, 90 + degrees, 180F, true, linearGradients[i])
                                drawTurn(turns[i].x.toFloat(), turns[i].y.toFloat() + u, 270F, clockwise = true, stayInPlace = false, linearGradients[i])
                            }
                            //counterclockwise top left
                            else {
                                val b = Math.toRadians(90.0 - degrees)
                                val cos = turns[i].x - u / 2F * cos(b).toFloat() + u
                                val sin = turns[i].y - u / 2F * sin(b).toFloat() + u
                                canvas.drawArc(cos - width / 2, sin - width / 2, cos + width / 2, sin + width / 2, 270 - degrees, 180F, true, linearGradients[i])
                                drawTurn(turns[i].x.toFloat() + u, turns[i].y.toFloat() + u, 180F, clockwise = false, stayInPlace = false, linearGradients[i])
                            }
                            pathDown(i)
                        }
                        turns[i + 1].x < turns[i].x -> {
                            //clockwise bottom right
                            if (y[bodyParts] < turns[i].y) {
                                val b = Math.toRadians(degrees.toDouble())
                                val cos = turns[i].x + u / 2f * cos(b).toFloat()
                                val sin = turns[i].y + u / 2f * sin(b).toFloat()
                                canvas.drawArc(cos - width / 2, sin - width / 2, cos + width / 2, sin + width / 2, 180 + degrees, 180F, true, linearGradients[i])
                                drawTurn(turns[i].x.toFloat(), turns[i].y.toFloat(), 0F, clockwise = true, stayInPlace = false, linearGradients[i])
                            }
                            //counterclockwise top right
                            else {
                                val b = Math.toRadians(90.0 - degrees)
                                val cos = turns[i].y - u / 2f * cos(b).toFloat() + u
                                val sin = turns[i].x + u / 2f * sin(b).toFloat()
                                canvas.drawArc(sin - width / 2, cos - width / 2, sin + width / 2, cos + width / 2, 0 - degrees, 180F, true, linearGradients[i])
                                drawTurn(turns[i].x.toFloat(), turns[i].y.toFloat() + u, 270F, clockwise = false, stayInPlace = false, linearGradients[i])
                            }
                            pathLeft(i)
                        }
                        else -> {
                            //counterclockwise bottom left
                            if (y[bodyParts] < turns[i].y) {
                                val b = Math.toRadians(90.0 - degrees)
                                val cos = turns[i].y + u / 2f * cos(b).toFloat()
                                val sin = turns[i].x - u / 2f * sin(b).toFloat() + u
                                canvas.drawArc(sin - width / 2, cos - width / 2, sin + width / 2, cos + width / 2, 180 - degrees, 180F, true, linearGradients[i])
                                drawTurn(turns[i].x.toFloat() + u, turns[i].y.toFloat(), 90F, clockwise = false, stayInPlace = false, linearGradients[i])
                            }
                            //clockwise top left
                            else {
                                val b = Math.toRadians(degrees.toDouble())
                                val cos = turns[i].x - u / 2f * cos(b).toFloat() + u
                                val sin = turns[i].y - u / 2f * sin(b).toFloat() + u
                                canvas.drawArc(cos - width / 2, sin - width / 2, cos + width / 2, sin + width / 2, 0 + degrees, 180F, true, linearGradients[i])
                                drawTurn(turns[i].x.toFloat() + u, turns[i].y.toFloat() + u, 180F, clockwise = true, stayInPlace = false, linearGradients[i])
                            }
                            pathRight(i)
                        }
                    }
                    //------------------------------------------------------------------------ending(tail)--------------------------------------------------------------
                    canvas.drawPath(path, linearGradients[i])
                    distance = tempDistance
                    degrees = tempDegrees
                }
                //--------------------------------------------------------------------------------body------------------------------------------------------------------
                else -> {
                    when {
                        turns[i + 1].y < turns[i].y -> {
                            //counterclockwise bottom right
                            if (turns[i - 1].x < turns[i].x)
                                drawTurn(turns[i].x.toFloat(), turns[i].y.toFloat(), 0F, clockwise = false, stayInPlace = true, sweepGradients[i - 1])
                            //clockwise bottom left
                            else
                                drawTurn(turns[i].x.toFloat() + u, turns[i].y.toFloat(), 90F, clockwise = true, stayInPlace = true, sweepGradients[i - 1])
                            pathUp(i)
                        }
                        turns[i].y < turns[i + 1].y -> {
                            //clockwise top right
                            if (turns[i - 1].x < turns[i].x)
                                drawTurn(turns[i].x.toFloat(), turns[i].y.toFloat() + u, 270F, clockwise = true, stayInPlace = true, sweepGradients[i - 1])
                            //counterclockwise top left
                            else
                                drawTurn(turns[i].x.toFloat() + u, turns[i].y.toFloat() + u, 180F, clockwise = false, stayInPlace = true, sweepGradients[i - 1])
                            pathDown(i)
                        }
                        turns[i + 1].x < turns[i].x -> {
                            //clockwise bottom right
                            if (turns[i - 1].y < turns[i].y)
                                drawTurn(turns[i].x.toFloat(), turns[i].y.toFloat(), 0F, clockwise = true, stayInPlace = true, sweepGradients[i - 1])
                            //counterclockwise top right
                            else
                                drawTurn(turns[i].x.toFloat(), turns[i].y.toFloat() + u, 270F, clockwise = false, stayInPlace = true, sweepGradients[i - 1])
                            pathLeft(i)
                        }
                        else -> {
                            //counterclockwise bottom left
                            if (turns[i - 1].y < turns[i].y)
                                drawTurn(turns[i].x.toFloat() + u, turns[i].y.toFloat(), 90F, clockwise = false, stayInPlace = true, sweepGradients[i - 1])
                            //clockwise top left
                            else
                                drawTurn(turns[i].x.toFloat() + u, turns[i].y.toFloat() + u, 180F, clockwise = true, stayInPlace = true, sweepGradients[i - 1])
                            pathRight(i)
                        }
                    }
                    canvas.drawPath(path, linearGradients[i])
                }
            }
        }
    }

    private fun failed(): Boolean {
        //collided with itself
        for (i in bodyParts - 1 downTo 1)
            if (x[i] == x[0] && y[i] == y[0])
                return true
        //collided with walls
        return x[0] < u
                || x[0] >= gameView.width - u - gameView.width % u
                || y[0] < 0
                || y[0] >= gameView.height - u - gameView.height % u
    }

    private fun addTurns() {
        var index = 0
        fun addTurn(i: Int) {
            if (turns.size > index)
                turns[index].set(x[i], y[i])
            else
                turns.add(Point(x[i], y[i]))
            index++
        }
        //first turn is the tail
        addTurn(bodyParts - 1)
        //all the 'real' turns
        for (i in bodyParts - 2 downTo 1) {
            if (y[i + 1] != y[i - 1] && x[i + 1] != x[i - 1]) {
                addTurn(i)
            }
        }
        //last turn is the head
        addTurn(0)
        turnsAmount = index
    }

    private fun rotateHead(direction: Direction?, frameNumber: Int) {
        if (direction == null) return
        when (direction) {
            Direction.LEFT -> {
                if (this.direction == Direction.UP) {
                    NORMAL_HEAD.fromDegrees = 360F
                } else {
                    NORMAL_HEAD.fromDegrees = 180F
                }
                NORMAL_HEAD.toDegrees = 270F
            }
            Direction.UP -> {
                if (this.direction == Direction.LEFT) {
                    NORMAL_HEAD.fromDegrees = -90F
                } else {
                    NORMAL_HEAD.fromDegrees = 90F
                }
                NORMAL_HEAD.toDegrees = 0F
            }
            Direction.RIGHT -> {
                if (this.direction == Direction.UP) {
                    NORMAL_HEAD.fromDegrees = 0F
                } else {
                    NORMAL_HEAD.fromDegrees = 180F
                }
                NORMAL_HEAD.toDegrees = 90F
            }
            Direction.DOWN -> {
                if (this.direction == Direction.LEFT) {
                    NORMAL_HEAD.fromDegrees = 270F
                } else {
                    NORMAL_HEAD.fromDegrees = 90F
                }
                NORMAL_HEAD.toDegrees = 180F
            }
        }
        val anim = ValueAnimator.ofInt(0, 10000)
        anim.interpolator = LinearInterpolator()
        anim.repeatCount = 0
        anim.duration =
            (moveTime - (moveTime * frameNumber.toFloat() / gameView.framesPerMove)).toLong()
        anim.addUpdateListener {
            NORMAL_HEAD.level = it.animatedValue as Int
        }
        anim.start()
    }


    /**
     * big brain time!
     *
     * looks like a flutter element lol
     */
    private fun addGradients(frameNumber: Int) {
        //color change for every body part = (HEAD_COLOR(R|G|B) - TAIL_COLOR(R|G|B)) / max(40, length) = (head(R|G|B) - tail(r|g|b)) / length
        //that also means that at 40 or more body parts is when the gradient will be completely visible
        //
        //when an apple is eaten the length of the snake will gradually change in the move but bodyParts instantly gets incremented,
        //therefore we need to subtract (1 - frameNumber / framesPerMove), so that length will gradually increase during the move
        val length = if (ateApple)
            bodyParts - 1 + frameNumber.toFloat() / gameView.framesPerMove
        else bodyParts.toFloat()
        //when an apple is eaten the tail stays stationary, so instead of the starting point of the first
        //gradient moving, it stays in place
        val distance = if (ateApple)
            gameView.framesPerMove * distancePerFrame.toInt()
        else frameNumber * distancePerFrame.toInt()
        //color change for every body part = (HEAD_COLOR(R|G|B) - TAIL_COLOR(R|G|B)) / max(40, length)
        val redChange = (Color.red(HEAD_COLOR) - Color.red(TAIL_COLOR)) / max(40F, length)
        val greenChange = (Color.green(HEAD_COLOR) - Color.green(TAIL_COLOR)) / max(40F, length)
        val blueChange = (Color.blue(HEAD_COLOR) - Color.blue(TAIL_COLOR)) / max(40F, length)
        //these variables start with the calculated color of the tail, and grow to the head color at the end of the loop
        var red = (Color.red(HEAD_COLOR) - redChange * length).toInt()
        var green = (Color.green(HEAD_COLOR) - greenChange * length).toInt()
        var blue = (Color.blue(HEAD_COLOR) - blueChange * length).toInt()
        //these are the points of the tail gradient
        val firstGradStartPoint = Point()
        val firstGradEndPoint = Point()
        //these are the colors of the tail gradient
        val firstGradStartColor = Color.rgb(red, green, blue)
        var firstGradEndColor = 0

        //----------------------------------------------------------------------helper methods to reduce duplicate code!------------------------------------------------
        fun addLinearGradient(i: Int, gradient: LinearGradient) {
            if (linearGradients.size > i) {
                linearGradients[i].shader = (gradient)
            } else {
                val paint = Paint()
                paint.shader = gradient
                linearGradients.add(paint)
            }
        }

        fun addSweepGradient(i: Int, gradient: SweepGradient) {
            if (sweepGradients.size > i) {
                sweepGradients[i].shader = gradient
            } else {
                val paint = Paint()
                paint.shader = gradient
                sweepGradients.add(paint)
            }
            red += redChange.toInt()
            green += greenChange.toInt()
            blue += blueChange.toInt()
        }

        //sets the red, green and blue variables to their calculated values in the start of the first turn
        //fraction is the fraction where the start of the turn is at, between the two first grad points, goes between 1 and 0,
        //1 is closest to the end point, 0 is closest to start point
        //visualisation: (end)-----------(turn start)-----------------------------------(start) : this would mean a value of about 0.75
        //see its use in the actual loop
        fun rgbInFirstTurn(fraction: Float) {
            red = (Color.red(firstGradStartColor) + fraction * (Color.red(firstGradEndColor) - Color.red(firstGradStartColor))).toInt()
            green = (Color.green(firstGradStartColor) + fraction * (Color.green(firstGradEndColor) - Color.green(firstGradStartColor))).toInt()
            blue = (Color.blue(firstGradStartColor) + fraction * (Color.blue(firstGradEndColor) - Color.blue(firstGradStartColor))).toInt()
        }
        //----------------------------------------------------------------------------------adding gradients------------------------------------------------------------
        //
        //note: 0 is the tail, turnsAmount - 1 is the head
        for (i in 0 until turnsAmount - 1) {
            when (i) {
                //--------------------------------------------------------------------------------tail------------------------------------------------------------------
                0 -> {
                    when {
                        turns[i + 1].y == turns[i].y -> {
                            //going right
                            if (turns[i].x < turns[i + 1].x) {
                                firstGradStartPoint.set(turns[0].x + distance, turns[0].y)
                                firstGradEndPoint.set(turns[1].x + distance, turns[1].y)
                                firstGradEndColor = Color.rgb(
                                    (red + redChange * (firstGradEndPoint.x - firstGradStartPoint.x) / u).toInt(),
                                    (green + greenChange * (firstGradEndPoint.x - firstGradStartPoint.x) / u).toInt(),
                                    (blue + blueChange * (firstGradEndPoint.x - firstGradStartPoint.x) / u).toInt()
                                )
                            }
                            //going left
                            else {
                                firstGradStartPoint.set(turns[0].x + u - distance, turns[0].y)
                                firstGradEndPoint.set(turns[1].x + u - distance, turns[1].y)
                                firstGradEndColor = Color.rgb(
                                    (red + redChange * (firstGradStartPoint.x - firstGradEndPoint.x) / u).toInt(),
                                    (green + greenChange * (firstGradStartPoint.x - firstGradEndPoint.x) / u).toInt(),
                                    (blue + blueChange * (firstGradStartPoint.x - firstGradEndPoint.x) / u).toInt()
                                )
                            }
                        }
                        turns[i + 1].x == turns[i].x -> {
                            //going down
                            if (turns[i].y < turns[i + 1].y) {
                                firstGradStartPoint.set(turns[0].x, turns[0].y + distance)
                                firstGradEndPoint.set(turns[1].x, turns[1].y + distance)
                                firstGradEndColor = Color.rgb(
                                    (red + redChange * (firstGradEndPoint.y - firstGradStartPoint.y) / u).toInt(),
                                    (green + greenChange * (firstGradEndPoint.y - firstGradStartPoint.y) / u).toInt(),
                                    (blue + blueChange * (firstGradEndPoint.y - firstGradStartPoint.y) / u).toInt()
                                )
                            }
                            //going up
                            else {
                                firstGradStartPoint.set(turns[0].x, turns[0].y + u - distance)
                                firstGradEndPoint.set(turns[1].x, turns[1].y + u - distance)
                                firstGradEndColor = Color.rgb(
                                    (red + redChange * (firstGradStartPoint.y - firstGradEndPoint.y) / u).toInt(),
                                    (green + greenChange * (firstGradStartPoint.y - firstGradEndPoint.y) / u).toInt(),
                                    (blue + blueChange * (firstGradStartPoint.y - firstGradEndPoint.y) / u).toInt()
                                )
                            }
                        }
                    }
                    addLinearGradient(i, LinearGradient(firstGradStartPoint.x.toFloat(), firstGradStartPoint.y.toFloat(), firstGradEndPoint.x.toFloat(), firstGradEndPoint.y.toFloat(), firstGradStartColor, firstGradEndColor, Shader.TileMode.CLAMP))
                }
                //--------------------------------------------------------------------------------body------------------------------------------------------------------
                else -> {
                    //---------------------------------------------------------------------------turns------------------------------------------------------------------
                    when {
                        turns[i + 1].y < turns[i].y -> {
                            //counterclockwise bottom right
                            if (turns[i - 1].x < turns[i].x) {
                                //i = 1 is the first 'real' turn
                                if (i == 1) {
                                    rgbInFirstTurn((turns[i].x - firstGradStartPoint.x).toFloat() / (firstGradEndPoint.x - firstGradStartPoint.x))
                                }
                                //i - 1 because when i = 1 the first sweep gradient is added
                                addSweepGradient(
                                    i - 1, SweepGradient(
                                        turns[i].x.toFloat(), turns[i].y.toFloat(),
                                        intArrayOf(Color.rgb(red + redChange.toInt(), green + greenChange.toInt(), blue + blueChange.toInt()), Color.rgb(red, green, blue)),
                                        floatArrayOf(0F, 0.25F)
                                    )
                                )
                            }
                            //clockwise bottom left
                            else {
                                if (i == 1) {
                                    rgbInFirstTurn((firstGradStartPoint.x - turns[i].x - u).toFloat() / (firstGradStartPoint.x - firstGradEndPoint.x))
                                }
                                addSweepGradient(
                                    i - 1, SweepGradient(
                                        turns[i].x.toFloat() + u, turns[i].y.toFloat(),
                                        intArrayOf(Color.rgb(red, green, blue), Color.rgb(red + redChange.toInt(), green + greenChange.toInt(), blue + blueChange.toInt())),
                                        floatArrayOf(0.25F, 0.5F)
                                    )
                                )
                            }
                            //linear gradient up
                            val topY = (turns[i + 1].y + u).toFloat()
                            val bottomY = turns[i].y.toFloat()
                            val x = turns[i].x.toFloat()
                            val addRed = (redChange * (bottomY - topY) / u).toInt()
                            val addGreen = (greenChange * (bottomY - topY) / u).toInt()
                            val addBlue = (blueChange * (bottomY - topY) / u).toInt()
                            addLinearGradient(i, LinearGradient(x, bottomY, x, topY, Color.rgb(red, green, blue), Color.rgb(red + addRed, green + addGreen, blue + addBlue), Shader.TileMode.CLAMP))
                            red += addRed; green += addGreen; blue += addBlue
                        }
                        turns[i].y < turns[i + 1].y -> {
                            //clockwise top right
                            if (turns[i - 1].x < turns[i].x) {
                                if (i == 1) {
                                    rgbInFirstTurn((turns[i].x - firstGradStartPoint.x).toFloat() / (firstGradEndPoint.x - firstGradStartPoint.x))
                                }
                                addSweepGradient(
                                    i - 1, SweepGradient(
                                        turns[i].x.toFloat(), turns[i].y.toFloat() + u,
                                        intArrayOf(Color.rgb(red, green, blue), Color.rgb(red + redChange.toInt(), green + greenChange.toInt(), blue + blueChange.toInt())),
                                        floatArrayOf(0.75f, 1F)
                                    )
                                )
                            }
                            //counterclockwise top left
                            else {
                                if (i == 1) {
                                    rgbInFirstTurn((firstGradStartPoint.x - turns[i].x - u).toFloat() / (firstGradStartPoint.x - firstGradEndPoint.x))
                                }
                                addSweepGradient(
                                    i - 1, SweepGradient(
                                        turns[i].x.toFloat() + u, turns[i].y.toFloat() + u,
                                        intArrayOf(Color.rgb(red + redChange.toInt(), green + greenChange.toInt(), blue + blueChange.toInt()), Color.rgb(red, green, blue)),
                                        floatArrayOf(0.5f, 0.75f)
                                    )
                                )
                            }
                            //linear gradient down
                            val topY = (turns[i].y + u).toFloat()
                            val bottomY = turns[i + 1].y.toFloat()
                            val x = turns[i].x.toFloat()
                            val addRed = (redChange * (bottomY - topY) / u).toInt()
                            val addGreen = (greenChange * (bottomY - topY) / u).toInt()
                            val addBlue = (blueChange * (bottomY - topY) / u).toInt()
                            addLinearGradient(i, LinearGradient(x, topY, x, bottomY, Color.rgb(red, green, blue), Color.rgb(red + addRed, green + addGreen, blue + addBlue), Shader.TileMode.CLAMP))
                            red += addRed; green += addGreen; blue += addBlue
                        }
                        turns[i + 1].x < turns[i].x -> {
                            //clockwise bottom right
                            if (turns[i - 1].y < turns[i].y) {
                                if (i == 1) {
                                    rgbInFirstTurn((turns[i].y - firstGradStartPoint.y).toFloat() / (firstGradEndPoint.y - firstGradStartPoint.y))
                                }
                                addSweepGradient(
                                    i - 1, SweepGradient(
                                        turns[i].x.toFloat(), turns[i].y.toFloat(),
                                        intArrayOf(Color.rgb(red, green, blue), Color.rgb(red + redChange.toInt(), green + greenChange.toInt(), blue + blueChange.toInt())),
                                        floatArrayOf(0F, 0.25F)
                                    )
                                )
                            }
                            //counterclockwise top right
                            else {
                                if (i == 1) {
                                    rgbInFirstTurn((firstGradStartPoint.y - turns[1].y - u).toFloat() / (firstGradStartPoint.y - firstGradEndPoint.y))
                                }
                                addSweepGradient(
                                    i - 1, SweepGradient(
                                        turns[i].x.toFloat(), turns[i].y.toFloat() + u,
                                        intArrayOf(Color.rgb(red + redChange.toInt(), green + greenChange.toInt(), blue + blueChange.toInt()), Color.rgb(red, green, blue)),
                                        floatArrayOf(0.75F, 1F)
                                    )
                                )
                            }
                            //linear gradient left
                            val leftX = (turns[i + 1].x + u).toFloat()
                            val rightX = turns[i].x.toFloat()
                            val y = turns[i].y.toFloat()
                            val addRed = (redChange * (rightX - leftX) / u).toInt()
                            val addGreen = (greenChange * (rightX - leftX) / u).toInt()
                            val addBlue = (blueChange * (rightX - leftX) / u).toInt()
                            addLinearGradient(i, LinearGradient(rightX, y, leftX, y, Color.rgb(red, green, blue), Color.rgb(red + addRed, green + addGreen, blue + addBlue), Shader.TileMode.CLAMP))
                            red += addRed; green += addGreen; blue += addBlue
                        }
                        else -> {
                            //counterclockwise bottom left
                            if (turns[i - 1].y < turns[i].y) {
                                if (i == 1) {
                                    rgbInFirstTurn((turns[1].y - firstGradStartPoint.y).toFloat() / (firstGradEndPoint.y - firstGradStartPoint.y))
                                }
                                addSweepGradient(
                                    i - 1, SweepGradient(
                                        turns[i].x.toFloat() + u, turns[i].y.toFloat(),
                                        intArrayOf(Color.rgb(red + redChange.toInt(), green + greenChange.toInt(), blue + blueChange.toInt()), Color.rgb(red, green, blue)),
                                        floatArrayOf(0.25F, 0.5F)
                                    )
                                )
                            }
                            //clockwise top left
                            else {
                                if (i == 1) {
                                    rgbInFirstTurn((firstGradStartPoint.y - turns[1].y - u).toFloat() / (firstGradStartPoint.y - firstGradEndPoint.y))
                                }
                                addSweepGradient(
                                    i - 1, SweepGradient(
                                        turns[i].x.toFloat() + u, turns[i].y.toFloat() + u,
                                        intArrayOf(Color.rgb(red, green, blue), Color.rgb(red + redChange.toInt(), green + greenChange.toInt(), blue + blueChange.toInt())),
                                        floatArrayOf(0.5F, 0.75F)
                                    )
                                )
                            }
                            //linear gradient right
                            val leftX = (turns[i].x + u).toFloat()
                            val rightX = turns[i + 1].x.toFloat()
                            val y = turns[i].y.toFloat()
                            val addRed = (redChange * (rightX - leftX) / u).toInt()
                            val addGreen = (greenChange * (rightX - leftX) / u).toInt()
                            val addBlue = (blueChange * (rightX - leftX) / u).toInt()
                            addLinearGradient(i, LinearGradient(leftX, y, rightX, y, Color.rgb(red, green, blue), Color.rgb(red + addRed, green + addGreen, blue + addBlue), Shader.TileMode.CLAMP))
                            red += addRed; green += addGreen; blue += addBlue
                        }
                    }
                }
            }
        }
    }
}