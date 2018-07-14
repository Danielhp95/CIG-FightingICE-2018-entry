#!/bin/bash

# Identify where java executables live, which classes need to be imported and which Main class to run
CLASSPATH="-classpath bin:lib/logback/:lib/slf4j-1.7.25/slf4j-api-1.7.25.jar:lib/slf4j-1.7.25/slf4j-simple-1.7.25.jar:lib/snakeyaml-1.17.jar:lib/natives/linux/lwjgl-glfw-natives-linux.jar:data/aiData/:lib/natives/linux/lwjgl-natives-linux.jar:lib/natives/linux/lwjgl-openal-natives-linux.jar:lib/natives/linux/lwjgl-opengl-natives-linux.jar:FightingICE.jar:lib/lwjgl/lwjgl_util.jar:lib/lwjgl/lwjgl-glfw.jar:lib/lwjgl/lwjgl-openal.jar:lib/lwjgl/lwjgl-opengl.jar:lib/lwjgl/lwjgl.jar:lib/javax.json-1.0.4.jar:lib/py4j0.10.4.jar"
MAINCLASS="Main"

# NOTE on flags: 
# --fast-mode disables window mode already. We add --disable-window to make sure that window does not show up.
# --json stores replay logs in json format
# --err-log system errors and AI logs are outputted as .txt files

# Detect port to be used between JVM and py4j. 
# Defaults to 4242, same as p4yj
PORT=$2
if [ "$PORT" = "" ]; then
    PORT=4242
fi

# Chooses mode to run the game in.
MODE=$1
if [ "$MODE" = "TRAIN_MODE" ]; then
    FLAGS="--grey-bg --inverted-player 1 --disable-window --fastmode --mute --py4j --port $PORT" 
elif [ "$MODE" = "HUMAN_JAVA_MODE" ]; then
    FLAGS="--mute" 
elif [ "$MODE" = "DEBUG_PYTHON_MODE" ]; then
    FLAGS="--grey-bg --inverted-player 1 --json --err-log --mute --py4j --port $PORT" 
elif [ "$MODE" = "V4.3" ]; then
    FLAGS="--grey-bg --inverted-player 1 --json --disable-window --fastmode --mute --py4j --port $PORT" 
elif [ "$MODE" = "V4.3_WINDOWED" ]; then
    FLAGS="--grey-bg --inverted-player 1 --json --mute --py4j --port $PORT" 
elif [ "$MODE" = "DATASETCREATOR" ]; then
    FLAGS="-n 2 --a1 DatasetCreator --a2 MctsAi --c1 ZEN --c2 ZEN --grey-bg --inverted-player 1 --mute " 
else
    echo "MODE: \"$MODE\" is not a valid mode"
    echo "Usage: ./start.sh MODE PORT_NUMBER (default 4242)"
    exit 1
fi

printf "RUNNING THE FOLLOWING COMMAND:\n $JAVA $CLASSPATH $MAINCLASS $FLAGS\n"

java $CLASSPATH $MAINCLASS $FLAGS
