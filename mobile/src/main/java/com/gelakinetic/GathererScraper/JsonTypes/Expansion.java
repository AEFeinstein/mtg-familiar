package com.gelakinetic.GathererScraper.JsonTypes;

/**
 * This class contains all information about an expansion to be parsed
 *
 * @author AEFeinstein
 *
 */
public class Expansion implements Comparable<Expansion>{
	/** Name used by Gatherer */
	public String				mName_gatherer	= "";
	/** expansion code used by Gatherer */
	public String				mCode_gatherer	= "";
	/** expansion code used by magiccards.info */
	public String				mCode_mtgi		= "";
	/** expansion name used by TCGPlayer.com */
	public String				mName_tcgp		= "";
	/** expansion name used by MagicCardMarket.eu */
	public String				mName_mkm		= "";
	/** Date the expansion was released */
	public long				mReleaseTimestamp	= 0;
	/** Whether or not this expansion has foil cards */
	public boolean				mCanBeFoil		= false;

	/** To scrape, or not to scrape ? */
	public transient Boolean	mChecked		= false;

	/** MD5 digest for scraped cards, to see when things change */
//	public byte[]				mDigest 		= new byte[16];
	public String				mDigest 		= "";
	
	/**
	 * The most basic constructor for an expansion. Only sets the gatherer name
	 *
	 * @param name_gatherer
	 *            The name of this expansion on Gatherer
	 */
	public Expansion(String name_gatherer) {
		mName_gatherer = name_gatherer;
	}

	
	@Override
	public int compareTo(Expansion o) {
		return this.mName_gatherer.compareTo(o.mName_gatherer);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Expansion) {
			return this.mName_gatherer.equals(((Expansion)obj).mName_gatherer);
		}
		return false;
	}

	/**
	 * Use the Gatherer code as a proxy if this expansion was scraped or not
	 * 
	 * @return true if the gatherer code exists, false if it does not
	 */
	public boolean isScraped() {
		return mCode_gatherer != null && !mCode_gatherer.isEmpty();
	}


}
