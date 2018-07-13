import aiinterface.AIInterface;
import org.slf4j.LoggerFactory;
import struct.FrameData;
import struct.GameData;
import struct.Key;
import struct.ScreenData;

import java.util.Deque;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetCreator implements AIInterface {

    final Logger logger = LoggerFactory.getLogger(DatasetCreator.class);

    // AI for which the datasetCreator delegates the gameplaying aspect to.
    private AIInterface playingAI;

    private RoundDataPointHandler datapointHandler; 

    private FrameData latestFrameData;

    private int artificialFrameDataDelay;
    private Deque<ScreenData> screenDataQueue;

    private int infoCalls = 0;


    public int initialize(GameData gameData, boolean b) {
        this.artificialFrameDataDelay = 15; // This magic number appears in the game documentation
        this.datapointHandler = new RoundDataPointHandler();
        this.screenDataQueue = new LinkedList<>();

        this.playingAI = new MctsAi();
        return this.playingAI.initialize(gameData, b);
    }

    public void getInformation(FrameData frameData) {
        this.latestFrameData = frameData;
        this.playingAI.getInformation(frameData);

        if (frameData.getEmptyFlag() || frameData.getRemainingTimeMilliseconds() <= 0) {
            return;
        }

        this.infoCalls++;
        if (screenDataQueue.size() == artificialFrameDataDelay) {
            logger.info("Matching frameData {} with screenData {}", this.infoCalls, this.infoCalls - artificialFrameDataDelay);
            datapointHandler.addDataPoint(frameData, screenDataQueue.pollFirst());
        }
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
        if (latestFrameData.getEmptyFlag() || latestFrameData.getRemainingTimeMilliseconds() <= 0) {
            return;
        }
        screenDataQueue.addLast(sd);
        logger.info("Size of queue: {}", screenDataQueue.size());
        this.playingAI.getScreenData(sd);
    }
}
