# QQL Functions Samples

The project includes examples of custom QQL functions implementation. 
There are 2 types of functions in QQL: 
stateless (in example it is `random(x)` function) and stateful (aggregate, `mul{}(x)` in example).

## Hot to use

1. Build jar with functions:

```
gradlew clean build
```

You will get `build/libs/qql-functions-<version>.jar`.

2. The jar (`build/libs/qql-functions-<version>.jar`) should be placed under `TimeBase-CI/lib/custom` directory 
or mount to docker container under `/timebase-server/lib/custom`:

```
docker run -p 8011:8011 -v /path/to/TimeBaseTutorial/qql-functions/build/libs:/timebase-server/lib/custom finos/timebase-ce-server:6.1
```

3. Enjoy custom functions:

```
SELECT running n, mul{}(n), random(), avg{}(random())
ARRAY JOIN range(1, 10) as n
```

```
SELECT RUNNING tsCount{}() FROM "KRAKEN" 
```

You can find many function implementations in  
[TimeBase server standard QQL functions.](https://github.com/finos/TimeBase-CE/tree/main-6.1/java/timebase/computations-std/src/main/java/com/epam/deltix/computations)
For example, stateful [count{}()](https://github.com/finos/TimeBase-CE/blob/main-6.1/java/timebase/computations-std/src/main/java/com/epam/deltix/computations/stateful/Count.java) 
and stateless [varchar functions](https://github.com/finos/TimeBase-CE/blob/main-6.1/java/timebase/computations-std/src/main/java/com/epam/deltix/computations/VarcharFunctions.java).