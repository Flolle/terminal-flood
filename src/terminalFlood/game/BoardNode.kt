package terminalFlood.game

import java.util.*

/**
 * Represents a continuous collection of fields on the game board that all have the same color.
 *
 * Do not try to modify the [BitSet] returned by this interface, as no guarantees are made towards whether the
 * [BoardNode]s internal state will stay valid if you do. Instead, always create copies of the BitSet if you
 * intend to use modifying operations on it.
 *
 * The API defined by this interface should be treated as read-only.
 */
interface BoardNode : Comparable<BoardNode> {
    val color: Color
    val borderingNodes: BitSet
    val occupiedFields: List<Point>

    /**
     * All [BoardNode]s of a [GameBoard] should have a continuously increasing id value starting from 0 up until
     * the amount of [BoardNode]s minus 1 with no [BoardNode] having the same id.
     *
     * The id is for example used by [GameState]s to have easy reference links between array indexes and [BoardNode]s.
     */
    val id: Int

    override fun compareTo(other: BoardNode): Int {
        if (color != other.color)
            return color.compareTo(other.color)
        if (occupiedFields.size != other.occupiedFields.size)
            return occupiedFields.size - other.occupiedFields.size
        for (i in occupiedFields.indices) {
            val pointComp = occupiedFields[i].compareTo(other.occupiedFields[i])
            if (pointComp != 0)
                return pointComp
        }

        return 0
    }

    companion object {
        val DUMMY_NODE: BoardNode = object : BoardNode {
            override val color: Color = Color.DUMMY
            override val borderingNodes: BitSet = BitSet()
            override val occupiedFields: List<Point> = emptyList()
            override val id: Int = -1
        }
    }
}