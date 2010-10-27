package org.rhq.enterprise.gui.coregui.client.resource.disambiguation;

import java.util.Iterator;
import java.util.List;

import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.DisambiguationReport.Resource;
import org.rhq.core.domain.resource.composite.DisambiguationReport.ResourceType;

/**
 * Class handles some of the data decoration that used to be done for legacy struts
 * tags.  See org.rhq.enterprise.gui.legacy.taglib.display.DisambiguatedResourceLineageDecorator and 
 * org.rhq.enterprise.gui.legacy.taglib.display.DisambiguatedResourceLineageTag
 * for original functionality.
 * 
 * @author Simeon Pinder
 */
public class ReportDecorator {

    //TODO: pull value from more bookmarking/history definition 
    public final static String GWT_RESOURCE_URL = "#Resource/";
    public static final String DEFAULT_SEPARATOR = " > ";

    /**
     * Generates HTML label that includes hyperlinks to each of the Resources in the linage from a DisambiguationReport.
     *
     * @param disambiguationReport a disambiguation report
     * @param resourceId the id of the Resource to which the disambiguation report corresponds
     * @param makeLink if true, the Resource name will be made into a link to go the Resource
     *
     * @return the HTML label
     */
    public static String decorateDisambiguationReport(DisambiguationReport disambiguationReport, int resourceId,
                                                      boolean makeLink) {
        String parentsHtml = decorateResourceLineage(disambiguationReport.getParents(), true);
        String resourceHtml = ReportDecorator.decorateResourceName(ReportDecorator.GWT_RESOURCE_URL,
            disambiguationReport.getResourceType(), disambiguationReport.getName(), resourceId, makeLink);
        String label;
        if (parentsHtml.length() >= 1) {
            label = parentsHtml + ReportDecorator.DEFAULT_SEPARATOR + resourceHtml;
        } else {
            label = resourceHtml;
        }
        return label;
    }

    /**
     * Generates HTML label from DisambiguationReport data.
     * 
     * @param type ResourceType - If !null, the ResourceType name is prepended to result.
     * @param resourceName Name of the element from report
     * @param resourceId Id for resource
     * @param makeLink if true, the Resource name will be made into a link to go the Resource
     *
     * @return String of generated html for a ResourceName.
     */
    public static String decorateResourceName(String specificUrl, ResourceType type, String resourceName,
                                              int resourceId, boolean makeLink) {
        String decorated = "";
        if (type != null) {
            decorated += type.getName();
            if (type.getPlugin() != null) {
                decorated += " (" + type.getPlugin() + " plugin)";
            }
            decorated += " ";
        }

        if (makeLink) {
            decorated += "<a href=\"" + specificUrl + resourceId + "\">";
        }
        decorated += resourceName;
        if (makeLink) {
            decorated += "</a>";
        }

        return decorated;
    }

    /**
     * Generates HTML label of Resource Lineage for disambiguation.
     * 
     * @param parents ResourceLineage provided by DisambiguationReport.
     * @param makeLink if true, the Resource name will be made into a link to go the Resource
     *
     * @return String of generated html for ResourceLineage.
     */
    public static String decorateResourceLineage(List<Resource> parents, boolean makeLink) {
        StringBuilder decorated = new StringBuilder();
        if (parents != null && parents.size() > 0) {

            Iterator<DisambiguationReport.Resource> it = parents.iterator();
            DisambiguationReport.Resource parent = it.next();
            //generate first link
            String parentUrl = ReportDecorator.decorateResourceName(GWT_RESOURCE_URL, null, parent.getName(), parent
                .getId(), true);
            decorated = writeResource(decorated, parentUrl, parent.getType());
            while (it.hasNext()) {
                decorated.append(DEFAULT_SEPARATOR);
                parent = it.next();
                decorated = writeResource(decorated, ReportDecorator.decorateResourceName(GWT_RESOURCE_URL, null,
                    parent.getName(), parent.getId(), makeLink), parent.getType());

            }
        }
        return decorated.toString();
    }

    /**
     * Appends resource lineage details with HTML anchors. 
     * 
     * @param existing
     * @param url
     * @param resourceType
     * @return
     */
    private static StringBuilder writeResource(StringBuilder existing, String url,
                                               ResourceType resourceType) {
        if (!resourceType.isSingleton()) {

            existing.append(resourceType.getName()).append(" ");

            if (resourceType.getPlugin() != null) {
                existing.append("(").append(resourceType.getPlugin()).append(" plugin) ");
            }
        }

        if (url != null) {
            existing.append(url);
        }

        if (resourceType.isSingleton() && resourceType.getPlugin() != null) {
            existing.append(" (").append(resourceType.getPlugin()).append(" plugin)");
        }
        return existing;
    }

}
