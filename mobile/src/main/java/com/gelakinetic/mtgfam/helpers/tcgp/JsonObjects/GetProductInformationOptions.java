package com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects;

public class GetProductInformationOptions {
    public final String sort;
    public final int limit;
    public final int offset;
    public final boolean includeAggregates;
    public final NameValuesPair filters[];

    public GetProductInformationOptions(NameValuesPair[] nameValuesPairs) {
        filters = nameValuesPairs;
        offset = 0;
        limit = 100;
        sort = "Relevance";
        includeAggregates = true;
    }

    public static class NameValuesPair {
        public final String name;
        public final String values[];

        public NameValuesPair(String _name, String[] _values) {
            this.name = _name;
            this.values = _values;
        }
    }
}
