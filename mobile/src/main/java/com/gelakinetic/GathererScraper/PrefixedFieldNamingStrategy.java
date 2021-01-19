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

package com.gelakinetic.GathererScraper;

import com.google.gson.FieldNamingStrategy;

import java.lang.reflect.Field;

/**
 * Implementation of a FieldNamingStrategy that translate a field's name by removing the prefix from it
 * and lowercase the following character.
 */
public class PrefixedFieldNamingStrategy implements FieldNamingStrategy {

    /**
     * The prefix to be match on the field's name
     */
    private final String mPrefix;

    /**
     * Create a PrefixedFieldNamingStrategy object that check for a prefix on the field's name,
     * and call transformName on the remaining of the field's name.
     *
     * @param prefix The prefix to check of the field's name.
     */
    public PrefixedFieldNamingStrategy(String prefix) {
        mPrefix = prefix;
    }

    @Override
    public String translateName(final Field f) {
        String name = f.getName();
        if (name.startsWith(mPrefix)) {
            return PrefixedFieldNamingStrategy.lowercaseFirstLetter(name.substring(mPrefix.length()));
        } else {
            throw new IllegalArgumentException("Don't know how to handle field not starting with m prefix: " + name);
        }
    }

    /**
     * Return a string with the first letter being lowercased.
     *
     * @param s the string to transform.
     * @return A string with a lowercase first letter
     */
    private static String lowercaseFirstLetter(String s) {
        if (s.length() > 0) {
            char[] c = s.toCharArray();
            c[0] = Character.toLowerCase(c[0]);
            s = new String(c);
        }
        return s;
    }
}
