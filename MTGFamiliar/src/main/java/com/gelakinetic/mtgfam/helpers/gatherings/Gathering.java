package com.gelakinetic.mtgfam.helpers.gatherings;

import java.util.ArrayList;

public class Gathering {
	public ArrayList<GatheringsPlayerData> mPlayerList;
	public int mDisplayMode;

	public Gathering() {
		mPlayerList = new ArrayList<GatheringsPlayerData>();
		mDisplayMode = 0;
	}

	public Gathering(ArrayList<GatheringsPlayerData> _playerList, int _displayMode) {
		mPlayerList = _playerList;
		mDisplayMode = _displayMode;
	}
}
