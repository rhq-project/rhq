/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.plugins.apache.augeas;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;

/**
 * This is a helper class to help us make sense of the augeas leaks.
 *
 * The byteman rules in the {@link AugeasReferenceLeakingTest} make use
 * of this class to record the locations that create instances of augeas
 * and match them with locations that close the instance.
 * <p>
 * We can then report on any locations that created an augeas instance
 * that was never closed.
 * 
 * @author Lukas Krejci
 */
public class CreateAndCloseTracker extends Helper {
    private static class EqualableWeakReference<T> extends WeakReference<T> {

        private final int hash;
        
        public EqualableWeakReference(T referent, ReferenceQueue<? super T> q) {
            super(referent, q);
            hash = referent.hashCode();
        }

        public EqualableWeakReference(T referent) {
            super(referent);
            hash = referent.hashCode();
        }

        @Override
        public int hashCode() {
            return hash;
        }
        
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            
            if (!(other instanceof EqualableWeakReference)) {
                return false;
            }
            
            Object ref = get();

            if (ref == null) {
                //the reference has been cleared. There is no telling
                //on what this reference pointed to, so it is not
                //possible to determine equality.
                return false;
            }
            
            EqualableWeakReference<?> o = (EqualableWeakReference<?>) other;
            
            Object oref = o.get();
            
            //reference equality is what we're after here...
            return ref == oref;
        }
    }

    private static class CreateAndCloseLocations {
        public String createLocation;
        public List<String> closeLocations = new ArrayList<String>();
    }
    
    private static final HashMap<EqualableWeakReference<Object>, CreateAndCloseLocations> REFERENCES = new HashMap<CreateAndCloseTracker.EqualableWeakReference<Object>, CreateAndCloseLocations>(); 

    /**
     * @param rule
     */
    public CreateAndCloseTracker(Rule rule) {
        super(rule);
    }

    
    private static synchronized void _recordCreate(Object object, String stackTrace) {
        EqualableWeakReference<Object> ref = new EqualableWeakReference<Object>(object);
        
        CreateAndCloseLocations locations = REFERENCES.get(ref);
        
        if (locations == null) {
            locations = new CreateAndCloseLocations();
            REFERENCES.put(ref, locations);
            
            locations.createLocation = stackTrace;
        } else {
            throw new IllegalStateException("Cannot record a create request on a single object twice. Object: " + object + ", Stacktrace: " + stackTrace);
        }
    }
    
    public void recordCreate(Object object, String stackTrace) {
        _recordCreate(object, stackTrace);
    }
    
    private static synchronized void _recordClose(Object object, String stackTrace) {
        EqualableWeakReference<Object> ref = new EqualableWeakReference<Object>(object);
        
        CreateAndCloseLocations locations = REFERENCES.get(ref);
        
        if (locations == null) {
            //weird, but actually track this so that we
            //can report about an object not being
            //recorded for create.
            locations = new CreateAndCloseLocations();
            REFERENCES.put(ref, locations);
        }
        
        locations.closeLocations.add(stackTrace);
    }
    
    public void recordClose(Object object, String stackTrace) {
        _recordClose(object, stackTrace);
    }
    
    public static synchronized void clear() {
        REFERENCES.clear();
    }
    
    public static synchronized List<String> getCreateLocationsWithoutClose() {
        ArrayList<String> ret = new ArrayList<String>();
        
        for(CreateAndCloseLocations l : REFERENCES.values()) {
            if (l.closeLocations.isEmpty()) {
                ret.add(l.createLocation);
            }
        }
        
        return ret;
    }
    
    public static synchronized Map<String, List<String>> getMultiplyClosingLocations() {
        HashMap<String, List<String>> ret = new HashMap<String, List<String>>();
        
        for(CreateAndCloseLocations l : REFERENCES.values()) {
            if (l.closeLocations.size() > 1) {
                ret.put(l.createLocation, l.closeLocations);
            }
        }
        
        return ret;
    }
}
