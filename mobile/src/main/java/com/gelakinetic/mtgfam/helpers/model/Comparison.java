package com.gelakinetic.mtgfam.helpers.model;

import android.support.annotation.NonNull;

import com.gelakinetic.mtgfam.R;

import org.jetbrains.annotations.Contract;

public enum Comparison {
    EMPTY(" ", R.string.NoComparison),
    EQ("=", R.string.Equals),
    NE("≠", R.string.DoesNotEqual),
    CT("∋", R.string.Contains),
    NC("∌", R.string.DoesNotContain);

    private final String shortDescription;
    private final int longDescriptionRes;

    Comparison(String shortDescription, int longDescriptionRes) {
        this.shortDescription = shortDescription;
        this.longDescriptionRes = longDescriptionRes;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public int getLongDescriptionRes() {
        return longDescriptionRes;
    }

    @NonNull
    @Contract(pure = true)
    public String appendToSql(String sqlString, String part1, String part2) {
        String searchCondition = "";
        switch (this) {
            case EQ: searchCondition = part1 + " = '" + part2 + "'";
                break;
            case NE: searchCondition = part1 + " <> '" + part2 + "'";
                break;
            case CT: searchCondition = part1 + " LIKE '%" + part2 + "%'";
                break;
            case NC: searchCondition = part1 + " NOT LIKE '%" + part2 + "%'";
                break;
        }
        if (sqlString.isEmpty()) {
            return searchCondition;
        } else if (searchCondition.isEmpty()) {
            return sqlString;
        } else {
            return sqlString + " AND " + searchCondition;
        }
    }
}
