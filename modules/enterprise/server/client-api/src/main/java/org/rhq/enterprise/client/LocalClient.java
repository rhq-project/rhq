/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.client;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.client.RhqFacade;
import org.rhq.bindings.client.RhqManager;
import org.rhq.bindings.util.InterfaceSimplifier;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerRemote;
import org.rhq.enterprise.server.alert.AlertManagerRemote;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.bundle.BundleManagerRemote;
import org.rhq.enterprise.server.configuration.ConfigurationManagerRemote;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.content.RepoManagerRemote;
import org.rhq.enterprise.server.discovery.DiscoveryBossRemote;
import org.rhq.enterprise.server.drift.DriftManagerRemote;
import org.rhq.enterprise.server.drift.DriftTemplateManagerRemote;
import org.rhq.enterprise.server.event.EventManagerRemote;
import org.rhq.enterprise.server.install.remote.RemoteInstallManagerRemote;
import org.rhq.enterprise.server.measurement.AvailabilityManagerRemote;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerRemote;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.report.DataAccessManagerRemote;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.ResourceTypeManagerRemote;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerRemote;
import org.rhq.enterprise.server.search.SavedSearchManagerRemote;
import org.rhq.enterprise.server.support.SupportManagerRemote;
import org.rhq.enterprise.server.sync.SynchronizationManagerRemote;
import org.rhq.enterprise.server.system.SystemManagerRemote;
import org.rhq.enterprise.server.tagging.TagManagerRemote;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class LocalClient implements RhqFacade {

    private static final Log LOG = LogFactory.getLog(LocalClient.class);

    private Subject subject;
    private Map<String, Object> managers;

    public LocalClient(Subject subject) {
        this.subject = subject;
    }

    @Override
    public Subject getSubject() {
        return subject;
    }

    @Override
    public Subject login(String user, String password) throws Exception {
        return subject;
    }

    @Override
    public void logout() {
    }

    @Override
    public boolean isLoggedIn() {
        return true;
    }

    @Override
    public AlertManagerRemote getAlertManager() {
        return AccessController.doPrivileged(new PrivilegedAction<AlertManagerRemote>() {
            @Override
            public AlertManagerRemote run() {
                return AccessController.doPrivileged(new PrivilegedAction<AlertManagerRemote>() {
                    @Override
                    public AlertManagerRemote run() {
                        return getProxy(LookupUtil.getAlertManager(), AlertManagerRemote.class);
                    }
                });
            }
        });
    }

    @Override
    public AlertDefinitionManagerRemote getAlertDefinitionManager() {
        return AccessController.doPrivileged(new PrivilegedAction<AlertDefinitionManagerRemote>() {
            @Override
            public AlertDefinitionManagerRemote run() {
                return getProxy(LookupUtil.getAlertDefinitionManager(), AlertDefinitionManagerRemote.class);
            }
        });
    }

    @Override
    public AvailabilityManagerRemote getAvailabilityManager() {
        return AccessController.doPrivileged(new PrivilegedAction<AvailabilityManagerRemote>() {
            @Override
            public AvailabilityManagerRemote run() {
                return getProxy(LookupUtil.getAvailabilityManager(), AvailabilityManagerRemote.class);
            }
        });
    }

    @Override
    public BundleManagerRemote getBundleManager() {
        return AccessController.doPrivileged(new PrivilegedAction<BundleManagerRemote>() {
            @Override
            public BundleManagerRemote run() {
                return AccessController.doPrivileged(new PrivilegedAction<BundleManagerRemote>() {
                    @Override
                    public BundleManagerRemote run() {
                        return getProxy(LookupUtil.getBundleManager(), BundleManagerRemote.class);
                    }
                });
            }
        });
    }

    @Override
    public CallTimeDataManagerRemote getCallTimeDataManager() {
        return AccessController.doPrivileged(new PrivilegedAction<CallTimeDataManagerRemote>() {
            @Override
            public CallTimeDataManagerRemote run() {
                return getProxy(LookupUtil.getCallTimeDataManager(), CallTimeDataManagerRemote.class);
            }
        });
    }

    @Override
    public RepoManagerRemote getRepoManager() {
        return AccessController.doPrivileged(new PrivilegedAction<RepoManagerRemote>() {
            @Override
            public RepoManagerRemote run() {
                return getProxy(LookupUtil.getRepoManagerLocal(), RepoManagerRemote.class);
            }
        });
    }

    @Override
    public ConfigurationManagerRemote getConfigurationManager() {
        return AccessController.doPrivileged(new PrivilegedAction<ConfigurationManagerRemote>() {
            @Override
            public ConfigurationManagerRemote run() {
                return getProxy(LookupUtil.getConfigurationManager(), ConfigurationManagerRemote.class);
            }
        });
    }

    @Override
    public ContentManagerRemote getContentManager() {
        return AccessController.doPrivileged(new PrivilegedAction<ContentManagerRemote>() {
            @Override
            public ContentManagerRemote run() {
                return getProxy(LookupUtil.getContentManager(), ContentManagerRemote.class);
            }
        });
    }

    @Override
    public DataAccessManagerRemote getDataAccessManager() {
        return AccessController.doPrivileged(new PrivilegedAction<DataAccessManagerRemote>() {
            @Override
            public DataAccessManagerRemote run() {
                return getProxy(LookupUtil.getDataAccessManager(), DataAccessManagerRemote.class);
            }
        });
    }

    @Override
    public DiscoveryBossRemote getDiscoveryBoss() {
        return AccessController.doPrivileged(new PrivilegedAction<DiscoveryBossRemote>() {
            @Override
            public DiscoveryBossRemote run() {
                return getProxy(LookupUtil.getDiscoveryBoss(), DiscoveryBossRemote.class);
            }
        });
    }

    @Override
    public DriftManagerRemote getDriftManager() {
        return AccessController.doPrivileged(new PrivilegedAction<DriftManagerRemote>() {
            @Override
            public DriftManagerRemote run() {
                return getProxy(LookupUtil.getDriftManager(), DriftManagerRemote.class);
            }
        });
    }

    public DriftTemplateManagerRemote getDriftTemplateManager() {
        return AccessController.doPrivileged(new PrivilegedAction<DriftTemplateManagerRemote>() {
            @Override
            public DriftTemplateManagerRemote run() {
                return getProxy(LookupUtil.getDriftTemplateManager(), DriftTemplateManagerRemote.class);
            }
        });
    }

    @Override
    public EventManagerRemote getEventManager() {
        return AccessController.doPrivileged(new PrivilegedAction<EventManagerRemote>() {
            @Override
            public EventManagerRemote run() {
                return getProxy(LookupUtil.getEventManager(), EventManagerRemote.class);
            }
        });
    }

    @Override
    public MeasurementBaselineManagerRemote getMeasurementBaselineManager() {
        return AccessController.doPrivileged(new PrivilegedAction<MeasurementBaselineManagerRemote>() {
            @Override
            public MeasurementBaselineManagerRemote run() {
                return getProxy(LookupUtil.getMeasurementBaselineManager(), MeasurementBaselineManagerRemote.class);
            }
        });
    }

    @Override
    public MeasurementDataManagerRemote getMeasurementDataManager() {
        return AccessController.doPrivileged(new PrivilegedAction<MeasurementDataManagerRemote>() {
            @Override
            public MeasurementDataManagerRemote run() {
                return getProxy(LookupUtil.getMeasurementDataManager(), MeasurementDataManagerRemote.class);
            }
        });
    }

    @Override
    public MeasurementDefinitionManagerRemote getMeasurementDefinitionManager() {
        return AccessController.doPrivileged(new PrivilegedAction<MeasurementDefinitionManagerRemote>() {
            @Override
            public MeasurementDefinitionManagerRemote run() {
                return getProxy(LookupUtil.getMeasurementDefinitionManager(), MeasurementDefinitionManagerRemote.class);
            }
        });
    }

    @Override
    public MeasurementScheduleManagerRemote getMeasurementScheduleManager() {
        return AccessController.doPrivileged(new PrivilegedAction<MeasurementScheduleManagerRemote>() {
            @Override
            public MeasurementScheduleManagerRemote run() {
                return getProxy(LookupUtil.getMeasurementScheduleManager(), MeasurementScheduleManagerRemote.class);
            }
        });
    }

    @Override
    public OperationManagerRemote getOperationManager() {
        return AccessController.doPrivileged(new PrivilegedAction<OperationManagerRemote>() {
            @Override
            public OperationManagerRemote run() {
                return getProxy(LookupUtil.getOperationManager(), OperationManagerRemote.class);
            }
        });
    }

    @Override
    public ResourceManagerRemote getResourceManager() {
        return AccessController.doPrivileged(new PrivilegedAction<ResourceManagerRemote>() {
            @Override
            public ResourceManagerRemote run() {
                return getProxy(LookupUtil.getResourceManager(), ResourceManagerRemote.class);
            }
        });
    }

    @Override
    public ResourceFactoryManagerRemote getResourceFactoryManager() {
        return AccessController.doPrivileged(new PrivilegedAction<ResourceFactoryManagerRemote>() {
            @Override
            public ResourceFactoryManagerRemote run() {
                return getProxy(LookupUtil.getResourceFactoryManager(), ResourceFactoryManagerRemote.class);
            }
        });
    }

    @Override
    public ResourceGroupManagerRemote getResourceGroupManager() {
        return AccessController.doPrivileged(new PrivilegedAction<ResourceGroupManagerRemote>() {
            @Override
            public ResourceGroupManagerRemote run() {
                return getProxy(LookupUtil.getResourceGroupManager(), ResourceGroupManagerRemote.class);
            }
        });
    }

    @Override
    public ResourceTypeManagerRemote getResourceTypeManager() {
        return AccessController.doPrivileged(new PrivilegedAction<ResourceTypeManagerRemote>() {
            @Override
            public ResourceTypeManagerRemote run() {
                return getProxy(LookupUtil.getResourceTypeManager(), ResourceTypeManagerRemote.class);
            }
        });
    }

    @Override
    public RoleManagerRemote getRoleManager() {
        return AccessController.doPrivileged(new PrivilegedAction<RoleManagerRemote>() {
            @Override
            public RoleManagerRemote run() {
                return getProxy(LookupUtil.getRoleManager(), RoleManagerRemote.class);
            }
        });
    }

    @Override
    public SavedSearchManagerRemote getSavedSearchManager() {
        return AccessController.doPrivileged(new PrivilegedAction<SavedSearchManagerRemote>() {
            @Override
            public SavedSearchManagerRemote run() {
                return getProxy(LookupUtil.getSavedSearchManager(), SavedSearchManagerRemote.class);
            }
        });
    }

    @Override
    public SubjectManagerRemote getSubjectManager() {
        return AccessController.doPrivileged(new PrivilegedAction<SubjectManagerRemote>() {
            @Override
            public SubjectManagerRemote run() {
                return getProxy(LookupUtil.getSubjectManager(), SubjectManagerRemote.class);
            }
        });
    }

    @Override
    public SupportManagerRemote getSupportManager() {
        return AccessController.doPrivileged(new PrivilegedAction<SupportManagerRemote>() {
            @Override
            public SupportManagerRemote run() {
                return getProxy(LookupUtil.getSupportManager(), SupportManagerRemote.class);
            }
        });
    }

    @Override
    public SystemManagerRemote getSystemManager() {
        return AccessController.doPrivileged(new PrivilegedAction<SystemManagerRemote>() {
            @Override
            public SystemManagerRemote run() {
                return getProxy(LookupUtil.getSystemManager(), SystemManagerRemote.class);
            }
        });
    }

    @Override
    public RemoteInstallManagerRemote getRemoteInstallManager() {
        return AccessController.doPrivileged(new PrivilegedAction<RemoteInstallManagerRemote>() {
            @Override
            public RemoteInstallManagerRemote run() {
                return getProxy(LookupUtil.getRemoteInstallManager(), RemoteInstallManagerRemote.class);
            }
        });
    }

    @Override
    public TagManagerRemote getTagManager() {
        return AccessController.doPrivileged(new PrivilegedAction<TagManagerRemote>() {
            @Override
            public TagManagerRemote run() {
                return getProxy(LookupUtil.getTagManager(), TagManagerRemote.class);
            }
        });
    }

    @Override
    public SynchronizationManagerRemote getSynchronizationManager() {
        return AccessController.doPrivileged(new PrivilegedAction<SynchronizationManagerRemote>() {
            @Override
            public SynchronizationManagerRemote run() {
                return getProxy(LookupUtil.getSynchronizationManager(), SynchronizationManagerRemote.class);
            }
        });
    }

    @Override
    public Map<String, Object> getManagers() {
        if (managers == null) {

            managers = new HashMap<String, Object>();

            for (RhqManager manager : RhqManager.values()) {
                try {
                    Method m = getClass().getMethod("get" + manager.name());
                    managers.put(manager.name(), m.invoke(this));
                } catch (Throwable e) {
                    LOG.error("Failed to load manager " + manager + " due to missing class.", e);
                }
            }
        }

        return managers;
    }

    private <T> T getProxy(Object slsb, Class<T> iface) {
        RhqManager manager = RhqManager.forInterface(iface);

        Class<?> simplified = null;

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(iface.getClassLoader());
            simplified = InterfaceSimplifier.simplify(iface);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }

        Object proxy =
            Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { simplified }, new LocalClientProxy(slsb,
                this, manager));

        return iface.cast(proxy);
    }
}
