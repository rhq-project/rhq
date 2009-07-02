package org.rhq.core.domain.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Query;

import org.rhq.core.domain.auth.Subject;

/**
 * A query generator used to generate queries with specific fetch join or sorting requirements,
 * Mainly built for the  generic find methods in local/remote EJBs
 * 
 * @author Asaf Shakarchi
 * @author Joseph Marques
 */
public class QueryGenerator {
    private enum EntityAnnotationsMethod {
        METHOD, FIELD
    }

    public EntityAnnotationsMethod entityAnnotationMethod = EntityAnnotationsMethod.FIELD;
    private Object criteriaObject;
    protected PageControl pageControl;
    protected Set<String> relationsToFetch;

    protected String alias;
    private static String NL = System.getProperty("line.separator");
    private String className;

    public QueryGenerator(Object criteriaObject, String[] relationsToFetch, PageControl pageControl) {
        this.criteriaObject = criteriaObject;

        if (relationsToFetch != null) {
            this.relationsToFetch = new HashSet<String>(Arrays.asList(relationsToFetch));
        }

        if (pageControl == null) {
            this.pageControl = PageControl.getUnlimitedInstance();
        } else {
            this.pageControl = pageControl;
        }

        className = criteriaObject.getClass().getName();

        StringBuilder aliasBuilder = new StringBuilder();
        for (char c : this.className.toCharArray()) {
            if (Character.isUpperCase(c)) {
                aliasBuilder.append(Character.toLowerCase(c));
            }
        }
        this.alias = aliasBuilder.toString();
    }

    public String getQueryString() throws Exception {
        StringBuilder results = new StringBuilder();
        results.append("SELECT ").append(alias).append(NL);
        results.append("FROM ").append(className).append(' ').append(alias).append(NL);
        for (String fetchJoin : relationsToFetch) {
            if (!isEntityCollectionPersistence(fetchJoin)) {
                throw new Exception("Can not fetchJoin '" + fetchJoin + "'.");
            }

            results.append("LEFT JOIN FETCH ").append(alias).append('.').append(fetchJoin).append(NL);
        }

        boolean firstCrit = true;
        Map<String, Object> critFields = getEntityPersistenceFields(criteriaObject);
        //criteria

        if (critFields.size() > 0)
            results.append("WHERE ");

        for (Map.Entry<String, Object> critField : critFields.entrySet()) {
            if (firstCrit) {
                firstCrit = false;
            } else {
                results.append(NL).append("AND ");
            }

            results.append(alias).append('.').append(critField.getKey() + " = :" + critField.getKey() + " ");
        }

        boolean first = true;
        for (OrderingField orderingField : pageControl.getOrderingFields()) {
            //verify persistency
            if (!isEntityFieldPersistence(orderingField.getField())) {
                throw new Exception("Can not order by '" + orderingField.getField() + "'.");
            }

            if (first) {
                results.append(NL).append("ORDER BY ");
                first = false;
            } else {
                results.append(", ");
            }
            results.append(alias).append('.').append(orderingField.getField());
            results.append(' ').append(orderingField.getOrdering());
        }

        return results.append(NL).toString();
    }

    public Query getQuery(EntityManager em) throws Exception {
        Query q = em.createQuery(getQueryString());

        return getQuery(q);
    }

    public Query getQuery(Query q) throws Exception {
        for (Map.Entry<String, Object> critField : getEntityPersistenceFields(criteriaObject).entrySet()) {
            q.setParameter(critField.getKey(), critField.getValue());
        }

        PersistenceUtility.setDataPage(q, pageControl);
        return q;
    }

    //helper
    private boolean isEntityCollectionPersistence(String fieldName) throws NoSuchMethodException, NoSuchFieldException {
        if (entityAnnotationMethod == EntityAnnotationsMethod.FIELD) {
            Field field = getFieldOfCriteriaClass(fieldName);
            return ((field.isAnnotationPresent(ManyToMany.class)) || (field.isAnnotationPresent(OneToMany.class)));
        } else if (entityAnnotationMethod == EntityAnnotationsMethod.METHOD) {
            Method method = getMethodOfCriteriaClass(getBeanGetterForProperty(fieldName));
            return ((method.isAnnotationPresent(ManyToMany.class)) || (method.isAnnotationPresent(OneToMany.class)));
        }

        return false;
    }

    private boolean isEntityFieldPersistence(String fieldName) throws NoSuchFieldException {
        Field field = getFieldOfCriteriaClass(fieldName);

        return isEntityFieldPersistence(field);
    }

    private boolean isEntityFieldPersistence(Field field) {
        //TODO: a little bit risk as column is not a must.
        return ((field.isAnnotationPresent(Column.class)));
    }

    private boolean isEntityMethodPersistence(Method method) {
        //TODO: a little bit risk as column is not a must.
        return ((method.isAnnotationPresent(Column.class)));
    }

    private boolean isEntity(Class<?> clazz) {
        return clazz.isAnnotationPresent(Entity.class);
    }

    private Map<String, Object> getEntityPersistenceFields(Object entityClass) throws Exception {
        if (!isEntity(entityClass.getClass())) {
            throw new Exception("The specified class is not an EJB3 persistence entity");
        }

        Map<String, Object> entityPersistenceProperties = new HashMap<String, Object>();

        if (entityAnnotationMethod == EntityAnnotationsMethod.FIELD) {
            for (Field currField : entityClass.getClass().getDeclaredFields()) {
                if (isEntityFieldPersistence(currField)) {
                    //get its value
                    currField.setAccessible(true);

                    Object fieldValue = currField.get(entityClass);
                    if ((fieldValue != null) && isEntityFieldPersistence(currField)) {
                        //if field is @id, make sure it's not 0 as most of the entities ID is a primitive int
                        if (currField.isAnnotationPresent(Id.class)) {
                            if ((Integer) fieldValue == 0) {
                                continue;
                            } else {
                                entityPersistenceProperties.put(currField.getName(), fieldValue);
                                continue;
                            }
                        }

                        //dirty hack but we have to filter primitives
                        if (currField.getType().isPrimitive()) {
                            continue;
                        }

                        entityPersistenceProperties.put(currField.getName(), fieldValue);
                    }
                }
            }
        } else if (entityAnnotationMethod == EntityAnnotationsMethod.METHOD) {
            for (Method currMethod : entityClass.getClass().getDeclaredMethods()) {
                if (currMethod.getName().startsWith("get")) {

                    if (isEntityMethodPersistence(currMethod)) {
                        //get its value

                        Object methodValue = currMethod.invoke(entityClass);
                        if ((methodValue != null) && isEntityMethodPersistence(currMethod)) {
                            //if field is @id, make sure it's not 0 as most of the entities ID is a primitive int
                            if (currMethod.isAnnotationPresent(Id.class)) {
                                if ((Integer) methodValue == 0) {
                                    continue;
                                } else {
                                    entityPersistenceProperties.put(getBeanPropertyOfGetter(currMethod.getName()),
                                        methodValue);
                                    continue;
                                }
                            }

                            //dirty hack but we have to filter primitives
                            if (currMethod.getReturnType().isPrimitive()) {
                                continue;
                            }

                            entityPersistenceProperties.put(getBeanPropertyOfGetter(currMethod.getName()), methodValue);
                        }
                    }

                }
            }
        }

        return entityPersistenceProperties;
    }

    private String getBeanPropertyOfGetter(String property) {
        return property.substring(3, 4).toLowerCase() + property.substring(4, property.length());
    }

    private String getBeanGetterForProperty(String property) {
        return "get" + property.substring(0, 1).toUpperCase() + property.substring(1, property.length());
    }

    private Method getMethodOfCriteriaClass(String methodName) throws NoSuchMethodException {
        try {
            Method method = criteriaObject.getClass().getDeclaredMethod(methodName);
            return method;
        } catch (NoSuchMethodException e) {
            throw e;
        }
    }

    private Field getFieldOfCriteriaClass(String fieldName) throws NoSuchFieldException {
        try {
            Field field = criteriaObject.getClass().getDeclaredField(fieldName);
            return field;
        } catch (NoSuchFieldException e) {
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        PageControl pc = PageControl.getUnlimitedInstance();
        pc.addDefaultOrderingField("firstName", PageOrdering.ASC);
        pc.addDefaultOrderingField("lastName", PageOrdering.DESC);
        String[] rtf = new String[] { "roles", "subjectNotifications" };

        Subject s = new Subject();
        s.setId(4);
        s.setFirstName("Asaf");
        s.setLastName("Sh");
        s.setSessionId(4);

        QueryGenerator a = new QueryGenerator(s, rtf, pc);

        System.out.println(a.getQueryString());
    }
}