import aiinterface.AIInterface;
import aiinterface.CommandCenter;
import enumerate.Action;
import struct.FrameData;
import struct.GameData;
import struct.Key;
import struct.MotionData;

/**
 * This class is a sample AI for Fighting ICE. It gives a very simple sample on
 * how to use the method CancelAbleFrame in the class MotionData. The AI does
 * nothing but displays its opponent�@character's return value of
 * CancelAbleFrame.
 *
 * @author Yamamoto, Team Fighting ICE.
 *
 */

public class MotionDataSample implements AIInterface {

	boolean p;
	GameData gd;
	Key inputKey;
	FrameData fd;
	CommandCenter cc;

	@Override
	public int initialize(GameData gameData, boolean playerNumber) {
		gd = gameData;
		p = playerNumber;

		inputKey = new Key();
		fd = new FrameData();
		cc = new CommandCenter();

		return 0;
	}

	@Override
	public void getInformation(FrameData frameData) {
		fd = frameData;
		cc.setFrameData(fd, p);

	}

	@Override
	public void processing() {
		if (!fd.getEmptyFlag()) {
			if (fd.getRemainingFramesNumber() > 0) {
				// In order to get CancelAbleFrame's information on the current
				// action of the opponent character, first you write as follows:
				Action oppAct = fd.getCharacter(!p).getAction();
				// If you want the same information on a specific action, say
				// "STAND_A", you can simply write:
				// Action action = Action.STAND_A;

				// Next, get the MotionData information on the opponent
				// character's action of interest from GameData.
				// You can access the MotionData information with
				// gd.getPlayer???Motion.elementAt("an instance of action (e.g.,
				// oppAct or action)".ordinal())
				MotionData oppMotion = new MotionData();
				oppMotion = gd.getMotionData(!p).get(oppAct.ordinal());

				System.out.println(
						oppMotion.getActionName() + ":cancelable " + oppMotion.getCancelAbleFrame() + " frame.");
			}
		}
	}

	@Override
	public Key input() {
		return inputKey;
	}

	@Override
	public void close() {

	}

	@Override
	public void roundEnd(int p1Hp, int p2Hp, int frames) {

	}
}
