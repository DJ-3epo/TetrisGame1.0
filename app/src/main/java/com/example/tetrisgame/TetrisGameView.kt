package com.example.tetrisgame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.Random
import android.util.AttributeSet
import android.os.Handler
import android.animation.ObjectAnimator
import android.widget.TextView
import android.app.Activity
import android.media.MediaPlayer
import java.util.*

class TetrisGameView(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs), Runnable {

    var isPlaying = false
    private val thread: Thread = Thread(this)
    private val paint: Paint = Paint()

    // Игровое поле
    private val board = Array(37) { IntArray(14) }

    private var currentTetromino: Tetromino = generateTetromino()
    private var nextTetromino: Tetromino = generateTetromino()  // Следующая фигура
    private var currentTetrominoColor: Int = generateRandomColor()  // Цвет текущей тетромино
    private var nextTetrominoColor: Int = generateRandomColor()  // Цвет следующей тетромино
    private var gameOver = false
    private var score = 0  // Счет игры
    private var speedDelay = 100L  // Начальная задержка (скорость игры)
    private var linesCleared = 0  // Количество очищенных линий
    private var isFastDropping = false // Флаг для быстрого падения
    private val normalSpeed = 30L // Нормальная скорость
    private val fastSpeed = 10L // Скорость быстрого падения
    private var fallProgress = 0f  // Прогресс падения (от 0 до 1)
    private val handler = Handler() // Для управления временем
    private var rotationProgress = 0f
    private lateinit var mediaPlayer: MediaPlayer
    private var isMusicPlaying = false

    private val musicList = listOf(
        R.raw.song1,
        R.raw.song2,
        R.raw.song3
    )
    private var currentSongIndex = 0  // Индекс текущей песни

    // Метод для начала воспроизведения музыки
    private fun startMusic() {
        if (!isMusicPlaying) {
            playSong(currentSongIndex)  // Воспроизвести песню по индексу
            isMusicPlaying = true  // Устанавливаем флаг, что музыка играет
        }
    }

    // Метод для воспроизведения песни
    private fun stopMusic() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.release()
            isMusicPlaying = false  // Сбрасываем флаг, что музыка не играет
        }
    }

    // Метод для воспроизведения песни
    private fun playSong(index: Int) {
        stopMusic()  // Останавливаем текущую музыку перед воспроизведением новой

        // Если игра закончена, играем музыку проигрыша
        if (gameOver) {
            mediaPlayer = MediaPlayer.create(context, R.raw.game_over_song)  // Песня проигрыша
        } else {
            mediaPlayer = MediaPlayer.create(context, musicList[index])  // Воспроизведение текущего трека
        }

        mediaPlayer.isLooping = false  // Песня не будет повторяться
        mediaPlayer.start()  // Начинаем воспроизведение

        // Устанавливаем слушатель для завершения песни
        mediaPlayer.setOnCompletionListener {
            // Если игра не окончена, продолжаем играть следующие песни
            if (!gameOver) {
                currentSongIndex = (currentSongIndex + 1) % musicList.size  // Следующая песня
                playSong(currentSongIndex)  // Воспроизвести следующую
            }
        }
    }


    // Метод для начала игры и воспроизведения музыки
    fun startGame() {
        if (isPlaying) return  // Если игра уже запущена, ничего не делаем
        resetGame()  // Сброс игры
        isPlaying = true
        gameOver = false
        score = 0
        linesCleared = 0
        speedDelay = normalSpeed
        // Запускаем новый поток игры, если он ещё не запущен
        if (!thread.isAlive) {
            thread.start()  // Запуск игрового потока
        }
        startMusic()  // Начать воспроизведение музыки
    }



    private fun resetGame() {
        // Очищаем игровое поле
        for (i in board.indices) {
            board[i].fill(0)
        }

        // Генерация нового тетромино
        currentTetromino = generateTetromino()
        nextTetromino = generateTetromino()

        // Сбросим другие параметры
        currentTetrominoColor = generateRandomColor()
        nextTetrominoColor = generateRandomColor()
        gameOver = false
        speedDelay = normalSpeed
        linesCleared = 0
        fallProgress = 0f

        // Останавливаем музыку и начинаем заново
        if (isMusicPlaying) {
            stopMusic()  // Останавливаем текущую музыку
        }
        startMusic()  // Начинаем музыку заново
    }

    // Проверка, можно ли двигаться вниз
    private fun canMoveDown(): Boolean {
        for (i in currentTetromino.shape.indices) {
            for (j in currentTetromino.shape[i].indices) {
                if (currentTetromino.shape[i][j] != 0) {
                    val newRow = currentTetromino.row + i + 1
                    if (newRow >= board.size || board[newRow][currentTetromino.col + j] != 0) {
                        return false
                    }
                }
            }
        }
        return true
    }


    private fun canMoveLeft(): Boolean {
        for (i in currentTetromino.shape.indices) {
            for (j in currentTetromino.shape[i].indices) {
                if (currentTetromino.shape[i][j] != 0) {
                    val newCol = currentTetromino.col + j - 1
                    val newRow = currentTetromino.row + i

                    if (newCol < 0 || newRow < 0 || newRow >= board.size || newCol >= board[0].size) {
                        return false
                    }

                    if (board[newRow][newCol] != 0) {
                        return false
                    }
                }
            }
        }
        return true
    }

    // Проверка, можно ли двигаться вправо
    private fun canMoveRight(): Boolean {
        for (i in currentTetromino.shape.indices) {
            for (j in currentTetromino.shape[i].indices) {
                if (currentTetromino.shape[i][j] != 0) {
                    val newCol = currentTetromino.col + j + 1
                    if (newCol >= board[0].size || board[currentTetromino.row + i][newCol] != 0) {
                        return false
                    }
                }
            }
        }
        return true
    }

    // Метод для перемещения тетромино влево
    fun moveTetrominoLeft() {
        if (canMoveLeft()) {
            currentTetromino.col-- // Сдвигаем влево
        }
    }

    // Метод для перемещения тетромино вправо
    fun moveTetrominoRight() {
        if (canMoveRight()) {
            currentTetromino.col++ // Сдвигаем вправо
        }
    }

    // Метод для вращения тетромино
    fun rotateTetromino() {
        val tempTetromino = Tetromino(currentTetromino.shape.copyOf(), currentTetromino.col, currentTetromino.row)
        tempTetromino.rotate()

        if (tempTetromino.canRotate(board)) {
            currentTetromino = tempTetromino  // Применяем вращение сразу, без плавности
        }
    }


    override fun run() {
        while (isPlaying) {
            update()
            draw()
            Thread.sleep(speedDelay)
        }
    }

    // Обновление состояния игры (логика падения)
    private fun update() {
        if (gameOver) return

        if (canMoveDown()) {
            // Плавное падение
            fallProgress += 0.1f  // Каждый шаг увеличивает прогресс
            if (fallProgress >= 1f) {
                currentTetromino.row++
                fallProgress = 0f  // Сброс прогресса после перемещения
            }
        } else {
            placeTetrominoOnBoard()

            // Заменим проверку на завершение игры с помощью canTetrominoSpawn
            if (!canTetrominoSpawn(nextTetromino)) {
                gameOver = true // Если тетромино не может появиться, игра завершена
                playSong(-1) // Проигрываем проигрышный трек
                return
            }

            removeFullLines()
            score += 100
            linesCleared++

            if (linesCleared % 5 == 0) {
                speedDelay = (speedDelay * 0.9).toLong()
            }

            currentTetromino = nextTetromino
            nextTetromino = generateTetromino()
            currentTetrominoColor = nextTetrominoColor
            nextTetrominoColor = generateRandomColor()
        }
    }



    // Генерация случайного цвета
    private fun generateRandomColor(): Int {
        val random = Random()
        val colors = listOf(
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.WHITE
        )
        return colors[random.nextInt(colors.size)]
    }

    // Отрисовка игрового поля
    private fun draw() {
        if (holder.surface.isValid) {
            val canvas: Canvas = holder.lockCanvas()
            canvas.drawColor(Color.BLACK) // Фон

            // Добавим отступ сверху для границы, например 100 пикселей
            val borderTopOffset = 100f

            // Отображение границ с отступом сверху
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f // Толщина рамки
            val boardWidth = (board[0].size * 50).toFloat() // Ширина поля
            val boardHeight = (board.size * 50).toFloat()   // Высота поля
            canvas.drawRect(
                0f, borderTopOffset, // Отступ сверху
                boardWidth, // Ширина поля
                boardHeight + borderTopOffset, // Высота поля с отступом
                paint
            )

            // Отрисовка клеток игрового поля
            paint.color = Color.GRAY
            for (i in board.indices) {
                for (j in board[i].indices) {
                    if (board[i][j] > 0) {
                        canvas.drawRect(
                            (j * 50).toFloat(),
                            (i * 50 + borderTopOffset).toFloat(),  // Смещаем по оси Y
                            ((j + 1) * 50).toFloat(),
                            ((i + 1) * 50 + borderTopOffset).toFloat(),  // Смещаем по оси Y
                            paint
                        )
                    }
                }
            }

            // Отрисовка текущей тетромино
            paint.color = currentTetrominoColor
            for (i in currentTetromino.shape.indices) {
                for (j in currentTetromino.shape[i].indices) {
                    if (currentTetromino.shape[i][j] != 0) {
                        canvas.drawRect(
                            ((currentTetromino.col + j) * 50).toFloat(),
                            ((currentTetromino.row + i) * 50 + borderTopOffset).toFloat(),  // Смещаем по оси Y
                            ((currentTetromino.col + j + 1) * 50).toFloat(),
                            ((currentTetromino.row + i + 1) * 50 + borderTopOffset).toFloat(),
                            paint
                        )
                    }
                }
            }

            // Сдвиг для отображения следующей тетромино вне игрового поля
            val nextTetrominoOffsetX = boardWidth + 50f // Сдвиг по оси X (чтобы была справа от поля)
            val nextTetrominoOffsetY = 100f // Сдвиг по оси Y (чтобы была ниже верхней границы)

            // Отображение следующей тетромино
            paint.color = nextTetrominoColor
            for (i in nextTetromino.shape.indices) {
                for (j in nextTetromino.shape[i].indices) {
                    if (nextTetromino.shape[i][j] != 0) {
                        // Рисуем каждую часть тетромино с учетом сдвигов по X и Y
                        canvas.drawRect(
                            ((j + 1) * 50 + nextTetrominoOffsetX).toFloat(),  // Сдвиг по оси X
                            ((i + 2) * 50 + nextTetrominoOffsetY).toFloat(),  // Сдвиг по оси Y
                            ((j + 2) * 50 + nextTetrominoOffsetX).toFloat(),  // Сдвиг по оси X
                            ((i + 3) * 50 + nextTetrominoOffsetY).toFloat(),  // Сдвиг по оси Y
                            paint
                        )
                    }
                }
            }

            // Отображение сообщения "GAME OVER"
            if (gameOver) {
                paint.color = Color.RED
                paint.textSize = 100f
                paint.textAlign = Paint.Align.CENTER

                // Рассчитываем координаты центра игрового поля
                val centerX = boardWidth / 2
                val centerY = (boardHeight / 2) + borderTopOffset

                // Рисуем текст по центру
                canvas.drawText("GAME OVER", centerX, centerY, paint)
            }

            holder.unlockCanvasAndPost(canvas)
        }
    }




    // Генерация случайной тетромино
    private fun generateTetromino(): Tetromino {
        val shapes = listOf(
            arrayOf(intArrayOf(1, 1, 1, 1)), // I
            arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)), // O
            arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1)), // T
            arrayOf(intArrayOf(1, 0, 0), intArrayOf(1, 1, 1)), // L
            arrayOf(intArrayOf(0, 0, 1), intArrayOf(1, 1, 1))  // J
        )

        val shape = shapes.random()

        // Устанавливаем начальную позицию тетромино так, чтобы оно начиналось с верхней части поля
        val startCol = (board[0].size - shape[0].size) / 2  // Центрируем по горизонтали
        val startRow = 0 // Начальная строка будет 0, так как тетромино должно начинаться в самой верхней строке видимой области

        // Создаём тетромино
        val newTetromino = Tetromino(shape, startCol, startRow)

        // Проверяем, не пересекает ли новое тетромино уже занятые клетки
        if (!canTetrominoSpawn(newTetromino)) {
            gameOver = true  // Если пересекает, игра окончена
            return newTetromino  // Возвращаем тетромино, но оно не будет падать
        }

        return newTetromino
    }
    private fun canTetrominoSpawn(tetromino: Tetromino): Boolean {
        for (i in tetromino.shape.indices) {
            for (j in tetromino.shape[i].indices) {
                if (tetromino.shape[i][j] != 0) { // Если это часть тетромино (не пустая клетка)
                    val row = tetromino.row + i
                    val col = tetromino.col + j
                    // Проверяем, не выходит ли тетромино за границы поля и не занята ли клетка
                    if (row < 0 || row >= board.size || col < 0 || col >= board[0].size || board[row][col] != 0) {
                        gameOver = true  // Если пересекает, игра окончена
                        playSong(-1)  // Воспроизводим песню проигрыша
                        return false
                    }
                }
            }
        }
        return true // Тетромино может быть размещено
    }


    // Фиксируем фигуру на доске
    private fun placeTetrominoOnBoard() {
        for (i in currentTetromino.shape.indices) {
            for (j in currentTetromino.shape[i].indices) {
                if (currentTetromino.shape[i][j] != 0) {
                    board[currentTetromino.row + i][currentTetromino.col + j] = 1
                }
            }
        }
    }

    // Убираем полные линии
    private fun removeFullLines() {
        var linesCleared = 0

        // Обходим все строки и проверяем, если она полностью заполнена
        for (i in board.indices) {
            if (board[i].all { it != 0 }) { // Если строка полностью заполнена
                // Увеличиваем счет за удаленную линию
                linesCleared++

                // Сдвигаем все строки ниже вниз
                for (j in i downTo 1) {
                    board[j] = board[j - 1].clone()
                }
                // Очищаем верхнюю строку
                board[0].fill(0)
            }
        }

        // Увеличиваем счет за удаленные линии
        if (linesCleared > 0) {
            score += linesCleared * 100 // Например, 100 очков за каждую удаленную линию
            updateScoreDisplay()  // Обновление UI для счета
        }
    }



    // Функция для обновления отображения счета на экране
    private fun updateScoreDisplay() {

        post {
            // Получаем доступ к UI элементу через Activity
            val scoreTextView: TextView = (context as Activity).findViewById(R.id.scoreTextView)
            scoreTextView.text = "Score: $score"
        }
    }


    // Обработка касаний экрана
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOver) {
            // Если игра окончена, можно перезапустить игру при нажатии
            if (event.action == MotionEvent.ACTION_DOWN) {
                resetGame()  // Перезапускаем игру
                startGame()  // Запускаем игру
            }
            return super.onTouchEvent(event)  // Возвращаем базовую обработку касания
        }

        val screenWidth = width.toFloat()
        val screenHeight = height.toFloat()

        // Обработка быстрого падения
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isFastDropping = true
                speedDelay = fastSpeed
            }
            MotionEvent.ACTION_UP -> {
                isFastDropping = false
                speedDelay = normalSpeed
            }
        }

        // Проверка нажатий в области для движения
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (event.x < screenWidth / 3) {
                moveTetrominoLeft() // Перемещение влево
            } else if (event.x > screenWidth * 2 / 3) {
                moveTetrominoRight() // Перемещение вправо
            }
        }

        return true
    }
}