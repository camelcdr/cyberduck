package ch.cyberduck.core.ftp;

/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
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
import ch.cyberduck.core.ftp.parser.CompositeFileEntryParser;
import ch.cyberduck.core.ftp.parser.LaxUnixFTPEntryParser;
import ch.cyberduck.core.ftp.parser.RumpusFTPEntryParser;
import ch.cyberduck.core.ftps.FTPSClient;
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.core.ssl.AbstractX509TrustManager;
import ch.cyberduck.core.ssl.IgnoreX509TrustManager;
import ch.cyberduck.core.ssl.KeychainX509TrustManager;
import ch.cyberduck.core.ssl.SSLSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.Configurable;
import org.apache.commons.net.ftp.FTPFileEntryParser;
import org.apache.commons.net.ftp.parser.NetwareFTPEntryParser;
import org.apache.commons.net.ftp.parser.ParserInitializationException;
import org.apache.commons.net.ftp.parser.UnixFTPEntryParser;
import org.apache.log4j.Logger;

import com.enterprisedt.net.ftp.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

/**
 * Opens a connection to the remote server via ftp protocol
 *
 * @version $Id$
 */
public class FTPSession extends Session implements SSLSession {
    private static Logger log = Logger.getLogger(FTPSession.class);

    private static class Factory extends SessionFactory {
        @Override
        protected Session create(Host h) {
            return new FTPSession(h);
        }
    }

    public static SessionFactory factory() {
        return new Factory();
    }

    private FTPSClient FTP;
    protected FTPFileEntryParser parser;

    public FTPSession(Host h) {
        super(h);
    }

    @Override
    protected FTPSClient getClient() throws ConnectionCanceledException {
        if(null == FTP) {
            throw new ConnectionCanceledException();
        }
        return FTP;
    }

    private AbstractX509TrustManager trustManager;

    /**
     * @return
     */
    public AbstractX509TrustManager getTrustManager() {
        if(null == trustManager) {
            if(Preferences.instance().getBoolean("ftp.tls.acceptAnyCertificate")) {
                trustManager = new IgnoreX509TrustManager();
            }
            else {
                trustManager = new KeychainX509TrustManager(host.getHostname());
            }
        }
        return trustManager;
    }

    @Override
    protected Path mount(String directory) throws IOException {
        final Path workdir = super.mount(directory);
        if(Preferences.instance().getBoolean("ftp.timezone.auto")) {
            if(null == host.getTimezone()) {
                // No custom timezone set
                final List<TimeZone> matches = this.calculateTimezone();
                for(TimeZone tz : matches) {
                    // Save in bookmark. User should have the option to choose from determined zones.
                    host.setTimezone(tz);
                    break;
                }
                if(!matches.isEmpty()) {
                    // Reset parser to use newly determined timezone
                    parser = null;
                }
            }
        }
        return workdir;
    }

    protected TimeZone getTimezone() throws IOException {
        if(null == host.getTimezone()) {
            return TimeZone.getTimeZone(
                    Preferences.instance().getProperty("ftp.timezone.default"));
        }
        return host.getTimezone();
    }

    private TimeZone tz;

    /**
     * @return
     * @throws IOException
     */
    protected FTPFileEntryParser getFileParser() throws IOException {
        try {
            if(!this.getTimezone().equals(this.tz)) {
                tz = this.getTimezone();
                log.info("Reset parser to timezone:" + tz);
                parser = null;
            }
            if(null == parser) {
                String system = null;
                try {
                    system = this.getClient().system();
                }
                catch(FTPException e) {
                    log.warn(this.host.getHostname() + " does not support the SYST command:" + e.getMessage());
                }
                parser = new FTPParserFactory().createFileEntryParser(system, tz);
                if(parser instanceof Configurable) {
                    // Configure with default configuration
                    ((Configurable) parser).configure(null);
                }
            }
            return parser;
        }
        catch(ParserInitializationException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Best guess of available timezones given the offset of the modification
     * date in the directory listing from the UTC timestamp returned from <code>MDTM</code>
     * if available. Result is error prone because of additional daylight saving offsets.
     */
    private List<TimeZone> calculateTimezone() throws IOException {
        try {
            // Determine the server offset from UTC
            final AttributedList<Path> list = this.workdir().children();
            if(list.isEmpty()) {
                log.warn("Cannot determine timezone with empty directory listing");
                return Collections.emptyList();
            }
            for(Path test : list) {
                if(test.attributes().isFile()) {
                    // Read the modify fact which must be UTC
                    long utc = this.getClient().mdtm(test.getAbsolute());
                    // Subtract seconds
                    utc -= utc % 60000;
                    long local = test.attributes().getModificationDate();
                    if(-1 == local) {
                        log.warn("No modification date in directory listing to calculate timezone");
                        continue;
                    }
                    // Subtract seconds
                    local -= local % 60000;
                    long offset = local - utc;
                    log.info("Calculated UTC offset is " + offset + "ms");
                    final List<TimeZone> zones = new ArrayList<TimeZone>();
                    if(TimeZone.getTimeZone(Preferences.instance().getProperty("ftp.timezone.default")).getOffset(utc) == offset) {
                        log.info("Offset equals local timezone offset.");
                        zones.add(TimeZone.getTimeZone(Preferences.instance().getProperty("ftp.timezone.default")));
                        return zones;
                    }
                    // The offset should be the raw GMT offset without the daylight saving offset.
                    // However the determied offset *does* include daylight saving time and therefore
                    // the call to TimeZone#getAvailableIDs leads to errorneous results.
                    final String[] timezones = TimeZone.getAvailableIDs((int) offset);
                    for(String timezone : timezones) {
                        log.info("Matching timezone identifier:" + timezone);
                        final TimeZone match = TimeZone.getTimeZone(timezone);
                        log.info("Determined timezone:" + match);
                        zones.add(match);
                    }
                    if(zones.isEmpty()) {
                        log.warn("Failed to calculate timezone for offset:" + offset);
                        continue;
                    }
                    return zones;
                }
            }
        }
        catch(FTPException e) {
            log.warn("Failed to calculate timezone:" + e.getMessage());
        }
        log.warn("No file in directory listing to calculate timezone");
        return Collections.emptyList();
    }

    private Map<FTPFileEntryParser, Boolean> parsers = new HashMap<FTPFileEntryParser, Boolean>(1);

    /**
     * @param p
     * @return True if the parser will read the file permissions
     */
    protected boolean isPermissionSupported(final FTPFileEntryParser p) {
        FTPFileEntryParser delegate;
        if(p instanceof CompositeFileEntryParser) {
            // Get the actual parser
            delegate = ((CompositeFileEntryParser) p).getCachedFtpFileEntryParser();
            if(null == delegate) {
                log.warn("Composite FTP parser has no cached delegate yet");
                return false;
            }
        }
        else {
            // Not a composite parser
            delegate = p;
        }
        if(null == parsers.get(delegate)) {
            // Cache the value as it might get queried frequently
            parsers.put(delegate, delegate instanceof UnixFTPEntryParser
                    || delegate instanceof LaxUnixFTPEntryParser
                    || delegate instanceof NetwareFTPEntryParser
                    || delegate instanceof RumpusFTPEntryParser
            );
        }
        return parsers.get(delegate);
    }

    @Override
    public void close() {
        try {
            if(this.isConnected()) {
                this.fireConnectionWillCloseEvent();
                this.getClient().quit();
            }
        }
        catch(FTPException e) {
            log.error("FTP Error: " + e.getMessage());
        }
        catch(IOException e) {
            log.error("IO Error: " + e.getMessage());
        }
        finally {
            FTP = null;
            this.fireConnectionDidCloseEvent();
        }
    }

    @Override
    public void interrupt() {
        try {
            this.fireConnectionWillCloseEvent();
            this.getClient().interrupt();
        }
        catch(IOException e) {
            log.error(e.getMessage());
        }
        finally {
            FTP = null;
            this.fireConnectionDidCloseEvent();
        }
    }

    @Override
    public void check() throws IOException {
        try {
            super.check();
        }
        catch(FTPException e) {
            log.debug(e.getMessage());
            this.interrupt();
            this.connect();
        }
        catch(FTPNullReplyException e) {
            log.debug(e.getMessage());
            this.interrupt();
            this.connect();
        }
    }

    final protected FTPMessageListener messageListener = new FTPMessageListener() {
        public void logCommand(String cmd) {
            FTPSession.this.log(true, cmd);
        }

        public void logReply(String reply) {
            FTPSession.this.log(false, reply);
        }
    };

    protected void configure(FTPSClient client) throws IOException {
        client.setTimeout(this.timeout());
        client.setStatListSupportedEnabled(Preferences.instance().getBoolean("ftp.sendStatListCommand"));
        client.setExtendedListEnabled(Preferences.instance().getBoolean("ftp.sendExtendedListCommand"));
        client.setMlsdListSupportedEnabled(Preferences.instance().getBoolean("ftp.sendMlsdListCommand"));
        client.setStrictReturnCodes(true);
        client.setConnectMode(this.getConnectMode());
        client.setSecureDataSocket(host.getProtocol().isSecure()
                && Preferences.instance().getProperty("ftp.tls.datachannel").equals("P"));
        client.setTrustManager(host.getProtocol().isSecure() ? this.getTrustManager() : null);
        // AUTH command required before login
        auth = true;
    }

    /**
     * @return True if the feaatures AUTH TLS, PBSZ and PROT are supported.
     * @throws IOException
     */
    private boolean isTLSSupported() throws IOException {
        return this.getClient().isFeatureSupported("AUTH TLS")
                && this.getClient().isFeatureSupported("PBSZ")
                && this.getClient().isFeatureSupported("PROT");
    }

    @Override
    protected void connect() throws IOException {
        if(this.isConnected()) {
            return;
        }
        this.fireConnectionWillOpenEvent();

        FTP = new FTPSClient(this.getEncoding(), messageListener);

        this.configure(this.getClient());

        this.getClient().connect(host.getHostname(true), host.getPort());
        if(!this.isConnected()) {
            throw new ConnectionCanceledException();
        }
        this.login();
        this.fireConnectionDidOpenEvent();
        if("UTF-8".equals(this.getEncoding())) {
            this.getClient().utf8();
        }
    }

    /**
     * @return The custom encoding specified in the host of this session
     *         or the default encoding if no cusdtom encoding is set
     * @see Preferences
     * @see Host
     */
    protected FTPConnectMode getConnectMode() {
        if(null == this.host.getFTPConnectMode()) {
            if(ProxyFactory.instance().usePassiveFTP()) {
                return FTPConnectMode.PASV;
            }
            return FTPConnectMode.ACTIVE;
        }
        return this.host.getFTPConnectMode();

    }

    /**
     * Send TLS AUTH command before user credentials.
     */
    private boolean auth;

    private boolean unsecureswitch =
            Preferences.instance().getBoolean("connection.unsecure.switch");

    public boolean isUnsecureswitch() {
        return unsecureswitch;
    }

    public void setUnsecureswitch(boolean unsecureswitch) {
        this.unsecureswitch = unsecureswitch;
    }

    /**
     * Propose protocol change if AUTH TLS is available.
     *
     * @param login
     * @param credentials
     * @throws IOException
     */
    @Override
    protected void warn(LoginController login, Credentials credentials) throws IOException {
        Host host = this.getHost();
        if(this.isUnsecureswitch()
                && !credentials.isAnonymousLogin()
                && !host.getProtocol().isSecure()
                && !Preferences.instance().getBoolean("connection.unsecure." + host.getHostname())
                && this.isTLSSupported()) {
            try {
                login.warn(MessageFormat.format(Locale.localizedString("Unsecured {0} connection", "Credentials"), host.getProtocol().getName()),
                        MessageFormat.format(Locale.localizedString("The server supports encrypted connections. Do you want to switch to {0}?", "Credentials"), Protocol.FTP_TLS.getName()),
                        Locale.localizedString("Change", "Credentials"),
                        Locale.localizedString("Continue", "Credentials"),
                        "connection.unsecure." + host.getHostname());
            }
            catch(LoginCanceledException e) {
                // Continue choosen. Login using plain FTP.
                return;
            }
            finally {
                // Do not warn again upon subsequent login
                this.setUnsecureswitch(false);
            }
            // Protocol switch
            host.setProtocol(Protocol.FTP_TLS);
            if(BookmarkCollection.defaultCollection().contains(host)) {
                BookmarkCollection.defaultCollection().save();
            }
            // Reconfigure client for TLS
            this.configure(this.getClient());
        }
        else {
            super.warn(login, credentials);
        }
    }

    @Override
    protected void login(LoginController controller, Credentials credentials) throws IOException {
        try {
            final FTPClient client = this.getClient();
            if(this.getHost().getProtocol().isSecure()) {
                if(auth) {
                    // Only send AUTH before the first login attempt
                    ((FTPSClient) client).auth();
                    auth = false;
                }
            }
            client.login(credentials.getUsername(), credentials.getPassword());
            this.message(Locale.localizedString("Login successful", "Credentials"));

            if(this.getHost().getProtocol().isSecure()) {
                // Negotiate data connection security
                ((FTPSClient) client).prot();
            }
        }
        catch(FTPException e) {
            this.message(Locale.localizedString("Login failed", "Credentials"));
            controller.fail(host.getProtocol(), credentials, e.getMessage());
            this.login();
        }
    }

    @Override
    public Path workdir() throws IOException {
        if(!this.isConnected()) {
            throw new ConnectionCanceledException();
        }
        if(null == workdir) {
            workdir = PathFactory.createPath(this, this.getClient().pwd(), Path.DIRECTORY_TYPE);
            if(workdir.isRoot()) {
                workdir.attributes().setType(Path.VOLUME_TYPE | Path.DIRECTORY_TYPE);
            }
        }
        return workdir;
    }

    @Override
    public void setWorkdir(Path workdir) throws IOException {
        if(workdir.equals(this.workdir)) {
            // Do not attempt to change the workdir if the same
            return;
        }
        if(!this.isConnected()) {
            throw new ConnectionCanceledException();
        }
        try {
            this.getClient().chdir(workdir.getAbsolute());
        }
        catch(FTPException e) {
            if(StringUtils.isNotBlank(workdir.getSymlinkTarget())) {
                // Try if CWD to symbolic link target succeeds
                this.getClient().chdir(workdir.getSymlinkTarget());
            }
            else {
                throw e;
            }
        }
        // Workdir change succeeded
        super.setWorkdir(workdir);
    }

    @Override
    protected void noop() throws IOException {
        if(this.isConnected()) {
            this.getClient().noop();
        }
    }

    @Override
    public boolean isSendCommandSupported() {
        return true;
    }

    @Override
    public void sendCommand(String command) throws IOException {
        if(this.isConnected()) {
            this.message(command);

            this.getClient().quote(command);
        }
    }

    @Override
    public boolean isDownloadResumable() {
        return this.isTransferResumable();
    }

    @Override
    public boolean isUploadResumable() {
        return this.isTransferResumable();
    }

    /**
     * No resume supported for ASCII mode transfers.
     *
     * @return
     */
    private boolean isTransferResumable() {
        return Preferences.instance().getProperty("ftp.transfermode").equals(
                FTPTransferType.BINARY.toString());
    }
}