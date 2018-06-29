import java.io.*;
import aiinterface.AIInterface;
import enumerate.Action;
import enumerate.State;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;
import struct.Key;
import struct.ScreenData;

import java.util.Arrays;

public class DatasetCreator implements AIInterface {

    private ScreenData latestScreenData;

    private AIInterface playingAI;

    private int callsToGetInformation;

    private FileWriter outputLogFile;

    public int initialize(GameData gameData, boolean b) {
        this.callsToGetInformation = 0;

        try {
            this.outputLogFile = initializeOutputLogFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.playingAI = new MctsAi();
        return this.playingAI.initialize(gameData, b);
    }

    private FileWriter initializeOutputLogFile() throws IOException {
        FileWriter f = null;
        f = new FileWriter("logFile.csv");

        // Player independent columns
        String playerIndependentColumns = String.join(",", "frameNumber", "xDistance", "yDistance");

        // Player dependent columns
        String playerColumns = ""; int numberOfPlayers = 2;
        for (int i = 0; i < numberOfPlayers; i++) {
            String player = (i+1) + "_";
            playerColumns += String.join(", ", player + "xCenter", player +"yCenter", player +"action",
                                                                  player +"energy", player +"hp", player +"xSpeed", player +"ySpeed",
                                                                   player +"state", player +"isFront");
            playerColumns += ", ";
        }
        f.append(String.join(",", playerIndependentColumns, playerColumns));
        return f;
    }


    public void getInformation(FrameData frameData) {
        this.playingAI.getInformation(frameData);

        if (frameData.getEmptyFlag() || frameData.getRemainingTimeMilliseconds() <= 0) {
            return;
        }

        this.callsToGetInformation++;
        int frameDataFrame = frameData.getFramesNumber();

        saveDataPoint(frameData);
    }

    private void saveDataPoint(FrameData frameData) {
        saveFrameDataInformation(frameData);
        savePixelInformationFromScreenData();
        // Append new line
        try {
            this.outputLogFile.append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePixelInformationFromScreenData(){
        if (this.latestScreenData == null) {
            System.out.println("Screen data not found");
            return;
        }
        byte[] grayscaleDownSampledPixels =
                this.latestScreenData.getDisplayByteBufferAsBytes(96, 64, true);
    }

    /*
        Java docs for frame data: http://www.ice.ci.ritsumei.ac.jp/~ftgaic/JavaDOC/structs/FrameData.html
    */
    private void saveFrameDataInformation(FrameData frameData) {
        // How to match with pixel values?
        int frameNumber = frameData.getFramesNumber();
        float xDistance = frameData.getDistanceX();
        float yDistance = frameData.getDistanceY();
        String s0 = String.join(", ", Integer.toString(frameNumber), Float.toString(xDistance), Float.toString(yDistance));

        // Character 1 data
        String s1 = getCharacterDataInformation(frameData, true);
        String s2 = getCharacterDataInformation(frameData, false);

        try {
            this.outputLogFile.append(s0);
            this.outputLogFile.append(", ");
            this.outputLogFile.append(s1);
            this.outputLogFile.append(", ");
            this.outputLogFile.append(s2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getCharacterDataInformation(FrameData frameData, boolean isCharacter1) {
        CharacterData ch_data = frameData.getCharacter(isCharacter1);
        Action a =  ch_data.getAction();
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


    public void processing() {
        this.playingAI.processing();
    }

    public Key input() {
        Key k = this.playingAI.input();
        return k;
    }

    public void close() {
        try {
            this.outputLogFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.playingAI.close();
    }

    public void roundEnd(int i, int i1, int i2) {
        this.playingAI.roundEnd(i, i1, i2);
    }

    public void getScreenData(ScreenData sd) {
        this.latestScreenData = sd;
        this.playingAI.getScreenData(sd);
    }
}