#!/bin/bash

# Identify where java executables live, which classes need to be imported and which Main class to run
CLASSPATH="-classpath bin:lib/natives/linux/lwjgl-glfw-natives-linux.jar:lib/natives/linux/lwjgl-natives-linux.jar:lib/natives/linux/lwjgl-openal-natives-linux.jar:lib/natives/linux/lwjgl-opengl-natives-linux.jar:FightingICE.jar:lib/lwjgl/lwjgl_util.jar:lib/lwjgl/lwjgl-glfw.jar:lib/lwjgl/lwjgl-openal.jar:lib/lwjgl/lwjgl-opengl.jar:lib/lwjgl/lwjgl.jar:lib/javax.json-1.0.4.jar:lib/py4j0.10.4.jar"
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
    FLAGS="-n 1 --c1 ZEN --c2 GARNET --grey-bg --inverted-player 1 --mute --json --err-log" 
elif [ "$MODE" = "DEBUG_PYTHON_MODE" ]; then
    FLAGS="--grey-bg --inverted-player 1 --json --err-log --mute --py4j --port $PORT" 
elif [ "$MODE" = "V4.3" ]; then
    FLAGS="--grey-bg --inverted-player 1 --json --disable-window --fastmode --mute --py4j --port $PORT" 
else
    echo "MODE: \"$MODE\" is not a valid mode, enter a valid mode: {TRAIN_MODE, DEBUG_MODE}"
    echo "Usage: ./start.sh MODE PORT_NUMBER (default 4242)"
    exit 1
fi

printf "RUNNING THE FOLLOWING COMMAND:\n $JAVA $CLASSPATH $MAINCLASS $FLAGS\n"

java $CLASSPATH $MAINCLASS $FLAGS
