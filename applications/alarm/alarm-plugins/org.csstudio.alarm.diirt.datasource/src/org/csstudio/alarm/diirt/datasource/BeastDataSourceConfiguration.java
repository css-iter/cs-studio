/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.csstudio.alarm.diirt.datasource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.diirt.datasource.DataSourceConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Configuration for {@link BeastDataSource}
 *
 * @author Kunal Shroff
 * @author Miha Vitorovič
 */
public class BeastDataSourceConfiguration extends DataSourceConfiguration<BeastDataSource> {
    private static final Logger LOG = Logger.getLogger(BeastDataSourceConfiguration.class.getCanonicalName());

    private String configName;                      // null default

    private List<String> configurationNames;       // null default

    private int version;                             // 0 default

    @Override
    public BeastDataSourceConfiguration read(InputStream input) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(input);

            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xPath = xpathFactory.newXPath();

            String ver = xPath.evaluate("/beast/@version", document);
            if (!ver.equals("1") && !ver.equals("2")) {
                throw new IllegalArgumentException("Unsupported version " + ver);
            }

            if (ver.equals("1")) {
                parseVersion1(xPath, document);
                version = 1;
            } else {
                parseVersion2(xPath, document);
                version = 2;
            }
        } catch (Exception e) {
            LOG.log(Level.FINEST, "Couldn't load file configuration", e);
            throw new IllegalArgumentException("Couldn't load file configuration", e);
        }
        return this;
    }

    @Override
    public BeastDataSource create() {
        return new BeastDataSource(this);
    }

    /** @return the default configuration name. */
    public String getConfigName() {
        return configName;
    }

    public List<String> getConfigNames() {
        return Collections.unmodifiableList(configurationNames);
    }

    public int getVersion() {
        return version;
    }

    private void parseVersion1(XPath xPath, Document document) throws XPathExpressionException {
        String configName = xPath.evaluate("/beast/dataSourceOptions/@configName", document);
        if ((configName != null) && !configName.isEmpty()) {
            this.configName = configName;
            this.configurationNames = Arrays.asList(configName);
        } else {
            LOG.log(Level.FINE, "Couldn't load configName from beast file configuration");
        }
    }

    private void parseVersion2(XPath xPath, Document document) throws XPathExpressionException {
        final NodeList nodes = (NodeList) xPath.evaluate("/beast/dataSourceOptions", document, XPathConstants.NODESET);
        if (nodes != null) {
            this.configurationNames = new ArrayList<>(nodes.getLength());
            boolean explicitDefaultSet = false;
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node dsOptionsNode = nodes.item(i);
                final NamedNodeMap attribs = dsOptionsNode.getAttributes();
                final String confName = getValue(attribs.getNamedItem("configName"));
                if ((confName != null) && !confName.isEmpty()){
                    if (this.configName == null) this.configName = confName; // set the implicit default
                    this.configurationNames.add(confName);
                    final boolean explicitDefault = "true".equalsIgnoreCase(getValue(attribs.getNamedItem("default")));
                    if (explicitDefault) {
                        if (!explicitDefaultSet)
                            this.configName = confName;
                        else
                            LOG.log(Level.WARNING,
                                    () -> String.format("The explicit default configuration already set (%s), but is trying to be set again by %s.",
                                            this.configName, confName));
                    }
                }
            }
        }

        if (this.configName == null){
            LOG.log(Level.FINE, "Couldn't load configName from beast file configuration");
        }
    }

    private String getValue(Node node) {
        if (node != null) return node.getNodeValue();
        return null;
    }
}
