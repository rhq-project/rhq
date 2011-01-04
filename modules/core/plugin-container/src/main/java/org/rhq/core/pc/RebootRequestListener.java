package org.rhq.core.pc;

/**
 * This listener can be notified by the plugin container when some condition occurs that
 * PC needs to be rebooted.
 */
public interface RebootRequestListener {

    void reboot();

}
