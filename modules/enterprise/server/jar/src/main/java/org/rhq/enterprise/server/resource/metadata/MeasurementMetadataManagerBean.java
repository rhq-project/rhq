package org.rhq.enterprise.server.resource.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;

@Stateless
public class MeasurementMetadataManagerBean implements MeasurementMetadataManagerLocal {

    private final Log log = LogFactory.getLog(MeasurementMetadataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityMgr;

    @EJB
    private MeasurementScheduleManagerLocal scheduleMgr;

    @EJB
    private MeasurementDefinitionManagerLocal measurementDefinitionMgr;

    @EJB
    private SubjectManagerLocal subjectMgr;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateMetadata(ResourceType existingType, ResourceType newType) {
        log.debug("Updating metric definitions for " + existingType);

        existingType = entityMgr.find(ResourceType.class, existingType.getId());
        Set<MeasurementDefinition> existingDefinitions = existingType.getMetricDefinitions();

        // if necessary insert the mandatory AvailabilityType metric
        Set<MeasurementDefinition> newTypeMetricDefinitions = getMetricDefinitions(newType);

        if (existingDefinitions.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(existingType + " currently does not define any metric definitions.");
                log.debug("New metric definitions to be added: " + newType.getMetricDefinitions());
            }
            // They're all new.
            for (MeasurementDefinition newDefinition : newType.getMetricDefinitions()) {
                if (newDefinition.getDefaultInterval() < MeasurementSchedule.MINIMUM_INTERVAL) {
                    newDefinition.setDefaultInterval(MeasurementSchedule.MINIMUM_INTERVAL);
                    log.info("Definition [" + newDefinition
                        + "] has too short of a default interval, setting to minimum");
                }
                existingType.addMetricDefinition(newDefinition);
                entityMgr.persist(newDefinition);

                // Now create schedules for already existing resources
                scheduleMgr.createSchedulesForExistingResources(existingType, newDefinition);
            }
        } else {
            // Update existing or add new metrics
            for (MeasurementDefinition newDefinition : newType.getMetricDefinitions()) {
                boolean found = false;
                for (MeasurementDefinition existingDefinition : existingDefinitions) {
                    if (existingDefinition.getName().equals(newDefinition.getName())
                        && (existingDefinition.isPerMinute() == newDefinition.isPerMinute())) {
                        found = true;

                        if (log.isDebugEnabled()) {
                            log.debug("Updating existing metric definition: " + existingDefinition);
                        }

                        // We normally protect the user's interval settings. But the Availability metric is
                        // a bit special. It's built-in and we know the resource category default values.  If the 
                        // existing setting is the default, we'll allow it to be changed by the plugin. It's possible 
                        // the user wants it the old way, and can set it back, but given that avail collection is
                        // critical to agent perf, we'll assume plugin knows best in this case. This only happens
                        // if the interval is at the default, a non-default value set by an earlier rev of the
                        // plugin will not be updated.  
                        boolean isAvail = MeasurementDefinition.AVAILABILITY_NAME.equals(newDefinition.getName());
                        long defaultInterval = (ResourceCategory.SERVER == existingDefinition.getResourceType()
                            .getCategory()) ? MeasurementDefinition.AVAILABILITY_DEFAULT_PERIOD_SERVER
                            : MeasurementDefinition.AVAILABILITY_DEFAULT_PERIOD_SERVICE;
                        boolean updateInterval = (isAvail && (defaultInterval == existingDefinition
                            .getDefaultInterval()));
                        existingDefinition.update(newDefinition, updateInterval);

                        // we normally do not want to touch interval in case a user changed it,
                        // but we cannot allow too-short of an interval, so override it if necessary
                        if (existingDefinition.getDefaultInterval() < MeasurementSchedule.MINIMUM_INTERVAL) {
                            existingDefinition.setDefaultInterval(MeasurementSchedule.MINIMUM_INTERVAL);
                            log.info("Definition [" + existingDefinition
                                + "] has too short of a default interval, setting to minimum");
                        }

                        entityMgr.merge(existingDefinition);

                        // There is nothing in the schedules that need to be updated.
                        // We do not want to change schedules (such as collection interval)
                        // because the user might have customized them. So leave them be.

                        break;
                    }
                }

                if (!found) {
                    // It's new - create it
                    log.info("Metadata update: Adding new " + newDefinition.getDataType().name().toLowerCase() + " definition ["
                            + newDefinition.getDisplayName() + "] to type " + existingType + "...");
                    existingType.addMetricDefinition(newDefinition);
                    entityMgr.persist(newDefinition);

                    // Now create schedules for already existing resources
                    scheduleMgr.createSchedulesForExistingResources(existingType, newDefinition);
                }
            }

            /*
            * Now delete outdated measurement definitions. First find them ...
            */
            List<MeasurementDefinition> definitionsToDelete = new ArrayList<MeasurementDefinition>();
            for (MeasurementDefinition existingDefinition : existingDefinitions) {
                if (!newType.getMetricDefinitions().contains(existingDefinition)) {
                    definitionsToDelete.add(existingDefinition);
                }
            }
            // ... and remove them
            existingDefinitions.removeAll(definitionsToDelete);
            for (MeasurementDefinition definitionToDelete : definitionsToDelete) {
                log.info("Metadata update: Removing " + definitionToDelete.getDataType().name().toLowerCase() + " definition ["
                        + definitionToDelete.getDisplayName() + "] from type " + existingType + "...");
                measurementDefinitionMgr.removeMeasurementDefinition(definitionToDelete);
            }

            entityMgr.flush();
        }
        // TODO what if they are null? --> delete everything from existingType
        // not needed see JBNADM-1639
    }

    public static Set<MeasurementDefinition> getMetricDefinitions(ResourceType newType) {
        Set<MeasurementDefinition> result = newType.getMetricDefinitions();
        result = (null == result) ? new HashSet<MeasurementDefinition>(1) : result;
        long period;

        switch (newType.getCategory()) {
        case PLATFORM:
            return result;
        case SERVER:
            period = MeasurementDefinition.AVAILABILITY_DEFAULT_PERIOD_SERVER;
            break;
        default:
            period = MeasurementDefinition.AVAILABILITY_DEFAULT_PERIOD_SERVICE;
        }

        MeasurementDefinition rhqAvailability = new MeasurementDefinition(newType,
            MeasurementDefinition.AVAILABILITY_NAME);
        rhqAvailability.setDefaultInterval(period);
        rhqAvailability.setDefaultOn(true);
        rhqAvailability.setCategory(MeasurementCategory.AVAILABILITY);
        rhqAvailability.setDisplayName(MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);
        rhqAvailability.setDescription(MeasurementDefinition.AVAILABILITY_DESCRIPTION);
        rhqAvailability.setDataType(DataType.AVAILABILITY);
        rhqAvailability.setUnits(MeasurementUnits.NONE); // n/a protects against non-null
        rhqAvailability.setNumericType(NumericType.DYNAMIC); // n/a protects against non-null
        rhqAvailability.setDisplayType(DisplayType.DETAIL); // n/a protects against non-null

        // Add the built in metric if it is not defined. Otherwise, override only allowed fields
        if (!result.contains(rhqAvailability)) {
            result.add(rhqAvailability);
        } else {
            MeasurementDefinition override = null;
            for (MeasurementDefinition aResult : result) {
                override = aResult;
                if (override.equals(rhqAvailability)) {
                    break;
                }
            }

            // don't let the override muck with fixed field values, only defaultOn and defaultInterval
            override.setCategory(MeasurementCategory.AVAILABILITY);
            override.setDisplayName(MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);
            override.setDescription(MeasurementDefinition.AVAILABILITY_DESCRIPTION);
            override.setDataType(DataType.AVAILABILITY);
            override.setUnits(MeasurementUnits.NONE); // n/a protects against non-null
            override.setNumericType(NumericType.DYNAMIC); // n/a protects against non-null
            override.setDisplayType(DisplayType.DETAIL); // n/a protects against non-null                        
        }

        return result;
    }

    @Override
    public void deleteMetadata(ResourceType existingType) {
        log.debug("Deleting metric definitions for " + existingType);

        MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
        criteria.addFilterResourceTypeId(existingType.getId());
        criteria.clearPaging();//disable paging as the code assumes all the results will be returned.

        List<MeasurementDefinition> definitions = measurementDefinitionMgr.findMeasurementDefinitionsByCriteria(
            subjectMgr.getOverlord(), criteria);

        // Remove the type's metric definitions. We do this separately, rather than just relying on cascade
        // upon deletion of the ResourceType, because the removeMeasurementDefinition() will also take care
        // of removing any associated schedules and those schedules' OOBs.
        if (definitions != null) {
            Iterator<MeasurementDefinition> defIter = definitions.iterator();
            while (defIter.hasNext()) {
                MeasurementDefinition def = defIter.next();
                measurementDefinitionMgr.removeMeasurementDefinition(def);
                defIter.remove();
            }
        }
    }
}
