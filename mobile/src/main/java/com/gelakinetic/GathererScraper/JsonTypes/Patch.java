package com.gelakinetic.GathererScraper.JsonTypes;

import java.util.Collection;

/**
 * This class contains all information of a patch.
 * It is mainly used to export to/import from a json file.: 
 */
public class Patch {
	/** The patch's expansion */
	public Expansion mExpansion;
	
	/** The patch's cards */
	public Collection<Card> mCards;
	
	
	/**
	 * Create and empty patch. Other attributes need to be set later.
	 */
	public Patch()
	{
	}
	
	/**
	 * Create a patch from an expansion and a list of card.
	 * @param expansion the expansion this patch represents.
	 * @param cards the collection of card this patch will represents.
	 */
	public Patch(Expansion expansion, Collection<Card> cards)
	{
		this.mExpansion = expansion;
		this.mCards = cards;
	}
}
