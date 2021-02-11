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

package com.gelakinetic.mtgfam.helpers.database;

import androidx.annotation.NonNull;

import com.gelakinetic.mtgfam.helpers.FamiliarLogger;

/**
 * Extend Exception instead of RuntimeException to force the compiler to whine about lack of try/catch blocks
 */
public class FamiliarDbException extends Exception {

    private static final long serialVersionUID = 5953780555438726164L;
    private final Exception mInnerException;

    /**
     * Encapsulate another exception in the FamiliarDbException
     *
     * @param e The exception initially thrown
     */
    public FamiliarDbException(Exception e) {
        mInnerException = e;
        // Log this exception
        StringBuilder sb = new StringBuilder(e.getMessage());
        sb.append(e.getMessage()).append('\n');
        sb.append(org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e)).append('\n');
        FamiliarLogger.appendToLogFile(sb, "FamiliarDbException");
    }

    /**
     * Just pass through the inner exception's string
     *
     * @return An explanation of the exception
     */
    @NonNull
    @Override
    public String toString() {
        return mInnerException.toString();
    }
}
