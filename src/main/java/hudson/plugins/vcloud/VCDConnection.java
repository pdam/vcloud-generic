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
package hudson.plugins.vcloud;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.constants.Version;
import com.vmware.vcloudapi.ConfigConstants;
import com.vmware.vcloudapi.Configuration;
import com.vmware.vcloudapi.FakeSSLSocketFactory;
import java.util.logging.Logger;

/**
 * Thread-Safe vCloud director connection implementing an
 * Initialization-on-demand holder idiom
 * 
 * @author <a href="mailto:janguenot@iland.com">Julien Anguenot</a>
 * 
 */
public class VCDConnection {

    private static final Log log = LogFactory.getLog(VCDConnection.class);

    /* Maximum number of connections allowed */
    private static final int MAX_CONS = 50;

    /* Default session timeout in minutes for the vCloud client */
    private static final int SESSION_TIMEOUT = 30;

    static VCDConnection getInstance() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /* When did the client last connected */
    private Date lastLoggedIn;

    /* Actual vCloud core client */
    private VcloudClient client;

    class Holder   {
        private VCDConnection INSTANCE;

        public Holder() throws KeyManagementException, UnrecoverableKeyException {
            this.INSTANCE = new VCDConnection();
        }

        /**
         * @return the INSTANCE
         */
        public VCDConnection getINSTANCE() {
            return INSTANCE;
        }

        /**
         * @param INSTANCE the INSTANCE to set
         */
        public   void setINSTANCE(VCDConnection INSTANCE) {
            this.INSTANCE = INSTANCE;
        }
    }

    

    private VCDConnection() throws KeyManagementException, UnrecoverableKeyException {
        init();
    }

    private void init() throws KeyManagementException, UnrecoverableKeyException {
        final String url = Configuration.getProperties().getString(
                ConfigConstants.VCD_WS_URL.getValue());
        final String username = Configuration.getProperties().getString(
                ConfigConstants.VCD_WS_USER.getValue());
        final String password = Configuration.getProperties().getString(
                ConfigConstants.VCD_WS_PASSWORD.getValue());
        VcloudClient.setLogLevel(Level.OFF);
        client = new VcloudClient(url, Version.V5_1);
        try {
            client.registerScheme("https", 443,
                    FakeSSLSocketFactory.getInstance());
            lastLoggedIn = new Date();
            client.login(username, password);
            client.setMaxConnections(MAX_CONS);
            log.info("Logged IN against VCD instance:  " + username + " @ "
                    + url);
        } catch (VCloudException ex) {
            Logger.getLogger(VCDConnection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(VCDConnection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeyStoreException ex) {
            Logger.getLogger(VCDConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //| UnrecoverableKeyException
             //   | NoSuchAlgorithmException | KeyStoreException
               // | VCloudException e
    }

    public VcloudClient getClient() throws KeyManagementException, UnrecoverableKeyException {
        if (sessionExpired()) {
            // XXX use extendSession() here when possible.
            init();
        }
        return client;
    }

    private boolean sessionExpired() {
        Calendar now = Calendar.getInstance();
        return now.compareTo(getTimeout()) >= 0;
    }

    private Calendar getTimeout() {
        Calendar timeout = Calendar.getInstance();
        timeout.setTime(lastLoggedIn);
        timeout.add(GregorianCalendar.MINUTE, SESSION_TIMEOUT);
        return timeout;
    }

}
