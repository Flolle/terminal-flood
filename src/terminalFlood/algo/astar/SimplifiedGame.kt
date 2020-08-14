package terminalFlood.algo.astar

import terminalFlood.game.ColorSet
import terminalFlood.game.GameBoard
import terminalFlood.game.GameState
import terminalFlood.game.NodeSet

/**
 * This is a stripped down version of a [GameState] that only contains the bare minimum functionality necessary for
 * [AdmissibleStrategy] and [InadmissibleSlowStrategy] to have better performance characteristics.
 *
 * This class shouldn't be used to actually play the game.
 *
 * This class doesn't implement hashCode and equals. This class is not thread safe.
 */
class SimplifiedGame(
    val gameBoard: GameBoard,
    val filled: NodeSet,
    val neighbors: NodeSet,
    val notFilledNotNeighbors: NodeSet
) {
    private val cachedBitset = NodeSet(gameBoard.amountOfNodes)

    /**
     * Creates an empty [SimplifiedGame] instance with the given [GameBoard].
     */
    constructor(gameBoard: GameBoard) : this(
        gameBoard,
        NodeSet(gameBoard.amountOfNodes),
        NodeSet(gameBoard.amountOfNodes),
        NodeSet(gameBoard.amountOfNodes)
    )

    val isWon: Boolean
        get() = neighbors.cardinality == 0

    /**
     * Sets the state of this SimplifiedGame to the state of the given [GameState].
     *
     * Please note that this SimplifiedGame and the given [GameState] have to be based on the [GameBoard], otherwise
     * this method may throw an exception or this SimplifiedGame will end up in an invalid state.
     */
    fun setToState(gameState: GameState) {
        filled.setTo(gameState.filled)
        neighbors.setTo(gameState.neighbors)
        notFilledNotNeighbors.setTo(gameState.notFilledNotNeighbors)
    }

    /**
     * Makes a move that takes all neighboring nodes of the given colors.
     */
    fun makeMultiColorMove(colorSet: ColorSet) {
        val firstColorValue = colorSet.nextSetBit(0)
        cachedBitset.setTo(gameBoard.boardNodesByColor[firstColorValue])
        colorSet.forEachSetBit(firstColorValue + 1) { colorValue ->
            cachedBitset.or(gameBoard.boardNodesByColor[colorValue])
        }
        cachedBitset.and(neighbors)
        computeMove(cachedBitset)
    }

    /**
     * Makes a move that ignores all colors. This move takes all neighboring nodes of this game state.
     */
    fun makeColorBlindMove() {
        cachedBitset.setTo(neighbors)
        computeMove(cachedBitset)
    }

    /**
     * Makes a move that takes the given nodes.
     */
    fun takeGivenNodes(nodes: NodeSet) {
        computeMove(nodes)
    }

    /**
     * Updates the internal state.
     */
    private fun computeMove(newNodes: NodeSet) {
        filled.or(newNodes)
        if (filled.cardinality < gameBoard.amountOfNodes) {
            newNodes.forEachNode(gameBoard) { neighbors.or(it.borderingNodes) }
            neighbors.andNot(filled)
            notFilledNotNeighbors.andNot(neighbors)
        } else {
            neighbors.clear()
            notFilledNotNeighbors.clear()
        }
    }
}