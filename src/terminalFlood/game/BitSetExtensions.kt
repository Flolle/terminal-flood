package terminalFlood.game

import java.util.*

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
        action(gameBoard.getNodeWithIndex(i))
        i = this.nextSetBit(i + 1)
    }
}