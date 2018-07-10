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
    public final boolean success;
    public final String errors[];
    public final Details results[];

    public ProductDetails() {
        success = false;
        errors = new String[]{};
        results = new Details[]{};
    }

    public static class Details {
        public final long productId;
        public final String productName;
        public final String image;
        public final CatalogData.CatalogDataItem category;
        public final Group group;
        public final String url;
        public final String modifiedOn; // Date modifiedOn;
        public final Conditions productConditions[];
        public final int imageCount;
        public final PresaleInfo presaleInfo;
        public final ExtendedData extendedData[];

        public Details() {
            productId = 0;
            productName = null;
            image = null;
            category = null;
            group = null;
            url = null;
            modifiedOn = null;
            productConditions = null;
            imageCount = 0;
            presaleInfo = null;
            extendedData = null;
        }
    }

    public static class Conditions {
        public final long productConditionId;
        public final String name;
        public final String language;
        public final boolean isFoil;

        public Conditions() {
            productConditionId = 0;
            name = null;
            language = null;
            isFoil = false;
        }
    }

    public static class PresaleInfo {
        public final boolean isPresale;
        public final String releasedOn; // Date releasedOn;
        public final String note;

        public PresaleInfo() {
            isPresale = false;
            releasedOn = null;
            note = null;
        }
    }

    public static class ExtendedData {
        public final String name;
        public final String displayName;
        public final String value;

        public ExtendedData() {
            name = null;
            displayName = null;
            value = null;
        }
    }

    public static class Group {
        public final long groupId;
        public final String name;
        public final String abbreviation;
        public final boolean supplemental;
        public final String publishedOn;
        public final CatalogData.CatalogDataItem category;
        public final String modifiedOn; // Date modifiedOn;

        public Group() {
            groupId = 0;
            name = null;
            abbreviation = null;
            supplemental = false;
            publishedOn = null;
            category = null;
            modifiedOn = null;
        }
    }
}
