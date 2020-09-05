This folder is dedicated to game board datasets and benchmarks based on them.

terminal-flood allows you to create and solve datasets consisting of compact Flood-It game board strings. Compact strings are defined as holding the whole game board in one single line with no whitespace characters in them. The board size is derived from the length of the string. terminal-flood only supports square game boards, in which case `boardSize = sqrt(string.length)` will always be correct.

Solving datasets will result in the output of two values, score and time taken. Time taken should be self-explanatory, and the score value is just the sum of the lengths of all solutions for the dataset. So lower values are better.


# Datasets

While terminal-flood allows you to create your own datasets, for the purpose of comparison it is useful to base benchmarks on the same datasets, which is why a variety of them are part of this repository. Most of them follow a systematic naming scheme that directly tells you the basic parameters of the dataset. (`b[boardSize]c[numberOfColors]n[numberOfBoards]`)

- dataset b10c15n1000
- dataset b10c35n1000
- dataset b12c12n1000
- dataset b14c8n1000
- dataset b18c6n1000
- dataset b24c4n10000
- dataset b24c6n1000
- dataset floodtest_simplified (b19c6n100000)
- dataset pc19 (b14c6n1000)

pc19 and floodtest_simplified are popular datasets used as benchmarks/programming challenges, see [pc19](https://web.archive.org/web/20150909200653/http://cplus.about.com/od/programmingchallenges/a/challenge19.htm) and [floodtest](https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai). The original floodtest dataset has a slightly different format, but for the purpose of only having to support a single dataset format, it was transformed to the compact string format used by terminal-flood.

Additionally, this repository contains either optimal solutionsets or in the case of b24c6n1000 a solutionset based on `astar_a (-queueCutoff)`, because running the admissible heuristic without limits needs more RAM than I have in my machine and would have taken *a lot* of time to compute.


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
| astar_a | 26717 | 28244 |
| astar_ias | 27070 | 2918 |
| astar_iaf | 27363 | 1513 |


### dataset b10c35n1000

All benchmarks with this dataset where done using only 1 thread.

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 41751 | 14005 |
| astar_ias | 42445 | 1425 |
| astar_iaf | 42689 | 1428 |


### dataset b12c12n1000

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 27635 | 132267 |
| astar_ias | 27897 | 3067 |
| astar_iaf | 28435 | 895 |


### dataset b14c8n1000

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 24582 | 85312 |
| astar_ias | 24740 | 3796 |
| astar_iaf | 25256 | 927 |


### dataset b18c6n1000

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 24837 | 108085 |
| astar_ias | 24951 | 6318 |
| astar_iaf | 25530 | 1170 |


### dataset b24c4n10000

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 219580 | 18395 |
| astar_ias | 220630 | 7528 |
| astar_ia | 223212 | 3794 |
| astar_iaf | 224670 | 2894 |
| astar_iaff | 232415 | 1901 |


### dataset b24c6n1000

I did not attempt to find an optimal solutionset with terminal-flood, because it would have taken too long to do so and it most likely would have needed more RAM than I have available in my PC.

`astar_a (-queueCutoff)` is the closest thing to an optimal solutionset that I have. It is not guaranteed to be optimal, but it should be relatively close.

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a (-queueCutoff) | 31804 | 14724149 |
| astar_ias | 31968 | 148207 |
| astar_ia | 32537 | 18035 |
| astar_iaf | 33178 | 3117 |
| astar_iaff | 35117 | 934 |


### dataset floodtest_simplified

This dataset was used in a code challenge [here](https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai). Please note that the starting position for this dataset is in the middle.

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 1985078 | 13031459 |
| astar_ias | 1992612 | 641453 |
| astar_ia | 2007765 | 190358 |
| astar_iaf | 2026214 | 68577 |
| astar_iaff | 2135051 | 12444 |


### dataset pc19

This dataset was used in a code challenge [here](https://web.archive.org/web/20150909200653/http://cplus.about.com/od/programmingchallenges/a/challenge19.htm).

All benchmarks with this dataset where done using only 1 thread.

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 20086 | 15750 |
| astar_ias | 20189 | 4384 |
| astar_iaf | 20529 | 1585 |
