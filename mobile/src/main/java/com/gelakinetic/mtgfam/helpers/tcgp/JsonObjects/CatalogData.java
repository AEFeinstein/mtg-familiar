package com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects;

import com.google.gson.GsonBuilder;

import java.net.URL;
import java.util.Date;

public class CatalogData {
    public final boolean success;
    public final String[] errors;
    public final CatalogDataItem[] results;

    public CatalogData() {
        success = false;
        errors = null;
        results = null;
    }

    public static class CatalogDataItem {
        public final int categoryId;
        public final String name;
        public final Date modifiedOn;
        public final String displayName;
        public final String seoCategoryName;
        public final String sealedLabel;
        public final String nonSealedLabel;
        public final URL conditionGuideUrl;
        public final boolean isScannable;
        public final int popularity;

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

        public final static void setDateFormat(GsonBuilder builder) {
            // 2017-03-01T11:03:09.737
            builder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        }
    }
}
