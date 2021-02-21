package com.gelakinetic.mtgfam.helpers;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class DeckListImporter {
    private final List<MtgCard> mParsedCards = new ArrayList<>();
    private final List<String> mErrorLines = new ArrayList<>();
    private boolean mReadingSideBoard = false;
    private static final Pattern mCardPattern = Pattern.compile("^(\\d+)\\s+(?:\\[(\\w{2,6})*])? *([^(\\[\\n\\r]+)(?:[(\\[](\\w{2,6})[)\\]])? *(\\d+)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern mNamePattern = Pattern.compile("^//\\s*NAME\\s?:\\s*(.*)\\s*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    @NonNull
    public List<MtgCard> getParsedCards() {
        return mParsedCards;
    }

    @NonNull
    public List<String> getErrorLines() {
        return mErrorLines;
    }

    public void parseLine(String line) {
        line = line.trim();
        if (line.startsWith("//")) {
            /* comment line, ignore */
            return;
        }

        if (line.isEmpty() || line.equalsIgnoreCase("Sideboard")) {
            mReadingSideBoard = true;
            return;
        }

        boolean currentIsSideBoard = mReadingSideBoard;
        if (line.startsWith("SB:")) {
            line = line.substring(3).trim();
            currentIsSideBoard = true;
        }

        try {
            Matcher match = mCardPattern.matcher(line);

            if(match.find()) {
                String cardName = match.group(3).trim();
                /* deal with split cards (/ or // in name): only include first half */
                cardName = cardName.split("\\s/+\\s", 2)[0];

                int numberOf = parseInt(match.group(1));

                String set = match.group(2);
                String set2 = match.group(4);
                String cardSet = null != set ? set : null != set2 ? set2 : "";
//              String idxInSet = match.group(5);
                final boolean isFoil = false;

                mParsedCards.add(new MtgCard(cardName, cardSet, isFoil, numberOf, currentIsSideBoard));
            }
            else{
                mErrorLines.add(currentIsSideBoard ? "SB: " + line : line);
            }
        } catch (NullPointerException | IllegalStateException e) {
            mErrorLines.add(currentIsSideBoard ? "SB: " + line : line);
        }
    }

    @NonNull
    public static String tryGuessDeckName(String deckLines) {
        Matcher match = mNamePattern.matcher(deckLines);
        String name;
        if (match.find() && (name = match.group(1)) != null) {
            return name;
        }
        return "";
    }
}
