import aiinterface.AIInterface;
import struct.FrameData;
import struct.GameData;
import struct.Key;
import struct.ScreenData;

public class DatasetCreator implements AIInterface {

    // AI for which the datasetCreator delegates the gameplaying aspect to.
    private AIInterface playingAI;

    private RoundDataPointHandler datapointHandler; 

    private ScreenData latestScreenData;

    public int initialize(GameData gameData, boolean b) {
        this.datapointHandler = new RoundDataPointHandler();
        this.playingAI = new MctsAi();
        return this.playingAI.initialize(gameData, b);
    }


    public void getInformation(FrameData frameData) {
        this.playingAI.getInformation(frameData);

        if (frameData.getEmptyFlag() || frameData.getRemainingTimeMilliseconds() <= 0) {
            return;
        }

        // TODO it is the responsability of datasetCreator to offset the framedata to match pixel input.
        datapointHandler.addDataPoint(frameData, this.latestScreenData);
        // TODO keep a buffer of screenDatas and frameDatas. Check if it's full, and if so start adding datapoints
    }

    public void processing() {
        this.playingAI.processing();
    }

    public Key input() {
        return this.playingAI.input();
    }

    public void close() {
        this.playingAI.close();
    }

    public void roundEnd(int i, int i1, int i2) {
        this.playingAI.roundEnd(i, i1, i2);
        datapointHandler.roundEnd();
    }

    public void getScreenData(ScreenData sd) {
        this.playingAI.getScreenData(sd);
        this.latestScreenData = sd;
    }
}
