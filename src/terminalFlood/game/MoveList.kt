package terminalFlood.game

/**
 * A simple immutable linked list implementation for played moves.
 *
 * A linked list because moves are only ever appended which is a very cheap operation for this data structure.
 * Immutable because every game state is immutable and using the standard List<T> implementations would make it
 * necessary to allocate a completely new list for every state.
 */
sealed class MoveList {
    abstract val size: Int

    abstract val lastMove: Color

    abstract val previousMoves: MoveList

    val isEmpty: Boolean
        get() = size == 0

    operator fun plus(move: Color): MoveList = MoveListImpl(size + 1, move, this)

    /**
     * Returns an array representation of this MoveList with its elements in the order they were added to this MoveList.
     */
    fun toArray(): ColorArray {
        if (isEmpty)
            return ColorArray.EMPTY

        val result = ByteArray(size)
        var index = this
        while (!index.isEmpty) {
            result[index.size - 1] = index.lastMove.value
            index = index.previousMoves
        }

        return ColorArray(result)
    }

    /**
     * Iterates over the elements of this MoveList in the order they were added to this MoveList.
     */
    inline fun forEach(action: (Color) -> Unit) {
        toArray().forEach { action(it) }
    }

    override fun toString(): String = toArray().toString()

    companion object {
        fun emptyMoveList(): MoveList = EmptyMoveList

        fun moveListOf(vararg colorValues: Byte): MoveList {
            var moveList = emptyMoveList()
            for (colorValue in colorValues)
                moveList += Color(colorValue)

            return moveList
        }
    }
}

private object EmptyMoveList : MoveList() {
    override val size: Int = 0

    override val lastMove: Nothing
        get() = throw NoSuchElementException("The move list is empty!")

    override val previousMoves: Nothing
        get() = throw NoSuchElementException("The move list is empty!")
}

private data class MoveListImpl(
    override val size: Int,
    override val lastMove: Color,
    override val previousMoves: MoveList
) : MoveList()