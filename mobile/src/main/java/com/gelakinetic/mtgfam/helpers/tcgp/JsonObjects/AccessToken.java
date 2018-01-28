package com.gelakinetic.mtgfam.helpers.tcgp.JsonObjects;

import java.util.Date;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class AccessToken {
    public final String access_token;
    public final String token_type;
    public final int expires_in;
    public final String userName;
    @SerializedName(".issued")
    public final Date issued;
    @SerializedName(".expires")
    public final Date expires;

    public AccessToken() {
        access_token = null;
        token_type = null;
        expires_in = 0;
        userName = null;
        issued = null;
        expires = null;
    }

    public final static void setDateFormat(GsonBuilder builder) {
        // Sun, 04 Feb 2018 23:30:02 GMT
        builder.setDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    }
}
