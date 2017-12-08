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

package com.gelakinetic.mtgfam.helpers.gatherings;

import java.io.Serializable;

/**
 * This class encapsulates all necessary data for a Gathering player: a name and a starting life
 */
public class GatheringsPlayerData implements Serializable {
    public String mName;
    public int mStartingLife;

    /**
     * Default constructor, set the name to empty and the life to 20
     */
    public GatheringsPlayerData() {
        mName = "";
        mStartingLife = 20;
    }

    /**
     * Constructor with parameters. Set the inner fields to the parameters
     *
     * @param _name The name of this player
     * @param _life The starting life of this player
     */
    public GatheringsPlayerData(String _name, int _life) {
        mName = _name;
        mStartingLife = _life;
    }

}

