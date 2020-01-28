package terminalFlood.game

import java.util.*

/**
 * This class represents any given Flood-It game state.
 *
 * No methods or accessors of this class modify its internal state and making moves will create new [Game] objects.
 * As a result, instances can be freely shared. If you do not need to share or remember previous game states, you can
 * use [MutableGame] instead to take advantage of its better performance characteristics. Use [toMutableGame] to
 * create [MutableGame] instances from [Game] objects.
 *
 * Warning:
 * Do not modify the exposed bitmaps or [Array]s, since doing so will invalidate the game state. If you want to do
 * modifying operations on those collections, you must create copies of them.
 *
 * This class is thread safe.
 */
class Game(
    override val gameBoard: GameBoard,
    override val playedMoves: MoveList<Color>,
    override val filled: BitSet,
    override val amountOfTakenNodes: Int,
    override val neighbors: BitSet,
    override val neighborsByColor: Array<BitSet?>,
    override val notFilledNotNeighbors: BitSet,
    override val sensibleMoves: ColorSet
) : GameState {
    /**
     * @see [GameState.makeMove]
     */
    override fun makeMove(move: Color): Game {
        val newNodes = neighborsByColor[move.value]!!
        var newAmountOfTakenNodes = amountOfTakenNodes
        val newFilled = filled.clone() as BitSet
        val newNeighbors = neighbors.clone() as BitSet
        newFilled.or(newNodes)
        newNodes.forEachNode(gameBoard) {
            newAmountOfTakenNodes++
            newNeighbors.or(it.borderingNodes)
        }
        newNeighbors.andNot(newFilled)
        val newNeighborsByColor = GameState.createNeighborsByColorArray(gameBoard, newNeighbors)
        val newNotFilledNotNeighbors = notFilledNotNeighbors.clone() as BitSet
        newNotFilledNotNeighbors.andNot(newNeighbors)

        return Game(
            gameBoard,
            playedMoves + move,
            newFilled,
            newAmountOfTakenNodes,
            newNeighbors,
            newNeighborsByColor,
            newNotFilledNotNeighbors,
            GameState.createSensibleMovesSet(newNeighborsByColor)
        )
    }

    /**
     * Returns a mutable version of this game state.
     */
    fun toMutableGame(): MutableGame {
        val newNeighborsByColor = arrayOfNulls<BitSet>(neighborsByColor.size)
        neighborsByColor.forEachIndexed { index, colorSet ->
            if (colorSet != null)
                newNeighborsByColor[index] = colorSet.clone() as BitSet
        }

        return MutableGame(
            gameBoard,
            playedMoves,
            filled.clone() as BitSet,
            amountOfTakenNodes,
            neighbors.clone() as BitSet,
            newNeighborsByColor,
            notFilledNotNeighbors.clone() as BitSet,
            sensibleMoves.copy()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Game

        if (amountOfTakenNodes != other.amountOfTakenNodes) return false
        if (playedMoves != other.playedMoves) return false
        if (gameBoard != other.gameBoard) return false
        if (filled != other.filled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = gameBoard.hashCode()
        result = 31 * result + playedMoves.hashCode()
        result = 31 * result + filled.hashCode()
        return result
    }

    companion object {
        /**
         * Creates a new [Game] instance based on the given [GameBoard].
         */
        operator fun invoke(gameBoard: GameBoard): Game {
            val filled = BitSet(gameBoard.boardNodes.size)
            val neighbors = BitSet(gameBoard.boardNodes.size)
            val startNode = gameBoard.getNodeAtPosition(gameBoard.startPos.x, gameBoard.startPos.y)
            filled.set(startNode.id)
            neighbors.or(startNode.borderingNodes)
            val neighborsByColor = GameState.createNeighborsByColorArray(gameBoard, neighbors)
            val notFilledNotNeighbors = BitSet(gameBoard.boardNodes.size)
            notFilledNotNeighbors.set(0, gameBoard.boardNodes.size)
            notFilledNotNeighbors.andNot(filled)
            notFilledNotNeighbors.andNot(neighbors)

            return Game(
                gameBoard,
                MoveList.emptyMoveList(),
                filled,
                1,
                neighbors,
                neighborsByColor,
                notFilledNotNeighbors,
                GameState.createSensibleMovesSet(neighborsByColor)
            )
        }
    }
}