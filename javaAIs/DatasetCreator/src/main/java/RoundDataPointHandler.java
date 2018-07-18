import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
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

import javax.imageio.ImageIO;

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

    private String roundDatasetName;

    private boolean isFirstRoundDatapoint;
    private int roundProcessedFrames;

    private int downSampledImageWidth = 96;
    private int downSampledImageHeight = 64;

    public RoundDataPointHandler() {


        this.datasetPath = (String) YamlHandler.openYamlFile(".datasetPath.yaml").get("datasetPath");
        this.yamlHandler = new YamlHandler(datasetPath);
        this.metadataFile = yamlHandler.findDatasetMetadata();
        parseTempFile(datasetPath + ".temp_match_info.yaml");

        if (!yamlHandler.doesContestantExist(this.metadataFile, this.contestantAI)) {
            logger.info("Instantiating metadata for AI {}", this.contestantAI);
            metadataFile = yamlHandler.addContestantToMetadata(metadataFile, this.contestantAI);
        }

        resetRoundStatistics();
        logger.info("Started new RoundDataPointHandler with RoundNumber {}: ContestantAI: {} Char 1: {} Char 2: {}",
                    roundNumber, contestantAI, character1, character2);
    }

    private void resetRoundStatistics() {
        this.roundProcessedFrames = 0; this.isFirstRoundDatapoint = true;
    }

    private void parseTempFile(String filePath) {
        Map tempInfoFile = YamlHandler.openYamlFile(datasetPath + ".temp_match_info.yaml");
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
        this.roundDatasetName = getNextRoundDatasetFileName(metaDataFile, contestantAI, character1, character2);
        logger.info("Creating new RoundDataset with name {}", roundDatasetName);
        FileWriter f = new FileWriter(roundDatasetName);
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
            initiateDataPointData();
        }

        int[] pixelInfo      = getPixelInformationFromScreenData(screenData);
        String frameDataInfo = getFrameDataInformation(frameData);

        saveFrameDataInfo(frameDataInfo, this.outputLogFile);
        savePixelInfo(pixelInfo);
        this.roundProcessedFrames++;
    }

    // TODO i really should not have an initiate function in add point.
    private void initiateDataPointData() {
        this.isFirstRoundDatapoint = false;
        logger.info("First frame where info can be added in Round {}", this.roundNumber);
        this.outputLogFile = startNewRoundDataset(this.metadataFile);
        String imageDirectoryPath = String.format("%s/%d_%s_%s_%s", this.datasetPath, this.roundNumber, this.contestantAI,
                                                                    this.character1, this.character2);
        new File(imageDirectoryPath).mkdir();
    }

    private void saveFrameDataInfo(String framedataInfo, FileWriter outputFile) {
        try {
            outputFile.append(framedataInfo);
            outputFile.append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePixelInfo(int[] pixelInfo) {
        String filename = String.format("%s/%d_%s_%s_%s/%d.jpeg", this.datasetPath, this.roundNumber, this.contestantAI,
                                                                  this.character1, this.character2, this.roundProcessedFrames);
        File imageFile = new File(filename);

        BufferedImage outputImage = new BufferedImage(this.downSampledImageWidth, this.downSampledImageHeight, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = outputImage.getRaster();
        raster.setSamples(0, 0, this.downSampledImageWidth, this.downSampledImageHeight, 0, pixelInfo);
        try {
            ImageIO.write(outputImage, "jpeg", imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkFrameDataAndScreenDataAreSafe(FrameData frameData, ScreenData screenData) {
        if (screenData == null || (frameData.getEmptyFlag() || frameData.getRemainingTimeMilliseconds() <= 0)) {
            logger.error("Tried to process bad dataframe, dataset will be corrupted");
        }

    }

    private int[] getPixelInformationFromScreenData(ScreenData screenData){
        byte[] grayscaleDownSampledPixels =
                screenData.getDisplayByteBufferAsBytes(this.downSampledImageWidth, this.downSampledImageHeight, true);
        int[] normalizedPixelValues = normalizePixelValuesWithinRange(grayscaleDownSampledPixels,
                -128, 127, 0, 255);
        return normalizedPixelValues;
    }

    private int[] normalizePixelValuesWithinRange(byte[] values, int currentMin, int currentMax,
                                                                       int minValue, int maxValue) {
        //ByteBuffer buffer = ByteBuffer.wrap(values);
        int[] normalizedValues = new int[values.length];
        for (int i = 0; i < normalizedValues.length; i++) {
            normalizedValues[i] = 127 + values[i];
        }

        //int[] normalizedValues = IntStream.generate(buffer::get).limit(buffer.remaining())
        //        .map(x -> (x - currentMin) / (currentMax - currentMin))
        //        .toArray();
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

    private String getCharacterDataInformation(FrameData frameData, boolean isCharacter1) {
        CharacterData ch_data = frameData.getCharacter(isCharacter1);
        int[] a =  encodeAsOneHot(ch_data.getAction());
        float xCenter =  ch_data.getCenterX();
        float yCenter =  ch_data.getCenterY();
        float energy =  ch_data.getEnergy();
        float hp =  ch_data.getHp();
        float xSpeed =  ch_data.getSpeedX();
        float ySpeed =  ch_data.getSpeedY();
        int[] s =  encodeAsOneHot(ch_data.getState());
        boolean isFront =  ch_data.isFront();
        return String.join(", ", Float.toString(xCenter),Float.toString(yCenter), Arrays.toString(a),
                                         Float.toString(energy), Float.toString(hp), Float.toString(xSpeed),
                                         Float.toString(ySpeed), Arrays.toString(s), (isFront) ? "1" : "0");
    }

    private int[] encodeAsOneHot(Enum e1) {
        int[] oneHotEncoding = new int[e1.getDeclaringClass().getEnumConstants().length];
        oneHotEncoding[e1.ordinal()] = 1;
        return oneHotEncoding;
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
