package org.rhq.enterprise.server.measurement.uibean;

import java.util.ArrayList;
import java.util.List;

/**
 * This holds the number of metrics of this type configured (the template type)
 * for the resource under discussion; the count of those currently
 * collecting data, the ones that aren't and the total.
 * 
 * When displaying availability status on an aggregate of resources, each
 * resource will counted in one of these buckets
 * <ol>
 * <li>availUp<br>
 * Resources that are configured for metric collection and for which we are 
 * currently collecting data
 * <li>availDown<br>
 * Resources that are configured for metric collection and for which we are 
 * <i>not</i> currently collecting data i.e. the resource is down, as far as we
 * can tell
 * <li>availUnknown<br>
 * This is the number of resources for which metric collection is configured
 * but whose availability status is unknown
 *  </ol> 
 */
public class MeasurementSummary implements java.io.Serializable {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    private Integer availUp;
    private Integer availDown;
    private Integer availUnknown;

    /** 
     * Default Constructor
     */
    public MeasurementSummary() {
    }

    /** 
     * Constructor with init values
     */
    public MeasurementSummary(Integer up, Integer down, Integer unknown) {
        this.availUp = up;
        this.availDown = down;
        this.availUnknown = unknown;
    }

    /**
     * @return Integer
     */
    public Integer getAvailUp() {
        return availUp;
    }

    /**
     * @param availUp The availUp to set
     */
    public void setAvailUp(Integer currentConfigured) {
        this.availUp = currentConfigured;
    }

    /**
     * @return Integer
     */
    public Integer getAvailDown() {
        return availDown;
    }

    /**
     * @param availDown The availDown to set
     */
    public void setAvailDown(Integer unavailConfigured) {
        this.availDown = unavailConfigured;
    }

    /**
     * @return Integer
     */
    public Integer getAvailUnknown() {
        return availUnknown;
    }

    /**
     * Sets the availUnknown.
     * @param availUnknown The availUnknown to set
     */
    public void setAvailUnknown(Integer totalConfigured) {
        this.availUnknown = totalConfigured;
    }

    /**
     * Returns a three element list with up, down and unknown
     * 
     * @return List
     */
    public List<Integer> asList() {
        List<Integer> returnList = new ArrayList<Integer>();
        returnList.add(availUp);
        returnList.add(availDown);
        returnList.add(availUnknown);
        return returnList;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(MeasurementSummary.class.getName());
        sb.append("(availUp=").append(availUp);
        sb.append(",availDown=").append(availDown);
        sb.append(",availUnknown=").append(availUnknown);
        sb.append(")");
        return sb.toString();
    }

}
