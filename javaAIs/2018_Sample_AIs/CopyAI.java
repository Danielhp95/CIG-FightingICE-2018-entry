import aiinterface.AIInterface;
import aiinterface.CommandCenter;
import struct.FrameData;
import struct.GameData;
import struct.Key;


public class CopyAI implements AIInterface {

	private Key inputKey;
	private boolean player;
	private FrameData frameData;
	private CommandCenter commandCenter;

	@Override
	public void close() {

	}

	@Override
	public void getInformation(FrameData frameData) {
		this.frameData = frameData;
		this.commandCenter.setFrameData(this.frameData, player);
	}

	@Override
	public int initialize(GameData arg0, boolean player) {
		inputKey = new Key();
		this.player = player;
		frameData = new FrameData();
		this.commandCenter = new CommandCenter();

		return 0;
	}

	@Override
	public Key input() {
		return inputKey;
	}

	@Override
	public void processing() {
		if(!frameData.getEmptyFlag()){
			if(frameData.getRemainingFramesNumber()>0){
				if (commandCenter.getSkillFlag()) {
					inputKey = commandCenter.getSkillKey();
				} else {
					inputKey.empty();
					commandCenter.skillCancel();
					commandCenter.commandCall(frameData.getCharacter(!player).getAction().name());
				}
			}
		}
	}

	@Override
	public void roundEnd(int p1Hp, int p2Hp, int frames) {

	}

}