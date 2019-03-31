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
import com.google.gson.annotations.SerializedName;

import java.util.Date;

@SuppressWarnings("FieldCanBeLocal")
public class AccessToken {
    public final String access_token;
    private final String token_type;
    private final int expires_in;
    private final String userName;
    @SerializedName(".issued")
    private final Date issued;
    @SerializedName(".expires")
    public final Date expires;

    public AccessToken() {
        access_token = "";
        token_type = "";
        expires_in = 0;
        userName = "";
        issued = new Date();
        expires = new Date();
    }

    public static void setDateFormat(GsonBuilder builder) {
        // Sun, 04 Feb 2018 23:30:02 GMT
        builder.setDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    }
}
