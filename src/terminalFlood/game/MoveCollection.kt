package terminalFlood.game

/**
 * This class is a relatively memory efficient implementation of a collection of linked move entries. It is intended
 * to be used together with [GameExternalMoveList].
 *
 * ### Implementation:
 * Move entries are saved to two internal arrays. One array saves the previous move's index as an Int and the other
 * array saves the move's color value as a Byte. This means that every entry takes up 40 bits.
 */
class MoveCollection {
    companion object {
        const val NO_MOVE_INDEX: Int = 0

        private const val NO_MOVE_VALUE: Int = -1

        private const val INITIAL_SIZE: Int = 10_000
    }

    // Move indexes. Every entry holds the value of the previous move's index.
    private var moveIndexArray: IntArray = IntArray(INITIAL_SIZE)

    // Color values. Every entry holds the color value of that move.
    private var moveColorArray: ByteArray = ByteArray(INITIAL_SIZE)

    init {
        // First entry gets a special value so we can tell that there are no further moves.
        moveIndexArray[0] = NO_MOVE_VALUE
        moveColorArray[0] = Color.DUMMY.value.toByte()
    }

    private var lastUsedIndex: Int = 0

    /**
     * Adds the given color as an entry into this move collection, with the given index as the previous move. Returns
     * the index of this newly added move.
     */
    fun addMoveEntry(previousMoveIndex: Int, move: Color): Int {
        val currentIndex = ++lastUsedIndex

        // Resize the arrays if they are too small.
        if (currentIndex == moveIndexArray.size) {
            var newSize = moveIndexArray.size + moveIndexArray.size / 2
            if (newSize < 0)
                newSize = Int.MAX_VALUE
            moveIndexArray = moveIndexArray.copyOf(newSize)
            moveColorArray = moveColorArray.copyOf(newSize)
        }

        moveIndexArray[currentIndex] = previousMoveIndex
        moveColorArray[currentIndex] = move.value.toByte()

        return currentIndex
    }

    /**
     * Returns the collection of moves of the given size that ended with the given move, ordered from first to last.
     *
     * Should have slightly better performance than getMoveList(Int), but requires you to know the amount of moves
     * beforehand.
     */
    fun getMoveList(moveIndex: Int, amountOfMoves: Int): Array<Color> {
        val moves = arrayOfNulls<Color>(amountOfMoves)

        var i = amountOfMoves
        var index = moveIndex
        while (--i >= 0) {
            moves[i] = Color.colorCache[moveColorArray[index].toInt()]
            index = moveIndexArray[index]
        }

        @Suppress("UNCHECKED_CAST")
        return moves as Array<Color>
    }

    /**
     * Returns the collection of moves that ended with the given move, ordered from first to last.
     *
     * Use getMoveList(Int, Int) if you know the amount of moves beforehand.
     */
    fun getMoveList(moveIndex: Int): List<Color> {
        val moves = ArrayList<Color>()

        var index = moveIndex
        while (index != NO_MOVE_INDEX) {
            moves += Color.colorCache[moveColorArray[index].toInt()]
            index = moveIndexArray[index]
        }
        moves.reverse()

        return moves
    }
}