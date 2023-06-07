# Temporal Java Benchmark

## Build the project

Either open the project in IntelliJ, which will automatically build it, or in the project's root directory run:

```
./gradlew build
```

## Run the Workflow

First, make sure the [Temporal server](https://docs.temporal.io/docs/server/quick-install) is running.

Run the worker

```
./gradlew startWorker
```

Run the Workflow PnumWorkflows times in parallel

```
./gradlew runWorkflows -PnumWorkflows=10
```
