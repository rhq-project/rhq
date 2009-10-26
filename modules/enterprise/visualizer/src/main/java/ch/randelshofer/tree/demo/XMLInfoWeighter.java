/*
 * @(#)InfoWeighter.java  1.0  23. Juni 2008
 *
 * Copyright (c) 2007 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */
package ch.randelshofer.tree.demo;

import ch.randelshofer.tree.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * InfoWeighter.
 *
 * @author  Werner Randelshofer
 * @version 1.0 23. Juni 2008 Created.
 */
public class XMLInfoWeighter implements Weighter {

    private XMLNodeInfo info;
    private String key;
    private int[] histogram;
    private Object min;
    private Object max;
    private Object median;
    private static DateFormat isoDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static DateFormat isoDateFormatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** Creates a new instance. */
    public XMLInfoWeighter(XMLNodeInfo info, String key) {
        this.info = info;
        this.key = key;
    }

    public void init(TreeNode root) {
        if (info.getType(key) == XMLNodeInfo.AttributeType.DATE) {
            Date minDate = new Date(Long.MAX_VALUE);
            Date maxDate = new Date(Long.MIN_VALUE);
            Set<String> stringValues = info.getValues(key);



            ArrayList<Date> dates = new ArrayList<Date>();
            collectDatesRecursive((XMLNode) root, dates);
            Collections.sort(dates);
            if (dates.size() > 0) {
                minDate = dates.get(0);
                maxDate = dates.get(dates.size() - 1);
                median = dates.get(dates.size() / 2);
                min = minDate;
                max = maxDate;
            }

            if (!maxDate.equals(minDate)) {
                histogram = new int[256];
                calculateDateHistogramRecursive(root);
            } else {
                histogram = new int[1];
                histogram[0] = 1;
            }
        } else {
            histogram = new int[1];
            histogram[0] = 1;
        }
    }

    private void collectDatesRecursive(XMLNode node, List<Date> dates) {
        String str = node.getAttribute(key);
        if (str != null) {
        try {
            Date value;
            try {
                value = (Date) isoDateFormatter.parse(str);
            } catch (ParseException ex) {
                value = (Date) isoDateFormatter2.parse(str);
            }
            dates.add(value);
        } catch (ParseException ex) {
        // skip unparsable dates
        }
        }
        for (TreeNode child : node.children()) {
            collectDatesRecursive((XMLNode) child, dates);
        }
    }

    /**
     * Calculates the date histogram recursively.
     *
     * @param root
     */
    private void calculateDateHistogramRecursive(TreeNode root) {
        XMLNode node = (XMLNode) root;
        String str = node.getAttribute(key);
        if (str != null) {
            try {
                Date value;
                try {
                    value = (Date) isoDateFormatter.parse(str);
                } catch (ParseException ex) {
                    value = (Date) isoDateFormatter2.parse(str);
                }

                int index = Math.min(histogram.length - 1, Math.max(0, (int) ((value.getTime() - ((Date) min).getTime()) * (histogram.length - 1) / (double) (((Date) max).getTime() - ((Date) min).getTime()))));
                histogram[index]++;
            } catch (NumberFormatException ex) {
            // skip unparsable dates
            } catch (ParseException ex) {
            // skip unparsable dates
            }
        }
        for (TreeNode child : root.children()) {
            calculateDateHistogramRecursive(child);
        }
    }

    public float getWeight(TreePath path) {
        XMLNode node = (XMLNode) path.getLastPathComponent();
        String str = node.getAttribute(key);
        if (str != null) {
            try {
                Date value;
                try {
                    value = (Date) isoDateFormatter.parse(str);

                } catch (ParseException ex) {
                    value = (Date) isoDateFormatter2.parse(str);
                }
                return (value.getTime() - ((Date) min).getTime()) / (float) (((Date) max).getTime() - ((Date) min).getTime());
            } catch (ParseException ex) {
            } catch (NumberFormatException ex) {
            } catch (ArrayIndexOutOfBoundsException ex) {
            }
        }
        return 0f;
    }

    public int[] getHistogram() {
        return histogram;
    }

    public String getMinimumWeightLabel() {
        return min.toString();
    }

    public String getMaximumWeightLabel() {
        return max.toString();
    }

    public float getMedianWeight() {
        Date value = (Date) median;
        return (value.getTime() - ((Date) min).getTime()) / (float) (((Date) max).getTime() - ((Date) min).getTime());
    }
}
