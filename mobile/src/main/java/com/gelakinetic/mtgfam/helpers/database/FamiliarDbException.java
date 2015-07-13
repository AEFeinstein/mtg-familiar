package com.gelakinetic.mtgfam.helpers.database;

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
    }

    /**
     * Just pass through the inner exception's string
     *
     * @return An explanation of the exception
     */
    @Override
    public String toString() {
        return mInnerException.toString();
    }
}
