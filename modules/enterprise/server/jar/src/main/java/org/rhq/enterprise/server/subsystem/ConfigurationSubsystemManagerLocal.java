package org.rhq.enterprise.server.subsystem;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.composite.ConfigurationUpdateComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Joseph Marques
 */
@Local
public interface ConfigurationSubsystemManagerLocal {

    PageList<ConfigurationUpdateComposite> getResourceConfigurationUpdates(Subject subject, String resourceFilter,
        String parentFilter, Long startTime, Long endTime, ConfigurationUpdateStatus status, PageControl pc);
}
