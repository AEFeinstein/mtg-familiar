package com.gelakinetic.GathererScraper.JsonTypes;

import java.util.ArrayList;

public class LegalityData {

    public Format mFormats[];
    public long mTimestamp;

    public class Format {
        public String mName;
        public ArrayList<String> mSets = new ArrayList<>();
        public ArrayList<String> mRestrictedlist = new ArrayList<>();
        public ArrayList<String> mBanlist = new ArrayList<>();
    }
}

