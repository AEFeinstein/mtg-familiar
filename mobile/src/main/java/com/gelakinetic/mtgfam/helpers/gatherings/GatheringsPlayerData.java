package com.gelakinetic.mtgfam.helpers.gatherings;

import java.io.Serializable;

/**
 * This class encapsulates all necessary data for a Gathering player: a name and a starting life
 */
public class GatheringsPlayerData implements Serializable {
    public String mName;
    public int mStartingLife;

    /**
     * Default constructor, set the name to empty and the life to 20
     */
    public GatheringsPlayerData() {
        mName = "";
        mStartingLife = 20;
    }

    /**
     * Constructor with parameters. Set the inner fields to the parameters
     *
     * @param _name The name of this player
     * @param _life The starting life of this player
     */
    public GatheringsPlayerData(String _name, int _life) {
        mName = _name;
        mStartingLife = _life;
    }

}

