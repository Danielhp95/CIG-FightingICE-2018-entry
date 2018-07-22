Entry to CIG 2018 FightingICE AI competition. This is a submission for the countdown track, where the AI needs to beat a sample MCTS agent as fast as possible.

## The environment:

The environment is FightingICE, FightingICE is a 2 player fighting game maintained by [Ritsumeikan](http://en.ritsumei.ac.jp/), as part of the [CIG Fighting game AI competition](http://www.ice.ci.ritsumei.ac.jp/~ftgaic/). There are different submission tracks for this competition: TODO, TODO and countdown.

For the countdown track the opponent AI always uses a vanilla(?) MCTS agent. Due to the stochastic nature of MCTS, the environment becomes stochastic.

## The reward function

Based on these papers (Microsoft fist of the lotus, and Rosas (Simon's paper)).

The reward function reflects the frame by frame health change difference. R(s,a,s') = healthChange(p1, s, s') - healthChange(p2, s, s').
Winning yields a high positive reward, dying yields a high negative reward.

## The agent

The agent uses Proximal Policy Optimization (Schulman 2017) to train. The algorithm alternates between experience collection and a policy update step:

1. Experience collection step:
    + Collects *T* experiences using old policy where *T* << episode length.
    + computes advantage function for all timesteps 1..T using GAE (link).
2. New policy paramer vector created by optimizing surrogate function L (we use the clipped version introduced in the original paper) wrt theta using K epochs and a minibatch size of M <= T. (That is, to take the gradient of the loss function)
3. update old policy 

### State space
Approximated game state.

### Action space
The action space defines all the possible (combat) moves that the agent can take at a specific frame.

#### Action space tricks
Actions that are not allowed are not taken into account for a given frame. Only the valid actions are taken into account. The actions probabilities are normalized, and then a new action resampled
