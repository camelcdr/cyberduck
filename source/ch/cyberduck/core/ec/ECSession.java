package ch.cyberduck.core.ec;

/*
 *  Copyright (c) 2008 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import ch.cyberduck.core.*;
import ch.cyberduck.core.http.StickyHostConfiguration;
import ch.cyberduck.core.s3.S3Session;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.protocol.DefaultProtocolSocketFactory;
import org.jets3t.service.CloudFrontServiceException;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.model.cloudfront.Distribution;

import java.io.IOException;

/**
 * Elastic Utility Computing Architecture for Linking Your Programs To Useful Systems - is an open-source software
 * infrastructure for implementing "cloud computing" on clusters. The current interface
 * to EUCALYPTUS is compatible with Amazon's EC2 interface, but the infrastructure
 * is designed to support multiple client-side interfaces. EUCALYPTUS is implemented
 * using commonly available Linux tools and basic Web-service technologies making it easy to install and maintain.
 *
 * @version $Id$
 * @see http://eucalyptus.cs.ucsb.edu/
 */
public class ECSession extends S3Session {

    static {
        SessionFactory.addFactory(Protocol.EUCALYPTUS, new Factory());
    }

    private static class Factory extends SessionFactory {
        protected Session create(Host h) {
            return new ECSession(h);
        }
    }

    protected ECSession(Host h) {
        super(h);
    }

    protected void configure() {
        super.configure();
        configuration.setProperty("s3service.disable-dns-buckets", String.valueOf(true));
        configuration.setProperty("s3service.s3-endpoint-virtual-path", Path.normalize("/services/Walrus"));
    }

    protected void login(final Credentials credentials) throws IOException {
        final HostConfiguration hostconfig = new StickyHostConfiguration();
        hostconfig.setHost(host.getHostname(), host.getPort(),
                new org.apache.commons.httpclient.protocol.Protocol(host.getProtocol().getScheme(),
                        new DefaultProtocolSocketFactory(), host.getPort())
        );
        super.login(credentials, hostconfig);
    }

    public void updateDistribution(boolean enabled, final Distribution distribution, String[] cnames) throws CloudFrontServiceException {
        throw new UnsupportedOperationException();
    }

    /**
     * Amazon CloudFront Extension used to list all configured distributions
     *
     * @return All distributions for the given AWS Credentials
     */
    public Distribution[] listDistributions(String bucket) throws CloudFrontServiceException {
        throw new UnsupportedOperationException();
    }

    /**
     * @param distribution A distribution (the distribution must be disabled and deployed first)
     */
    public void deleteDistribution(final Distribution distribution) throws CloudFrontServiceException {
        throw new UnsupportedOperationException();
    }
}