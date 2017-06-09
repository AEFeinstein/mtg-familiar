package com.gelakinetic.mtgfam.helpers;

import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;

import java.io.Serializable;

/**
 * Encapsulate all information about a search query. It is serializable to save to a file easily
 */
public class SearchCriteria implements Serializable {
    private static final long serialVersionUID = 4712329695735151964L;
    public String name = null;
    public String text = null;
    public String type = null;
    public String color = "wubrgl";
    public int colorLogic = 0;
    public String set = null;
    public Float powChoice = (float) CardDbAdapter.NO_ONE_CARES;
    public String powLogic = null;
    public Float touChoice = (float) CardDbAdapter.NO_ONE_CARES;
    public String touLogic = null;
    public int cmc = -1;
    public Boolean hasManaX = false;
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
    public boolean noTokens;
}