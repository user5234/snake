package game.snake

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

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
}