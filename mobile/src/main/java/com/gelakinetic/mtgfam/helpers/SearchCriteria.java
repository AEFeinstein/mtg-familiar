package com.gelakinetic.mtgfam.helpers;

import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.model.Comparison;

import java.io.Serializable;
import java.util.List;

/**
 * Encapsulate all information about a search query. It is serializable to save to a file easily
 */
public class SearchCriteria implements Serializable {
    private static final long serialVersionUID = 4712329695735151965L;
    public String name = null;
    public String text = null;
    public List<String> superTypes = null;
    public List<String> subTypes = null;
    public String color = "wubrgl";
    public int colorLogic = 0;
    public List<String> sets = null;
    public float powChoice = (float) CardDbAdapter.NO_ONE_CARES;
    public String powLogic = null;
    public float touChoice = (float) CardDbAdapter.NO_ONE_CARES;
    public String touLogic = null;
    public int cmc = -1;
    public String cmcLogic = null;
    public String format = null;
    public String rarity = null;
    public String flavor = null;
    public String artist = null;
    public int typeLogic = 0;
    public int textLogic = 0;
    public int setLogic = CardDbAdapter.MOST_RECENT_PRINTING;
    public String collectorsNumber = null;
    public String colorIdentity = "wubrgl";
    public int colorIdentityLogic = 0;
    public List<String> manaCost = null;
    public Comparison manaCostLogic;
    public boolean moJhoStoFilter;
}