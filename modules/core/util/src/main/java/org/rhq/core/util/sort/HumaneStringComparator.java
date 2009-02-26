/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.util.sort;


import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * <p>A comparator for Strings that imposes a more "human" order on Strings.
 * Modeled on the way the Mac OS X Finder orders file names, it breaks the
 * Strings into segments of non-numbers and numbers (e.g. Foo123Bar into 'Foo',
 * 123, 'Bar') and compares the segments from each String one at a time.</p>
 *
 * <p>The following rules are applied when comparing segments:</p>
 * <ul>
 *   <li>A null segment sorts before a non-null segment (e.g. Foo < Foo123)</li>
 *   <li>String segments are compared case-insensitively </li>
 *   <li>Number segments are converted to java.lang.Doubles and compared</li>
 * </ul>
 *
 * <p>If two Strings are identical after analyzing them in a segment-at-time
 * manner then lhs.compareTo(rhs) will be invoked to ensure that the result
 * provided by the comparator is reflexive (i.e. compare(x,y) = -compare(y,x))
 * and agrees with String.equals() in all cases.</p>
 *
 * <p>As an example, the following list of Strings is presented in the order in
 * which the comparator will sort them:</p>
 *
 * <ul>
 *   <li>Foo</li>
 *   <li>Foo1Bar2</li>
 *   <li>Foo77.7Boo</li>
 *   <li>Foo123</li>
 *   <li>Foo777Boo</li>
 *   <li>Foo1234</li>
 *   <li>FooBar</li>
 *   <li>Fooey123</li>
 *   <li>Splat</li>
 * </ul>
 *
 * @author Tim Fennell (oss@tfenne.com)
 */
public class HumaneStringComparator implements Comparator<String> {
    /**
     * The Regular Expression used to match out segments of each String. Will
     * incrementally match a number segment defined as one or more digits
     * followed optionally by a decimal point and more digits, or a non-number
     * segment consisting of any non-digit characters.
     */
    Pattern SEGMENT_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?|\\D+)");

    /**
     * A default instance of the comparator that can be used without instantiating
     * a new copy every time one is needed.
     */
    public static final HumaneStringComparator DEFAULT = new HumaneStringComparator();

    /**
     * The implementation of the Comparable interface method that compares two
     * Strings. Implements the algorithm described in the class level javadoc.
     *
     * @param lhs the first of two Strings to compare
     * @param rhs the second of two Strings to compare
     * @return int -1, 0 or 1 respectively if the first String (lhs) sorts before
     *         equally or after the second String
     */
    public int compare(String lhs, String rhs) {
        // Take care of nulls first
        if (lhs == null && rhs == null) return 0;
        else if (lhs == null) return -1;
        else if (rhs == null) return 1;

        Matcher lhsMatcher = SEGMENT_PATTERN.matcher(lhs);
        Matcher rhsMatcher = SEGMENT_PATTERN.matcher(rhs);

        int result = 0;
        while (result == 0) {
            boolean lhsFound = lhsMatcher.find();
            boolean rhsFound = rhsMatcher.find();

            if (!lhsFound && !rhsFound) {
                // Both Strings ran out and they matched so far! Return a full compareTo
                // of the same Strings so that we don't violate equality checks
                return lhs.compareTo(rhs);
            }
            else if (!lhsFound) result = -1;
            else if (!rhsFound) result = 1;
            else {
                String lhsSegment = lhsMatcher.group();
                String rhsSegment = rhsMatcher.group();

                if (Character.isDigit(lhsSegment.toCharArray()[0]) && Character.isDigit(rhsSegment.toCharArray()[0])) {
                    result = compareNumberSegments(lhsSegment, rhsSegment);
                }
                else {
                    result = compareStringSegments(lhsSegment, rhsSegment);
                }
            }
        }
        return result;
    }

    /**
     * Converts the two Strings to doubles and then compares then numerically
     * by invoking Double.compareTo()
     */
    protected int compareNumberSegments(String lhs, String rhs) {
        return new Double(lhs).compareTo(new Double(rhs));
    }

    /**
     * Compares the left hand String to the right hand String case-insensitively
     * by invoking lhs.compareToIgnoreCase(rhs).
     */
    protected int compareStringSegments(String lhs, String rhs) {
        return lhs.compareToIgnoreCase(rhs);
    }
}