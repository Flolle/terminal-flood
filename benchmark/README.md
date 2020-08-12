This folder is dedicated to game board datasets and benchmarks based on them.

terminal-flood allows you to create and solve datasets consisting of compact Flood-It game board strings. Compact strings are defined as holding the whole game board in one single line with no whitespace characters in them. The board size is derived from the length of the string. terminal-flood only supports square game boards, in which case `boardSize = sqrt(string.length)` will always be correct.

Solving datasets will result in the output of two values, scores and time taken. Time taken should be self-explanatory, and the score value is just the sum of the lengths of all solutions for the dataset. So lower values are better.


# Datasets

While terminal-flood allows you to create your own datasets, for the purpose of comparison it is useful to base benchmarks on the same datasets, which is why a variety of them are part of this repository. Most of them follow a systematic naming scheme that directly tells you the basic parameters of the dataset. (`b[boardSize]c[numberOfColors]n[numberOfBoards]`)

- dataset b10c15n1000
- dataset b10c35n1000
- dataset b12c12n1000
- dataset b14c8n1000
- dataset b18c6n1000
- dataset b24c4n1000
- dataset b24c6n1000
- dataset floodtest_simplified (b19c6n100000)
- dataset pc19 (b14c6n1000)

pc19 and floodtest_simplified are popular datasets used as benchmarks/programming challenges, see [pc19](https://web.archive.org/web/20150909200653/http://cplus.about.com/od/programmingchallenges/a/challenge19.htm) and [floodtest](https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai). The original floodtest dataset has a slightly different format, but for the purpose of only having to support a single dataset format, it was transformed to the compact string format used by terminal-flood.

Additionally, this repository contains either optimal solutionsets or in the case of b24c6n1000 a solutionset based on `astar_ias`, because running the admissible heuristic needs more RAM than I have in my machine and would have taken *a lot* of time to compute.


# Benchmark results

All benchmarks were run on my personal computer (Ryzen 5 1600(12nm), 16GB DDR4-3000) running OpenJDK 8 on Windows 10. If not stated otherwise the standard command used is as follows:

```
java -Xmx12G -jar terminal-flood.jar -solutionsForDataset 12 [strategy] ul "path/to/dataset"
```

Where `[strategy]` denotes the heuristic algorithm used with A* (see the `-help` documentation or the [documentation page](https://github.com/Flolle/terminal-flood/wiki/Documentation) for exact details).

If you are using a Java version newer than 8, I recommend using the ParallelGC garbage collector which in Java 8 is the default GC and gives better performance than G1 which is the default GC in Java 9 and newer. You can select it by using the `-XX:+UseParallelGC` program argument when running Java.


### dataset b10c15n1000

All benchmarks with this dataset where done using only 1 thread.

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 26717 | 29656 |
| astar_ias | 27070 | 2983 |
| astar_ia | 27161 | 2593 |
| astar_iaf | 27363 | 1562 |
| astar_iaff | 28053 | 720 |


### dataset b10c35n1000

All benchmarks with this dataset where done using only 1 thread.

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 41751 | 16009 |
| astar_ias | 42445 | 1399 |
| astar_ia | 42712 | 1150 |
| astar_iaf | 42689 | 1533 |
| astar_iaff | 43186 | 833 |


### dataset b12c12n1000

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a (-lessMemory) | 27635 | 297602 |
| astar_ias | 27897 | 3249 |
| astar_ia | 28236 | 2361 |
| astar_iaf | 28435 | 807 |
| astar_iaff | 29492 | 478 |


### dataset b14c8n1000

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a (-lessMemory) | 24582 | 170978 |
| astar_ias | 24740 | 4050 |
| astar_ia | 25061 | 1397 |
| astar_iaf | 25256 | 867 |
| astar_iaff | 26356 | 491 |


### dataset b18c6n1000

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a (-lessMemory) | 24837 | 205937 |
| astar_ias | 24951 | 6424 |
| astar_ia | 25266 | 2067 |
| astar_iaf | 25530 | 1090 |
| astar_iaff | 26707 | 581 |


### dataset b24c4n1000

All benchmarks with this dataset where done using only 1 thread.

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 21952 | 11368 |
| astar_ias | 22062 | 5063 |
| astar_ia | 22337 | 2573 |
| astar_iaf | 22496 | 1768 |
| astar_iaff | 23242 | 1055 |


### dataset b24c6n1000

I did not attempt to find an optimal solutionset with terminal-flood, because it would have taken too long to do so and it most likely would have needed more RAM than I have available in my PC.

`astar_a (-lessMemoryQueueCutoff)` is the closest thing to an optimal solutionset that I have. It is not guaranteed to be optimal, but it should be relatively close.

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a (-lessMemoryQueueCutoff, 4 threads) | 31804 | 32974595 |
| astar_ias (-lessMemory) | 31968 | 185670 |
| astar_ia (-lessMemory) | 32537 | 21126 |
| astar_iaf | 33178 | 3050 |
| astar_iaff | 35117 | 854 |


### dataset floodtest_simplified

This dataset was used in a code challenge [here](https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai). Please note that the starting position for this dataset is in the middle.

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a (-lessMemory, 6 threads) | 1985078 | 20559726 |
| astar_ias | 1992612 | 675800 |
| astar_ia | 2007765 | 201057 |
| astar_iaf | 2026214 | 75095 |
| astar_iaff | 2135051 | 12219 |


### dataset pc19

This dataset was used in a code challenge [here](https://web.archive.org/web/20150909200653/http://cplus.about.com/od/programmingchallenges/a/challenge19.htm).

Results using 1 thread:

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 20086 | 17031 |
| astar_ias | 20189 | 4535 |
| astar_ia | 20428 | 2130 |
| astar_iaf | 20529 | 1592 |
| astar_iaff | 21250 | 714 |


Results using 12 threads:

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 20086 | 3917 |
| astar_ias | 20189 | 1287 |
| astar_ia | 20428 | 931 |
| astar_iaf | 20529 | 578 |
| astar_iaff | 21250 | 379 |
