/*
 * Copyright (c) 2013, iland Internet Solutions
 * 
 * 
 * This software is licensed under the Terms and Conditions contained within the
 * "LICENSE.txt" file that accompanied this software. Any inquiries concerning
 * the scope or enforceability of the license should be addressed to:
 * 
 * iland Internet Solutions 1235 North Loop West, Suite 205 Houston, TX 77008
 * USA
 * 
 * http://www.iland.com
 */
package com.vmware.vcloudapi;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.configuration.ConfigurationException;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Splunk properties.
 * 
 * @author <a href="mailto:janguenot@iland.com">Julien Anguenot</a>
 * 
 */
public class Configuration {

    private static final Log log = LogFactory.getLog(Configuration.class);

    private static PropertiesConfiguration properties;

    static {

        properties = new PropertiesConfiguration();

        final String name = "splunk.properties";
        InputStream raw = null;
        try {
            raw = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(name);
            if (raw != null) {
                properties.load(raw);
                raw.close();
            }
        } catch (ConfigurationException e) {
            log.error("Error occured loading properties file", e);
        } catch (IOException e) {
            log.error("Error occured loading properties file", e);
        }

        properties.setReloadingStrategy(new FileChangedReloadingStrategy());

    }

    public static synchronized PropertiesConfiguration getProperties() {
        return properties;
    }

}
