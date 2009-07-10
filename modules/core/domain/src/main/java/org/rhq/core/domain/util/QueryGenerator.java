package org.rhq.core.domain.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Query;

import org.rhq.core.domain.alert.AlertDefinition;

/**
 * A query generator used to generate queries with specific fetch join or sorting requirements,
 * Mainly built for the  generic find methods in local/remote EJBs
 * 
 * @author Asaf Shakarchi
 * @author Joseph Marques
 */
public final class QueryGenerator {
    public enum AuthorizationTokenType {
        RESOURCE, // specifies the resource alias to join on for standard res-group-role-subject authorization checking 
        GROUP; // specifies the group alias to join on for standard group-role-subject authorization checking
    }

    private static final String argPrefix = ":arg";
    private int argumentCounter = 0;
    private List<String> filterExpressions = new ArrayList<String>();
    private List<Object> filterValues = new ArrayList<Object>();

    private Object criteriaObject;
    private PageControl pageControl;
    private Set<String> relationsToFetch;

    private String authorizationJoinFragment;
    private int authorizationSubjectId;

    private String alias;
    private String className;
    private static String NL = System.getProperty("line.separator");

    public QueryGenerator(Object criteriaObject, PageControl pageControl) {
        this.criteriaObject = criteriaObject;

        String[] relationsToFetch = pageControl.getOptionalData();
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

    public void setAuthorizationResourceFragment(AuthorizationTokenType type, int subjectId) {
        String defaultFragment = null;
        if (type == AuthorizationTokenType.RESOURCE) {
            defaultFragment = "resource";
        } else if (type == AuthorizationTokenType.GROUP) {
            defaultFragment = "group";
        }
        setAuthorizationResourceFragment(type, defaultFragment, subjectId);
    }

    /**
     * Supports and arbitrarily style for adding more WHERE conditions. Use a question mark as a 
     * place holder for where you want values to be inserted.  You should include as many values
     * as you do question marks in the expression.  For example, if your criteria object contained
     * a field called 'grams', the call to this method might look like:
     * 
     *    addFilter("grams between ? and ?", 100, 1000); 
     * 
     * @param propertyExpression an expression contained question marks that should be parameter
     *                           replaced during query generation time
     * @param values             a list of objects that should replace the question marks in the
     *                           propertyExpression during query generation
     */
    public void addFilter(String expression, Object... values) {
        int argumentsFound = 0;
        while (expression.indexOf('?') != -1) {
            expression = expression.replaceFirst("\\?", argPrefix + String.valueOf(argumentCounter++));
            argumentsFound++;
        }
        if (argumentsFound != values.length) {
            throw new IllegalArgumentException("Passed an expression with " + argumentsFound
                + " placeholders, but provided " + values.length + " arguments");
        }

        filterExpressions.add(this.alias + "." + expression);
        filterValues.addAll(Arrays.asList(values));
    }

    public void setAuthorizationResourceFragment(AuthorizationTokenType type, String fragment, int subjectId) {
        this.authorizationSubjectId = subjectId;
        if (type == AuthorizationTokenType.RESOURCE) {
            this.authorizationJoinFragment = "" //
                + "JOIN " + alias + "." + fragment + " authRes " + NL // 
                + "JOIN authRes.implicitGroup authGroup " + NL //
                + "JOIN authGroup.roles authRole " + NL //
                + "JOIN authRole.subject authSubject " + NL;
        } else if (type == AuthorizationTokenType.GROUP) {
            this.authorizationJoinFragment = "" //
                + "JOIN " + alias + "." + fragment + " authGroup " + NL //
                + "JOIN authGroup.roles authRole " + NL //
                + "JOIN authRole.subject authSubject " + NL;
        } else {
            throw new IllegalArgumentException(this.getClass().getSimpleName()
                + " does not yet support generating queries for '" + type + "' token types");
        }
    }

    // for testing purposes only, should use getQuery(EntityManager) or getCountQuery(EntityManager) instead
    public String getQueryString(boolean countQuery) {
        StringBuilder results = new StringBuilder();
        results.append("SELECT ");
        if (countQuery) {
            results.append("COUNT(").append(alias).append(")").append(NL);
        } else {
            results.append(alias).append(NL);
        }
        results.append("FROM ").append(className).append(' ').append(alias).append(NL);
        for (String fetchJoin : relationsToFetch) {
            Field field = null;
            try {
                field = getFieldOfCriteriaClass(fetchJoin);
            } catch (NoSuchFieldException nsfe) {
                throw new IllegalArgumentException("Can not fetchJoin non-existent relationship '" + fetchJoin + "'");
            }
            if (!isEntityRelationshipPersistence(field)) {
                throw new IllegalArgumentException("Relationship '" + fetchJoin + "' is not available for fetchJoin");
            }

            results.append("LEFT JOIN FETCH ").append(alias).append('.').append(fetchJoin).append(NL);
        }
        if (authorizationJoinFragment != null) {
            results.append(authorizationJoinFragment);
        }

        Map<String, Object> critFields = getEntityPersistenceFields(criteriaObject);
        if (critFields.size() > 0 || filterExpressions.size() > 0 || authorizationJoinFragment != null) {
            results.append("WHERE ");
        }

        // criteria
        boolean firstCrit = true;
        for (Map.Entry<String, Object> critField : critFields.entrySet()) {
            if (firstCrit) {
                firstCrit = false;
            } else {
                results.append(NL).append("AND ");
            }
            results.append(alias).append('.').append(critField.getKey() + " = :" + critField.getKey() + " ");
        }

        // custom filters
        for (String filter : filterExpressions) {
            if (firstCrit) {
                firstCrit = false;
            } else {
                results.append(NL).append("AND ");
            }
            results.append(filter);
        }

        // authorization
        if (firstCrit) {
            firstCrit = false;
        } else {
            results.append(NL).append("AND ");
        }
        results.append("authSubject.id = " + authorizationSubjectId + " ");

        if (countQuery == false) {
            boolean first = true;
            for (OrderingField orderingField : pageControl.getOrderingFields()) {
                //verify persistency
                String fieldName = orderingField.getField();
                Field field = null;
                try {
                    field = getFieldOfCriteriaClass(fieldName);
                } catch (NoSuchFieldException nsfe) {
                    throw new IllegalArgumentException("Can not order results by non-existent field '" + fieldName
                        + "'");
                }
                if (!isEntityFieldPersistence(field)) {
                    throw new IllegalArgumentException("Field '" + fieldName
                        + "' is not available for results ordering");
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
        }

        return results.append(NL).toString();
    }

    public Query getQuery(EntityManager em) {
        String queryString = getQueryString(false);
        Query query = em.createQuery(queryString);
        setBindValues(query, false);
        PersistenceUtility.setDataPage(query, pageControl);
        return query;
    }

    public Query getCountQuery(EntityManager em) {
        String countQueryString = getQueryString(true);
        Query query = em.createQuery(countQueryString);
        setBindValues(query, false);
        return query;
    }

    private void setBindValues(Query query, boolean countQuery) {
        for (Map.Entry<String, Object> critField : getEntityPersistenceFields(criteriaObject).entrySet()) {
            query.setParameter(critField.getKey(), critField.getValue());
        }
        for (int i = 0; i < filterValues.size(); i++) {
            String argumentName = argPrefix + i;
            query.setParameter(argumentName, filterValues.get(i));
        }
    }

    private boolean isEntityRelationshipPersistence(Field field) {
        return (field.isAnnotationPresent(ManyToMany.class) || field.isAnnotationPresent(OneToMany.class) || field
            .isAnnotationPresent(ManyToOne.class));
    }

    private boolean isEntityFieldPersistence(Field field) {
        //TODO: a little bit risk as column is not a must.
        return ((field.isAnnotationPresent(Column.class)));
    }

    private Map<String, Object> getEntityPersistenceFields(Object entityClass) {
        if (!entityClass.getClass().isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("The specified class is not an EJB3 persistence entity");
        }

        Map<String, Object> entityPersistenceProperties = new HashMap<String, Object>();

        for (Field currField : entityClass.getClass().getDeclaredFields()) {
            if (isEntityFieldPersistence(currField)) {
                //get its value
                currField.setAccessible(true);

                Object fieldValue = null;
                try {
                    fieldValue = currField.get(entityClass);
                } catch (IllegalAccessException iae) {
                    throw new RuntimeException(iae);
                }
                if ((fieldValue != null)) {
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

        return entityPersistenceProperties;
    }

    private Field getFieldOfCriteriaClass(String fieldName) throws NoSuchFieldException {
        Field field = criteriaObject.getClass().getDeclaredField(fieldName);
        return field;
    }

    public static void main(String[] args) {
        PageControl pc = PageControl.getUnlimitedInstance();
        pc.addDefaultOrderingField("priority", PageOrdering.DESC);
        pc.addDefaultOrderingField("name", PageOrdering.ASC);
        pc.setOptionalData(new String[] { "resourceType" });

        AlertDefinition ad = new AlertDefinition();
        ad.setId(4);
        ad.setName("JBoss");

        QueryGenerator generator = new QueryGenerator(ad, pc);

        System.out.println(generator.getQueryString(false));
        System.out.println(generator.getQueryString(true));

        generator.setAuthorizationResourceFragment(AuthorizationTokenType.RESOURCE, 1);

        System.out.println(generator.getQueryString(false));
        System.out.println(generator.getQueryString(true));

        generator.addFilter("mtime between ? and ?", 0, 1);

        System.out.println(generator.getQueryString(false));
        System.out.println(generator.getQueryString(true));
    }
}