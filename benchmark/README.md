This folder is dedicated to game board datasets and benchmarks based on them.

terminal-flood allows you to create and solve datasets consisting of compact Flood-It game board strings. Compact strings are defined as holding the whole game board in one single line with no whitespace characters in them. The board size is derived from the length of the string. terminal-flood only supports square game boards, in which case `boardSize = sqrt(string.length)` will always be correct.

Solving datasets will result in the output of two values, scores and time taken. Time taken should be self-explanatory and the score value is just the sum of the lengths of all solutions for the dataset. So lower values are better.


# Datasets

While terminal-flood allows you to create your own datasets, for the purpose of comparison it is useful to base benchmarks on the same datasets, which is why a variety of them are part of this repository. Most of them follow a systematic naming scheme that directly tells you the basic parameters of the dataset. (`b[boardSize]c[numberOfColors]n[numberOfBoards]`)

- dataset b10c15n1000
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

Where `[strategy]` denotes the heuristic algorithm used with A* (see the `-help` documentation for exact details).

If you are using a Java version newer than 8, I recommend to use the ParallelGC garbage collector which in Java 8 is the default GC and gives better performance than G1 which is the default GC in Java 9 and newer. You can select it by using the `-XX:+UseParallelGC` program argument when running Java.


### dataset b10c15n1000

All benchmarks with this dataset where done using only 1 thread.

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 26717 | 45966 |
| astar_ias | 27070 | 5143 |
| astar_ia | 27161 | 4693 |
| astar_iaf | 27363 | 2404 |
| astar_iaff | 28053 | 1085 |


### dataset b12c12n1000

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a (6 threads, 14G heap) | 27635 | 394233 |
| astar_ias | 27897 | 5441 |
| astar_ia | 28236 | 3819 |
| astar_iaf | 28435 | 1562 |
| astar_iaff | 29492 | 902 |


### dataset b14c8n1000

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a (6 threads) | 24582 | 222099 |
| astar_ias | 24740 | 7104 |
| astar_ia | 25061 | 2209 |
| astar_iaf | 25256 | 1745 |
| astar_iaff | 26356 | 932 |


### dataset b18c6n1000

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a (6 threads) | 24837 | 308016 |
| astar_ias | 24951 | 12325 |
| astar_ia | 25266 | 3606 |
| astar_iaf | 25530 | 2068 |
| astar_iaff | 26707 | 1054 |


### dataset b24c4n1000

All benchmarks with this dataset where done using only 1 thread.

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 21952 | 23021 |
| astar_ias | 22062 | 10243 |
| astar_ia | 22337 | 5006 |
| astar_iaf | 22496 | 3762 |
| astar_iaff | 23242 | 1952 |


### dataset b24c6n1000

I did not attempt to find an optimal solutionset with terminal-flood, because it would have taken too long to do so and it most likely would have needed more RAM than I have available in my PC.

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | none | none |
| astar_ias | 31968 | 344942 |
| astar_ia | 32537 | 37393 |
| astar_iaf | 33178 | 5812 |
| astar_iaff | 35117 | 1814 |


### dataset floodtest_simplified

This dataset was used in a code challenge [here](https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai). Please note that the starting position for this dataset is in the middle.

I did not attempt to find an optimal solutionset with terminal-flood, because it would have taken too long to do so (probably multiple days of runtime) and it most likely would have needed more RAM than I have available in my PC. Luckily, smack42 (creator of [ColorFill](https://github.com/smack42/ColorFill)) did compute an optimal solutionset, allowing us to at least know the score that `astar_a` would have gotten.

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 1985078 | none |
| astar_ias | 1992612 | 1310318 |
| astar_ia | 2007765 | 383041 |
| astar_iaf | 2026214 | 141110 |
| astar_iaff | 2135051 | 22922 |


### dataset pc19

This dataset was used in a code challenge [here](https://web.archive.org/web/20150909200653/http://cplus.about.com/od/programmingchallenges/a/challenge19.htm).

Results using 1 thread:

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 20086 | 27050 |
| astar_ias | 20189 | 8378 |
| astar_ia | 20428 | 3624 |
| astar_iaf | 20529 | 2612 |
| astar_iaff | 21250 | 1188 |


Results using 12 threads:

| strategy | score | time in milliseconds |
| :--- | ---: | ---: |
| astar_a | 20086 | 5805 |
| astar_ias | 20189 | 2236 |
| astar_ia | 20428 | 1418 |
| astar_iaf | 20529 | 1116 |
| astar_iaff | 21250 | 912 |
