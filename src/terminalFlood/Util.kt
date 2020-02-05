package terminalFlood

import terminalFlood.algo.astar.AStar
import terminalFlood.algo.astar.AStarStrategies
import terminalFlood.game.Game
import terminalFlood.game.GameBoard
import java.util.concurrent.ThreadLocalRandom
import kotlin.system.measureTimeMillis

object Util {
    fun strategyFromString(str: String): AStarStrategies = when (str) {
        "astar_a"    -> AStarStrategies.ADMISSIBLE
        "astar_ia"   -> AStarStrategies.INADMISSIBLE
        "astar_iaf"  -> AStarStrategies.INADMISSIBLE_FAST
        "astar_iaff" -> AStarStrategies.INADMISSIBLE_FASTEST
        "astar_ias"  -> AStarStrategies.INADMISSIBLE_SLOW
        else         -> throw IllegalArgumentException("Incorrect value for simulation method!")
    }

    fun generateRandomSeed(): String {
        val seedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789"
        val rng = ThreadLocalRandom.current()

        val seedLength = 5 + rng.nextInt(11)
        val seed = StringBuilder(15)
        for (i in 0 until seedLength)
            seed.append(seedChars[rng.nextInt(seedChars.length)])

        return seed.toString()
    }

    fun aStarSimulation(gameBoard: GameBoard, strategy: AStarStrategies) {
        println("AStar $strategy")
        var simResult: Game? = null
        val t = measureTimeMillis {
            simResult = AStar.calculateMovesParallel(gameBoard, strategy)
        }
        simResult?.let {
            val isFoundWin = it.playedMoves.size <= gameBoard.maximumSteps
            println("Time taken: ${t}ms")
            println("Winning move found: $isFoundWin")
            println("Play: ${it.playedMoves}")
            println("Amount of moves: ${it.playedMoves.size}")
            if (!isFoundWin)
                println("Couldn't find win within maximum amount of moves!")
        }
    }
}