/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
 * All rights reserved.
 *
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

package org.rhq.core.domain.util;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.testng.annotations.Test;

/**
 * @author Lukas Krejci
 * @since 4.9
 */
@Test
public class PageControlTest {

    private static class FakeCollection extends AbstractCollection<Object> {
        private int size;

        public static FakeCollection ofSize(int size) {
            return new FakeCollection(size);
        }

        public FakeCollection(int size) {
            this.size = size;
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {
                int count;
                @Override
                public boolean hasNext() {
                    return count < size;
                }

                @Override
                public Object next() {
                    count++;
                    return null;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public int size() {
            return size;
        }
    }

    public void consistencyOfUnlimitedPageControl() {
        int[] consistentSizes = { 5 };
        int[] inconsistentSizes = {0, 3, 6, 1000};

        assertConsistencies(PageControl.getUnlimitedInstance(), FakeCollection.ofSize(5), consistentSizes, inconsistentSizes);
    }

    public void consistencyOfTooLargeCollection() {
        PageControl pc = new PageControl(0, 2);

        Collection<?> collection = FakeCollection.ofSize(5);

        int[] consistentSizes = {};
        int[] inconsistentSizes = {0, 1, 2, 3, 5, 7, 10}; //any size, really

        assertConsistencies(pc, collection, consistentSizes, inconsistentSizes);

        pc.setPageNumber(1000); // this doesn't really matter, because the collection should never be larger than the
                                // page size
        assertConsistencies(pc, collection, consistentSizes, inconsistentSizes);
    }

    public void consistencyOfFirstPage() {
        PageControl pc = new PageControl(0, 5);

        // the collection size should never be more than the total size
        Collection<?> collection = FakeCollection.ofSize(2);
        int[] consistentTotalSizes = { 2 };
        int[] inconsistentTotalSizes = {0, 1, 3, 4, 15};
        assertConsistencies(pc, collection, consistentTotalSizes, inconsistentTotalSizes);

        // the collection size should be at most the page size
        collection = FakeCollection.ofSize(5);
        consistentTotalSizes = new int[] {5, 6, 10, 1000};
        inconsistentTotalSizes = new int[] {0, 1, 2, 3, 4};
        assertConsistencies(pc, collection, consistentTotalSizes, inconsistentTotalSizes);
    }

    public void consistencyOfMiddlePage() {
        PageControl pc = new PageControl(1, 5);

        // the collection size in the middle of the total number of results can only be the size of the page
        Collection<?> collection = FakeCollection.ofSize(5);
        int[] consistentTotalSizes = {10, 11, 12, 15, 20}; // the total size must be at least 2 pages - the preceding
                                                           // pages + the number of results on the current page -
                                                           // we're in the "middle" of results in this test, so that
                                                           // that number is the full page size.
        int[] inconsistentTotalSizes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        assertConsistencies(pc, collection, consistentTotalSizes, inconsistentTotalSizes);
    }

    public void consistencyOfLastPage() {
        PageControl pc = new PageControl(1, 5);

        //we're testing the "last" page here, so the total has to be less than 10, because we're on page 1 (of 0 and 1).

        //the "full" 2 pages of results
        Collection<?> collection = FakeCollection.ofSize(5);
        int[] consistentTotalSizes = { 10 };
        int[] inconsistentTotalSizes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        assertConsistencies(pc, collection, consistentTotalSizes, inconsistentTotalSizes);

        // now let's test that the totalSize must correspond to the size of the collection if it is less than the full
        // page size
        collection = FakeCollection.ofSize(3);
        consistentTotalSizes = new int[] { 8 }; // second page, 3 results on it
        inconsistentTotalSizes = new int[] {0, 3, 5, 7, 9, 10}; // 11 would be OK again, because that would mean we're
                                                                // not on the last page
        assertConsistencies(pc, collection, consistentTotalSizes, inconsistentTotalSizes);
    }

    public void consistencyOfPagePastTheNumberOfResults() {
        PageControl pc = new PageControl(1,2);

        Collection<?> collection = Collections.emptyList();

        int[] consistentSizes = {0, 1, 2}; // page 1 of pages of 2 elements mean that the total size is at least 3.
                                           // if it is less than that, we're just past the number of results and empty
                                           // collection is therefore deemed consistent.
        int[] inconsistentSizes = {3, 4, 5, 10, 15}; // any size that would suggest that the collection should contain
                                                     // some elements

        assertConsistencies(pc, collection, consistentSizes, inconsistentSizes);
    }

    private void assertConsistencies(PageControl pc, Collection<?> collection, int[] consistentSizes, int[] inconsistentSizes) {
        for(int i = 0; i < consistentSizes.length; ++i) {
            assertTrue(pc.isConsistentWith(collection, consistentSizes[i]),
                "Collection " + collection + " and totalSize " + consistentSizes[i] + " inconsistent with " + pc +
                    " even though it should have been.");
        }

        for(int i = 0; i < inconsistentSizes.length; ++i) {
            assertFalse(pc.isConsistentWith(collection, inconsistentSizes[i]),
                "Collection " + collection + " and totalSize " + inconsistentSizes[i] + " consistent with " + pc +
                    " even though it shouldn't have been.");
        }
    }
}
