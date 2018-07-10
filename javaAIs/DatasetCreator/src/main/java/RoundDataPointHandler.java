import java.io.*;

import enumerate.Action;
import enumerate.State;
import org.yaml.snakeyaml.Yaml;
import struct.CharacterData;
import struct.FrameData;
import struct.ScreenData;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoundDataPointHandler {

    final Logger logger = LoggerFactory.getLogger(RoundDataPointHandler.class);

    private Map metadataFile;
    private FileWriter outputLogFile;

    private String datasetPath = "dataset/";
    private final String METADATAFILEPATH = datasetPath + ".meta.yaml";

    // Information to create roundDataset.
    private int roundNumber;
    private String contestantAI;
    private String character1;
    private String character2;

    private int roundProcessedFrames;

    public RoundDataPointHandler() {
        resetRoundStatistics();
        this.metadataFile = findDatasetMetadata(METADATAFILEPATH);
        this.outputLogFile = startNewRoundDataset(metadataFile);
        logger.info("Started new RoundDataPointHandler with RoundNumber" + roundNumber +
                " ContestantAI: " + contestantAI + " Char 1: " + character1 + " Char 2: " + character2);
    }

    private void resetRoundStatistics() {
        this.roundProcessedFrames = 0;
    }

    private Map findDatasetMetadata(String filePath) {
        if (!exists(filePath)) {
            logger.info("Metadata file NOT FOUND. Creating new metadatafile at directory: " + filePath);
            createMetaDataFile(filePath);
        }
        return openYamlFile(filePath);
    }

    private boolean exists(String filePath) {
        return new File(filePath).isFile();
    }

    private void createMetaDataFile(String filePath) {
        Map<String, Object> skeleton = new HashMap();
        skeleton.put("TotalFrames", 0);
        writeMapToYamlFile(filePath, skeleton);
    }

    private Map openYamlFile(String filePath) {
        Yaml yaml = new Yaml();
        Map yamlFile = null;
        try {
            yamlFile = (Map) yaml.load(new FileReader(filePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return yamlFile;
    }

    private void writeMapToYamlFile(String filePath, Map content) {
        logger.info("Writting to file {} with content {}", filePath, content.toString());
        try {
            Yaml yaml = new Yaml();
            FileWriter newFile = new FileWriter(filePath);
            newFile.append(yaml.dump(content));
            newFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private FileWriter startNewRoundDataset(Map metadataFile) {
        resetRoundStatistics();
        try {
            return createEmptyRoundDataset(metadataFile);
        } catch (IOException e) {
            logger.error("Could not create new dataset file");
            e.printStackTrace();
        }
        return null;
    }

    private FileWriter createEmptyRoundDataset(Map metaDataFile) throws IOException {
        this.roundProcessedFrames = 0;
        String newRoundDatasetName = getNextRoundDatasetFileName(metaDataFile);
        logger.info("Creating new RoundDataset with name {}", newRoundDatasetName);
        FileWriter f = new FileWriter(newRoundDatasetName);
        createDatasetHeaders(f);
        return f;
    }

    private String getNextRoundDatasetFileName(Map metaDataFile) {
        // TODO add propername
        // Read from temporary file
        Map tempInfoFile = openYamlFile(datasetPath + ".temp_match_info.yaml");

        this.contestantAI = (String) tempInfoFile.get("contestantAI");
        this.character1 = (String) tempInfoFile.get("character1");
        this.character2 = (String) tempInfoFile.get("character2");

        // From other metadatafile
        String roundNumber = "WhatRoundIsIt";
        String fileName = String.join("_", roundNumber, contestantAI, character1, character2) + ".csv";
        // Update temporary file
        return datasetPath + fileName;
    }

    private void createDatasetHeaders(FileWriter file) {
        String playerIndependentColumns = String.join(", ", "frameNumber", "xDistance", "yDistance");
        String playerColumns = ""; int numberOfPlayers = 2;
        for (int i = 0; i < numberOfPlayers; i++) {
            String player = (i+1) + "_";
            // TODO map fun, map the string variable (player) to the list of variables to record
            playerColumns += String.join(", ", player + "xCenter", player +"yCenter", player +"action",
                                                                  player +"energy", player +"hp", player +"xSpeed", player +"ySpeed",
                                                                   player +"state", player +"isFront");
        }
        try {
            file.append(String.join(", ", playerIndependentColumns, playerColumns));
            file.append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addDataPoint(FrameData frameData, ScreenData screenData) {
        checkFrameDataAndScreenDataAreSafe(frameData, screenData);
        String pixelInfo     = getPixelInformationFromScreenData(screenData);
        String frameDataInfo = getFrameDataInformation(frameData);
        String dataPoint = String.join(", ", pixelInfo, frameDataInfo);
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
        return null;
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
        logger.info("Round finished");
        close();
        this.outputLogFile = startNewRoundDataset(this.metadataFile);
    }

    private void updateDatasetMetadata(Map metadataFile) {
        int previousFrames = (int) metadataFile.get("TotalFrames");
        metadataFile.put("TotalFrames", previousFrames + this.roundProcessedFrames);
        // TODO add MORE information to metadata file

        writeMapToYamlFile(METADATAFILEPATH, metadataFile);
    }

    public void close() {
        updateDatasetMetadata(metadataFile);
        try {
            this.outputLogFile.close(); // May break
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
