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
 * along with MTG Familiar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.helpers.gatherings;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class encapsulates all necessary data for a Gathering: a list of players and a display mode
 */
public class Gathering implements Serializable {
    public final ArrayList<GatheringsPlayerData> mPlayerList;
    public int mDisplayMode;

    /**
     * Default constructor, make a new mPlayerList and set the display mode to normal (0)
     */
    public Gathering() {
        mPlayerList = new ArrayList<>();
        mDisplayMode = 0;
    }

    /**
     * Constructor with parameters. Set the inner fields to the parameters
     *
     * @param _playerList  A list of GatheringsPlayerData
     * @param _displayMode The default display mode
     */
    public Gathering(ArrayList<GatheringsPlayerData> _playerList, int _displayMode) {
        mPlayerList = _playerList;
        mDisplayMode = _displayMode;
    }
}
