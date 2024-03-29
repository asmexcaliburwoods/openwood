/**
 * $RCSfile$
 * $Revision: 38648 $
 * $Date: 2006-12-27 01:46:18 -0800 (Wed, 27 Dec 2006) $
 *
 * Copyright (C) 2004-2005 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software.
 * Use is subject to license terms.
 */

package org.jivesoftware.smackx.workgroup.util;

import java.util.*;

/**
 * Utility methods frequently used by data classes and design-time
 * classes.
 */
public final class ModelUtil {
    private ModelUtil() {
        //  Prevents instantiation.
    }

    /**
     * This is a utility method that compares two objects when one or
     * both of the objects might be <CODE>null</CODE>  The result of
     * this method is determined as follows:
     * <OL>
     * <LI>If <CODE>o1</CODE> and <CODE>o2</CODE> are the same object
     * according to the <CODE>==</CODE> operator, return
     * <CODE>true</CODE>.
     * <LI>Otherwise, if either <CODE>o1</CODE> or <CODE>o2</CODE> is
     * <CODE>null</CODE>, return <CODE>false</CODE>.
     * <LI>Otherwise, return <CODE>o1.equals(o2)</CODE>.
     * </OL>
     * <p/>
     * This method produces the exact logically inverted result as the
     * {@link #areDifferent(Object, Object)} method.<P>
     * <p/>
     * For array types, one of the <CODE>equals</CODE> methods in
     * {@link java.util.Arrays} should be used instead of this method.
     * Note that arrays with more than one dimension will require some
     * custom code in order to implement <CODE>equals</CODE> properly.
     */
    public static final boolean areEqual(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        else if (o1 == null || o2 == null) {
            return false;
        }
        else {
            return o1.equals(o2);
        }
    }

    /**
     * This is a utility method that compares two Booleans when one or
     * both of the objects might be <CODE>null</CODE>  The result of
     * this method is determined as follows:
     * <OL>
     * <LI>If <CODE>b1</CODE> and <CODE>b2</CODE> are both TRUE or
     * neither <CODE>b1</CODE> nor <CODE>b2</CODE> is TRUE,
     * return <CODE>true</CODE>.
     * <LI>Otherwise, return <CODE>false</CODE>.
     * </OL>
     * <p/>
     */
    public static final boolean areBooleansEqual(Boolean b1, Boolean b2) {
        // !jwetherb treat NULL the same as Boolean.FALSE
        return (b1 == Boolean.TRUE && b2 == Boolean.TRUE) ||
                (b1 != Boolean.TRUE && b2 != Boolean.TRUE);
    }

    /**
     * This is a utility method that compares two objects when one or
     * both of the objects might be <CODE>null</CODE>.  The result
     * returned by this method is determined as follows:
     * <OL>
     * <LI>If <CODE>o1</CODE> and <CODE>o2</CODE> are the same object
     * according to the <CODE>==</CODE> operator, return
     * <CODE>false</CODE>.
     * <LI>Otherwise, if either <CODE>o1</CODE> or <CODE>o2</CODE> is
     * <CODE>null</CODE>, return <CODE>true</CODE>.
     * <LI>Otherwise, return <CODE>!o1.equals(o2)</CODE>.
     * </OL>
     * <p/>
     * This method produces the exact logically inverted result as the
     * {@link #areEqual(Object, Object)} method.<P>
     * <p/>
     * For array types, one of the <CODE>equals</CODE> methods in
     * {@link java.util.Arrays} should be used instead of this method.
     * Note that arrays with more than one dimension will require some
     * custom code in order to implement <CODE>equals</CODE> properly.
     */
    public static final boolean areDifferent(Object o1, Object o2) {
        return !areEqual(o1, o2);
    }


    /**
     * This is a utility method that compares two Booleans when one or
     * both of the objects might be <CODE>null</CODE>  The result of
     * this method is determined as follows:
     * <OL>
     * <LI>If <CODE>b1</CODE> and <CODE>b2</CODE> are both TRUE or
     * neither <CODE>b1</CODE> nor <CODE>b2</CODE> is TRUE,
     * return <CODE>false</CODE>.
     * <LI>Otherwise, return <CODE>true</CODE>.
     * </OL>
     * <p/>
     * This method produces the exact logically inverted result as the
     * {@link #areBooleansEqual(Boolean, Boolean)} method.<P>
     */
    public static final boolean areBooleansDifferent(Boolean b1, Boolean b2) {
        return !areBooleansEqual(b1, b2);
    }


    /**
     * Returns <CODE>true</CODE> if the specified array is not null
     * and contains a non-null element.  Returns <CODE>false</CODE>
     * if the array is null or if all the array elements are null.
     */
    public static final boolean hasNonNullElement(Object[] array) {
        if (array != null) {
            final int n = array.length;
            for (int i = 0; i < n; i++) {
                if (array[i] != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a single string that is the concatenation of all the
     * strings in the specified string array.  A single space is
     * put between each string array element.  Null array elements
     * are skipped.  If the array itself is null, the empty string
     * is returned.  This method is guaranteed to return a non-null
     * value, if no expections are thrown.
     */
    public static final String concat(String[] strs) {
        return concat(strs, " ");  //NOTRANS
    }

    /**
     * Returns a single string that is the concatenation of all the
     * strings in the specified string array.  The strings are separated
     * by the specified delimiter.  Null array elements are skipped.  If
     * the array itself is null, the empty string is returned.  This
     * method is guaranteed to return a non-null value, if no expections
     * are thrown.
     */
    public static final String concat(String[] strs, String delim) {
        if (strs != null) {
            final StringBuilder buf = new StringBuilder();
            final int n = strs.length;
            for (int i = 0; i < n; i++) {
                final String str = strs[i];
                if (str != null) {
                    buf.append(str).append(delim);
                }
            }
            final int length = buf.length();
            if (length > 0) {
                //  Trim trailing space.
                buf.setLength(length - 1);
            }
            return buf.toString();
        }
        else {
            return ""; // NOTRANS
        }
    }

    /**
     * Returns <CODE>true</CODE> if the specified {@link String} is not
     * <CODE>null</CODE> and has a length greater than zero.  This is
     * a very frequently occurring check.
     */
    public static final boolean hasLength(String s) {
        return (s != null && s.length() > 0);
    }


    /**
     * Returns <CODE>null</CODE> if the specified string is empty or
     * <CODE>null</CODE>.  Otherwise the string itself is returned.
     */
    public static final String nullifyIfEmpty(String s) {
        return ModelUtil.hasLength(s) ? s : null;
    }

    /**
     * Returns <CODE>null</CODE> if the specified object is null
     * or if its <CODE>toString()</CODE> representation is empty.
     * Otherwise, the <CODE>toString()</CODE> representation of the
     * object itself is returned.
     */
    public static final String nullifyingToString(Object o) {
        return o != null ? nullifyIfEmpty(o.toString()) : null;
    }

    /**
     * Determines if a string has been changed.
     *
     * @param oldString is the initial value of the String
     * @param newString is the new value of the String
     * @return true If both oldString and newString are null or if they are
     *         both not null and equal to each other.  Otherwise returns false.
     */
    public static boolean hasStringChanged(String oldString, String newString) {
        if (oldString == null && newString == null) {
            return false;
        }
        else if ((oldString == null && newString != null)
                || (oldString != null && newString == null)) {
            return true;
        }
        else {
            return !oldString.equals(newString);
        }
    }

    public static String getTimeFromLong(long diff) {
        final String HOURS = "h";
        final String MINUTES = "min";
        final String SECONDS = "sec";

        final long MS_IN_A_DAY = 1000 * 60 * 60 * 24;
        final long MS_IN_AN_HOUR = 1000 * 60 * 60;
        final long MS_IN_A_MINUTE = 1000 * 60;
        final long MS_IN_A_SECOND = 1000;
        new Date();
        diff = diff % MS_IN_A_DAY;
        long numHours = diff / MS_IN_AN_HOUR;
        diff = diff % MS_IN_AN_HOUR;
        long numMinutes = diff / MS_IN_A_MINUTE;
        diff = diff % MS_IN_A_MINUTE;
        long numSeconds = diff / MS_IN_A_SECOND;
        diff = diff % MS_IN_A_SECOND;
        @SuppressWarnings("unused")
		long numMilliseconds = diff;

        StringBuilder buf = new StringBuilder();
        if (numHours > 0) {
            buf.append(numHours + " " + HOURS + ", ");
        }

        if (numMinutes > 0) {
            buf.append(numMinutes + " " + MINUTES + ", ");
        }

        buf.append(numSeconds + " " + SECONDS);

        String result = buf.toString();
        return result;
    }


    /**
     * Build a List of all elements in an Iterator.
     */
    public static List iteratorAsList(Iterator i) {
        ArrayList list = new ArrayList(10);
        while (i.hasNext()) {
            list.add(i.next());
        }
        return list;
    }

    /**
     * Creates an Iterator that is the reverse of a ListIterator.
     */
    public static Iterator reverseListIterator(ListIterator i) {
        return new ReverseListIterator(i);
    }
}

/**
 * An Iterator that is the reverse of a ListIterator.
 */
class ReverseListIterator implements Iterator {
    private ListIterator _i;

    ReverseListIterator(ListIterator i) {
        _i = i;
        while (_i.hasNext())
            _i.next();
    }

    public boolean hasNext() {
        return _i.hasPrevious();
    }

    public Object next() {
        return _i.previous();
    }

    public void remove() {
        _i.remove();
    }
}











