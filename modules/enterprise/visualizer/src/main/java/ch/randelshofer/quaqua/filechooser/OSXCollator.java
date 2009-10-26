/*
 * @(#)OSXCollator.java  1.1  2007-02-10
 *
 * Copyright (c) 2004-2007 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */

package ch.randelshofer.quaqua.filechooser;

import java.util.*;
import java.text.*;

/**
 * The OSXCollator strives to match the collation rules used by the Mac OS X
 * Finder and of Mac OS X file dialogs.
 * <p>
 * If we wanted to match the OS X collation rules exactly, we would have to
 * implement the rules for all languages supported by Mac OS X and Java.
 * To reduce the amount of work needed for implementing these rules, the
 * OSXCollator changes the collation rules returned by
 * java.text.Collator.getInstance() to do the following:
 * <ul>
 * <li>Space characters are treated as primary collation differences</li>
 * <li>Hyphen characters are treated as primary collation differences</li>
 * <li>Sequence of digits (characters '0' through '9') is treated as a single
 * collation object. The current implementation supports sequences of up to
 * 999 characters length.</li>
 * </ul>
 * If java.text.Collator.getInstance() does not return an instance of
 * java.text.RuleBasedCollator, then the returned collator is used, and only
 * sequences of digits are changed to match the collation rules of Mac OS X.
 *
 *
 * @author  Werner Randelshofer
 * @version 1.1 2007-02-10 Improved performance of method expandNumbers.
 * <br>1.0.1 2005-02-09 Changed the code to make it compilable with
 * Java 1.3.
 * <br>1.0  23 January 2005  Created.
 */
public class OSXCollator extends Collator {
    private Collator collator;

    /** Creates a new instance. */
    public OSXCollator() {
        this(Locale.getDefault());
    }

    public OSXCollator(Locale locale) {
            collator = Collator.getInstance(locale);

            if (collator instanceof RuleBasedCollator) {
                String rules = ((RuleBasedCollator) collator).getRules();

                // If hyphen is ignored except for tertiary difference, make it
                // a primary difference, and move in front of the first primary
                // difference found in the rules
                int pos = rules.indexOf(",'-'");
                int primaryRelationPos = rules.indexOf('<');
                if (primaryRelationPos == rules.indexOf("'<'")) {
                    primaryRelationPos = rules.indexOf('<', primaryRelationPos + 2);
                }
                if (pos != -1 && pos < primaryRelationPos) {
                    rules = rules.substring(0, pos)
                    + rules.substring(pos + 4, primaryRelationPos)
                    + "<'-'"
                    + rules.substring(primaryRelationPos);
                }

                // If space is ignored except for secondary and tertiary
                // difference, make it a primary difference, and move in front
                // of the first primary difference found in the rules
                pos = rules.indexOf(";' '");
                primaryRelationPos = rules.indexOf('<');
                if (primaryRelationPos == rules.indexOf("'<'")) {
                    primaryRelationPos = rules.indexOf('<', primaryRelationPos + 2);
                }
                if (pos != -1 && pos < primaryRelationPos) {
                    rules = rules.substring(0, pos)
                    + rules.substring(pos + 4, primaryRelationPos)
                    + "<' '"
                    + rules.substring(primaryRelationPos);
                }

                try {
                collator = new RuleBasedCollator(rules);
                } catch (ParseException e) {
                    e.printStackTrace();
                    }
            }
    }

    public int compare(String source, String target) {
        return collator.compare(expandNumbers(source), expandNumbers(target));
    }

    public CollationKey getCollationKey(String source) {
        return collator.getCollationKey(expandNumbers(source));
    }

    public boolean equals(Object o) {
        if (o instanceof OSXCollator) {
            OSXCollator that = (OSXCollator) o;
            return this.collator.equals(that.collator);
        } else {
            return false;
        }
    }
    public int hashCode() {
        return collator.hashCode();
    }

    private String expandNumbers(String s) {
        if (s == null) return null;

        // FIXME - Use StringBuilder here when we abandon support for J2SE 1.4.
        StringBuffer out = new StringBuffer();
        StringBuffer digits = new StringBuffer();

        for (int i=0, n = s.length(); i < n; i++) {
            char ch = s.charAt(i);
            //if (Character.isDigit(ch)) {
            if (ch >= '0' && ch <= '9') {
                digits.append(ch);
            } else {
                if (digits.length() != 0) {
                    if (digits.length() < 10) {
                        out.append("00");
                        out.append(digits.length());
                    } else if (digits.length() < 100) {
                        out.append("0");
                        out.append(digits.length());
                    } else if (digits.length() < 1000) {
                        out.append(digits.length());
                    } else if (digits.length() > 999) {
                        out.append("999");
                    }
                    out.append(digits.toString());
                    digits.delete(0, digits.length());
                }
                out.append(ch);
            }
        }
        if (digits.length() != 0) {
            if (digits.length() < 10) {
                out.append("00");
                out.append(digits.length());
            } else if (digits.length() < 100) {
                out.append("0");
                out.append(digits.length());
            } else if (digits.length() < 1000) {
                out.append(digits.length());
            } else if (digits.length() > 999) {
                out.append("999");
            }
            out.append(digits);
        }

        return out.toString();
    }
}
