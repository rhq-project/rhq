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
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;

@Stateless
public class MeasurementMetadataManagerBean implements MeasurementMetadataManagerLocal {

    private final Log log = LogFactory.getLog(MeasurementMetadataManagerBean.class);

    private static final Long AVAILABILITY_DEFAULT_PERIOD_SERVER = 60L * 1000; // 1 minute in ms
    private static final Long AVAILABILITY_DEFAULT_PERIOD_SERVICE = 60L * 1000 * 5; // 5 minutes in ms

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

        // if necessary insert the mandatory AvailabilityScanPeriod metric
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

                        existingDefinition.update(newDefinition, false);

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
                    // Its new, create it
                    if (log.isDebugEnabled()) {
                        log.debug("Adding metric definition: " + newDefinition);
                    }
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
                measurementDefinitionMgr.removeMeasurementDefinition(definitionToDelete);
            }
            if (!definitionsToDelete.isEmpty() && log.isDebugEnabled()) {
                log.debug("Metadata update: Measurement definitions deleted from resource type ["
                    + existingType.getName() + "]:" + definitionsToDelete);
            }

            entityMgr.flush();
        }
        // TODO what if they are null? --> delete everything from existingType
        // not needed see JBNADM-1639
    }

    private Set<MeasurementDefinition> getMetricDefinitions(ResourceType newType) {
        Set<MeasurementDefinition> result = newType.getMetricDefinitions();
        result = (null == result) ? new HashSet<MeasurementDefinition>(1) : result;
        long period;

        switch (newType.getCategory()) {
        case PLATFORM:
            return result;
        case SERVER:
            period = AVAILABILITY_DEFAULT_PERIOD_SERVER;
            break;
        default:
            period = AVAILABILITY_DEFAULT_PERIOD_SERVICE;
        }

        MeasurementDefinition availabilityScanPeriod = new MeasurementDefinition(newType,
            MeasurementDefinition.AVAILABILITY_TYPE_NAME);
        // TODO I18N for this?
        availabilityScanPeriod.setDisplayName("Availability Type");
        availabilityScanPeriod.setCategory(MeasurementCategory.AVAILABILITY);
        availabilityScanPeriod
            .setDescription("The number of seconds between availability checks. The agent honors this setting as best as possible but the actual period can be longer based on agent activity.");
        availabilityScanPeriod.setDefaultInterval(period);
        availabilityScanPeriod.setDataType(DataType.AVAILABILITY);
        availabilityScanPeriod.setDefaultOn(true);
        availabilityScanPeriod.setUnits(MeasurementUnits.NONE); // n/a protects against non-null
        availabilityScanPeriod.setNumericType(NumericType.DYNAMIC); // n/a protects against non-null
        availabilityScanPeriod.setDisplayType(DisplayType.DETAIL); // n/a protects against non-null

        if (!result.contains(availabilityScanPeriod)) {
            result.add(availabilityScanPeriod);
        }

        return result;
    }

    @Override
    public void deleteMetadata(ResourceType existingType) {
        log.debug("Deleting metric definitions for " + existingType);

        MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
        criteria.addFilterResourceTypeId(existingType.getId());
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
