/*
 * Copyright 2017 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers.model;

import androidx.annotation.NonNull;

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
    public StringBuilder appendToSql(StringBuilder sqlString, String part1, String part2) {
        String searchCondition = "";
        switch (this) {
            case EQ:
                searchCondition = part1 + " = '" + part2 + "'";
                break;
            case NE:
                searchCondition = part1 + " <> '" + part2 + "'";
                break;
            case CT:
                searchCondition = part1 + " LIKE '%" + part2 + "%'";
                break;
            case NC:
                searchCondition = part1 + " NOT LIKE '%" + part2 + "%'";
                break;
        }
        if (sqlString.length() == 0) {
            return sqlString.append(searchCondition);
        } else if (searchCondition.isEmpty()) {
            return sqlString;
        } else {
            return sqlString.append(" AND ").append(searchCondition);
        }
    }
}
