package terminalFlood

import terminalFlood.algo.astar.AStar
import terminalFlood.algo.astar.AStarStrategies
import terminalFlood.game.GameBoard
import terminalFlood.game.MoveList
import terminalFlood.game.StartPos
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlin.streams.asSequence
import kotlin.system.exitProcess

object Datasets {
    fun playFromDataset(file: Path, lineNumber: Int, startPos: StartPos) {
        val noMaxStepsGameBoard = findGameBoardInDataset(file, lineNumber, startPos)

        PlayGame.play(noMaxStepsGameBoard.withMaximumStepsLimitCopy())
    }

    fun solveFromDataset(
        file: Path,
        lineNumber: Int,
        startPos: StartPos,
        strategy: AStarStrategies,
        memoryScheme: MemorySavingScheme
    ) {
        Util.aStarSimulation(findGameBoardInDataset(file, lineNumber, startPos), strategy, memoryScheme)
    }

    private fun findGameBoardInDataset(file: Path, lineNumber: Int, startPos: StartPos): GameBoard {
        if (!Files.exists(file) || Files.isDirectory(file))
            throw IllegalArgumentException("The file doesn't exist or is a directory!")

        println("Given dataset: $file")
        println("Line number: $lineNumber")
        println("Starting position: $startPos\n")

        val compactBoardString = try {
            Files.lines(file, Charsets.UTF_8).use { lines ->
                lines.asSequence()
                    .map { line -> line.trim() }
                    .filter { line -> line.isNotEmpty() }
                    .withIndex()
                    .first { indexedLine -> indexedLine.index == lineNumber - 1 }
                    .value
            }
        } catch (ex: NoSuchElementException) {
            println("Couldn't find a game board! Make sure that you specified the right file and line number!")
            throw ex
        }

        return GameBoard.createBoardFromCompactString(compactBoardString, startPos, Int.MAX_VALUE)
    }

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

                outputWriter.appendLine(gameBoard.createCompactString())

                val index = i + 1
                if (index % 100 == 0)
                    println("$index boards finished")
            }
        }

        println("Dataset created and saved to: $file")
    }

    fun findSolutionsForDataset(
        file: Path,
        threads: Int,
        strategy: AStarStrategies,
        startPos: StartPos,
        memoryScheme: MemorySavingScheme
    ) {
        if (!Files.exists(file) || Files.isDirectory(file))
            throw IllegalArgumentException("The file doesn't exist or is a directory!")

        println("Finding solutions for the given dataset: $file")
        println("Number of threads used: $threads")
        println("Start position on the game boards: $startPos")
        println("Using strategy: $strategy")
        println("Memory scheme: $memoryScheme")

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
                        val finishedGame = try {
                            when (memoryScheme) {
                                MemorySavingScheme.NO_MEMORY_SAVING         -> AStar.calculateMoves(
                                    GameBoard.createBoardFromCompactString(line, startPos, Int.MAX_VALUE),
                                    strategy
                                )
                                MemorySavingScheme.LESS_MEMORY              -> AStar.calculateMovesLessMemory(
                                    GameBoard.createBoardFromCompactString(line, startPos, Int.MAX_VALUE),
                                    strategy
                                )
                                MemorySavingScheme.LESS_MEMORY_QUEUE_CUTOFF -> AStar.calculateMovesLessMemory(
                                    GameBoard.createBoardFromCompactString(line, startPos, Int.MAX_VALUE),
                                    strategy,
                                    1_000_000
                                )
                            }
                        } catch (ex: Exception) {
                            // Print stacktrace and stop the program.
                            ex.printStackTrace()
                            exitProcess(-1)
                        }

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
                    game.playedMoves.forEach { outputWriter.append(it.toString()) }
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
    val playedMoves: MoveList
)