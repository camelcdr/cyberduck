package ch.cyberduck.core.http;

/*
 *  Copyright (c) 2004 David Kocher. All rights reserved.
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

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.Logger;

import ch.cyberduck.core.*;

/**
 * Opens a connection to the remote server via http protocol
 *
 * @version $Id$
 */
public class HTTPSession extends Session {
    private static Logger log = Logger.getLogger(Session.class);

    static {
        SessionFactory.addFactory(Session.HTTP, new Factory());
    }

    private static class Factory extends SessionFactory {
        protected Session create(Host h) {
            return new HTTPSession(h);
        }
    }

    protected HttpClient HTTP;
    //    protected HttpConnection HTTP;

    private HTTPSession(Host h) {
        super(h);
        //        this.HTTP = new HttpConnection(h.getHostname(), h.getPort());
    }

    public synchronized void close() {
        try {
            if (this.HTTP != null) {
                this.log("Disconnecting...", Message.PROGRESS);
                this.HTTP.quit();
                this.getHost().getLogin().setPassword(null);
                this.HTTP = null;
            }
        }
        catch (IOException e) {
            log.error("IO Error: " + e.getMessage());
//            this.log("IO Error: " + e.getMessage(), Message.ERROR);
        }
        finally {
            this.log("Disconnected", Message.PROGRESS);
            this.setClosed();
        }
    }

    public synchronized void connect() throws IOException {
        this.log("Opening HTTP connection to " + host.getIp() + "...", Message.PROGRESS);
        this.setConnected();
        this.log(new java.util.Date().toString(), Message.TRANSCRIPT);
        this.log(host.getIp(), Message.TRANSCRIPT);
        this.HTTP = new HttpClient();
        if (Preferences.instance().getProperty("connection.proxy.useProxy").equals("true")) {
            this.HTTP.connect(host.getHostname(),
                    host.getPort(),
                    Preferences.instance().getProperty("connection.proxy.host"),
                    Integer.parseInt(Preferences.instance().getProperty("connection.proxy.port")));
        }
        else {
            this.HTTP.connect(host.getHostname(), host.getPort(), false);
        }
        this.log("HTTP connection opened", Message.PROGRESS);
    }

    public synchronized Path workdir() {
        this.log("Invalid Operation", Message.ERROR);
        return null;
    }
	
	public synchronized void check() throws IOException {
        this.log("Working", Message.START);
		//		this.log("Checking connection...", Message.PROGRESS);
        if (null == this.HTTP) {
			this.connect(); return;
		}
		if(!this.HTTP.isAlive()) {
            this.close(); this.connect();
        }
    }
}