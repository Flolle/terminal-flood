package terminalFlood.algo

import terminalFlood.game.ColorSet
import terminalFlood.game.GameState
import terminalFlood.game.NodeSet

/**
 * A simple greedy algorithm to find solutions for Flood-It boards.
 *
 * This algorithm chooses the move that will either remove a color from the board or give access to the most new
 * border fields as its next move.
 */
object Greedy {

    /**
     * Returns the amount of moves needed to reach a winning state from the given [GameState].
     */
    fun calculateAmountOfMovesNeeded(gameState: GameState): Int {
        val currentState = gameState.toSimpleBoardState()
        var neighborNodes = NodeSet(currentState.gameBoard.amountOfNodes)
        var bestColorNodes = NodeSet(currentState.gameBoard.amountOfNodes)
        val newBorderNodes = NodeSet(currentState.gameBoard.amountOfNodes)
        var notEliminatedColors = ColorSet.getNotEliminatedColors(gameState)
        var amountOfMoves = 0

        while (!currentState.isWon) {
            // If we can eliminate colors, that is always the optimal move.
            // If we eliminated colors, start the loop over.
            val colorEliminationMoves = ColorSet.getColorEliminations(currentState, notEliminatedColors)
            if (!colorEliminationMoves.isEmpty) {
                currentState.makeMultiColorMove(colorEliminationMoves)
                notEliminatedColors = notEliminatedColors.andNot(colorEliminationMoves)
                amountOfMoves += colorEliminationMoves.size
                continue
            }

            // If we couldn't eliminate colors, find the move that results in the most amount of new border fields.
            var amountBestColor = Int.MIN_VALUE
            notEliminatedColors.forEachColor { move ->
                neighborNodes.setToNeighborsWithColor(currentState, move)
                if (!neighborNodes.isEmpty) {
                    newBorderNodes.setToBorderingNodesOf(currentState.gameBoard, neighborNodes)
                    newBorderNodes.and(currentState.notFilledNotNeighbors)

                    val amountOfNewFields = newBorderNodes.amountOfFields(currentState.gameBoard)
                    if (amountOfNewFields > amountBestColor) {
                        amountBestColor = amountOfNewFields
                        val t = bestColorNodes
                        bestColorNodes = neighborNodes
                        neighborNodes = t
                    }
                }
            }

            currentState.takeGivenNodes(bestColorNodes)
            amountOfMoves++
        }

        return amountOfMoves
    }
}