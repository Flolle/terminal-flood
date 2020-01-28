package terminalFlood.game

/**
 * A simple immutable linked list implementation for played moves.
 *
 * A linked list because moves are only ever appended which is a very cheap operation for this data structure.
 * Immutable because every game state is immutable and using the standard List<T> implementations would make it
 * necessary to allocate a completely new list for every state.
 */
sealed class MoveList<out T> {
    abstract val size: Int
    abstract val lastMove: T
    abstract val previousMoves: MoveList<T>

    val isEmpty: Boolean
        get() = size == 0

    fun toList(): List<T> {
        if (isEmpty)
            return emptyList()

        val result = ArrayList<T>(size)
        var index = this
        while (!index.isEmpty) {
            result.add(index.lastMove)
            index = index.previousMoves
        }
        result.reverse()

        return result
    }

    override fun toString(): String = toList().toString()

    companion object {
        fun <T> emptyMoveList(): MoveList<T> = EmptyMoveList

        fun <T> moveListOf(vararg elements: T): MoveList<T> {
            var moveList = emptyMoveList<T>()
            for (element in elements)
                moveList += element

            return moveList
        }
    }
}

operator fun <T> MoveList<T>.plus(move: T): MoveList<T> = MoveListImpl(size + 1, move, this)

private object EmptyMoveList : MoveList<Nothing>() {
    override val size: Int = 0

    override val lastMove: Nothing
        get() = throw NoSuchElementException("The move list is empty!")

    override val previousMoves: MoveList<Nothing>
        get() = throw NoSuchElementException("The move list is empty!")
}

private class MoveListImpl<T>(
    override val size: Int,
    override val lastMove: T,
    override val previousMoves: MoveList<T>
) : MoveList<T>() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MoveListImpl<*>

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