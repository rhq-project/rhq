/**
 * @(#)CircleRadiusComparator.java  1.0  Jan 17, 2008
 *
 * Copyright (c) 2008 Werner Randelshofer
 * Staldenmattweg 2, CH-6405 Immensee, Switzerland
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */

package ch.randelshofer.tree.circlemap;

import java.util.Comparator;

/**
 * Compares two circles by the size of their radius.
 *
 * @author Werner Randelshofer
 * @version 1.0 Jan 17, 2008 Created.
 */
public class CircleRadiusComparator implements Comparator<Circle> {
    private static CircleRadiusComparator ascendingInstance;
    private static CircleRadiusComparator descendingInstance;
    private int asc = 1;

    public static CircleRadiusComparator getAscendingInstance() {
        if (ascendingInstance == null) {
            ascendingInstance = new CircleRadiusComparator();
        }
        return ascendingInstance;
    }
    public static CircleRadiusComparator getDescendingInstance() {
        if (descendingInstance == null) {
            descendingInstance = new CircleRadiusComparator();
            descendingInstance.asc = -1;
        }
        return descendingInstance;
    }




    public int compare(Circle c1, Circle c2) {
        double cmp = c1.radius - c2.radius;
        return (cmp < 0) ? -asc : ((cmp > 0) ? asc : 0);
    }
}
