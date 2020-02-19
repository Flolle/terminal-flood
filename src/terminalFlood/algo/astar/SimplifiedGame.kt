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
        val newNodes = gameBoard.boardNodesByColor[firstColorValue].copy()
        colorSet.forEachSetBit(firstColorValue + 1) { colorValue ->
            newNodes.or(gameBoard.boardNodesByColor[colorValue])
        }
        newNodes.and(neighbors)
        computeMove(newNodes)
    }

    fun makeColorBlindMove() {
        computeMove(neighbors.copy())
    }

    private fun computeMove(newNodes: BitSet) {
        filled.or(newNodes)
        newNodes.forEachNode(gameBoard) { neighbors.or(it.borderingNodes) }
        neighbors.andNot(filled)
        notFilledNotNeighbors.andNot(neighbors)
    }
}