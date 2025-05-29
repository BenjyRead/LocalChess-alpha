package com.example.mob_dev_portfolio

import com.github.bhlangonijr.chesslib.Side

data class ResignData(
    val side: Side,
    val resigning: Boolean = false,
    val confirmed: Boolean = false
) {
}