import sys
import os
path = os.path.abspath(os.path.join('AIs'))
sys.path.append(path)
from time import sleep
from py4j.java_gateway import JavaGateway, GatewayParameters, CallbackServerParameters, get_field
from unmodifiedAIs.LogDisplayInfo import LogDisplayInfo
from unmodifiedAIs.DisplayInfo import DisplayInfo
from unmodifiedAIs.Machete import Machete
from unmodifiedAIs.LogInfoMachete import LogInfoMachete
from unmodifiedAIs.Machete import Machete


def check_args(args):
    for i in range(argc):
        if args[i] == "-n" or args[i] == "--n" or args[i] == "--number":
            global GAME_NUM
            GAME_NUM = int(args[i+1])


def get_port():
    for i in range(argc):
        if args[i] == "--port":
            return int(args[i+1])


def start_game():
    #p1 = LogInfoMachete(gateway)
    p1 = DisplayInfo(gateway)
    #p2 = Machete(gateway)
    #p2 = LogDisplayInfo(gateway)
    p2 = DisplayInfo(gateway)
    # p2 = RandomAI(gateway)
    manager.registerAI(p1.__class__.__name__, p1)
    manager.registerAI(p2.__class__.__name__, p2)
    print("Start game")
    
    game = manager.createGame("ZEN", "ZEN",
                                  p1.__class__.__name__,
                                  p2.__class__.__name__,
                                  GAME_NUM)
    
    manager.runGame(game)
    
    print("After game")
    sys.stdout.flush()

def close_gateway():
    gateway.close_callback_server()
    gateway.close()
	
def main_process():
   check_args(args)
   start_game()
   close_gateway()

args = sys.argv
argc = len(args)
GAME_NUM = 1
PORT = 4284
gateway = JavaGateway(gateway_parameters=GatewayParameters(port=PORT), callback_server_parameters=CallbackServerParameters());
manager = gateway.entry_point
main_process()

