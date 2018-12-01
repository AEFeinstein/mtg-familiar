package com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects;

public class CategoryGroups {
    public final boolean success;
    public final String[] errors;
    public final Group[] results;
    public long totalItems;

    public CategoryGroups() {
        totalItems = 0;
        success = false;
        errors = null;
        results = null;
    }

    public class Group {
        public final long groupId;
        public final String name;
        public final String abbreviation;
        public final boolean isSupplemental;
        public final String publishedOn;
        public final long categoryId;
        public final String modifiedOn;

        public Group() {
            groupId = 0;
            name = null;
            abbreviation = null;
            isSupplemental = false;
            publishedOn = null;
            categoryId = 0;
            modifiedOn = null;
        }
    }
}
