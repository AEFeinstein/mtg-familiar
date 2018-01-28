package com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects;

public class ProductInformation {
    public final int totalItems;
    public final boolean success;
    public final String errors[];
    public final long results[];

    public ProductInformation() {
        totalItems = 0;
        success = false;
        errors = null;
        results = null;
    }
}
