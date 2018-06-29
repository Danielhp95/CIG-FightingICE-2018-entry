import java.util.ArrayList;

import aiinterface.CommandCenter;
import enumerate.Action;
import aiinterface.AIInterface;
import network.*;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;
import struct.Key;
import structs.InputData;
import util.Matrix2D;

public class LoadTorchWeightAI implements AIInterface {
	Network network;
	ArrayList<FrameData> f_list = new ArrayList<FrameData>();
	ArrayList<CharacterData> mc_list = new ArrayList<CharacterData>();
	ArrayList<CharacterData> oc_list = new ArrayList<CharacterData>();
	ArrayList<Action> a_list = new ArrayList<Action>();
	private Key key;
	private CommandCenter commandCenter;
	private boolean playerNumber;
	private GameData gameData;

	private FrameData frameData;
	private CharacterData myCharacter;
	private CharacterData oppCharacter;
	
	int[] action_count = new int[56];
	
	@Override
	public void close() {
		
	}

	@Override
	public void getInformation(FrameData frameData) {
		this.frameData = frameData;
		this.commandCenter.setFrameData(this.frameData, playerNumber);
		
		myCharacter = frameData.getCharacter(playerNumber);
		oppCharacter = frameData.getCharacter(!playerNumber);

	}

	@Override
	public int initialize(GameData gameData, boolean playerNumber) {
		
		this.playerNumber = playerNumber;
		this.gameData = gameData;

		this.key = new Key();
		this.frameData = new FrameData();
		this.commandCenter = new CommandCenter();
		
		//initializes the network
		//defines the network architecture
		this.network = new Network();
		Layer layer_1 = new Layer(1376, 1000, new ReLU());
		layer_1.loadWeight("data/aiData/LoadTorchWeightAI/model_weight1.csv");
		layer_1.loadBias("data/aiData/LoadTorchWeightAI/model_bias1.csv");
		network.addLayer(layer_1);
		Layer layer_2 = new Layer(1000, 1000, new ReLU());
		layer_2.loadWeight("data/aiData/LoadTorchWeightAI/model_weight3.csv");
		layer_2.loadBias("data/aiData/LoadTorchWeightAI/model_bias3.csv");
		network.addLayer(layer_2);
		Layer layer_3 = new Layer(1000, 56, new None());
		layer_3.loadWeight("data/aiData/LoadTorchWeightAI/model_weight5.csv");
		layer_3.loadBias("data/aiData/LoadTorchWeightAI/model_bias5.csv");
		network.addLayer(layer_3);

		return 0;
	}

	@Override
	public Key input() {
		return key;
	}

	@Override
	public void processing() {
		double time = System.currentTimeMillis();
		if (canProcessing()) {
			
			//This part is for the input representation.
			prepare();
			
			if (commandCenter.getSkillFlag()) {
				key = commandCenter.getSkillKey();
				
				//This is for the input representation.
				a_list.add(Action.STAND);
				
			} else {

				key.empty();
				commandCenter.skillCancel();

				//creates the input from NowFrameData and other data
				InputData temp = new InputData(gameData, playerNumber, mc_list, oc_list, f_list, a_list);
				double[][] inputs = new Matrix2D(temp.Input).getArrays();
				
				//forward propagation
				double[][] outputs = this.network.forward(inputs);

				//chooses the highest evaluation action from the outputs
				int action_n = 0;
				double max_value=0;
				for(int i=0; i<outputs.length; i++){
					if(max_value<outputs[i][0]){
						max_value = outputs[i][0];
						action_n=i;
					}
				}
				System.out.println("Choice:" + Action.values()[action_n].name() + "(No." + action_n + ")..."+max_value);
				
				//This part is for the input representation..
				a_list.add(Action.values()[action_n]);
				action_count[action_n]++;
				
				//returns an action to Framework
				commandCenter.commandCall(Action.values()[action_n].name());
				
				//prints required time to return an action  
				System.out.println("Required time to return action:" + (System.currentTimeMillis()-time)+"ms");
			}
			
		}else{

		}
	}
	public boolean canProcessing() {
		return !frameData.getEmptyFlag() && frameData.getRemainingTimeMilliseconds() > 0;
	}

	private void prepare(){
		if (f_list.size() >= 4)
			f_list.remove(0);
		if (mc_list.size() >= 4)
			mc_list.remove(0);
		if (oc_list.size() >= 4)
			oc_list.remove(0);
		if (a_list.size() >= 16)
			a_list.remove(0);

		f_list.add(frameData);
		mc_list.add(myCharacter);
		oc_list.add(oppCharacter);
	}
	
	@Override
	public void roundEnd(int arg0, int arg1, int arg2) {
		// TODO 自動生成されたメソッド・スタブ

	}
}
