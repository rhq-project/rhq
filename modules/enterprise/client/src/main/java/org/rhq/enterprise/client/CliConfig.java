package org.rhq.enterprise.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.rhq.core.domain.util.PageControl;

public class CliConfig {

    private static HashMap<File, CliConfig> cache = null;
    private long lastModified = 0;
    private List<Manager> managers;

    private CliConfig() {
    }

    public static synchronized CliConfig getConfig(File configXML) {
        if (null == cache) {
            cache = new HashMap<File, CliConfig>();
        }

        CliConfig cfg = cache.get(configXML);

        long lastModified = configXML.lastModified();

        if ((null == cfg) || (lastModified != cfg.lastModified)) {
            cfg = new CliConfig();
            cfg.lastModified = lastModified;
            cache.put(configXML, cfg);

            try {
                cfg.read(configXML);
            } catch (IOException e) {
            }
        }

        return cfg;
    }

    private void read(File file) throws IOException {
        FileInputStream is = null;

        try {
            is = new FileInputStream(file);
            parse(is);
        } catch (SAXException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void parse(InputStream is) throws IOException, SAXException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        CliHandler handler = new CliHandler();
        parser.parse(is, handler);

        this.managers = handler.getManagers();
    }

    private static class CliHandler extends DefaultHandler {
        private List<Manager> managers = new ArrayList<Manager>();
        private Manager currentManager;
        private Service currentService;
        private Parameter currentParameter;

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("manager")) {
                currentManager = new Manager(attributes);
                managers.add(currentManager);
                return;
            }

            if (qName.equals("service")) {
                currentService = new Service(attributes);
                currentManager.getServices().add(currentService);
                return;
            }

            if (qName.equals("parameter")) {
                currentParameter = ParameterFactory.getParameter(attributes);
                currentService.getParameters().add(currentParameter);
                return;
            }

            if (qName.equals("criteria")) {
                ((CriteriaParameter) currentParameter).getCriteria().add(new Criteria(attributes));
                return;
            }

            if (qName.equals("optionalData")) {
                ((PageControlParameter) currentParameter).getOptionalData().add(new OptionalData(attributes));
                return;
            }

            if (qName.equals("sortOption")) {
                ((PageControlParameter) currentParameter).getSortOptions().add(new SortOption(attributes));
                return;
            }
        }

        public List<Manager> getManagers() {
            return managers;
        }
    }

    public List<Manager> getManagers() {
        return managers;
    }

    public List<Manager> getManagers(String startingWith) {
        List<Manager> result = new ArrayList<Manager>();

        for (Manager manager : managers) {
            if (manager.name.startsWith(startingWith)) {
                result.add(manager);
            }
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            CliConfig cfg = CliConfig.getConfig(new File(args[i]));

            System.out.println("managers=" + cfg.getManagers() + " [" + args[i] + "]");
        }
    }

    public static class Manager {
        private String name;
        private List<Service> services = new ArrayList<Service>();

        public Manager(Attributes attributes) {
            this.setName(attributes.getValue("name"));
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Service> getServices() {
            return services;
        }

        public List<Service> getServices(String startingWith) {
            List<Service> result = new ArrayList<Service>();

            for (Service service : services) {
                if (service.name.startsWith(startingWith)) {
                    result.add(service);
                }
            }

            return result;
        }

        public void setServices(List<Service> services) {
            this.services = services;
        }

    }

    public static class Service {
        private String name;
        private List<Parameter> parameters;

        public Service(Attributes attributes) {
            this.setName(attributes.getValue("name"));
            this.parameters = new ArrayList<Parameter>();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Parameter> getParameters() {
            return parameters;
        }

        public void setParameters(List<Parameter> parameters) {
            this.parameters = parameters;
        }

        public Parameter getParameter(int position) {
            Parameter result = null;

            for (Parameter parameter : parameters) {
                if (parameter.position == position) {
                    result = parameter;
                    break;
                }
            }

            return result;
        }

        public Parameter getParameter(String name) {
            Parameter result = null;

            for (Parameter parameter : parameters) {
                if (parameter.name.equalsIgnoreCase(name)) {
                    result = parameter;
                    break;
                }
            }

            return result;
        }
    }

    private static class ParameterFactory {
        public static Parameter getParameter(Attributes attributes) {
            Parameter result = null;
            Parameter.ParameterType type = Parameter.ParameterType.valueOf(attributes.getValue("type"));
            if (Parameter.ParameterType.Criteria == type) {
                result = new CriteriaParameter(attributes);
            } else if (Parameter.ParameterType.PageControl == type) {
                result = new PageControlParameter(attributes);
            } else {
                result = new Parameter(attributes);
            }

            return result;
        }
    }

    public static class Parameter {
        public enum ParameterType {
            Criteria, Default, PageControl, Update
        };

        private int position;
        private String name;
        private String className;
        private ParameterType type;

        public Parameter(Attributes attributes) {
            this.position = Integer.valueOf(attributes.getValue("position"));
            this.name = attributes.getValue("name");
            this.className = attributes.getValue("className");
            this.type = ParameterType.valueOf(attributes.getValue("type"));
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public void getCandidates(String prefix, String currentValue, List<String> candidates) {
            String candidate = prefix + this.name + "=";

            if (null == candidates) {
                candidates = new ArrayList<String>();
            }

            candidates.add(candidate);
        }
    }

    public static class CriteriaParameter extends Parameter {
        List<Criteria> criteria;

        public CriteriaParameter(Attributes attributes) {
            super(attributes);
            this.criteria = new ArrayList<Criteria>();
        }

        public List<Criteria> getCriteria() {
            return criteria;
        }

        public void setCriteria(List<Criteria> criteria) {
            this.criteria = criteria;
        }

        public void getCandidates(String prefix, String currentValue, List<String> candidates) {
            if (null == currentValue) {
                currentValue = "";
            } else {
                currentValue = currentValue.trim();
            }

            if ("".equals(currentValue) || currentValue.toLowerCase().endsWith("and")) {
                for (Criteria criteria : this.getCriteria()) {
                    String candidate = criteria.getDisplayName() + "=";

                    if (!currentValue.contains(candidate)) {
                        String spacer = (currentValue.endsWith(" ")) ? "" : " ";
                        candidates.add(prefix + currentValue + spacer + candidate);
                    }
                }
            } else {
                String[] allCriteria = currentValue.split("\\s+and|And|AND\\s+");
                String endCriteria = allCriteria[allCriteria.length - 1];
                String startCriteria = currentValue.substring(0, (currentValue.length() - endCriteria.length()));

                for (Criteria criteria : this.getCriteria()) {
                    String candidate = criteria.getDisplayName() + "=";

                    if (endCriteria.startsWith(candidate) && !endCriteria.equals(candidate)) {
                        candidates.add(prefix + currentValue + " AND ");
                        candidates.add(prefix + currentValue + ", ");
                    } else if (candidate.startsWith(endCriteria)) {
                        String spacer = (startCriteria.endsWith(" ")) ? "" : " ";
                        candidates.add(prefix + startCriteria + spacer + candidate);
                    }
                }
            }
        }
    }

    public static class PageControlParameter extends Parameter {
        List<OptionalData> optionalData;
        List<SortOption> sortOptions;

        public PageControlParameter(Attributes attributes) {
            super(attributes);
            this.setClassName(PageControl.class.getName());
            this.optionalData = new ArrayList<OptionalData>();
            this.sortOptions = new ArrayList<SortOption>();
        }

        public List<OptionalData> getOptionalData() {
            return optionalData;
        }

        public void setOptionalData(List<OptionalData> optionalData) {
            this.optionalData = optionalData;
        }

        public List<SortOption> getSortOptions() {
            return sortOptions;
        }

        public void setSortOptions(List<SortOption> sortOptions) {
            this.sortOptions = sortOptions;
        }
    }

    public static class Criteria {
        private String field;
        private String display;
        private String displayType;

        public Criteria(Attributes attributes) {
            this.field = attributes.getValue("field");
            this.display = attributes.getValue("display");
            if (null == this.display) {
                this.display = this.field;
            }
            this.displayType = attributes.getValue("displayType");
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getDisplayName() {
            return display;
        }

        public void setDisplayName(String displayName) {
            this.display = displayName;
        }

        public String getDisplayType() {
            return displayType;
        }

        public void setDisplayType(String displayType) {
            this.displayType = displayType;
        }
    }

    public static class OptionalData {
        private String field;

        public OptionalData(Attributes attributes) {
            this.field = attributes.getValue("field");
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }

    public static class SortOption {
        private String field;

        public SortOption(Attributes attributes) {
            this.field = attributes.getValue("field");
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

    }

}
