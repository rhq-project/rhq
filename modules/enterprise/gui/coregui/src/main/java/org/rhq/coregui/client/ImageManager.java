package org.rhq.coregui.client;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.notification.ResultState;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.DeleteResourceStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite.GroupAvailabilityType;

/**
 * Provides an API to obtain links to images and icons, thus avoiding hardcoding image URLs throughout client code.
 *
 * For each icon, there is typically a small and large version (16x16 and 24x24). To obtain the smaller icon,
 * you use the "getXXXIcon" methods and to obtain the larger icon you use the "getXXXLargeIcon" method.
 *
 * @author John Mazzitelli
 *
 */
public class ImageManager {

    public static final String IMAGES_DIR = "images/";

    /**
     * Returns the base icon name for the discovery queue's platform folder nodes.
     * There are actually two icons, open and closed - the returned string is just
     * the base icon name.
     */
    public static String getDiscoveryQueuePlatformIconBase() {
        return "global/DiscoveryQueue_Platform_16.png";
    }

    /**
     * Returns a generic "help" icon. This will also have a peer "disabled" help icon.
     */
    public static String getHelpIcon() {
        return "global/help.png";
    }

    public static String getLoadingIcon() {
        return "ajax-loader.gif";
    }

    public static String getStatusComplete() {
        return "status_complete.gif";
    }

    public static String getStatusError() {
        return "status_error.gif";
    }

    /**
     * Returns a generic "upload" icon.
     */
    public static String getUploadIcon() {
        return "global/upload.png";
    }

    /**
     * Returns a generic view icon.
     */
    public static String getViewIcon() {
        return "[SKIN]/actions/view.png";
    }

    /**
     * Returns a generic edit icon.
     */
    public static String getEditIcon() {
        return "[SKIN]/actions/edit.png";
    }

    /**
     * Returns a generic edit icon.
     */
    public static String getEditDisabledIcon() {
        return "[SKIN]/actions/edit_Disabled.png";
    }

    /**
     * Returns a generic remove icon.
     */
    public static String getRemoveIcon() {
        return "[SKIN]/actions/remove.png";
    }

    /**
     * Returns a generic add icon.
     */
    public static String getAddIcon() {
        return "[SKIN]/actions/add.png";
    }

    /**
     * Returns a generic approve (aka "ok") icon.
     */
    public static String getApproveIcon() {
        return "[SKIN]/actions/approve.png";
    }

    /**
     * Returns a generic cancel icon.
     */
    public static String getCancelIcon() {
        return "[SKIN]/actions/undo.png";
    }

    /**
     * Returns a generic pinned icon.
     */
    public static String getUnpinnedIcon() {
        return "[SKIN]/headerIcons/pin_left.png";
    }

    /**
     * Returns a generic pinned icon.
     */
    public static String getPinnedIcon() {
        return "subsystems/drift/Pinned_active.png";
    }

    /**
     * Returns a generic pinned icon.
     */
    public static String getPinnedInactiveIcon() {
        return "subsystems/drift/Pinned_inactive.png";
    }

    public static String getDriftIcon() {
        return "subsystems/drift/Drift_16.png";
    }

    /**
     * Returns a drift icon given the category of the drift.
     * Note that if the category is null, it will be assumed the drift icon
     * should be one that indicates the file is "new" (presumably from
     * a coverage change set report - that is, its the first time the file
     * has been seen).
     *
     * @param category
     * @return path to icon
     */
    public static String getDriftCategoryIcon(DriftCategory category) {
        if (category == null) {
            return "subsystems/drift/Drift_new_16.png";
        }

        switch (category) {
        case FILE_ADDED:
            return "subsystems/drift/Drift_add_16.png";
        case FILE_CHANGED:
            return "subsystems/drift/Drift_change_16.png";
        case FILE_REMOVED:
            return "subsystems/drift/Drift_remove_16.png";
        }
        return null; // should never happen
    }

    /**
     * Returns the operation status icon. If status is null, returns
     * the plain, unbadged, operation icon.
     */
    public static String getOperationResultsIcon(OperationRequestStatus status) {
        String icon = "";
        if (status != null) {
            switch (status) {
            case INPROGRESS:
                icon = "_inprogress";
                break;
            case SUCCESS:
                icon = "_ok";
                break;
            case FAILURE:
                icon = "_failed";
                break;
            case CANCELED:
                icon = "_cancel";
                break;
            }
        }

        return "subsystems/control/Operation" + icon + "_16.png";
    }

    /**
     * All methods in this ImageManager class return image paths relative to the top
     * {@link #IMAGES_DIR images directory}. If you need a full path to the image, including
     * this top images directory name (for example, if you need to populate an explicit HTML
     * img tag's src attribute) pass in an image path to this {@link #getFullImagePath(String)}
     * method to obtain the full path.  The caller can optionally prepend {@link #IMAGES_DIR}
     * to any path returned by ImageManager, which is all this method really does.
     *
     * @param image a relative image path
     * @return a full image path
     */
    public static String getFullImagePath(String image) {
        return IMAGES_DIR + image;
    }

    public static String getResourceIcon(Resource resource) {
        return getResourceIcon(resource, "16");
    }

    public static String getResourceLargeIcon(Resource resource) {
        return getResourceIcon(resource, "24");
    }

    private static String getResourceIcon(Resource resource, String size) {
        ResourceType type = resource.getResourceType();
        ResourceCategory category;
        if (type != null) {
            category = type.getCategory();
        } else {
            category = ResourceCategory.SERVICE;
        }

        ResourceAvailability resourceAvail = resource.getCurrentAvailability();
        return getResourceIcon(category, resourceAvail.getAvailabilityType(), size);
    }

    public static String getClusteredResourceIcon(ResourceCategory category) {
        String categoryName = null;

        switch (category) {
        case PLATFORM: {
            categoryName = "Platform";
            break;
        }
        case SERVER: {
            categoryName = "Server";
            break;
        }
        case SERVICE: {
            categoryName = "Service";
            break;
        }
        }

        return "resources/" + categoryName + "_Group_16.png";
    }

    public static String getResourceTypeIcon(ResourceCategory category) {


        return "types/" + category.getDisplayName() + "_type_24.png";
    }

    public static String getResourceIcon(ResourceCategory category) {
        return getResourceIcon(category, AvailabilityType.UP);
    }

    public static String getResourceLargeIcon(ResourceCategory category) {
        return getResourceLargeIcon(category, AvailabilityType.UP);
    }

    public static String getResourceIcon(ResourceCategory category, AvailabilityType avail) {
        return getResourceIcon(category, avail, "16");
    }

    public static String getResourceLargeIcon(ResourceCategory category, AvailabilityType avail) {
        return getResourceIcon(category, avail, "24");
    }

    private static String getResourceIcon(ResourceCategory category, AvailabilityType avail, String size) {
        String categoryName = null;
        String availName = null;

        if (null == avail) {
            avail = AvailabilityType.UNKNOWN;
        }

        switch (category) {
        case PLATFORM: {
            categoryName = "Platform";
            // platform can only be up or down
            availName = (AvailabilityType.UP == avail) ? "up" : "down";
            break;
        }
        case SERVER: {
            categoryName = "Server";
            // no break
        }
        case SERVICE: {
            if (null == categoryName) {
                categoryName = "Service";
            }

            availName = avail.name().toLowerCase();
            break;
        }
        }

        return "types/" + categoryName + "_" + availName + "_" + size + ".png";
    }

    public static String getGroupIcon(GroupCategory groupType) {
        String category = groupType == GroupCategory.COMPATIBLE ? "Cluster" : "Group";
        return "types/" + category + "_up_16.png";
    }

    public static String getGroupLargeIcon(GroupCategory groupType) {
        String category = groupType == GroupCategory.COMPATIBLE ? "Cluster" : "Group";
        return "types/" + category + "_up_24.png";
    }

    /**
     * Returns the group icon badged with availability icon. Group avail
     * badging is determined in the following way, in order:
     * allDown      = red
     * someDown     = yellow
     * someDisabled = orange
     * otherwise    = green
     *
     * @param groupType the type of group
     * @return the group badge icon
     */
    public static String getGroupIcon(GroupCategory groupType, GroupAvailabilityType groupAvailType) {
        return getGroupIcon(groupType, groupAvailType, "16");
    }

    /**
     * @see #getGroupIcon(GroupCategory, GroupAvailabilityType)
     */
    public static String getGroupLargeIcon(GroupCategory groupType, GroupAvailabilityType groupAvailType) {
        return getGroupIcon(groupType, groupAvailType, "24");
    }

    /**
     * @see #getGroupIcon(GroupCategory, GroupAvailabilityType)
     */
    private static String getGroupIcon(GroupCategory groupType, GroupAvailabilityType groupAvailType, String size) {
        String category = groupType == GroupCategory.COMPATIBLE ? "Cluster" : "Group";

        switch (groupAvailType) {
        case EMPTY:
            return "types/" + category + "_up_" + size + ".png";
        case DOWN:
            return "types/" + category + "_down_" + size + ".png";
        case WARN:
            return "types/" + category + "_warning_" + size + ".png";
        case DISABLED:
            return "types/" + category + "_disabled_" + size + ".png";
        default:
            return "types/" + category + "_up_" + size + ".png";
        }
    }

    public static String getAvailabilityIconFromAvailType(AvailabilityType availType) {
        return getAvailabilityIconFromAvailTypeAndSize(availType, false);
    }

    public static String getAvailabilityLargeIconFromAvailType(AvailabilityType availType) {
        return getAvailabilityIconFromAvailTypeAndSize(availType, true);
    }

    private static String getAvailabilityIconFromAvailTypeAndSize(AvailabilityType availType, boolean isLarge) {
        if (null == availType) {
            availType = AvailabilityType.UNKNOWN;
        }

        String color = null;
        switch (availType) {
        case UP:
            color = "green";
            break;
        case DOWN:
            color = "red";
            break;
        case DISABLED:
            color = "orange";
            break;
        case UNKNOWN:
            color = "grey";
            break;
        }

        return "subsystems/availability/availability_" + color + "_" + (isLarge ? "24" : "16") + ".png";
    }

    // TODO: Fix this overloading of avail icons, status should have their own icons
    public static String getAvailabilityIcon(Boolean status) {
        AvailabilityType availType = (null == status) ? AvailabilityType.UNKNOWN
            : (Boolean.TRUE == status) ? AvailabilityType.UP : AvailabilityType.DOWN;

        return getAvailabilityIconFromAvailTypeAndSize(availType, false);
    }

    // TODO: Fix this overloading of avail icons, status should have their own icons
    public static String getAvailabilityLargeIcon(Boolean status) {
        AvailabilityType availType = (null == status) ? AvailabilityType.UNKNOWN
            : (Boolean.TRUE == status) ? AvailabilityType.UP : AvailabilityType.DOWN;

        return getAvailabilityIconFromAvailTypeAndSize(availType, true);
    }

    /**
     * Returns the large availability icon based on group availability. Determined in the following way, in order:
     * empty        = grey
     * allDown      = red
     * someDown     = yellow
     * someDisabled = orange
     * otherwise    = green
     *
     * @return the large avail icon
     */
    public static String getAvailabilityGroupLargeIcon(GroupAvailabilityType groupAvailType) {
        switch (groupAvailType) {
        case EMPTY:
            return "subsystems/availability/availability_grey_24.png";
        case DOWN:
            return "subsystems/availability/availability_red_24.png";
        case WARN:
            return "subsystems/availability/availability_yellow_24.png";
        case DISABLED:
            return "subsystems/availability/availability_orange_24.png";
        default:
            return "subsystems/availability/availability_green_24.png";
        }
    }

    public static String getAvailabilityYellowIcon() {
        return "subsystems/availability/availability_yellow_16.png";
    }


    public static String getAvailBarImagePath(AvailabilityType availType) {

        switch (availType) {
        case UP:
            return "availBar/up.png";

        case DOWN:
            return "availBar/down.png";

        case DISABLED:
            return "availBar/disabled.png";

        case UNKNOWN:
        default:
            return "availBar/unknown.png";
        }
    }

    public static String getAlertStatusCheckedIcon() {
        return "global/Check_16.png";
    }

    public static String getAlertIcon(AlertPriority priority) {
        return "subsystems/alert/Alert_" + priority.name() + "_16.png";
    }

    public static String getAlertIcon() {
        return "subsystems/alert/Flag_blue_16.png";
    }


    public static String getMetricEditIcon() {
        return "subsystems/monitor/Edit_Metric.png";
    }

    public static String getPluginConfigurationIcon(ConfigurationUpdateStatus updateStatus) {
        if (updateStatus != null) {
            switch (updateStatus) {
            case SUCCESS: {
                return "subsystems/inventory/Connection_ok_16.png";
            }
            case FAILURE: {
                return "subsystems/inventory/Connection_failed_16.png";
            }
            case INPROGRESS: {
                return "subsystems/inventory/Connection_inprogress_16.png";
            }
            case NOCHANGE:
                return "subsystems/inventory/Connection_16.png";
            }
        }

        return "subsystems/inventory/Connection_16.png";
    }

    public static String getResourceConfigurationIcon(ConfigurationUpdateStatus updateStatus) {
        if (updateStatus != null) {
            switch (updateStatus) {
            case SUCCESS: {
                return "subsystems/configure/Configure_ok_16.png";
            }
            case FAILURE: {
                return "subsystems/configure/Configure_failed_16.png";
            }
            case INPROGRESS: {
                return "subsystems/configure/Configure_inprogress_16.png";
            }
            case NOCHANGE:
                return "subsystems/configure/Configure_16.png";
            }
        }

        return "subsystems/configure/Configure_16.png";
    }

    public static String getLockedIcon() {
        return "global/Locked_16.png";
    }

    public static String getEventSeverityIcon(EventSeverity severity) {
        String icon = "";
        if (severity != null) {
            switch (severity) {
            case DEBUG:
                icon = "_debug";
                break;
            case INFO:
                icon = "_info";
                break;
            case WARN:
                icon = "_warning";
                break;
            case ERROR:
                icon = "_error";
                break;
            case FATAL:
                icon = "_fatal";
                break;
            }
        }

        return "subsystems/event/Events" + icon + "_16.png";
    }

    public static String getAlertNotificationResultIcon(ResultState status) {
        if (status == null) {
            status = ResultState.UNKNOWN;
        }

        switch (status) {
        case SUCCESS:
            return ImageManager.getAvailabilityIcon(true);
        case FAILURE:
            return ImageManager.getAvailabilityIcon(false);
        case PARTIAL:
            return ImageManager.getAvailabilityYellowIcon();
        case DEFERRED:
            return "[skin]/actions/redo.png"; // for lack of a better icon
        case UNKNOWN:
        default:
            return ImageManager.getAvailabilityIcon(null);
        }
    }

    /**
     * This returns an icon of the badge (e.g. the red X or the blue I) without the
     * event icon. This is used if you have a table of events and the user knows they are
     * events so there is no need to show the event icon itself- showing a bigger badge
     * to indicate severity is more useful.
     *
     * @param severity
     * @return badge icon
     */
    public static String getEventSeverityBadge(EventSeverity severity) {
        return "subsystems/event/" + severity.name() + "_16.png";
    }


    public static String getEventIcon() {
        return "subsystems/event/Events_16.png";
    }

    public static String getMonitorIcon() {
        return "subsystems/monitor/Monitor_16.png";
    }


    public static String getMonitorFailedIcon() {
        return "subsystems/monitor/Monitor_failed_16.png";
    }

    public static String getOperationIcon() {
        return "subsystems/control/Operation_16.png";
    }


    public static String getActivityPackageIcon() {
        return "subsystems/content/Package_16.png";
    }


    public static String getBundleIcon() {
        return "subsystems/bundle/Bundle_16.png";
    }

    public static String getBundleGroupIcon() {
        return "subsystems/bundle/BundleGroup_16.png";
    }

    public static String getConfigureIcon() {
        return "subsystems/configure/Configure_16.png";
    }

    public static String getChildCreateIcon() {
        return "subsystems/inventory/CreateChild_16.png";
    }

    public static String getBlankIcon() {
        return IMAGES_DIR+"blank.png";
    }

    public static String getChildCreateIcon(CreateResourceStatus createStatus) {
        if (createStatus == null) {
            return getChildCreateIcon();
        } else {
            switch (createStatus) {
            case SUCCESS: {
                return "subsystems/inventory/CreateChild_success_16.png";
            }
            case IN_PROGRESS: {
                return getChildCreateIcon();
            }
            default: {
                // all others will use the failure icon
                return "subsystems/inventory/CreateChild_failed_16.png";
            }
            }
        }
    }

    public static String getChildDeleteIcon() {
        return "subsystems/inventory/DeleteChild_16.png";
    }

    public static String getChildDeleteIcon(DeleteResourceStatus deleteStatus) {
        if (deleteStatus == null) {
            return getChildDeleteIcon();
        } else {
            switch (deleteStatus) {
            case SUCCESS: {
                return "subsystems/inventory/DeleteChild_success_16.png";
            }
            case IN_PROGRESS: {
                return getChildDeleteIcon();
            }
            default: {
                // all others will use the failure icon
                return "subsystems/inventory/DeleteChild_failed_16.png";
            }
            }
        }
    }
}
