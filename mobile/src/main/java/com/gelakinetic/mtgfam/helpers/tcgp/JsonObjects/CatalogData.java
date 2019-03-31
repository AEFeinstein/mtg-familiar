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

import com.google.gson.GsonBuilder;

import java.net.URL;

@SuppressWarnings("FieldCanBeLocal")
public class CatalogData {
    private final boolean success;
    private final String[] errors;
    private final CatalogDataItem[] results;

    public CatalogData() {
        success = false;
        errors = new String[]{};
        results = new CatalogDataItem[]{};
    }

    public static class CatalogDataItem {
        final int categoryId;
        final String name;
        final String modifiedOn; // Date modifiedOn;
        final String displayName;
        final String seoCategoryName;
        final String sealedLabel;
        final String nonSealedLabel;
        final URL conditionGuideUrl;
        final boolean isScannable;
        final int popularity;

        public CatalogDataItem() {
            categoryId = 0;
            name = null;
            modifiedOn = null;
            displayName = null;
            seoCategoryName = null;
            sealedLabel = null;
            nonSealedLabel = null;
            conditionGuideUrl = null;
            isScannable = false;
            popularity = 0;
        }

        public static void setDateFormat(GsonBuilder builder) {
            // 2017-03-01T11:03:09.737
            builder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        }
    }
}
