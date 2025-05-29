package com.example.mob_dev_portfolio

import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square

data class PromotionData(
    var midPromotion: Boolean = false,
) {
    var pawnSquare: Square? = null
        set(value) {
            if (midPromotion == true) {
                field = value
            } else {
                field = null
            }
        }

    var promotionPiece: Piece? = null
        set(value) {
            if (midPromotion == true && pawnSquare != null) {
                field = value
            } else {
                field = null
            }
        }

    var promotionSquare: Square? = null
        set(value) {
            if (midPromotion == true && pawnSquare != null) {
                field = value
            } else {
                field = null
            }
        }

    // updating this for debug logs
    override fun toString(): String {
        return "PromotionData(midPromotion=$midPromotion, pawnSquare=$pawnSquare, promotionPiece=$promotionPiece, promotionSquare=$promotionSquare)"
    }
}
