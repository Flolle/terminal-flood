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
    fun toArray(): Array<Color> {
        if (isEmpty)
            return emptyArray()

        val result = Array(size) { Color.DUMMY }
        var index = this
        while (!index.isEmpty) {
            result[index.size - 1] = index.lastMove
            index = index.previousMoves
        }

        return result
    }

    /**
     * Returns a list representation of this MoveList with its elements in the order they were added to this MoveList.
     */
    fun toList(): List<Color> = toArray().asList()

    /**
     * Iterates over the elements of this MoveList in the order they were added to this MoveList.
     */
    inline fun forEach(action: (Color) -> Unit) {
        for (color in toArray())
            action(color)
    }

    override fun toString(): String = toList().toString()

    companion object {
        fun emptyMoveList(): MoveList = EmptyMoveList

        fun moveListOf(vararg elements: Color): MoveList {
            var moveList = emptyMoveList()
            for (element in elements)
                moveList += element

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

private class MoveListImpl(
    override val size: Int,
    override val lastMove: Color,
    override val previousMoves: MoveList
) : MoveList() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MoveListImpl

        if (size != other.size) return false
        if (lastMove != other.lastMove) return false
        if (previousMoves != other.previousMoves) return false

        return true
    }

    override fun hashCode(): Int {
        var result = size
        result = 31 * result + lastMove.hashCode()
        result = 31 * result + previousMoves.hashCode()
        return result
    }
}