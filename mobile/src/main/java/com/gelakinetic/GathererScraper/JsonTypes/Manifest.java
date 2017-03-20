package com.gelakinetic.GathererScraper.JsonTypes;

import java.util.ArrayList;

public class Manifest {
	public long mTimestamp;
	public ArrayList<ManifestEntry> mPatches = new ArrayList<ManifestEntry>();
	
	public class ManifestEntry {
		public String mName;
		public String mURL;
		public String mCode;
		public String mDigest;
	}

}
