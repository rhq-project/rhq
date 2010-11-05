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

        Set<EventDefinition> toDelete = CollectionsUtil.missingInFirstSet(newEventDefs, existingEventDefs);
        Set<EventDefinition> newOnes = CollectionsUtil.missingInFirstSet(existingEventDefs, newEventDefs);
        Set<EventDefinition> toUpdate = CollectionsUtil.intersection(newEventDefs, existingEventDefs);

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

}
