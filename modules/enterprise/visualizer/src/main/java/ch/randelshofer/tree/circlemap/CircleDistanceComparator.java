/**
 * @(#)CircleDistanceComparator.java  1.0  Jan 17, 2008
 *
 * Copyright (c) 2008 by Werner Randelshofer, Staldenmattweg 2, CH-6405 Immensee.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer ("Confidential Information"). You shall not disclose
 * such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with
 * Werner Randelshofer.
 */
package ch.randelshofer.tree.circlemap;

import java.util.Comparator;

/**
 * Compares two circles by their distance to the origin (0,0) of the coordinate
 * system.
 *
 * @author Werner Randelshofer
 * @version 1.0 Jan 17, 2008 Created.
 */
public class CircleDistanceComparator implements Comparator<Circle> {

    private double cx,  cy;

    public CircleDistanceComparator(double cx, double cy) {
        this.cx = cx;
        this.cy = cy;
    }

    public int compare(Circle c1, Circle c2) {
        double qdist1 =
                (cx - c1.cx) * (cx - c1.cx) +
                (cy - c1.cy) * (cy - c1.cy);
        double qdist2 =
                (cx - c2.cx) * (cx - c2.cx) +
                (cy - c2.cy) * (cy - c2.cy);
        double cmp = qdist1 - qdist2;
        return (cmp < 0) ? -1 : ((cmp > 0) ? 1 : 0);
    }
}
