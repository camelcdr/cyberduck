package ch.cyberduck.core.cloud;

/*
 * Copyright (c) 2002-2010 David Kocher. All rights reserved.
 *
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to:
 * dkocher@cyberduck.ch
 */

import ch.cyberduck.core.Host;
import ch.cyberduck.core.http.HTTP3Session;
import ch.cyberduck.core.ssl.AbstractX509TrustManager;

/**
 * @version $Id$
 */
public abstract class CloudHTTP3Session extends HTTP3Session implements CloudSession {

    protected CloudHTTP3Session(Host h) {
        super(h);
    }

    /**
     * Use ACL support.
     *
     * @return Always returning false because permissions should be set using ACLs
     */
    @Override
    public boolean isUnixPermissionsSupported() {
        return false;
    }

    @Override
    public boolean isTimestampSupported() {
        return false;
    }

    /**
     * @param hostname
     * @return
     */
    public String getContainerForHostname(String hostname) {
        if(hostname.equals(host.getProtocol().getDefaultHostname())) {
            return null;
        }
        // Bucket name is available in URL's host name.
        if(hostname.endsWith(host.getProtocol().getDefaultHostname())) {
            // Bucket name is available as S3 subdomain
            return hostname.substring(0, hostname.length() - host.getProtocol().getDefaultHostname().length() - 1);
        }
        return null;
    }

    @Override
    public AbstractX509TrustManager getTrustManager() {
        return this.getTrustManager(this.getHostnameForContainer(this.getHost().getCredentials().getUsername()));
    }

    /**
     * @param container
     * @return
     */
    public String getHostnameForContainer(String container) {
        return container + "." + this.getHost().getHostname(true);
    }

    @Override
    public boolean isMetadataSupported() {
        return true;
    }
}
