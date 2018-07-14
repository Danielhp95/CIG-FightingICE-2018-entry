import java.io.*;

import enumerate.Action;
import enumerate.State;
import struct.CharacterData;
import struct.FrameData;
import struct.ScreenData;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoundDataPointHandler {

    final Logger logger = LoggerFactory.getLogger(RoundDataPointHandler.class);

    private YamlHandler yamlHandler;
    private Map metadataFile;
    private FileWriter outputLogFile;

    private String datasetPath = "dataset/";

    // Information to create roundDataset.
    private int roundNumber;
    private String contestantAI;
    private String character1;
    private String character2;

    private int roundProcessedFrames;
    private boolean isFirstRoundDatapoint;

    public RoundDataPointHandler() {
        this.yamlHandler = new YamlHandler(datasetPath);

        resetRoundStatistics();

        this.metadataFile = yamlHandler.findDatasetMetadata();
        parseTempFile(datasetPath + ".temp_match_info.yaml");

        if (!yamlHandler.doesContestantExist(this.metadataFile, this.contestantAI)) {
            logger.info("Instantiating metadata for AI {}", this.contestantAI);
            metadataFile = yamlHandler.addContestantToMetadata(metadataFile, this.contestantAI);
        }

        logger.info("Started new RoundDataPointHandler with RoundNumber {}: ContestantAI: {} Char 1: {} Char 2: {}",
                    roundNumber, contestantAI, character1, character2);
    }

    private void resetRoundStatistics() {
        this.roundProcessedFrames = 0; this.isFirstRoundDatapoint = true;
    }

    private void parseTempFile(String filePath) {
        Map tempInfoFile = yamlHandler.openYamlFile(datasetPath + ".temp_match_info.yaml");
        this.contestantAI = (String) tempInfoFile.get("contestantAI");
        this.character1 = (String) tempInfoFile.get("character1");
        this.character2 = (String) tempInfoFile.get("character2");
    }

    private FileWriter startNewRoundDataset(Map metaDataFile) {
        try {
            return createEmptyRoundDataset(metaDataFile);
        } catch (IOException e) {
            logger.error("Could not create new dataset file");
            e.printStackTrace();
        }
        return null;
    }

    private FileWriter createEmptyRoundDataset(Map metaDataFile) throws IOException {
        String newRoundDatasetName = getNextRoundDatasetFileName(metaDataFile, contestantAI, character1, character2);
        logger.info("Creating new RoundDataset with name {}", newRoundDatasetName);
        FileWriter f = new FileWriter(newRoundDatasetName);
        createDatasetHeaders(f);
        return f;
    }

    private String getNextRoundDatasetFileName(Map metaDataFile, String contestantAI, String character1, String character2) {
        this.roundNumber = 1 + yamlHandler.findRoundNumberForContestant(metaDataFile, contestantAI);

        String fileName = String.join("_", Integer.toString(roundNumber), contestantAI, character1, character2) + ".csv";
        return datasetPath + fileName;
    }

    private void createDatasetHeaders(FileWriter file) {
        String pixelInfo = "pixels";
        String playerIndependentColumns = String.join(", ", "frameNumber", "xDistance", "yDistance");
        String playerColumns = ""; int numberOfPlayers = 2;
        for (int i = 0; i < numberOfPlayers; i++) {
            String player = (i+1) + "_";
            List<String> fields = Arrays.asList("xCenter", "yCenter", "action", "energy",
                                                 "hp", "xSpeed", "ySpeed", "state", "isFront")
                                                .stream()
                                                .map(s -> player + s)
                                                .collect(Collectors.toList());
            playerColumns += String.join(", ", fields);
        }
        try {
            file.append(String.join(", ", playerIndependentColumns, playerColumns, pixelInfo));
            file.append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addDataPoint(FrameData frameData, ScreenData screenData) {
        checkFrameDataAndScreenDataAreSafe(frameData, screenData);

        if (isFirstRoundDatapoint) {
            isFirstRoundDatapoint = false;
            logger.info("First frame where info can be added in Round {}", this.roundNumber);
            this.outputLogFile = startNewRoundDataset(this.metadataFile);
        }

        String pixelInfo     = getPixelInformationFromScreenData(screenData);
        String frameDataInfo = getFrameDataInformation(frameData);
        String dataPoint = String.join(", ", frameDataInfo, pixelInfo);

        try {
            this.outputLogFile.append(dataPoint);
            this.outputLogFile.append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.roundProcessedFrames++;
    }

    private void checkFrameDataAndScreenDataAreSafe(FrameData frameData, ScreenData screenData) {
        if (screenData == null || (frameData.getEmptyFlag() || frameData.getRemainingTimeMilliseconds() <= 0)) {
            logger.error("Tried to process bad dataframe, dataset will be corrupted");
        }

    }

    // TODO save point. Find way of turning byte[] into string
    private String getPixelInformationFromScreenData(ScreenData screenData){
        byte[] grayscaleDownSampledPixels =
                screenData.getDisplayByteBufferAsBytes(96, 64, true);
        double[] normalizedPixelValues = normalizePixelValuesWithinRange(grayscaleDownSampledPixels,
                -127, 127, 0, 1);
        return normalizedPixelValues.toString();
    }

    private double[] normalizePixelValuesWithinRange(byte[] values, float currentMin, float currentMax,
                                                                       float minValue, float maxValue) {
        ByteBuffer buffer = ByteBuffer.wrap(values);
        double[] normalizedValues = IntStream.generate(buffer::get).limit(buffer.remaining())
                .mapToDouble(x -> (x - currentMin) / (currentMax - currentMin))
                .toArray();
        return normalizedValues;
    }

    /*
        Java docs for frame data: http://www.ice.ci.ritsumei.ac.jp/~ftgaic/JavaDOC/structs/FrameData.html
    */
    private String getFrameDataInformation(FrameData frameData) {
        // How to match with pixel values?
        int frameNumber = frameData.getFramesNumber();
        float xDistance = frameData.getDistanceX();
        float yDistance = frameData.getDistanceY();
        String characterIndependentData = String.join(", ", Integer.toString(frameNumber), Float.toString(xDistance), Float.toString(yDistance));
        String character1Data = getCharacterDataInformation(frameData, true);
        String character2Data = getCharacterDataInformation(frameData, false);
        String frameDataInfo = String.join(", ", characterIndependentData, character1Data, character2Data);
        return frameDataInfo;
    }

    // TODO turn into one-hot encoding.
    private String getCharacterDataInformation(FrameData frameData, boolean isCharacter1) {
        CharacterData ch_data = frameData.getCharacter(isCharacter1);
        Action a =  ch_data.getAction(); // TODO one hot
        float xCenter =  ch_data.getCenterX();
        float yCenter =  ch_data.getCenterY();
        float energy =  ch_data.getEnergy();
        float hp =  ch_data.getHp();
        float xSpeed =  ch_data.getSpeedX();
        float ySpeed =  ch_data.getSpeedY();
        State s =  ch_data.getState();
        boolean isFront =  ch_data.isFront();
        return String.join(", ", Float.toString(xCenter),Float.toString(yCenter), a.toString(),
                                         Float.toString(energy), Float.toString(hp), Float.toString(xSpeed),
                                         Float.toString(ySpeed), s.toString(), (isFront) ? "1" : "0");
    }

    public void roundEnd() {
        logger.info("Round {} finished", this.roundNumber);
        close();
        resetRoundStatistics();
    }

    public void close() {
        yamlHandler.updateDatasetMetadata(metadataFile, this.contestantAI, this.roundProcessedFrames);
        try {
            this.outputLogFile.close(); // May break
        } catch (IOException e) {
            logger.error("OutputLogFile breaks Somehow");
            e.printStackTrace();
        }
    }
}
