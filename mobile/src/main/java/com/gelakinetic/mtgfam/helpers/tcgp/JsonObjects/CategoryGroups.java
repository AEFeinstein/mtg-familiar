package com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects;

public class CategoryGroups {
    public final boolean success;
    public final String[] errors;
    public final Group[] results;

    public CategoryGroups() {
        success = false;
        errors = null;
        results = null;
    }

    public class Group {
        public final long groupId;
        public final String name;
        public final String abbreviation;
        public final boolean supplemental;
        public final String publishedOn;
        public final CatalogData.CatalogDataItem category[];
        public final String modifiedOn;

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
