import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import enumerate.Action;
import enumerate.State;
import org.yaml.snakeyaml.Yaml;
import struct.CharacterData;
import struct.FrameData;
import struct.ScreenData;

import java.util.Map;


public class RoundDataPointHandler {

   
    private Map metadataFile;
    private FileWriter outputLogFile;

    private String datasetPath = "dataset/";

    // Information to create roundDataset.
    private int roundNumber;
    private String contestantAI;
    private String character1;
    private String character2;

    private int roundProcessedFrames;

    public RoundDataPointHandler() {

        resetRoundStatistics();
        try {
            this.metadataFile = findDatasetMetadata(datasetPath);
            this.outputLogFile = startNewRoundDataset(metadataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetRoundStatistics() {
        this.roundProcessedFrames = 0;
    }

    private Map findDatasetMetadata(String filePath) {
        String metadataFilePath = filePath + ".meta";
        if (!exists(metadataFilePath)) {
            createMetaDataFile(metadataFilePath);
        }
        return loadMetadataFile(metadataFilePath);
    }

    private boolean exists(String filePath) {
        return new File(filePath).isFile();
    }

    private void createMetaDataFile(String filePath) {
        String skeleton = "\n- NumberOfAIs: 0\nTotalFrames: 0\nContestants:\n";
        try {
           FileWriter newMetadataFile = new FileWriter(filePath);
           newMetadataFile.append(skeleton);
           newMetadataFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map loadMetadataFile(String filePath) {
        Yaml yaml = new Yaml();
        Map metadata = (Map) yaml.load(filePath);
        return metadata;
    }

    private FileWriter startNewRoundDataset(Map metadataFile) throws IOException {
        resetRoundStatistics();
        try {
            return createEmptyRoundDataset(metadataFile);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException();
        }
    }

    private FileWriter createEmptyRoundDataset(Map metaDataFile) throws IOException {
        this.roundProcessedFrames = 0;
        String newRoundDatasetName = getNextRoundDatasetFileName(metaDataFile);
        FileWriter f = new FileWriter(newRoundDatasetName);
        createDatasetHeaders(f);
        return f;
    }

    private String getNextRoundDatasetFileName(Map metaDataFile) {
        // TODO add propername
        String roundNumber = "";
        String contestantAI = "";
        String character1 = "";
        String character2 = "newFileName";
        String fileName = String.join("_", roundNumber, contestantAI, character1, character2) + ".csv";
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
            playerColumns += ", ";
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
            System.out.println("TODO add proper error message");
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
        close();
        try {
            this.outputLogFile = startNewRoundDataset(this.metadataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateDatasetMetadata(Map metadataFile) {
        // TODO add information to metadata file
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
