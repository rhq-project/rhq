package org.rhq.plugins.lsof;

/**
 * Indicator of the detection mechanism to use to detect network resources.
 *
 * @author John Mazzitelli
 */
public enum DetectionMechanism {
    /**
     * Use an external executable (such as 'lsof') to detect network resources. 
     */
    EXTERNAL,

    /**
     * Use a Java-based mechanism that is internal to this plugin.
     */
    INTERNAL
}
