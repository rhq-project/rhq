///*
// * JBoss, a division of Red Hat.
// * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
// */
//
//package org.rhq.enterprise.client;
//
//
//import org.rhq.enterprise.server.auth.SubjectManagerRemote;
//import org.rhq.enterprise.server.resource.ResourceManagerRemote;
//import org.rhq.enterprise.server.report.DataAccessRemote;
//import org.rhq.core.domain.auth.Subject;
//import org.rhq.core.domain.resource.Resource;
//
//import javax.naming.NamingException;
//import javax.security.auth.login.LoginException;
//import java.util.Hashtable;
//import java.util.List;
//
//public class RemoteClient {
//
//
//    public static void main(String[] args) throws NamingException, LoginException {
//        Hashtable properties = new Hashtable();
//
////        properties.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
////
////        properties.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
////        properties.put("java.naming.provider.url", "jnp://localhost:2099");
////
////        InitialContext ic = new InitialContext(properties);
//
//        System.out.println("yo");
//
//        SubjectManagerRemote smr = LookupUtil.getSubjectManagerRemote();
//
//        Subject s = smr.login("rhqadmin","rhqadmin");
//
//        ResourceManagerRemote rmr = LookupUtil.getResourceManagerRemote();
//
//        long start = System.currentTimeMillis();
//        Resource resource = null;//rmr.getResourceTree( 500050,true);
//        System.out.println("Time Taken: " + (System.currentTimeMillis() - start));
//
////        outputTree(resource, 0);
//
//        DataAccessRemote dataAccess = LookupUtil.getDataAccess();
//
//        //            "select r.id, r.parent_resource_id, r.name, a.availability_type \n" +
////                    "from rhq_resource r join rhq_availability a on r.id = a.resource_id\n" +
////                    "where  a.end_time is null";
//
//
//        List<Object[]> result = dataAccess.executeQuery(s, "select r.id, r.parentResource.id, r.name, a.availabilityType " +
//                "FROM Resource r JOIN r.availability a with a.endTime is null");
//        for (Object[] row :result) {
//            for (Object col : row) {
//                System.out.print(col + "\t");
//            }
//            System.out.println("");
//        }
//
////        List<Resource> lineage = rmr.getResourceLineage(819);
////
////        System.out.println("Lineage");
////        for (Resource lin : lineage) {
////            System.out.println("\t" + lin);
////        }
//    }
//
//    private static void outputTree(Resource res, int depth) {
//        for (int i = 0; i < depth; i++) {
//            System.out.print("\t");
//        }
//        System.out.println(res);
//        if (res.getChildResources() != null) {
////            for (Resource child : res.getChildResources()) {
////                outputTree(child, depth + 1);
////            }
//        }
//
//    }
//}
