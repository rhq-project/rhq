/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.test;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.Hashtable;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

import org.jboss.ejb3.embedded.EJB3StandaloneBootstrap;
import org.jboss.ejb3.embedded.EJB3StandaloneDeployer;

public abstract class AbstractEJB3Test extends AssertJUnit {

    @BeforeClass
    public void resetDB() throws Exception {
        clearDB();
    }

    @BeforeSuite(groups = "integration.ejb3")
    //@BeforeGroups(groups = "integration.ejb3")
    public static void startupEmbeddedJboss() {
        System.out.println("Starting ejb3...");
        String classesDir = System.getProperty("ejbjarDirectory", "target/classes");
        System.out.println("Loading EJB3 classes from directory: " + classesDir);
        try {
            EJB3StandaloneBootstrap.boot(null);
            EJB3StandaloneBootstrap.scanClasspath(classesDir);

            System.err.println("...... embedded-jboss-beans deployed....");

            // Add all EJBs found in the archive that has this file
            EJB3StandaloneDeployer deployer = new EJB3StandaloneDeployer();

            File deployment = new File(classesDir);
            if (!deployment.exists()) {
                System.err.println("Deployment directory does not exist: " + deployment.getAbsolutePath());
            }

            URL archive = deployment.toURI().toURL();
            deployer.getArchives().add(archive);

            System.err.println("...... deploying MM ejb3.....");
            System.err.println("...... ejb3 deployed....");

            // Deploy everything we got
            deployer.setKernel(EJB3StandaloneBootstrap.getKernel());
            deployer.create();
            System.err.println("...... deployer created....");

            deployer.start();
            System.err.println("...... deployer started....");
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

//    @AfterSuite
    @AfterGroups(groups = "integration.ejb3")
    public static void shutdownEmbeddedJboss() {
        EJB3StandaloneBootstrap.shutdown();
    }

    private TransactionManager tm;

    public TransactionManager getTransactionManager() {
        try {
            tm = (TransactionManager) getInitialContext().lookup("java:/TransactionManager");
            return tm;
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load transaction manager", e);
        }
    }

    public EntityManager getEntityManager() {
        try {
            return ((EntityManagerFactory) getInitialContext().lookup("java:/RHQEntityManagerFactory"))
                .createEntityManager();
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load entity manager", e);
        }
    }

    public boolean isPostgres(EntityManager em) throws Exception {
        return ((org.hibernate.ejb.EntityManagerImpl) em).getSession().connection().getMetaData()
            .getDatabaseProductName().toLowerCase().indexOf("postgres") > -1;
    }

    public InitialContext getInitialContext() {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
        env.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
        try {
            return new InitialContext(env);
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load initial context", e);
        }
    }

    private final long DEFAULT_OFFSET = 50;
    private long referenceTime = new Date().getTime();

    public Date getAnotherDate() {
        return getAnotherDate(DEFAULT_OFFSET);
    }

    public Date getAnotherDate(long offset) {
        referenceTime += offset;
        return new Date(referenceTime);
    }

    protected void executeInTransaction(TransactionCallback callback) {
        try {
            getTransactionManager().begin();
            callback.execute();
            getTransactionManager().commit();
        } catch (Throwable t) {
            try {
                getTransactionManager().rollback();
            } catch (SystemException e) {
                throw new RuntimeException("Failed to rollback transaction", e);
            }
            throw new RuntimeException(t.getCause());
        }
    }

    void clearDB() throws Exception {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();

                em.createNativeQuery("delete from jms_subscriptions");
                em.createNativeQuery("delete from jms_roles");
                em.createNativeQuery("delete from jms_users");
                em.createNativeQuery("delete from jms_transactions");
                em.createNativeQuery("delete from jms_messages");
                em.createNativeQuery("delete from rhq_drift_config_map");
                em.createNativeQuery("delete from rhq_drift_template_map");
                em.createNativeQuery("delete from rhq_delete_res_hist");
                em.createNativeQuery("delete from rhq_create_res_hist");
                em.createNativeQuery("delete from rhq_tagging_bundle_dest_map");
                em.createNativeQuery("delete from rhq_tagging_bundle_deploy_map");
                em.createNativeQuery("delete from rhq_tagging_bundle_version_map");
                em.createNativeQuery("delete from rhq_tagging_bundle_map");
                em.createNativeQuery("delete from rhq_bundle_res_dep_hist");
                em.createNativeQuery("delete from rhq_bundle_res_deploy");
                em.createNativeQuery("delete from rhq_bundle_deployment");
                em.createNativeQuery("delete from rhq_bundle_destination");
                em.createNativeQuery("delete from rhq_bundle_file");
                em.createNativeQuery("delete from rhq_bundle_version_repo");
                em.createNativeQuery("delete from rhq_bundle_version");
                em.createNativeQuery("delete from rhq_bundle");
                em.createNativeQuery("delete from rhq_bundle_type");
                em.createNativeQuery("delete from rhq_repo_advisory");
                em.createNativeQuery("delete from rhq_advisory_buglist");
                em.createNativeQuery("delete from rhq_advisory_cve");
                em.createNativeQuery("delete from rhq_cve");
                em.createNativeQuery("delete from rhq_advisory_package");
                em.createNativeQuery("delete from rhq_advisory");
                em.createNativeQuery("delete from rhq_distribution_file");
                em.createNativeQuery("delete from rhq_repo_distribution");
                em.createNativeQuery("delete from rhq_distribution where id not in (1, 2)");
                em.createNativeQuery("delete from rhq_pkg_prd_map");
                em.createNativeQuery("delete from rhq_pkg_ver_content_src_map");
                em.createNativeQuery("delete from rhq_repo_pkg_version_map");
                em.createNativeQuery("delete from rhq_repo_repo_relation_map");
                em.createNativeQuery("delete from rhq_repo_repo_group_map");
                em.createNativeQuery("delete from rhq_repo_content_src_map");
                em.createNativeQuery("delete from rhq_repo_resource_map");
                em.createNativeQuery("delete from rhq_package_inst_step");
                em.createNativeQuery("delete from rhq_repo_sync");
                em.createNativeQuery("delete from rhq_content_src_sync");
                em.createNativeQuery("delete from rhq_installed_pkg_hist");
                em.createNativeQuery("delete from rhq_installed_package");
                em.createNativeQuery("delete from rhq_content_req");
                em.createNativeQuery("delete from rhq_package_version");
                em.createNativeQuery("delete from rhq_package_bits");
                em.createNativeQuery("delete from rhq_package");
                em.createNativeQuery("delete from rhq_package_type");
                em.createNativeQuery("delete from rhq_repo_relation");
                em.createNativeQuery("delete from rhq_repo_relation_type where id not in (1, 2)");
                em.createNativeQuery("delete from rhq_repo");
                em.createNativeQuery("delete from rhq_repo_group");
                em.createNativeQuery("delete from rhq_repo_group_type where id <> 1");
                em.createNativeQuery("delete from rhq_archirtecture where id < 1 and id > 38");
                em.createNativeQuery("delete from rhq_meas_data_num_r14");
                em.createNativeQuery("delete from rhq_meas_data_num_r13");
                em.createNativeQuery("delete from rhq_meas_data_num_r12");
                em.createNativeQuery("delete from rhq_meas_data_num_r11");
                em.createNativeQuery("delete from rhq_meas_data_num_r10");
                em.createNativeQuery("delete from rhq_meas_data_num_r09");
                em.createNativeQuery("delete from rhq_meas_data_num_r08");
                em.createNativeQuery("delete from rhq_meas_data_num_r07");
                em.createNativeQuery("delete from rhq_meas_data_num_r06");
                em.createNativeQuery("delete from rhq_meas_data_num_r05");
                em.createNativeQuery("delete from rhq_meas_data_num_r04");
                em.createNativeQuery("delete from rhq_meas_data_num_r03");
                em.createNativeQuery("delete from rhq_meas_data_num_r02");
                em.createNativeQuery("delete from rhq_meas_data_num_r01");
                em.createNativeQuery("delete from rhq_meas_data_num_r00");
                em.createNativeQuery("delete from rhq_measurement_oob_tmp");
                em.createNativeQuery("delete rhq_measurement_oob");
                em.createNativeQuery("delete rhq_resource_avail");
                em.createNativeQuery("delete from rhq_availability");
                em.createNativeQuery("delete from rhq_calltime_data_value");
                em.createNativeQuery("delete from rhq_calltime_data_key");
                em.createNativeQuery("delete from rhq_measurement_data_trait");
                em.createNativeQuery("delete from rhq_measurement_data_num_1d");
                em.createNativeQuery("delete from rhq_measurement_data_num_6h");
                em.createNativeQuery("delete from rhq_measurement_data_num_1h");
                em.createNativeQuery("delete from rhq_measurement_bline");
                em.createNativeQuery("delete from rhq_measurement_sched");
                em.createNativeQuery("delete from rhq_measurement_def");
                em.createNativeQuery("delete from rhq_plugin");
                em.createNativeQuery("delete from rhq_system_config where id not in (1, 2, 3, 4, 9, 10, 32, 34, 35, 36, 51, 52, 53, 54, 55, 56)");
                em.createNativeQuery("delete from rhq_alert_notification");
                em.createNativeQuery("delete from rhq_alert_condition_log");
                em.createNativeQuery("delete from rhq_alert");
                em.createNativeQuery("delete from rhq_alert_condition");
                em.createNativeQuery("delete from rhq_alert_dampen_event");
                em.createNativeQuery("delete from rhq_alert_definition");
                em.createNativeQuery("delete from rhq_event");
                em.createNativeQuery("delete from rhq_event_source");
                em.createNativeQuery("delete from rhq_event_def");
                em.createNativeQuery("delete from rhq_operation_schedule");
                em.createNativeQuery("delete from rhq_operation_history");
                em.createNativeQuery("delete from rhq_operation_def");
                em.createNativeQuery("delete from rhq_dashboard_portlet");
                em.createNativeQuery("delete from rhq_dashboard");
                em.createNativeQuery("delete from rhq_saved_search");
                em.createNativeQuery("delete from rhq_subject_role_ldap_map");
                em.createNativeQuery("delete from rhq_subject_role_map where id not in (1, 2)");
                em.createNativeQuery("delete from rhq_permission where role_id not in (1, 2)");
                em.createNativeQuery("delete from rhq_role_ldap_group");
                em.createNativeQuery("delete from rhq_role_resource_group_map");
                em.createNativeQuery("delete from rhq_role where id not in (1, 2)");
                em.createNativeQuery("delete from rhq_tagging_res_group_map");
                em.createNativeQuery("delete from rhq_tagging_resource_map");
                em.createNativeQuery("delete from rhq_tagging");
                em.createNativeQuery("delete from rhq_config_update");
                em.createNativeQuery("delete from rhq_config_group_update");
                em.createNativeQuery("delete from rhq_resource_group_res_exp_map");
                em.createNativeQuery("delete from rhq_resource_group_res_imp_map");
                em.createNativeQuery("delete from rhq_resource_group");
                em.createNativeQuery("delete from rhq_group_def");
                em.createNativeQuery("delete from rhq_resource_error");
                em.createNativeQuery("delete from rhq_resource");
                em.createNativeQuery("delete from rhq_prd_ver");
                em.createNativeQuery("delete from rhq_process_scan");
                em.createNativeQuery("delete from rhq_resource_type_parents");
                em.createNativeQuery("delete from rhq_resource_subcat");
                em.createNativeQuery("delete from rhq_resource_type");
                em.createNativeQuery("delete from rhq_subject where id not in (1, 2)");
                em.createNativeQuery("delete from rhq_principal where id <> 2");
                em.createNativeQuery("delete from rhq_failover_details");
                em.createNativeQuery("delete from rhq_failover_list");
                em.createNativeQuery("delete from rhq_partition_details");
                em.createNativeQuery("delete from rhq_partition_event");
                em.createNativeQuery("delete from rhq_agent");
                em.createNativeQuery("delete from rhq_server");
                em.createNativeQuery("delete from rhq_affinity_group");
                em.createNativeQuery("delete from rhq_raw_config");
                em.createNativeQuery("delete from rhq_config_template");
                em.createNativeQuery("delete from rhq_config_property");
                em.createNativeQuery("delete from rhq_config");
                em.createNativeQuery("delete from rhq_config_prop_constr");
                em.createNativeQuery("delete from rhq_conf_prop_def_enum");
                em.createNativeQuery("delete from rhq_config_pd_osrc");
                em.createNativeQuery("delete from rhq_config_prop_def");
                em.createNativeQuery("delete from rhq_config_prop_grp_def");
                em.createNativeQuery("delete from rhq_config_def");
            }
        });
    }

    public static interface TransactionCallback {
        void execute() throws Exception;
    }
}