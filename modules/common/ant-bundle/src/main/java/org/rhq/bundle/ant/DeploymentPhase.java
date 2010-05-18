package org.rhq.bundle.ant;

/**
 * A bundle deployment phase. One or more tasks are associated with each phase. The phases are executed as part of
 * three user-initiated actions:
 *
 * Deploy: INSTALL,START
 * Redeploy: STOP,UPGRADE,START
 * Undeploy: STOP,UNINSTALL
 *
 * TODO (ips): This should probably be moved to domain or somewhere else not specific to the Ant bundle type.
 */
public enum DeploymentPhase {
    INSTALL,
    START,
    STOP,
    UPGRADE,
    UNINSTALL;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
