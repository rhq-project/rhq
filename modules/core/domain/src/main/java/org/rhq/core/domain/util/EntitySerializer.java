package org.rhq.core.domain.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.serial.ExternalizableStrategy;
import org.rhq.core.domain.util.serial.ExternalizableStrategy.Subsystem;

public class EntitySerializer {
    private static Set<Class<? extends Annotation>> PERSISTENCE_ANNOTATIONS = new HashSet<Class<? extends Annotation>>();
    static {
        PERSISTENCE_ANNOTATIONS.add(Column.class);
        PERSISTENCE_ANNOTATIONS.add(ManyToOne.class);
        PERSISTENCE_ANNOTATIONS.add(OneToMany.class);
        PERSISTENCE_ANNOTATIONS.add(ManyToMany.class);
    }

    private static Set<Class<?>> BASIC_TYPES = new HashSet<Class<?>>();
    static {
        BASIC_TYPES.add(Byte.TYPE);
        BASIC_TYPES.add(Short.TYPE);
        BASIC_TYPES.add(Integer.TYPE);
        BASIC_TYPES.add(Long.TYPE);
        BASIC_TYPES.add(Float.TYPE);
        BASIC_TYPES.add(Double.TYPE);
        BASIC_TYPES.add(Boolean.TYPE);
    }

    private static Comparator<Field> fieldComparator = new Comparator<Field>() {
        public int compare(Field first, Field second) {
            return first.getName().compareTo(second.getName());
        }
    };

    private static Field[] getEntityFields(Object entity) {
        Class<?> entityClass = entity.getClass();
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        if (entityAnnotation == null) {
            throw new IllegalArgumentException("EntitySerializer only introspects objects annotated with @Entity ");
        }

        Field[] fields = entityClass.getDeclaredFields();
        List<Field> serializableFields = new ArrayList<Field>();

        for (Field field : fields) {
            Annotation[] fieldAnnotations = field.getAnnotations();
            for (Annotation fieldAnnotation : fieldAnnotations) {
                if (PERSISTENCE_ANNOTATIONS.contains(fieldAnnotation.annotationType())) {
                    serializableFields.add(field);
                    field.setAccessible(true);
                    break;
                }
            }
        }

        Collections.sort(serializableFields, fieldComparator);

        Field[] results = serializableFields.toArray(new Field[serializableFields.size()]);
        return results;
    }

    public static void writeExternalRemote(Object entity, ObjectOutput out) throws IOException {
        Field[] entityFields = getEntityFields(entity);
        for (Field field : entityFields) {
            //System.out.println("Serializing " + field.getName() + "...");
            try {
                Class<?> type = field.getType();
                Object value = field.get(entity);

                if (BASIC_TYPES.contains(type)) {
                    if (type.equals(Byte.TYPE)) {
                        out.writeByte((Byte) value);
                    } else if (type.equals(Short.TYPE)) {
                        out.writeShort((Short) value);
                    } else if (type.equals(Integer.TYPE)) {
                        out.writeInt((Integer) value);
                    } else if (type.equals(Long.TYPE)) {
                        out.writeLong((Long) value);
                    } else if (type.equals(Float.TYPE)) {
                        out.writeFloat((Float) value);
                    } else if (type.equals(Double.TYPE)) {
                        out.writeDouble((Double) value);
                    } else if (type.equals(Boolean.TYPE)) {
                        out.writeBoolean((Boolean) value);
                    } else {
                        throw new IllegalStateException(
                            "BASIC_TYPES contains an entry that doesn't have serialization support: " + type);
                    }
                    continue;
                }

                // either a string, an enum, numeric wrapper, collection, or some other object
                out.writeObject(value);
            } catch (IllegalAccessException iae) {
                throw new IllegalStateException("Could not access field '" + field.getName() + "' for serialization");
            }
        }
    }

    public static void readExternalRemote(Object entity, ObjectInput in) throws IOException, ClassNotFoundException {
        Field[] entityFields = getEntityFields(entity);
        for (Field field : entityFields) {
            //System.out.println("Deserializing " + field.getName() + "...");
            try {
                Class<?> type = field.getType();

                if (BASIC_TYPES.contains(type)) {
                    if (type.equals(Byte.TYPE)) {
                        field.setByte(entity, in.readByte());
                    } else if (type.equals(Short.TYPE)) {
                        field.setShort(entity, in.readShort());
                    } else if (type.equals(Integer.TYPE)) {
                        field.setInt(entity, in.readInt());
                    } else if (type.equals(Long.TYPE)) {
                        field.setLong(entity, in.readLong());
                    } else if (type.equals(Float.TYPE)) {
                        field.setFloat(entity, in.readFloat());
                    } else if (type.equals(Double.TYPE)) {
                        field.setDouble(entity, in.readDouble());
                    } else if (type.equals(Boolean.TYPE)) {
                        field.setBoolean(entity, in.readBoolean());
                    } else {
                        throw new IllegalStateException(
                            "BASIC_TYPES contains an entry that doesn't have serialization support: " + type);
                    }
                    continue;
                }

                // either a string, an enum, numeric wrapper, collection, or some other object
                field.set(entity, in.readObject());
            } catch (IllegalAccessException iae) {
                throw new IllegalStateException("Could not access field '" + field.getName() + "' for serialization");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ExternalizableStrategy.setStrategy(Subsystem.REFLECTIVE_SERIALIZATION);

        // create objects
        Agent writeAgent = new Agent("reflectiveAgent", "reflectiveAddress", 0, "reflectiveEndpoint", "reflectiveToken");

        ResourceType writeResourceType = new ResourceType();
        writeResourceType.setName("reflectiveType");
        writeResourceType.setPlugin("reflectivePlugin");
        writeResourceType.setId(7);

        Resource writeParentResource = new Resource();
        writeParentResource.setId(11);
        writeParentResource.setName("reflectiveParentResource");
        writeParentResource.setResourceKey("reflectiveParentKey");

        Resource writeResource = new Resource();
        writeResource.setId(42);
        writeResource.setName("reflectiveResource");
        writeResource.setResourceKey("reflectiveKey");

        // setup relationships
        writeResource.setAgent(writeAgent);
        writeResource.setResourceType(writeResourceType);
        writeResource.setParentResource(writeParentResource);

        System.out.println("BEFORE");
        System.out.println(writeResource.toString());
        System.out.println("BEFORE");

        ObjectOutput output = new ObjectOutputStream(new FileOutputStream("S:\\test.txt"));
        writeExternalRemote(writeResource, output);
        output.close();

        Resource readResource = new Resource();
        ObjectInput input = new ObjectInputStream(new FileInputStream("S:\\test.txt"));
        readExternalRemote(readResource, input);
        input.close();

        // quick verification
        System.out.println("AFTER");
        System.out.println(readResource.toString());
        System.out.println("AFTER");

        // deeper verification
        boolean equalsResource = writeResource.equals(readResource);
        boolean equalsParentResource = writeParentResource.equals(readResource.getParentResource());
        boolean equalsResourceType = writeResourceType.equals(readResource.getResourceType());
        boolean equalsAgent = writeAgent.equals(readResource.getAgent());

        System.out.println("equalsResource: " + equalsResource);
        System.out.println("equalsParentResource: " + equalsParentResource);
        System.out.println("equalsResourceType: " + equalsResourceType);
        System.out.println("equalsAgent: " + equalsAgent);
    }

}
