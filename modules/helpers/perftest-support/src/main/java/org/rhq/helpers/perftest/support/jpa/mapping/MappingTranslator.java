/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.jpa.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.rhq.helpers.perftest.support.jpa.Edge;
import org.rhq.helpers.perftest.support.jpa.JPAUtil;
import org.rhq.helpers.perftest.support.jpa.Node;


/**
 * Translates the {@link Node} and its {@link Edge}s into table and column names.
 * 
 * @author Lukas Krejci
 */
public class MappingTranslator {
    private static class AnnotatedField {
        private Field field;
        private Annotation[] annotations;

        public AnnotatedField(Field f) {
            field = f;
            annotations = new Annotation[f.getAnnotations().length];
            System.arraycopy(f.getAnnotations(), 0, annotations, 0, annotations.length);
        }

        public void addAnnotations(Annotation[] annotations) {
            Annotation[] thisAnnotations = this.annotations;

            this.annotations = new Annotation[this.annotations.length + annotations.length];
            System.arraycopy(thisAnnotations, 0, this.annotations, 0, thisAnnotations.length);
            System.arraycopy(annotations, 0, this.annotations, thisAnnotations.length, annotations.length);
        }

        public Annotation[] getAnnotations() {
            return annotations;
        }

        public <T extends Annotation> T getAnnotation(Class<T> type) {
            for (int i = 0; i < annotations.length; ++i) {
                if (annotations[i].annotationType().equals(type)) {
                    return type.cast(annotations[i]);
                }
            }

            return null;
        }

        public String getName() {
            return field.getName();
        }
    }

    public EntityTranslation translate(Node node) {
        EntityTranslation translation = new EntityTranslation();

        translation.setTableName(getTableName(node));
        translation.setPkColumns(getPkColumns(node));
        
        return translation;
    }
    
    public RelationshipTranslation translate(Edge edge) {
        switch (edge.getDependencyType()) {
        case ONE_TO_ONE:
            return analyzeOneToOne(edge);
        case ONE_TO_MANY:
            return analyzeOneToMany(edge);
        case MANY_TO_MANY:
            return analyzeManyToMany(edge);
        default:
            return null;
        }
    }


    private static String getTableName(Node node) {
        return getTableName(node.getEntity());
    }

    private static String[] getPkColumns(Node node) {
        Set<AnnotatedField> pkFields = getIdFields(node.getEntity());

        if (pkFields.isEmpty()) {
            return null;
        }

        String[] columns = new String[pkFields.size()];
        int idx = 0;
        for (AnnotatedField f : pkFields) {
            columns[idx] = getColumnName(f);
            if (columns[idx] == null) {
                //check for the special case, @Id with @JoinColumn and @ManyToOne
                ManyToOne manyToOne = f.getAnnotation(ManyToOne.class);
                if (manyToOne == null) {
                    columns[idx] = f.getName().toUpperCase();
                } else {
                    JoinColumn joinColumn = f.getAnnotation(JoinColumn.class);
                    columns[idx] = joinColumn.name();
                    if (columns[idx].isEmpty()) {
                        columns[idx] = f.getName().toUpperCase();
                    }
                }
            }
            idx++;
        }
        return columns;
    }

    private static String getColumnName(Field field) {
        if (field == null) {
            return null;
        }

        Column colSpec = field.getAnnotation(Column.class);

        if (colSpec != null && !colSpec.name().isEmpty()) {
            return colSpec.name();
        } else {
            return null;
        }
    }

    private static List<JoinColumn> getJoinColumns(Field field) {
        if (field == null) {
            return null;
        }
        
        JoinColumn colSpec = field.getAnnotation(JoinColumn.class);
        
        if (colSpec != null) {
            //a single join column specified
            return Collections.singletonList(colSpec);
        } else {
            //see, if there are more join cols
            JoinColumns joinColumns = field.getAnnotation(JoinColumns.class);
            if (joinColumns != null) {
                JoinColumn[] cols = joinColumns.value();
                List<JoinColumn> ret = new ArrayList<JoinColumn>();
                
                for(int i = 0; i < cols.length; ++i) {
                    ret.add(cols[i]);
                }
                
                return ret;
            }
        }
        
        return null;
    }
    
    private static String[] referencedJoinColumnNames(Field field) {
        if (field == null) {
            return null;
        }
        
        JoinColumn colSpec = field.getAnnotation(JoinColumn.class);
        
        if (colSpec != null) {
            //a single join column specified
            return new String[] { colSpec.referencedColumnName().toUpperCase() };
        } else {
            //see, if there are more join cols
            JoinColumns joinColumns = field.getAnnotation(JoinColumns.class);
            if (joinColumns != null) {
                JoinColumn[] cols = joinColumns.value();
                String[] ret = new String[cols.length];
                
                for(int i = 0; i < cols.length; ++i) {
                    ret[i] = cols[i].referencedColumnName().toUpperCase();
                }
                
                return ret;
            }
        }
        
        return null;
    }
    
    private static String getColumnName(AnnotatedField field) {
        if (field == null) {
            return null;
        }

        Column colSpec = field.getAnnotation(Column.class);

        if (colSpec != null && !colSpec.name().isEmpty()) {
            return colSpec.name().toUpperCase();
        } else {
            return null;
        }
    }
    
    public static String getTableName(Class<?> entity) {
        Table tableAnnotation = entity.getAnnotation(Table.class);
        if (tableAnnotation != null) {
            String name = tableAnnotation.name();
            if (name.isEmpty()) {
                name = entity.getSimpleName();
            }

            return name.toUpperCase();
        } else {
            DiscriminatorValue discriminatorValueAnnotation = entity.getAnnotation(DiscriminatorValue.class);
            if (discriminatorValueAnnotation != null) {
                return getTableName(entity.getSuperclass());
            }
        }

        return null;
    } 
    
    public static Set<AnnotatedField> getIdFields(Class<?> entity) {
        //we have 3 ways of defining ids of an entity
        //1) single @Id 
        //2) @IdClass and multiple @Id fields
        //3) these rules applied recursively in @Embedded fields

        //3 is handled implicitly by JPAUtil

        Set<Field> idFields = JPAUtil.getJPAFields(entity, Id.class);

        if (idFields.size() == 0) {
            return null;
        } else if (idFields.size() == 1) {
            return Collections.singleton(new AnnotatedField(idFields.iterator().next()));
        } else {
            //@IdClass
            Class<?> idClass = entity.getAnnotation(IdClass.class).value();

            Set<AnnotatedField> ret = new HashSet<AnnotatedField>();
            for (Field f : idFields) {
                AnnotatedField af = new AnnotatedField(f);

                try {
                    Field idF = idClass.getDeclaredField(f.getName());
                    af.addAnnotations(idF.getAnnotations());
                } catch (Exception e) {
                }

                ret.add(af);
            }

            return ret;
        }
    }   
    
    private static RelationshipTranslation analyzeOneToOne(Edge relationship) {
        Field fromField = relationship.getFromField();
        Field toField = relationship.getToField();

        RelationshipTranslation translation = new RelationshipTranslation();

        List<JoinColumn> joins = getJoinColumns(fromField);

        String[] fCols = new String[joins.size()];
        String[] tCols = new String[joins.size()];
        
        int i = 0;
        for(JoinColumn c : joins) {
            String fkey = c.name().toUpperCase();
            String refCol = c.referencedColumnName().toUpperCase();
            
            //determine whether we have the foreign key in the from or to table
            if (toField == null) {
                //unidirectional mapping, the fkey is in the from table
                fCols[i] = fkey;
                if (!refCol.isEmpty()) {
                    tCols[i] = refCol;
                }
            } else if (refCol.isEmpty()) {
                //bidirectional with no referenced column definition.
                //the fkey is in the from table, referencing the pk in the target table
                fCols[i] = fkey;
            } else {
                //bidirectional with referenced column definition.
                //the fkey is in the from table, referencing the refCol
                fCols[i] = fkey;
                tCols[i] = refCol;
            }
            ++i;
        }
        
        //now fill in the empty fCols and tCols with the corresponding primary keys of the to table
        String[] fromPks = relationship.getFrom().getTranslation().getPkColumns();
        for(i = 0; i < fCols.length; ++i) {
            if (fCols[i] == null) {
                fCols[i] = fromPks[i];
            }
        }

        String[] toPks = relationship.getTo().getTranslation().getPkColumns();
        for(i = 0; i < tCols.length; ++i) {
            if (tCols[i] == null) {
                tCols[i] = toPks[i];
            }
        }
        
        translation.setFromColumns(fCols);
        translation.setToColumns(tCols);

        return translation;
    }  
    
    private static RelationshipTranslation analyzeOneToMany(Edge relationship) {
        Field fromField = relationship.getFromField();
        Field toField = relationship.getToField();

        RelationshipTranslation translation = new RelationshipTranslation();

        if (toField == null) {
            JoinTable t = fromField.getAnnotation(JoinTable.class);
            JoinColumn c = fromField.getAnnotation(JoinColumn.class);
            if (t != null) {
                analyzeJoinTable(translation, t);
            } else if (c != null) {
                //join column represents the column in the target table.
                String fromColumn = getColumnName(fromField);
                if (fromColumn == null) {
                    translation.setFromColumns(relationship.getFrom().getTranslation().getPkColumns());
                } else {
                    translation.setFromColumns(new String[] { fromColumn });
                }

                translation.setToColumns(joinColumnNames(new JoinColumn[] { c }));
            } else {
                throw new IllegalArgumentException("Default mappings on @OneToMany not implemented.");
            }

        } else {
            String fromColumn = getColumnName(fromField);
            if (fromColumn == null) {
                translation.setFromColumns(relationship.getFrom().getTranslation().getPkColumns());
            } else {
                translation.setFromColumns(new String[] { fromColumn });
            }

            String toColumn = getColumnName(toField);
            if (toColumn == null) {
                AnnotatedField fullField = getFieldWithFullAnnotations(toField);
                JoinColumn joinColumn = fullField.getAnnotation(JoinColumn.class);
                if (joinColumn == null) {
                    translation.setToColumns(relationship.getTo().getTranslation().getPkColumns());
                } else {
                    translation.setToColumns(joinColumnNames(new JoinColumn[] { joinColumn }));
                }
            } else {
                translation.setToColumns(new String[] { toColumn });
            }
        }

        return translation;
    }    

    private static RelationshipTranslation analyzeManyToMany(Edge relationship) {
        Field fromField = relationship.getFromField();
        Field toField = relationship.getToField();

        JoinTable t = fromField.getAnnotation(JoinTable.class);

        if (t == null) {
            t = toField.getAnnotation(JoinTable.class);
        }

        if (t == null) {
            throw new IllegalStateException("Default values for a @JoinTable are not supported.");
        }

        RelationshipTranslation translation = new RelationshipTranslation();

        analyzeJoinTable(translation, t);

        return translation;
    }
    
    private static void analyzeJoinTable(RelationshipTranslation translation, JoinTable joinTable) {
        JoinColumn[] joinCols = joinTable.joinColumns();
        JoinColumn[] inverseCols = joinTable.inverseJoinColumns();
        String tableName = joinTable.name().toUpperCase();

        translation.setRelationTable(tableName);
        translation.setRelationTableFromColumns(joinColumnNames(joinCols));
        translation.setRelationTableToColumns(joinColumnNames(inverseCols));

        translation.setFromColumns(new String[joinCols.length]);
        translation.setToColumns(new String[inverseCols.length]);

        updateWithJoinColumnReferencedNames(translation.getFromColumns(), joinCols);
        updateWithJoinColumnReferencedNames(translation.getToColumns(), inverseCols);
    }

    private static String[] joinColumnNames(JoinColumn[] columns) {
        String[] ret = new String[columns.length];

        for (int i = 0; i < columns.length; ++i) {
            ret[i] = columns[i].name().toUpperCase();
        }

        return ret;
    }

    private static void updateWithJoinColumnReferencedNames(String[] names, JoinColumn[] columns) {
        if (names.length != columns.length) {
            return;
        }

        for (int i = 0; i < columns.length; ++i) {
            if (!columns[i].referencedColumnName().isEmpty()) {
                names[i] = columns[i].referencedColumnName().toUpperCase();
            }
        }
    }
    
    /**
     * This method returns something else than just wrapped f only in case
     * when f is a part of composite id defined by an {@link IdClass @IdClass}.
     * In that case the returned annotated field contains annotations defined on
     * both the field itself and the corresponding field from the id class.
     * 
     * @param f
     * @return
     */
    private static AnnotatedField getFieldWithFullAnnotations(Field f) {
        AnnotatedField ret = new AnnotatedField(f);

        IdClass idClass = f.getDeclaringClass().getAnnotation(IdClass.class);
        if (idClass != null) {
            try {
                Field correspondingIdField = idClass.value().getDeclaredField(f.getName());
                ret.addAnnotations(correspondingIdField.getAnnotations());
            } catch (Exception e) {
            }
        }

        return ret;
    }    
}
