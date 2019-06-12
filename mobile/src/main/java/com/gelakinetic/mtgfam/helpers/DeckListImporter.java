package com.gelakinetic.mtgfam.helpers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gelakinetic.mtgfam.helpers.DecklistHelpers.CompressedDecklistInfo;

import static java.lang.Integer.parseInt;

public class DeckListImporter {
    private List<MtgCard> mParsedCards;
    private boolean mReadingSideBoard = false;
    private static final Pattern mCardPattern = Pattern.compile("^(\\d+)\\s+(?:\\[(\\w{2,6})])?\\s*([^(\\d]+)(?:\\((\\w{2,6})\\))?\\s*(\\d+)?$");

    public void parseLine(String line) {
        line = line.trim();
        if (line.startsWith("//")) {
            /* comment line, TODO: possibly parse deck name (and more?) */
            return;
        }

        if (line.isEmpty() || line.equalsIgnoreCase("Sideboard")) {
            mReadingSideBoard = true;
            return;
        }

        boolean currentIsSideBoard = mReadingSideBoard;
        if (line.startsWith("SB:")){
            line = line.substring(3).trim();
            currentIsSideBoard = true;
        }

        Matcher match = mCardPattern.matcher(line);
        String cardName = match.group(3).trim();
        int numberOf = parseInt(match.group(1));
        String set = match.group(2);
        String set2 = match.group(4);
        String cardSet = null != set ? set : null != set2 ? set2 : "";
        String idxInSet = match.group(5);
        final boolean isFoil = false;

        mParsedCards.add(new MtgCard(cardName, cardSet, isFoil, numberOf, currentIsSideBoard));
    }
}
