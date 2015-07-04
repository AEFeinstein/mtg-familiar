package com.gelakinetic.mtgfam.helpers;

import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;

import java.io.Serializable;

/**
 * Encapsulate all information about a search query. It is serializable to save to a file easily
 */
public class SearchCriteria implements Serializable {
    private static final long serialVersionUID = 4712329695735151964L;
    public String name;
    public String text;
    public String type;
    public String color = "wubrgl";
    public int colorLogic = 0;
    public String set;
    public Float powChoice = (float) CardDbAdapter.NO_ONE_CARES;
    public String powLogic;
    public Float touChoice = (float) CardDbAdapter.NO_ONE_CARES;
    public String touLogic;
    public int cmc = -1;
    public String cmcLogic;
    public String format;
    public String rarity;
    public String flavor;
    public String artist;
    public int typeLogic = 0;
    public int textLogic = 0;
    public int setLogic;
    public String collectorsNumber;
}