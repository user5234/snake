package game.snake

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass

private const val PREFERENCES = "app_preferences"
private const val SNAKE_LAST_MAP_SIZE = "snake_map_size"
private const val SNAKE_LAST_SPEED = "snake_speed"
private const val SNAKE_LAST_APPLES_AMOUNT = "snake_apples_amount"
private const val SNAKE_HIGH_SCORE = "snake_high_score"

class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var instance: MainActivity private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        instance = this
        setContentView(R.layout.activity_main)
        layoutInflater.inflate(R.layout.play_screen, findViewById(R.id.mainLayout))
    }

    override fun onPause() {
        super.onPause()
        findViewById<GameView>(R.id.gameView).pause()
    }

    override fun onResume() {
        super.onResume()
        findViewById<GameView>(R.id.gameView).resume()
    }

    fun saveLast(vararg options : GameView.GameOption) {
        val preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        val editor = preferences.edit()
        for (option in options) {
            when (option) {
                is GameView.MapSize -> { editor.putInt(SNAKE_LAST_MAP_SIZE, option.squaresAmount) }
                is GameView.Speed -> { editor.putLong(SNAKE_LAST_SPEED, option.moveTime) }
                is GameView.ApplesAmount -> { editor.putInt(SNAKE_LAST_APPLES_AMOUNT, option.amount) }
            }
        }
        editor.apply()
    }

    inline fun <reified T : GameView.GameOption> getLast() : T {
        return getLast(T::class)
    }

    fun <T : GameView.GameOption> getLast(t : KClass<T>) : T {
        val preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        when (t) {
            GameView.MapSize::class -> {
                enumValues<GameView.MapSize>().forEach {
                    if (it.squaresAmount == preferences.getInt(SNAKE_LAST_MAP_SIZE, GameView.MapSize.NORMAL.squaresAmount)) return it as T
                }
            }
            GameView.Speed::class -> {
                enumValues<GameView.Speed>().forEach {
                    if (it.moveTime == preferences.getLong(SNAKE_LAST_SPEED, GameView.Speed.NORMAL.moveTime)) return it as T
                }
            }
            GameView.ApplesAmount::class -> {
                enumValues<GameView.ApplesAmount>().forEach {
                    if (it.amount == preferences.getInt(SNAKE_LAST_APPLES_AMOUNT, GameView.ApplesAmount.ONE.amount)) return it as T
                }
            }
        }
        throw IllegalArgumentException("")
    }
}