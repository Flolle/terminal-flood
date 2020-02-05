package terminalFlood

import terminalFlood.game.Color
import terminalFlood.game.Game
import terminalFlood.game.GameBoard
import terminalFlood.game.GameState
import java.util.*

object PlayGame {
    fun play(gameBoard: GameBoard) {
        println("Thank you for playing terminal-flood!\n")
        println("You start in one of the corners or the middle of the board, every field you own is denoted")
        println("by an \"@\". Not taken fields consist of groups with the same color denoted by a number.")
        println("Every turn you choose a color and every bordering group of that color will be taken over.")
        println("You win the game by taking over the whole board within the allotted number of moves.\n\n")

        val colorMaxValue = gameBoard.maximumColorValue.toString(Character.MAX_RADIX)
        val gameStack = Stack<Game>()
        gameStack.push(Game(gameBoard))

        while (!gameStack.peek().isFinished) {
            printCurrentState(gameStack.peek())

            print("\nNext move (enter character between 1-$colorMaxValue")
            if (gameStack.size > 1)
                print(" or undo):")
            else
                print("):")

            val input = readLine() ?: error("Couldn't read input!")
            val inputStr = input.trim()

            if (inputStr == "undo") {
                if (gameStack.size == 1) {
                    println("No moves played. Cannot undo!")
                } else {
                    println("Undoing last move.")
                    gameStack.pop()
                }
            } else {
                try {
                    if (inputStr.length > 1)
                        throw NumberFormatException()
                    if (java.lang.String.valueOf(inputStr[0]).toInt(Character.MAX_RADIX) > gameBoard.maximumColorValue)
                        throw IllegalArgumentException()

                    val currentGame = gameStack.peek()
                    val move = Color.fromValue(inputStr[0])
                    if (currentGame.sensibleMoves[move.value])
                        gameStack.push(currentGame.makeMove(move))
                    else
                        println("Move doesn't do anything!")
                } catch (ex: NumberFormatException) {
                    println("Only characters between 1-$colorMaxValue and undo are valid inputs!")
                } catch (ex: IllegalArgumentException) {
                    println("The specified move is outside the color range of this game! (range: 1-$colorMaxValue)")
                }
            }

            println("\n#############################################################\n\n")
        }

        printCurrentState(gameStack.peek())

        if (gameStack.peek().isWon)
            println("\nYou've won the game!")
        else
            println("\nYou've lost! Better luck next time.")
    }

    private fun printCurrentState(gameState: GameState) {
        println("Game board:\n")
        for (y in 0 until gameState.gameBoard.boardSize) {
            for (x in 0 until gameState.gameBoard.boardSize) {
                val node = gameState.gameBoard.getNodeAtPosition(x, y)
                if (gameState.filled[node.id])
                    print("@")
                else
                    print(node.color)
            }
            println()
        }
        println("\nPlayed moves: ${gameState.playedMoves}")
        println("Number of moves left: ${gameState.gameBoard.maximumSteps - gameState.playedMoves.size}")
    }
}