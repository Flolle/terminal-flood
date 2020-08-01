package terminalFlood.game

import java.util.*

/**
 * This interface defines all the necessary data to represent any given Flood-It game state.
 */
interface GameState {
    /**
     * The game board of this game.
     */
    val gameBoard: GameBoard

    /**
     * The moves played so far.
     */
    val playedMoves: MoveList

    /**
     * The nodes already taken over.
     *
     * [BoardNode]s and this bitmap are linked through [BoardNode.id], meaning the id is used as the bit index.
     */
    val filled: BitSet

    /**
     * The nodes bordering [filled].
     *
     * [BoardNode]s and this bitmap are linked through [BoardNode.id], meaning the id is used as the bit index.
     */
    val neighbors: BitSet

    /**
     * The nodes that are neither in [filled] nor in [neighbors]. Basically all not taken nodes that don't directly
     * border the filled nodes.
     *
     * [BoardNode]s and this bitmap are linked through [BoardNode.id], meaning the id is used as the bit index.
     */
    val notFilledNotNeighbors: BitSet

    /**
     * All the moves that would make sense to play in this game state. In effect this bitmap contains all the
     * distinct colors of the nodes in [neighbors].
     *
     * [Color]s and this bitmap are linked through [Color.value], meaning the value is used as the bit index.
     */
    val sensibleMoves: ColorSet

    /**
     * Returns true if the game is won. Games are considered won if there are no more bordering nodes.
     */
    val isWon: Boolean
        get() = sensibleMoves.isEmpty

    /**
     * Returns true if the game is finished. Games are considered finished if they are won or if the move limit
     * is reached.
     */
    val isFinished: Boolean
        get() = playedMoves.size == gameBoard.maximumSteps || isWon

    /**
     * The amount of fields already filled.
     *
     * Please note that this property is computed on every call and has O(n) runtime, where n is the amount of nodes in
     * [filled].
     */
    val amountOfTakenFields: Int
        get() {
            var amountOfTakenFields = 0
            filled.forEachNode(gameBoard) {
                amountOfTakenFields += it.occupiedFields.size
            }

            return amountOfTakenFields
        }

    /**
     * Makes a move that takes all [neighbors] of the given color and returns the new state.
     */
    fun makeMove(move: Color): GameState

    /**
     * Returns a collection of all the colors that can be completely eliminated.
     */
    fun findAllColorEliminationMoves(): ColorSet {
        val presentColors = sensibleMoves.copy()

        sensibleMoves.forEachSetBit { colorValue ->
            if (gameBoard.boardNodesByColor[colorValue].intersects(notFilledNotNeighbors))
                presentColors.clear(colorValue)
        }

        return presentColors
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun BitSet.copy(): BitSet = this.clone() as BitSet

/**
 * Sets this bitset to the neighboring [BoardNode]s with the given [Color]. The bitset will be empty if no nodes with
 * that color are present.
 *
 * [BoardNode]s and this bitset are linked through [BoardNode.id], meaning the id is used as the bit index.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun BitSet.setToNeighborsWithColor(gameState: GameState, color: Color) {
    this.clear()
    this.or(gameState.gameBoard.boardNodesByColor[color.value])
    this.and(gameState.neighbors)
}

/**
 * Performs the given action on every node in the [BitSet].
 *
 * Nodes are found by using the index of a bit with [GameBoard.boardNodes].
 */
inline fun BitSet.forEachNode(gameBoard: GameBoard, action: (node: BoardNode) -> Unit) {
    var i = this.nextSetBit(0)
    while (i >= 0) {
        action(gameBoard.boardNodes[i])
        i = this.nextSetBit(i + 1)
    }
}