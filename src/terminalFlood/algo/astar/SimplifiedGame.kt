package terminalFlood.algo.astar

import terminalFlood.game.*
import java.util.*

/**
 * This is a stripped down version of a [GameState] that only contains the bare minimum functionality necessary for
 * [AdmissibleStrategy] to have better performance characteristics.
 *
 * This class shouldn't be used to actually play the game.
 *
 * This class doesn't implement hashCode and equals. This class is not thread safe.
 */
class SimplifiedGame(
    val gameBoard: GameBoard,
    val filled: BitSet,
    val neighbors: BitSet,
    val notFilledNotNeighbors: BitSet
) {
    private val cachedBitset = BitSet(gameBoard.amountOfNodes)

    /**
     * Creates a [SimplifiedGame] instance from the given [GameState]. It does so safely by creating deep copies of
     * the necessary state.
     */
    constructor(gameState: GameState) : this(
        gameState.gameBoard,
        gameState.filled.copy(),
        gameState.neighbors.copy(),
        gameState.notFilledNotNeighbors.copy()
    )

    val isWon: Boolean
        get() = neighbors.cardinality() == 0

    fun makeMultiColorMove(colorSet: ColorSet) {
        val firstColorValue = colorSet.nextSetBit(0)
        cachedBitset.clear()
        cachedBitset.or(gameBoard.boardNodesByColor[firstColorValue])
        colorSet.forEachSetBit(firstColorValue + 1) { colorValue ->
            cachedBitset.or(gameBoard.boardNodesByColor[colorValue])
        }
        cachedBitset.and(neighbors)
        computeMove(cachedBitset)
    }

    fun makeColorBlindMove() {
        cachedBitset.clear()
        cachedBitset.or(neighbors)
        computeMove(cachedBitset)
    }

    private fun computeMove(newNodes: BitSet) {
        filled.or(newNodes)
        newNodes.forEachNode(gameBoard) { neighbors.or(it.borderingNodes) }
        neighbors.andNot(filled)
        notFilledNotNeighbors.andNot(neighbors)
    }
}