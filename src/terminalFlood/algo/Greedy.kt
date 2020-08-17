package terminalFlood.algo

import terminalFlood.game.ColorSet
import terminalFlood.game.Game
import terminalFlood.game.NodeSet

/**
 * A simple greedy algorithm to find solutions for Flood-It boards.
 *
 * This algorithm chooses the move that will either remove a color from the board or give access to the most new
 * border fields as its next move.
 */
object Greedy {

    /**
     * Returns the total amount of moves needed to reach a winning state. This includes both the amount of moves already
     * played and the additional amount of moves this algorithm computes.
     */
    fun calculateAmountOfMovesNeeded(gameState: Game): Int {
        val currentState = gameState.toSimpleBoardState()
        var neighborNodes = NodeSet(currentState.gameBoard.amountOfNodes)
        var bestColorNodes = NodeSet(currentState.gameBoard.amountOfNodes)
        val newBorderNodes = NodeSet(currentState.gameBoard.amountOfNodes)
        val notEliminatedColors = ColorSet()
        notEliminatedColors.setToNotEliminatedColors(gameState)
        val colorEliminationMoves = ColorSet()
        var amountOfMoves = gameState.playedMoves.size

        while (!currentState.isWon) {
            // If we can eliminate colors, that is always the optimal move.
            // If we eliminated colors, start the loop over.
            colorEliminationMoves.setToColorEliminations(currentState, notEliminatedColors)
            if (!colorEliminationMoves.isEmpty) {
                currentState.makeMultiColorMove(colorEliminationMoves)
                notEliminatedColors.andNot(colorEliminationMoves)
                amountOfMoves += colorEliminationMoves.size
                continue
            }

            // If we couldn't eliminate colors, find the move that results in the most amount of new border fields.
            var amountBestColor = Int.MIN_VALUE
            notEliminatedColors.forEachColor { move ->
                neighborNodes.setToNeighborsWithColor(currentState, move)
                if (!neighborNodes.isEmpty) {
                    newBorderNodes.clear()
                    neighborNodes.forEachNode(currentState.gameBoard) { node ->
                        newBorderNodes.or(node.borderingNodes)
                    }
                    newBorderNodes.and(currentState.notFilledNotNeighbors)

                    var amountOfNewFields = 0
                    newBorderNodes.forEachNode(currentState.gameBoard) { node ->
                        amountOfNewFields += node.amountOfFields
                    }

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