package com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects;

public class CategorySearchManifest {
    public final boolean success;
    public final String errors[];
    public final TextValuePair[] sortingOptions;
    public final FilterOptions[] filterOptions;

    public CategorySearchManifest() {
        success = false;
        errors = null;
        sortingOptions = null;
        filterOptions = null;
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
