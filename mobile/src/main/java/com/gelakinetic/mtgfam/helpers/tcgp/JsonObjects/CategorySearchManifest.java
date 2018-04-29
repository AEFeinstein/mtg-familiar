/*
 * Copyright 2018 Adam Feinstein
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

package com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects;

@SuppressWarnings("FieldCanBeLocal")
public class CategorySearchManifest {
    public final boolean success;
    public final String errors[];
    public final TextValuePair[] sortingOptions;
    public final FilterOptions[] filterOptions;

    public CategorySearchManifest() {
        success = false;
        errors = new String[]{};
        sortingOptions = new TextValuePair[]{};
        filterOptions = new FilterOptions[]{};
    }

    public CategorySearchManifest(boolean success, String[] errors, TextValuePair[] sorting, FilterOptions[] filters) {
        this.success = success;
        this.errors = errors;
        this.sortingOptions = sorting;
        this.filterOptions = filters;
    }

    public static class TextValuePair {
        public final String text;
        public final String value;

        public TextValuePair(String _text, String _value) {
            this.text = _text;
            this.value = _value;
        }
    }

    public static class FilterOptions {
        public final String name;
        public final String displayName;
        public final String inputType;
        public final TextValuePair[] items;

        public FilterOptions() {
            name = null;
            displayName = null;
            inputType = null;
            items = null;
        }
    }

}
