package com.example.mob_dev_portfolio

data class gameOverData(var gameOver: Boolean = false) {
    var gameOverMessageId: Int? = null
        set(value) {
            if (gameOver) {
                field = value
            } else {
                field = null
            }
        }

}
