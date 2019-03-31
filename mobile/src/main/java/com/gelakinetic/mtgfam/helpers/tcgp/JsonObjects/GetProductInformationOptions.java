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
public class GetProductInformationOptions {
    private final String sort;
    private final int limit;
    private final int offset;
    private final boolean includeAggregates;
    private final NameValuesPair[] filters;

    public GetProductInformationOptions(NameValuesPair[] nameValuesPairs) {
        filters = nameValuesPairs;
        offset = 0;
        limit = 100;
        sort = "Relevance";
        includeAggregates = true;
    }

    public static class NameValuesPair {
        final String name;
        final String[] values;

        public NameValuesPair(String _name, String[] _values) {
            this.name = _name;
            this.values = _values;
        }
    }
}
