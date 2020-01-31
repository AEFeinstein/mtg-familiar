/*
 * Copyright 2017 bmaurer
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

package com.gelakinetic.mtgfam.helpers.util;

import android.content.Context;

import androidx.fragment.app.Fragment;

import com.gelakinetic.mtgfam.FamiliarActivity;
import com.gelakinetic.mtgfam.R;

public class FragmentHelpers {

    public static boolean isInstanceOf(final Context context, final Class<?> pClass) {
        final Fragment fragment = ((FamiliarActivity) context).getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        return pClass.isInstance(fragment);
    }

}
