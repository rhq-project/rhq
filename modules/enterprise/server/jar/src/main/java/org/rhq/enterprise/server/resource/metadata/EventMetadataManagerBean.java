package org.rhq.enterprise.server.resource.metadata;

import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.event.EventManagerLocal;

@Stateless
public class EventMetadataManagerBean implements EventMetdataManagerLocal {

    private static Log log = LogFactory.getLog(EventMetadataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityMgr;

    @EJB
    private EventManagerLocal eventMgr;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateMetadata(ResourceType existingType, ResourceType newType) {
        log.debug("Updating event definitions for " + existingType);

        existingType = entityMgr.find(ResourceType.class, existingType.getId());

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

        if (log.isDebugEnabled()) {
            log.debug("Event definitions to be added: " + newOnes);
            log.debug("Event defininitions to be updated: " + toUpdate);
            log.debug("Event definitions to be removed: " + toDelete);
        }

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
