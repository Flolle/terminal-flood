package terminalFlood

import terminalFlood.algo.astar.AStar
import terminalFlood.algo.astar.AStarStrategies
import terminalFlood.game.Color
import terminalFlood.game.GameBoard
import terminalFlood.game.MoveList
import terminalFlood.game.StartPos
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

object Datasets {
    fun createDataset(file: Path, boardSize: Int, numberOfColors: Int, numberOfBoards: Int) {
        if (Files.isDirectory(file))
            throw IllegalArgumentException("The file is a directory!")

        if (Files.exists(file))
            Files.delete(file)

        println("Creating dataset.")
        println("Board dimensions: ${boardSize}x$boardSize")
        println("Number of colors: $numberOfColors")
        println("Number of boards to be created: $numberOfBoards")

        Files.newBufferedWriter(file, Charsets.UTF_8).use { outputWriter ->
            for (i in 0 until numberOfBoards) {
                // Writing random numbers directly to the file would be more efficient, but this way we have automatic
                // validation of the boards and this isn't really performance critical code unless you want to create
                // a million or more boards.
                val seed = Util.generateRandomSeed()
                val gameBoard = GameBoard.initBoard(
                    seed = seed,
                    boardSize = boardSize,
                    numberOfColors = numberOfColors,
                    startPos = StartPos.UPPER_LEFT // Doesn't matter
                )

                outputWriter.appendln(gameBoard.createCompactString())

                val index = i + 1
                if (index % 100 == 0)
                    println("$index boards finished")
            }
        }

        println("Dataset created and saved to: $file")
    }

    fun findSolutionsForDataset(file: Path, threads: Int, strategy: AStarStrategies, startPos: StartPos) {
        if (!Files.exists(file) || Files.isDirectory(file))
            throw IllegalArgumentException("The file doesn't exist or is a directory!")

        println("Finding solutions for the given dataset: $file")
        println("Number of threads used: $threads")
        println("Start position on the game boards: $startPos")
        println("Using strategy: $strategy")

        val executor = Executors.newFixedThreadPool(threads)
        val promisedGames = ArrayList<Future<GameResult>>(100_000)
        val index = AtomicInteger(0)
        val t = System.nanoTime()

        Files.lines(file, Charsets.UTF_8).use { lines ->
            lines
                .map { line -> line.trim() }
                .filter { line -> line.isNotEmpty() }
                .forEach { line ->
                    promisedGames.add(executor.submit<GameResult> {
                        val gameBoard = GameBoard.createBoardFromCompactString(line, startPos, Int.MAX_VALUE)
                        val finishedGame = AStar.calculateMovesSequential(gameBoard, strategy)

                        val i = index.incrementAndGet()
                        if (i % 100 == 0)
                            println("$i boards finished")

                        GameResult(finishedGame.isWon, finishedGame.playedMoves)
                    })
                }
        }

        val finishedGames = promisedGames.map { it.get() }
        val time = System.nanoTime() - t
        executor.shutdown()

        val output = file.parent.resolve("solutions.txt")
        if (Files.exists(output))
            Files.delete(output)

        Files.newBufferedWriter(output, Charsets.UTF_8).use { outputWriter ->
            for (game in finishedGames) {
                if (!game.isWon)
                    outputWriter.append("game not won")
                else
                    game.playedMoves.toList().forEach { outputWriter.append(it.toString()) }
                outputWriter.newLine()
            }
        }

        println("\n\nSolutions saved to: $output")
        println("Time taken: ${(time / 1_000_000.0).roundToInt()}ms")
        println("Total score: ${finishedGames.sumBy { it.playedMoves.size }}")
    }
}

private data class GameResult(
    val isWon: Boolean,
    val playedMoves: MoveList<Color>
)