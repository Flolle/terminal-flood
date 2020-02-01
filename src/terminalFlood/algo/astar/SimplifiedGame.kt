package terminalFlood.algo.astar

import terminalFlood.game.ColorSet
import terminalFlood.game.GameBoard
import terminalFlood.game.GameState
import terminalFlood.game.forEachNode
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
        gameState.filled.clone() as BitSet,
        gameState.neighbors.clone() as BitSet,
        gameState.notFilledNotNeighbors.clone() as BitSet
    )

    val isWon: Boolean
        get() = neighbors.cardinality() == 0

    fun makeMultiColorMove(colorSet: ColorSet) {
        val newNodes = BitSet()
        val oldNeighbors = neighbors.clone() as BitSet
        oldNeighbors.forEachNode(gameBoard) { node ->
            if (colorSet[node.color.value]) {
                newNodes.set(node.id)
                neighbors.or(node.borderingNodes)
            }
        }
        filled.or(newNodes)
        neighbors.andNot(filled)
        notFilledNotNeighbors.andNot(neighbors)
    }

    fun makeColorBlindMove() {
        val oldNeighbors = neighbors.clone() as BitSet
        filled.or(neighbors)
        oldNeighbors.forEachNode(gameBoard) { neighbors.or(it.borderingNodes) }
        neighbors.andNot(filled)
        notFilledNotNeighbors.andNot(neighbors)
    }
}