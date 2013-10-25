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

/**
 * Enumeration of constants defining property names.
 * 
 * @author <a href="mailto:janguenot@iland.com">Julien Anguenot</a>
 * 
 */
public enum ConfigConstants {

    VCD_WS_URL("vcd.ws.url"),

    VCD_WS_USER("vcd.ws.username"),

    VCD_VDC_NAME("vcd.vdc.name"),
    
    VCD_WS_PASSWORD("vcd.ws.password"),

    VCD_VAPP_TEMPLATE_NAME("vcd.vapp.template.name"),

    VCD_ORG_NAME("vcd.org.name");

    private final String value;

    private ConfigConstants(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }

}
