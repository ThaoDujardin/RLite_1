# RLite_1

Java foundation for a 2D roguelite.

Project location: `rlite/`

## Run tests

```bash
cd rlite
mvn test
```

## Run sample loop

```bash
cd rlite
mvn -q -DskipTests package
java -cp target/rlite-1.0-SNAPSHOT.jar com.rlite.App
```

Controls: `W/A/S/D` (or `up/down/left/right`), `.` to wait, `q` to quit. Reach `X` before 30 turns.