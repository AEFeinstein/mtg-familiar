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
public class ProductDetails {
    private final boolean success;
    public final String[] errors;
    public final Details[] results;

    public ProductDetails() {
        success = false;
        errors = new String[]{};
        results = new Details[]{};
    }

    public static class Details {
        public final long productId;
        public final String name;
        final String cleanName;
        final String imageUrl;
        final long categoryId;
        public final long groupId;
        public final String url;
        final String modifiedOn; // Date modifiedOn;

        public Details() {
            productId = 0;
            name = null;
            cleanName = null;
            imageUrl = null;
            categoryId = -1;
            groupId = -1;
            url = null;
            modifiedOn = null;
        }
    }
}
