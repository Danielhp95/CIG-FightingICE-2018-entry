from py4j.java_gateway import get_field

class KickAI(object):
    def __init__(self, gateway, match_info=None):
        self.gateway = gateway
        if match_info is not None:
            self.match_info = match_info

    def close(self):
        pass

    def getInformation(self, frameData):
        # Getting the frame data of the current frame
        self.frameData = frameData

    # please define this method when you use FightingICE version 3.20 or later
    def roundEnd(self, p1_hp, p2_hp, frames_remaining):
        self.match_info.p1_hp = p1_hp
        self.match_info.p2_hp = p2_hp

    # please define this method when you use FightingICE version 4.00 or later
    def getScreenData(self, sd):
        pass

    def initialize(self, gameData, player):
        # Initializng the command center, the simulator and some other things
        self.inputKey = self.gateway.jvm.struct.Key()
        self.frameData = self.gateway.jvm.struct.FrameData()
        self.cc = self.gateway.jvm.aiinterface.CommandCenter()
            
        self.player = player
        self.gameData = gameData
        self.simulator = self.gameData.getSimulator()
                
        return 0
        
    def input(self):
        # Return the input for the current frame
        return self.inputKey
        
    def processing(self):
        # Just compute the input for the current frame
        if self.frameData.getEmptyFlag() or self.frameData.getRemainingTime() <= 0:
                self.isGameJustStarted = True
                return
                
        self.cc.setFrameData(self.frameData, self.player)
                
        if self.cc.getSkillFlag():
                self.inputKey = self.cc.getSkillKey()
                return
                
        # Just spam kick
        self.cc.commandCall("B")
        self.match_info.duration +=1
                        
    # This part is mandatory
    class Java:
        implements = ["aiinterface.AIInterface"]
        
