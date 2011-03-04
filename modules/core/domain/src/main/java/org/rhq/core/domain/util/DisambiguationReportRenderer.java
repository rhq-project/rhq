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

package org.rhq.core.domain.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.DisambiguationReport.Resource;
import org.rhq.core.domain.resource.composite.DisambiguationReport.ResourceType;

/**
 * This class can be used to produce a "pretty" textual representation of a {@link DisambiguationReport}.
 * It supports a simple templating mechanism to configure the representation's format.
 * <p>
 * In the {@link DisambiguationReport}, a resource (either the resource being disambiguated or one of its parents)
 * is represented by an instance of {@link DisambiguationReport.Resource}. The template uses this instance and its
 * properties:
 * <ul>
 * <li>id - the resource id
 * <li>name - the name of the resource
 * <li>type - the type of the resource
 * <ul>
 * <li>name - the name of the resource type
 * <li>plugin - the name of the plugin defining the resource type
 * <li>singleton - a boolean whether the resource type is a singleton type or not
 * </ul>
 * </li>
 * </ul>
 * <p>
 * The template has the following format:<br/>
 * <code>...text...%([..text...])?&lt;identifier&gt;([...text...])?...text...</code>
 * <p>
 * It is best explained by an example. The {@link #DEFAULT_SEGMENT_TEMPLATE default segment template} looks like this:<br/>
 * <code>%type.name[ ]%[(]type.plugin[) ]%name</code>
 * <p>
 * The <code>%type.name[ ]</code> is rendered as the name of the resource type followed by a space <b>IF</b> the
 * resource type and the name are not null. If either of them is null <code>%type.name[ ]</code> renders as an
 * empty string.
 * <p>
 * <code>%[(]type.plugin[) ]</code> renders as a bracket followed by the name of the plugin followed by a bracket and
 * a space if both resource type and plugin are not null. If either of them is null, again the whole <code>%...</code> is 
 * rendered as an empty string.
 * <p>
 * The escape character is \.
 * 
 * @author Lukas Krejci
 */
public class DisambiguationReportRenderer {

    public enum RenderingOrder {
        ASCENDING, DESCENDING
    }

    public static final String DEFAULT_SEGMENT_TEMPLATE = "%type.name[ ]%[(]type.plugin[) ]%name";
    public static final String DEFALUT_SEGMENT_SEPARATOR = " > ";

    private boolean includeResource = true;
    private boolean includeParents = true;
    private RenderingOrder renderingOrder = RenderingOrder.ASCENDING;
    private String segmentTemplate = DEFAULT_SEGMENT_TEMPLATE;
    private String singletonSegmentTemplate = DEFAULT_SEGMENT_TEMPLATE;
    private String segmentSeparator = DEFALUT_SEGMENT_SEPARATOR;
    private List<Segment> parsedTemplate = null;
    private List<Segment> parsedSingletonTemplate = null;
    
    /*
     * The Field, ResourceField and ResourceTypeField constructs are here to
     * prevent the usage of java reflection. This class is exposed (and probably
     * used) in the GWT javascript user interface which does not support reflection.
     * 
     * The below 3 classes are therefore here to mimic what we'd need from reflection
     * at the cost of lost flexibility and additional verbosity. 
     * 
     * If the DisambiguationReport.Resource or DisambiguationReport.ResourceType classes
     * change, the below enums will have to accomodate for that change. If we could use
     * reflection, it would work generically..
     */
    
    private interface Field {
        Object getValue(Object object);
        
        Field getRepresentation();
        
        Field getSiblingField(String name);
    }
    
    private enum ResourceField implements Field {
        ANY("") {
            public Object getValue(Object object) {
                return null;
            }
            
            public Field getRepresentation() {
                return null;
            }
        },
        ID("id") {
            public Object getValue(Object object) {
                return ((Resource)object).getId();
            }
            
            public Field getRepresentation() {
                return null;
            }
        }, 
        NAME("name") {
            public Object getValue(Object object) {
                return ((Resource)object).getName();
            }
            
            public Field getRepresentation() {
                return null;
            }
            
        }, 
        TYPE("type") {
            public Object getValue(Object object) {
                return ((Resource)object).getType();
            }
            
            public Field getRepresentation() {
                return ResourceTypeField.ANY;
            }            
        };
        
        private String name;
        
        private ResourceField(String name) {
            this.name = name;
        }      
        
        public Field getSiblingField(String name) {
            return getByName(name);
        }
        
        public static ResourceField getByName(String name) {
            for(ResourceField f : ResourceField.values()) {
                if (f.name.equals(name)) {
                    return f;
                }
            }
            
            return null;
        }
    }
    
    private enum ResourceTypeField implements Field {
        ANY("") {
            public Field getRepresentation() {
                return null;
            }
            
            public Object getValue(Object object) {
                return null;
            }            
        }, 
        NAME("name") {
            public Field getRepresentation() {
                return null;
            }
            
            public Object getValue(Object object) {
                return ((ResourceType)object).getName();
            }            
        }, 
        PLUGIN("plugin") {
            public Field getRepresentation() {
                return null;
            }
            
            public Object getValue(Object object) {
                return ((ResourceType)object).getPlugin();
            }            
        }, 
        SINGLETON("singleton") {
            public Field getRepresentation() {
                return null;
            }
            
            public Object getValue(Object object) {
                return ((ResourceType)object).isSingleton();
            }                        
        };
        
        private String name;
        
        private ResourceTypeField(String name) {
            this.name = name;
        }

        public Field getSiblingField(String name) {
            return getByName(name);
        }
        
        public static ResourceTypeField getByName(String name) {
            for(ResourceTypeField f : ResourceTypeField.values()) {
                if (f.name.equals(name)) {
                    return f;
                }
            }
            
            return null;
        }
    }
    
    /**
     * Segment represents a part of the template with some semantics.
     * A segment is able to render itself using data from a resource.
     * 
     * @author Lukas Krejci
     */
    private interface Segment {
        void render(DisambiguationReport.Resource resource, StringBuilder bld);
       
        /**
         * Each segment holds an internal string builder to build up the data 
         * from the template it will later need to render itself.
         * 
         * @return the internal string builder.
         */
        StringBuilder getCurrentString();
    }
    
    private static class TextSegment implements Segment {
        public StringBuilder text = new StringBuilder();
        
        public void render(DisambiguationReport.Resource resource, StringBuilder bld) {
            if (text != null) {                
                bld.append(text);
            }
        }
        
        public StringBuilder getCurrentString() {
            return text;
        }
    }
    private static class ResourceSegment implements Segment {
        public String prefix;
        public List<Field> fields = new ArrayList<Field>();
        public String suffix;
        
        public StringBuilder currentString = new StringBuilder();
        public Field currentField = ResourceField.ANY;
        
        public StringBuilder getCurrentString() {
            return currentString;
        }
        
        public void render(DisambiguationReport.Resource resource, StringBuilder bld) {
            String value = null;
            if (fields != null && fields.size() > 0) {
                Object object = resource;
                for(Field f : fields) {
                    if (object == null) {
                        break;
                    }
                    object = f.getValue(object);
                }
                
                if (object != null) {
                    value = object.toString();
                }
            }
            
            if (value != null) {
                if (prefix != null) {
                    bld.append(prefix);
                }
                
                bld.append(value.toString());
                
                if (suffix != null) {
                    bld.append(suffix);
                }
            }
        }
    }
    
    private static class SegmentAndState {
        public Segment segment;
        public ParserState state;
        public ParserState lastState;     
    }
    
    /**
     * The guts of the template parser. Each state can process a single character and modify the state for the next
     * char. 
     */
    private enum ParserState {
        START {
            public SegmentAndState process(SegmentAndState seg, Character c) {
                if (c == null) {
                    return null;
                }
                   
                seg.lastState = null;
                
                switch(c) {
                case '%':
                    seg.segment = new ResourceSegment();
                    seg.state = ParserState.IN_RESOURCE_START;
                    break;
                case '\\':
                    seg.segment = new TextSegment();
                    seg.lastState = ParserState.IN_TEXT;                    
                    seg.state = ParserState.ESCAPING;
                    break;
                default:
                    seg.segment = new TextSegment();
                    seg.segment.getCurrentString().append(c);
                    seg.state = ParserState.IN_TEXT;
                    break;
                }
                
                return seg;
            }
        }, IN_TEXT {
            public SegmentAndState process(SegmentAndState seg, Character c) {
                if (c == null) {
                    return null;
                }
                
                seg.lastState = null;
                
                switch(c) {
                case '%':
                    seg.segment = new ResourceSegment();
                    seg.state = ParserState.IN_RESOURCE_START;
                    break;
                case '\\':
                    seg.lastState = seg.state;
                    seg.state = ParserState.ESCAPING;
                    break;
                default:
                    seg.segment.getCurrentString().append(c);
                    break;
                }
                
                return seg;
            }
        }, IN_RESOURCE_START {
            public SegmentAndState process(SegmentAndState seg, Character c) {
                if (c == null) {
                    return null;
                }
                
                seg.lastState = null;
                
                switch(c) {
                case '[':
                    seg.state = ParserState.IN_RESOURCE_PREFIX;
                    break;
                default:
                    seg.segment.getCurrentString().append(c);
                    seg.state = ParserState.IN_RESOURCE_DEF;
                    break;
                }
                
                return seg;
            }
        }, IN_RESOURCE_PREFIX {
            public SegmentAndState process(SegmentAndState seg, Character c) {
                if (c == null) {
                    return null;
                }
                
                seg.lastState = null;
                
                switch(c) {
                case '\\':
                    seg.lastState = seg.state;
                    seg.state = ParserState.ESCAPING;
                    break;
                case ']':
                    ((ResourceSegment)seg.segment).prefix = seg.segment.getCurrentString().toString();
                    ((ResourceSegment)seg.segment).currentString = new StringBuilder();
                    seg.state = ParserState.IN_RESOURCE_DEF;
                    break;
                default:
                    seg.segment.getCurrentString().append(c);
                    break;
                }
                
                return seg;
            }
        }, IN_RESOURCE_DEF {
            public SegmentAndState process(SegmentAndState seg, Character c) {
                if (c == null) {
                    processField(seg);
                    return null;
                }
                
                seg.lastState = null;
                
                switch(c) {
                case '\\':
                    seg.lastState = seg.state;
                    seg.state = ParserState.ESCAPING;
                    break;
                case '.':  
                    processField(seg);
                    break;
                case '%':
                    processField(seg);
                    seg.segment = new ResourceSegment();
                    seg.state = ParserState.IN_RESOURCE_START;
                    break;
                case '[':
                    processField(seg);
                    seg.state = ParserState.IN_RESOURCE_SUFFIX;
                    break;
                default:
                    if (isWhitespace(c)) {
                        processField(seg);
                        seg.segment = new TextSegment();
                        seg.segment.getCurrentString().append(c);
                        seg.state = ParserState.IN_TEXT;
                    } else {
                        seg.segment.getCurrentString().append(c);
                    }                    
                    break;
                }
                
                return seg;
            }
            
            //this would have been better implemented if we had reflection in GWT.
            private void processField(SegmentAndState seg) {
                ResourceSegment s = (ResourceSegment)seg.segment;
                String fieldName = s.getCurrentString().toString();
                try {
                    Field field = s.currentField.getSiblingField(fieldName);
                    s.fields.add(field);
                    s.currentField = field.getRepresentation();
                    s.currentString = new StringBuilder();
                } catch(Exception e) {
                    s.fields = null;
                    seg.state = ParserState.START;
                }
            }
            
            private boolean isWhitespace(char c) {
                //if we had the Character class in GWT...
                //return Character.isWhitespace(c)
                
                return c == ' ' || c == '\n' || c == '\t';
            }
        }, IN_RESOURCE_SUFFIX {
            public SegmentAndState process(SegmentAndState seg, Character c) {
                if (c == null) {
                    return null;
                }
                
                seg.lastState = null;
                
                switch(c) {
                case '\\':
                    seg.lastState = seg.state;
                    seg.state = ParserState.ESCAPING;
                    break;
                case ']':
                    ((ResourceSegment)seg.segment).suffix = seg.segment.getCurrentString().toString();
                    ((ResourceSegment)seg.segment).currentString = new StringBuilder();
                    seg.state = ParserState.START;
                    break;
                default:
                    seg.segment.getCurrentString().append(c);
                    break;
                }
                
                return seg;
            }
        }, ESCAPING {
            public SegmentAndState process(SegmentAndState seg, Character c) {
                if (c == null) {
                    return null;
                }                
                
                seg.segment.getCurrentString().append(c);
                seg.state = seg.lastState;
                seg.lastState = null;
                
                return seg;
            }

        };
        
        public abstract SegmentAndState process(SegmentAndState currentSegment, Character c);
    }
    
    public DisambiguationReportRenderer() {
        parsedTemplate = parse(segmentTemplate);
        parsedSingletonTemplate = parse(singletonSegmentTemplate);
    }
    
    /**
     * @return the includeResource
     */
    public boolean isIncludeResource() {
        return includeResource;
    }

    /**
     * @param includeResource the includeResource to set
     */
    public void setIncludeResource(boolean includeResource) {
        this.includeResource = includeResource;
    }

    /**
     * @return the includeParents
     */
    public boolean isIncludeParents() {
        return includeParents;
    }

    /**
     * @param includeParents the includeParents to set
     */
    public void setIncludeParents(boolean includeParents) {
        this.includeParents = includeParents;
    }

    /**
     * @return the renderingOrder
     */
    public RenderingOrder getRenderingOrder() {
        return renderingOrder;
    }

    /**
     * @param renderingOrder the renderingOrder to set
     */
    public void setRenderingOrder(RenderingOrder renderingOrder) {
        this.renderingOrder = renderingOrder;
    }

    /**
     * @return the segmentTemplate
     */
    public String getSegmentTemplate() {
        return segmentTemplate;
    }

    /**
     * @param segmentTemplate the segmentTemplate to set
     */
    public void setSegmentTemplate(String segmentTemplate) {
        this.segmentTemplate = segmentTemplate;
        parsedTemplate = parse(segmentTemplate);
    }

    /**
     * @return the singletonSegmentTemplate
     */
    public String getSingletonSegmentTemplate() {
        return singletonSegmentTemplate;
    }
    
    /**
     * @param singletonSegmentTemplate the singletonSegmentTemplate to set
     */
    public void setSingletonSegmentTemplate(String singletonSegmentTemplate) {
        this.singletonSegmentTemplate = singletonSegmentTemplate;
        parsedSingletonTemplate = parse(singletonSegmentTemplate);
    }
    
    /**
     * @return the segmentSeparator
     */
    public String getSegmentSeparator() {
        return segmentSeparator;
    }

    /**
     * @param segmentSeparator the segmentSeparator to set
     */
    public void setSegmentSeparator(String segmentSeparator) {
        this.segmentSeparator = segmentSeparator;
    }

    public String render(DisambiguationReport<?> report) {
        if (parsedTemplate != null) {
            StringBuilder bld = new StringBuilder();
            
            List<DisambiguationReport.Resource> resources = new ArrayList<DisambiguationReport.Resource>();
            
            switch(renderingOrder) {
            case ASCENDING:
                if (includeResource) {
                    resources.add(report.getResource());
                }
                if (includeParents) {
                    resources.addAll(report.getParents());
                }
                break;
            case DESCENDING:
                if (includeParents) {
                    ArrayList<DisambiguationReport.Resource> reverseCopy = new ArrayList<DisambiguationReport.Resource>(report.getParents());
                    Collections.reverse(reverseCopy);
                    resources.addAll(reverseCopy);
                }
                
                if (includeResource) {
                    resources.add(report.getResource());
                }
                break;
            default:;
            }
            
            String separator = getSegmentSeparator();
            for(DisambiguationReport.Resource r : resources) {
                renderResource(r, bld);
                bld.append(separator);
            }
            
            if (resources.size() > 0) {
                bld.replace(bld.length() - separator.length(), bld.length(), "");
            }
            
            return bld.toString();
        } else {
            return null;
        }
    }
    
    private List<Segment> parse(String template) {
        List<Segment> ret = new ArrayList<Segment>();
        
        int idx = 0;
        
        SegmentAndState currentState = new SegmentAndState();
        currentState.state = ParserState.START;
        
        while(idx < template.length()) {
            char c = template.charAt(idx);
            Segment lastSegment = currentState.segment;
            currentState = currentState.state.process(currentState, c);
            
            //if the state created a new segment, store it in the results and
            //continue with it. yes, the reference equality is what we want here
            if (lastSegment != currentState.segment) {
                ret.add(currentState.segment);
            }
            
            ++idx;
        }
        
        currentState.state.process(currentState, null);
        
        return ret;
    }
    
    private void renderResource(DisambiguationReport.Resource resource, StringBuilder bld) {
        List<Segment> template = parsedTemplate;
        if (resource.getType() != null && resource.getType().isSingleton()) {
            template = parsedSingletonTemplate;
        }
        
        for(Segment seg : template) {
            seg.render(resource, bld);
        }
    }
}
