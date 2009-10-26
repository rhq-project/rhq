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

package ch.randelshofer.tree.rectmap;

import ch.randelshofer.tree.circlemap.*;
import java.util.Comparator;

/**
 * CircleRadiusComparator.
 *
 * @author Werner Randelshofer
 * @version 1.0 Jan 17, 2008 Created.
 */
public class RectmapCumulatedSizeComparator implements Comparator<RectmapNode> {
    private static RectmapCumulatedSizeComparator ascendingInstance;
    private static RectmapCumulatedSizeComparator descendingInstance;
    private int asc = 1;

    public static RectmapCumulatedSizeComparator getAscendingInstance() {
        if (ascendingInstance == null) {
            ascendingInstance = new RectmapCumulatedSizeComparator();
        }
        return ascendingInstance;
    }
    public static RectmapCumulatedSizeComparator getDescendingInstance() {
        if (descendingInstance == null) {
            descendingInstance = new RectmapCumulatedSizeComparator();
            descendingInstance.asc = -1;
        }
        return descendingInstance;
    }




    public int compare(RectmapNode c1, RectmapNode c2) {
        double cmp = c1.getCumulatedWeight() - c2.getCumulatedWeight();
        return (cmp < 0) ? -asc : ((cmp > 0) ? asc : 0);
    }
}
