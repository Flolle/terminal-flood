package terminalFlood

import terminalFlood.algo.astar.AStar
import terminalFlood.game.Color
import terminalFlood.game.ColorArray
import terminalFlood.game.Game
import terminalFlood.game.GameBoard
import java.util.*
import kotlin.system.exitProcess

object PlayGame {
    fun play(gameBoard: GameBoard) {
        println("Thank you for playing terminal-flood!\n")
        println("You start in one of the corners or the middle of the board, every field you own is denoted")
        println("by an \"@\". Not taken fields consist of groups with the same color denoted by a number.")
        println("Every turn you choose a color and every bordering group of that color will be taken over.")
        println("You win the game by taking over the whole board within the allotted number of moves.\n\n")

        val colorMaxValue = gameBoard.maximumColorValue.toString(Character.MAX_RADIX)
        val gameStack = ArrayDeque<GameAndHint>()
        gameStack.push(GameAndHint(Game(gameBoard)))

        while (!gameStack.peek().game.isFinished) {
            val currentGame = gameStack.peek().game
            val currentHint = gameStack.peek().hint
            printCurrentState(currentGame)

            if (currentHint != null) {
                val amountOfMovesMade = currentGame.amountOfMovesMade
                println("\nThe game can be finished in ${currentHint.size - amountOfMovesMade} or less moves.")
                println("The recommended move is: ${currentHint[amountOfMovesMade]}")
            }

            print("\nNext move (enter character between 1-$colorMaxValue or hint")
            if (gameStack.size > 1)
                print("/undo):")
            else
                print("):")

            val input = readlnOrNull()
            // Ctrl+C seems to result in a null value, so we print a short message and exit the program in that case.
            if (input == null) {
                println("Invalid or null input, ending program.")
                exitProcess(0)
            }

            val inputStr = input.trim()

            if (inputStr == "exit" || inputStr == "quit") {
                println("Ending program.")
                exitProcess(0)
            } else if (inputStr == "undo") {
                if (gameStack.size == 1) {
                    println("No moves played. Cannot undo!")
                } else {
                    println("Undoing last move.")
                    gameStack.pop()
                }
            } else if (inputStr == "hint") {
                println("Computing move. This may take a moment...")
                gameStack.peek().hint = AStar.calculateMoves(currentGame)
            } else {
                try {
                    if (inputStr.length != 1)
                        throw NumberFormatException()
                    if (inputStr[0].digitToInt(Character.MAX_RADIX) > gameBoard.maximumColorValue)
                        throw IllegalArgumentException()

                    val move = Color.fromValue(inputStr[0])
                    if (move in currentGame.sensibleMoves)
                        gameStack.push(GameAndHint(currentGame.makeMove(move)))
                    else
                        println("Move doesn't do anything!")
                } catch (ex: NumberFormatException) {
                    println("Only characters between 1-$colorMaxValue and hint/undo are valid inputs!")
                } catch (ex: IllegalArgumentException) {
                    println("The specified move is outside the color range of this game! (range: 1-$colorMaxValue)")
                }
            }

            println("\n#############################################################\n\n")
        }

        printCurrentState(gameStack.peek().game)

        if (gameStack.peek().game.isWon)
            println("\nYou've won the game!")
        else
            println("\nYou've lost! Better luck next time.")
    }

    private fun printCurrentState(gameState: Game) {
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

private data class GameAndHint(
    val game: Game,
    var hint: ColorArray? = null
)