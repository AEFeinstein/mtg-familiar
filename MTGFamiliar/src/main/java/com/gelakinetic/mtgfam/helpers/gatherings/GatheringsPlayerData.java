package com.gelakinetic.mtgfam.helpers.gatherings;

public class GatheringsPlayerData {
	public String mName;
	public int mStartingLife;

	public GatheringsPlayerData() {
		mName = "";
		mStartingLife = 20;
	}

	public GatheringsPlayerData(String _name, int _life) {
		mName = _name;
		mStartingLife = _life;
	}

}

