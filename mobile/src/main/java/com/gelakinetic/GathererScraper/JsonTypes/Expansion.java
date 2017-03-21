package com.gelakinetic.GathererScraper.JsonTypes;

/*
 * This class contains all information about an expansion to be parsed
 *
 * @author AEFeinstein
 *
 */
public class Expansion {

    // Name used by Gatherer
    public String mName_gatherer = "";

    // expansion code used by Gatherer
    public String mCode_gatherer = "";

    // expansion code used by magiccards.info
    public String mCode_mtgi = "";

    // expansion mName used by TCGPlayer.com
    public String mName_tcgp = "";

    // expansion name used by MagicCardMarket.eu
    public String mName_mkm = "";

    // Date the expansion was released
    public long mReleaseTimestamp = 0;

    // Whether or not this expansion has foil cards
    public boolean mCanBeFoil = false;

    // MD5 digest for scraped cards, to see when things change
    public String mDigest = "";
}
