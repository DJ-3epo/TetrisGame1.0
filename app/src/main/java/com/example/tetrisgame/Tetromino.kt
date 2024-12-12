package com.example.tetrisgame

data class Tetromino(
    var shape: Array<IntArray>, // Форма тетромино
    var col: Int = 0,           // Колонка на поле
    var row: Int = 0            // Строка на поле
) {

    // Метод для вращения тетромино
    fun rotate() {
        val newShape = Array(shape[0].size) { IntArray(shape.size) }

        // Поворот матрицы на 90 градусов по часовой стрелке
        for (i in shape.indices) {
            for (j in shape[i].indices) {
                newShape[j][shape.size - 1 - i] = shape[i][j]
            }
        }

        shape = newShape // Присваиваем новый массив
    }

    // Метод для проверки, можно ли вращать тетромино
    fun canRotate(board: Array<IntArray>): Boolean {
        val testTetromino = Tetromino(shape.copyOf(), col, row)
        testTetromino.rotate()

        // Проверяем, не выходит ли тетромино за границы или не сталкивается ли с другими фигурами
        for (i in testTetromino.shape.indices) {
            for (j in testTetromino.shape[i].indices) {
                if (testTetromino.shape[i][j] != 0) {
                    if (testTetromino.col + j < 0 || testTetromino.col + j >= board[0].size ||
                        testTetromino.row + i >= board.size ||
                        testTetromino.row + i < 0 ||
                        board[testTetromino.row + i][testTetromino.col + j] != 0) {
                        return false
                    }
                }
            }
        }
        return true
    }
}
