package com.gelakinetic.GathererScraper.JsonTypes;

import java.util.Collection;

/**
 * This class contains all information of a patch.
 * It is mainly used to export to/import from a json file.:
 */
public class Patch {

    // The patch's expansion
    public Expansion mExpansion;

    // The patch's cards
    public Collection<Card> mCards;

}
