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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;

/**
 * Helper class to accept the self-signed certificates.
 * 
 * @author <a href="mailto:janguenot@iland.com">Julien Anguenot</a>
 */
public class FakeSSLSocketFactory {

    private FakeSSLSocketFactory() {
    }

    public static SSLSocketFactory getInstance() throws KeyManagementException,
            UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException {
        return new SSLSocketFactory(new TrustStrategy() {
            public boolean isTrusted(final X509Certificate[] chain,
                    final String auth) throws CertificateException {
                // XXX register development / staging instances.
                return true;
            }

        }, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    }

}