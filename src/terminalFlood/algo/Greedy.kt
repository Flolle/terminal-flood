package terminalFlood.algo

import terminalFlood.game.*

/**
 * A simple greedy algorithm to find solutions for Flood-It boards.
 *
 * This algorithm chooses the move that will either remove a color from the board or give access to the most new
 * border fields as its next move.
 */
object Greedy {
    fun calculateMoves(gameBoard: GameBoard): GameState = calculateMoves(Game(gameBoard))

    fun calculateMoves(startingState: Game): GameState = calculateMoves(startingState.toMutableGame())

    fun calculateMoves(gameState: MutableGame): MutableGame {
        val moveBorderNodes = NodeSet(gameState.gameBoard.amountOfNodes)
        val newBorderNodes = NodeSet(gameState.gameBoard.amountOfNodes)

        while (!gameState.isWon) {
            // If we can eliminate colors, that is always the optimal move.
            // Note: We can't use makeMultiColorMove because we actually need to log every single move.
            var isColorEliminationFound = false
            gameState.findAllColorEliminationMoves().forEachColor {
                isColorEliminationFound = true
                gameState.makeMove(it)
            }

            // If we eliminated colors, start the loop over.
            if (isColorEliminationFound)
                continue

            // If we couldn't eliminate colors, find the move that results in the most amount of new border fields.
            var bestColor = Color.DUMMY
            var amountBestColor = Int.MIN_VALUE
            gameState.sensibleMoves.forEachColor { move ->
                moveBorderNodes.setToNeighborsWithColor(gameState, move)
                newBorderNodes.clear()
                moveBorderNodes.forEachNode(gameState.gameBoard) { node ->
                    newBorderNodes.or(node.borderingNodes)
                }
                newBorderNodes.and(gameState.notFilledNotNeighbors)

                var amountOfNewFields = 0
                newBorderNodes.forEachNode(gameState.gameBoard) { node ->
                    amountOfNewFields += node.amountOfFields
                }

                if (amountOfNewFields > amountBestColor) {
                    bestColor = move
                    amountBestColor = amountOfNewFields
                }
            }

            gameState.makeMove(bestColor)
        }

        return gameState
    }
}