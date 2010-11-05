package org.rhq.enterprise.server.resource.metadata;

import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.event.EventManagerLocal;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Stateless
public class EventMetadataManagerBean implements EventMetdataManagerLocal {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityMgr;

    @EJB
    private EventManagerLocal eventMgr;

    @Override
    public void updateMetadata(ResourceType existingType, ResourceType newType) {
        Set<EventDefinition> newEventDefs = newType.getEventDefinitions();
        // Loop over the newEventDefs and set the resourceTypeId, so equals() will work
        for (EventDefinition def : newEventDefs) {
            def.setResourceTypeId(existingType.getId());
        }

        Set<EventDefinition> existingEventDefs = existingType.getEventDefinitions();
        for (EventDefinition def : existingEventDefs) {
            entityMgr.refresh(def);
        }

        Set<EventDefinition> toDelete = missingInFirstSet(newEventDefs, existingEventDefs);
        Set<EventDefinition> newOnes = missingInFirstSet(existingEventDefs, newEventDefs);
        Set<EventDefinition> toUpdate = intersection(newEventDefs, existingEventDefs);

        // update existing ones
        for (EventDefinition eDef : existingEventDefs) {
            for (EventDefinition nDef : toUpdate) {
                if (eDef.equals(nDef)) {
                    eDef.setDescription(nDef.getDescription());
                    eDef.setDisplayName(nDef.getDisplayName());
                }
            }
        }

        // Persist new definitions
        for (EventDefinition eDef : newOnes) {
            EventDefinition e2 = new EventDefinition(existingType, eDef.getName());
            e2.setDescription(eDef.getDescription());
            e2.setDisplayName(eDef.getDisplayName());
            entityMgr.persist(e2);
            existingType.addEventDefinition(e2);
        }

        // and finally remove deleted ones. First flush the EM to be on the save side
        // for a bulk delete.
        existingEventDefs.removeAll(toDelete);
        entityMgr.flush();
        for (EventDefinition eDef : toDelete) {
            // remove EventSources and events on it.
            eventMgr.deleteEventSourcesForDefinition(eDef);
            entityMgr.remove(eDef);
        }
    }

    /**
     * Return a set containing those element that are in reference, but not in first. Both input sets are not modified
     *
     * @param  <T>
     * @param  first
     * @param  reference
     *
     * @return
     */
    private <T> Set<T> missingInFirstSet(Set<T> first, Set<T> reference) {
        Set<T> result = new HashSet<T>();

        if (reference != null) {
            // First collection is null -> everything is missing
            if (first == null) {
                result.addAll(reference);
                return result;
            }

            // else loop over the set and sort out the right items.
            for (T item : reference) {
                //                if (!first.contains(item)) {
                //                    result.add(item);
                //                }
                boolean found = false;
                Iterator<T> iter = first.iterator();
                while (iter.hasNext()) {
                    T f = iter.next();
                    if (f.equals(item)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    result.add(item);
            }
        }

        return result;
//        return new HashSet<T>(CollectionUtils.retainAll(first, reference));
    }

    /**
     * Return a new Set with elements that are in the first and second passed collection.
     * If one set is null, an empty Set will be returned.
     * @param  <T>    Type of set
     * @param  first  First set
     * @param  second Second set
     *
     * @return a new set (depending on input type) with elements in first and second
     */
    private <T> Set<T> intersection(Set<T> first, Set<T> second) {
        Set<T> result = new HashSet<T>();
        if ((first != null) && (second != null)) {
            result.addAll(first);
            //            result.retainAll(second);
            Iterator<T> iter = result.iterator();
            boolean found;
            while (iter.hasNext()) {
                T item = iter.next();
                found = false;
                for (T s : second) {
                    if (s.equals(item))
                        found = true;
                }
                if (!found)
                    iter.remove();
            }
        }

        return result;
//        return new HashSet<T>(CollectionUtils.intersection(first, second));
    }
}
