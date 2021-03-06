from tqdm import tqdm
from subprocess import Popen
import os
import sys
import logging

logging.basicConfig()
logger = logging.getLogger("createDataset")
logger.setLevel(logging.INFO)


def createDataset(datasetPath):
    numberOfMatchesPerConfiguration = 10
    contestantAIs = ["MctsAi", "JerryMizunoAI", "LoadTorchWeightAI", "RandomAI"]
    availableCharacters = ["ZEN", "GARNET", "LUD"]
    positions = ["player 1", "player 2"]

    datasetPath += 'dataset/'
    if not os.path.isdir(datasetPath):
        os.mkdir(datasetPath)
    createTemporaryFileWithDatasetLocation(datasetPath)

    try:
        for contestant in tqdm(contestantAIs):
            for position in positions:
                for character1 in availableCharacters:
                    for character2 in availableCharacters:
                        prepareTemporaryFile(datasetPath, contestant, character1, character2, position)
                        playMatch(contestant, character1, character2, position, numberOfGames=numberOfMatchesPerConfiguration)
    except KeyboardInterrupt:
        os.remove('.datasetPath.yaml')
        logger.info("DATASET CREATION STOPPED BY USER")


def createTemporaryFileWithDatasetLocation(datasetPath):
    with open('.datasetPath.yaml', 'w') as f:
        f.write('datasetPath: ' + datasetPath)


def prepareTemporaryFile(datasetPath, contestant, character1, character2, playerPosition):
    content = 'contestantAI: {}\ncharacter1: {}\ncharacter2: {}\n'.format(contestant, character1, character2)
    with open(datasetPath + ".temp_match_info.yaml", "w+") as f:
        f.write(content)
        f.truncate()


def playMatch(contestant, character1, character2, position, numberOfGames):
    classpath = "bin:lib/logback/:lib/slf4j-1.7.25/slf4j-api-1.7.25.jar:lib/slf4j-1.7.25/slf4j-simple-1.7.25.jar:lib/snakeyaml-1.17.jar:lib/natives/linux/lwjgl-glfw-natives-linux.jar:data/aiData/:lib/natives/linux/lwjgl-natives-linux.jar:lib/natives/linux/lwjgl-openal-natives-linux.jar:lib/natives/linux/lwjgl-opengl-natives-linux.jar:FightingICE.jar:lib/lwjgl/lwjgl_util.jar:lib/lwjgl/lwjgl-glfw.jar:lib/lwjgl/lwjgl-openal.jar:lib/lwjgl/lwjgl-opengl.jar:lib/lwjgl/lwjgl.jar:lib/javax.json-1.0.4.jar:lib/py4j0.10.4.jar"
    mainClass = "Main"

    numberOfMatches = '-n {}'.format(numberOfGames)
    matchContestants = '--a1 DatasetCreator --a2 {}'.format(contestant) if position == "player 1" else '--a1 {} --a2 DatasetCreator'.format(contestant)
    playableCharacters = '--c1 {} --c2 {}'.format(character1, character2)
    otherFlags = '--grey-bg --inverted-player 1 --mute'
    flags     = (numberOfMatches + ' ' + matchContestants + ' ' + playableCharacters + ' ' + otherFlags).split(' ')

    p = Popen(["java", "-classpath", classpath, mainClass] + flags)
    p.wait()


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print('\n')
        logger.error("The script takes exactly 1 argument, which represents the path where the dataset will be created")
        print('\n')
    else:
        datasetPath = str(sys.argv[1])
        createDataset(datasetPath)
