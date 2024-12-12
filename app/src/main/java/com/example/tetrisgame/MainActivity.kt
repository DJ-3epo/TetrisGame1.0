package com.example.tetrisgame

import android.os.Bundle
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import com.example.tetrisgame.TetrisGameView
import com.example.tetrisgame.Tetromino
import android.widget.Button
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var tetrisGameView: TetrisGameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tetrisGameView = findViewById(R.id.gameSurfaceView)

        val startButton: Button = findViewById(R.id.startButton)

        startButton.setOnClickListener {
            tetrisGameView.startGame()
        }


    }
    fun onStartButtonClick(view: android.view.View) {
        tetrisGameView.startGame() // Запуск игры
    }



    // Обработчик клика для кнопки "Rotate"
    fun onRotateButtonClick(view: android.view.View) {
        tetrisGameView.rotateTetromino() // Вращаем текущую тетромино
    }
}