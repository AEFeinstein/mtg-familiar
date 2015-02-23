package com.gelakinetic.mtgfam.helpers.gatherings;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class encapsulates all necessary data for a Gathering: a list of players and a display mode
 */
public class Gathering implements Serializable {
    public final ArrayList<GatheringsPlayerData> mPlayerList;
    public int mDisplayMode;

    /**
     * Default constructor, make a new mPlayerList and set the display mode to normal (0)
     */
    public Gathering() {
        mPlayerList = new ArrayList<>();
        mDisplayMode = 0;
    }

    /**
     * Constructor with parameters. Set the inner fields to the parameters
     *
     * @param _playerList  A list of GatheringsPlayerData
     * @param _displayMode The default display mode
     */
    public Gathering(ArrayList<GatheringsPlayerData> _playerList, int _displayMode) {
        mPlayerList = _playerList;
        mDisplayMode = _displayMode;
    }
}
