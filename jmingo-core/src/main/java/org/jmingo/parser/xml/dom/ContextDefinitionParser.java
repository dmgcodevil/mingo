/**
 * Copyright 2013-2014 The JMingo Team
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmingo.parser.xml.dom;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jmingo.config.ContextDefinition;
import org.jmingo.config.MongoConfig;
import org.jmingo.config.QuerySetConfig;
import org.jmingo.exceptions.JMingoParserException;
import org.jmingo.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;

import static org.jmingo.parser.xml.dom.DocumentBuilderFactoryCreator.createDocumentBuilderFactory;
import static org.jmingo.parser.xml.dom.util.DomUtil.assertPositive;
import static org.jmingo.parser.xml.dom.util.DomUtil.getAllChildNodes;
import static org.jmingo.parser.xml.dom.util.DomUtil.getAttributeInt;
import static org.jmingo.parser.xml.dom.util.DomUtil.getAttributeString;
import static org.jmingo.parser.xml.dom.util.DomUtil.getFirstTagOccurrence;

/**
 * This class is implementation of {@link Parser} interface to parse context configuration.
 * See context.xsd schema for details.
 */
public class ContextDefinitionParser implements Parser<ContextDefinition> {

    private DocumentBuilderFactory documentBuilderFactory;

    private ErrorHandler parseErrorHandler;

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextDefinitionParser.class);

    private static final String CONTEXT_TAG = "context";

    private static final String QUERY_SET_CONFIG_TAG = "querySetConfig";
    private static final String QUERY_SET_TAG = "querySet";
    private static final String QUERY_SET_PATH_ATTR = "path";

    private static final String MONGO_TAG = "mongo";
    private static final String MONGO_HOST_ATTR = "host";
    private static final String MONGO_PORT_ATTR = "port";
    private static final String MONGO_DBNAME_ATTR = "dbName";

    private static final String DEFAULT_CONVERTER_TAG = "defaultConverter";
    private static final String CONVERTERS_TAG = "converters";
    private static final String CONVERTERS_PACKAGE_ATTR = "package";
    private static final String CONVERTER_CLASS_ATTR = "class";
    private static final String WRITE_CONCERN_ATTR = "writeConcern";
    private static final String OPTIONS_TAG = "options";
    private static final String OPTION_TAG = "option";

    /**
     * Constructor with parameters.
     *
     * @param parserConfiguration parser configuration {@link ParserConfiguration}
     * @param parseErrorHandler   parser error handler {@link ErrorHandler}
     * @throws ParserConfigurationException {@link ParserConfigurationException}
     */
    public ContextDefinitionParser(ParserConfiguration parserConfiguration, ErrorHandler parseErrorHandler)
            throws ParserConfigurationException {
        this.documentBuilderFactory = createDocumentBuilderFactory(parserConfiguration);
        this.parseErrorHandler = parseErrorHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContextDefinition parse(Path path) throws JMingoParserException {
        LOGGER.debug("parse context configuration: {}", path);
        ContextDefinition contextDefinition;
        try (InputStream is = new FileInputStream(path.toFile())) {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            builder.setErrorHandler(parseErrorHandler);
            Document document = builder.parse(new InputSource(is));
            contextDefinition = new ContextDefinition();
            Element element = document.getDocumentElement();
            contextDefinition.setQuerySetConfig(parseQuerySetConfigTag(element));
            contextDefinition.setMongoConfig(parseMongoTag(element));
            parseConvertersTag(contextDefinition, element);
            contextDefinition.setDefaultConverter(parseDefaultConverterTag(element));
        } catch(Exception e) {
            throw new JMingoParserException(e);
        }
        return contextDefinition;
    }

    /**
     * Parse <querySetConfig/> tag.
     *
     * @param element element of XML document
     * @return set of paths queries definitions
     */
    private QuerySetConfig parseQuerySetConfigTag(Element element) throws JMingoParserException {
        QuerySetConfig querySetConfig = new QuerySetConfig();
        Set<String> querySets = ImmutableSet.of();
        // expected what xml contains single <querySetConfig> tag
        // therefore take first element from node list is normal
        Node querySetConfigNode = getFirstTagOccurrence(element, QUERY_SET_CONFIG_TAG);
        if (querySetConfigNode != null && querySetConfigNode.hasChildNodes()) {
            querySets = parseQuerySets(querySetConfigNode.getChildNodes());
        }
        querySetConfig.addQuerySet(querySets);
        return querySetConfig;
    }

    /**
     * Parse <querySet/> tag.
     *
     * @param querySetNodeList list of 'querySet' tag nodes. root element must be <querySetConfig/> tag
     * @return set of paths queries definitions
     */
    private Set<String> parseQuerySets(NodeList querySetNodeList) {
        Set<String> querySets = Sets.newHashSet();
        for (int count = 0; count < querySetNodeList.getLength(); count++) {
            Node querySetNode = querySetNodeList.item(count);
            if (QUERY_SET_TAG.equals(querySetNode.getNodeName())) {
                querySets.add(getAttributeString(querySetNode, QUERY_SET_PATH_ATTR));
            }
        }
        return querySets;
    }

    /**
     * Parse <mongo/> tag. All information from this tag will pass to context configuration.
     * Throws exception if 'mongo' tag was not found.
     *
     * @param element element of XML document
     * @throws org.jmingo.exceptions.JMingoParserException {@link org.jmingo.exceptions.JMingoParserException}
     */
    private MongoConfig parseMongoTag(Element element)
            throws JMingoParserException {
        MongoConfig mongoConfig = null;
        Node mongoNode = getFirstTagOccurrence(element, MONGO_TAG);
        if (mongoNode != null) {

            String databaseHost = getAttributeString(mongoNode, MONGO_HOST_ATTR, MongoConfig.DEF_HOST);
            int databasePort = getAttributeInt(mongoNode, MONGO_PORT_ATTR, MongoConfig.DEF_PORT);
            String dbName = getAttributeString(mongoNode, MONGO_DBNAME_ATTR);
            String writeConcern = getAttributeString(mongoNode, WRITE_CONCERN_ATTR);
            Validate.notBlank(dbName, "attribute 'dbName' is required");
            assertPositive(databasePort, "wrong value for database port. database port must be gt 0");
            Validate.notBlank(databaseHost, "database host cannot be null or empty");
            MongoConfig.Builder mongoConfigBuilder = MongoConfig.builder().dbPort(databasePort)
                    .dbHost(databaseHost).dbName(dbName).writeConcern(writeConcern);
            // parse options
            Node optionsNode = getFirstTagOccurrence(element, OPTIONS_TAG);
            if (optionsNode != null) {
                getAllChildNodes(mongoNode, OPTION_TAG).forEach(optNode -> {
                    if (OPTION_TAG.equals(optNode.getNodeName())) {
                        mongoConfigBuilder.option(getAttributeString(optNode, "name"), getAttributeString(optNode, "value"));
                    }
                });
            }
            mongoConfig = mongoConfigBuilder.build();
        } else {
            throw new JMingoParserException("<"+MONGO_TAG+"/>" + " is required");
        }
        return mongoConfig;
    }

    private void parseConvertersTag(ContextDefinition contextDefinition, Element element) {
        Node convertersNode = getFirstTagOccurrence(element, CONVERTERS_TAG);
        if (convertersNode != null) {
            String convertersPackage = getAttributeString(convertersNode, CONVERTERS_PACKAGE_ATTR);
            contextDefinition.setConverterPackageScan(convertersPackage);
        }
    }

    private String parseDefaultConverterTag(Element element) {
        String defaultConverter = StringUtils.EMPTY;
        Node defaultConverterNode = getFirstTagOccurrence(element, DEFAULT_CONVERTER_TAG);
        if (defaultConverterNode != null) {
            defaultConverter = getAttributeString(defaultConverterNode, CONVERTER_CLASS_ATTR);
        }
        return defaultConverter;
    }
}
