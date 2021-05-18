/*
 * Copyright 2017 Adam Feinstein
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

package com.gelakinetic.GathererScraper.JsonTypes;

/*
 * This class contains all information about an expansion to be parsed
 *
 * @author AEFeinstein
 *
 */
@SuppressWarnings("CanBeFinal")
public class Expansion {

    // Name used by Gatherer
    public String mName_gatherer = "";

    // expansion code used by Gatherer
    public String mCode_gatherer = "";

    // expansion code used by magiccards.info
    public String mCode_mtgi = "";

    // expansion mName used by TCGPlayer.com
    public String mName_tcgp = "";

    // expansion name used by MagicCardMarket.eu
    public String mName_mkm = "";

    // Date the expansion was released
    public long mReleaseTimestamp = 0;

    // Whether or not this expansion has foil cards
    public boolean mCanBeFoil = false;

    // Whether this expansion is online-only or has paper printings
    public boolean mIsOnlineOnly = false;

    // The color of the border, either Black, White, or Silver
    public String mBorderColor = "";

    // The type of set
    public String mType;

    // MD5 digest for scraped cards, to see when things change
    public String mDigest = "";
}
