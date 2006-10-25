package ch.cyberduck.ui.cocoa;

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

import com.sshtools.j2ssh.transport.AbstractKnownHostsKeyVerification;
import com.sshtools.j2ssh.transport.InvalidHostFileException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;

import ch.cyberduck.core.Preferences;

import com.apple.cocoa.application.NSAlertPanel;
import com.apple.cocoa.application.NSWindow;
import com.apple.cocoa.foundation.NSBundle;

import org.apache.log4j.Logger;

/**
 * Concrete Coccoa implementation of a SSH HostKeyVerification
 *
 * @version $Id$
 */
public class CDHostKeyController extends AbstractKnownHostsKeyVerification
{
    protected static Logger log = Logger.getLogger(CDHostKeyController.class);

    private CDWindowController parent;

    public CDHostKeyController(final CDWindowController windowController) {
        this.parent = windowController;
        try {
            this.setKnownHostFile(Preferences.instance().getProperty("ssh.knownhosts"));
        }
        catch (Exception e) {
            log.error("Could not open or read the host file");
        }
    }

    public void onHostKeyMismatch(final String host, final SshPublicKey allowedHostKey, final SshPublicKey actualHostKey) {
        NSWindow sheet = NSAlertPanel.criticalAlertPanel(NSBundle.localizedString("Host key mismatch:", "") + " " + host, //title
                NSBundle.localizedString("The host key supplied is", "") + ": "
                        + actualHostKey.getFingerprint() +
                        "\n" + NSBundle.localizedString("The current allowed key for this host is", "") + " : "
                        + allowedHostKey.getFingerprint() + "\n" + NSBundle.localizedString("Do you want to allow the host access?", ""),
                NSBundle.localizedString("Allow", ""), // defaultbutton
                NSBundle.localizedString("Deny", ""), //alternative button
                this.isHostFileWriteable() ? NSBundle.localizedString("Always", "") : null //other button
        );
        CDSheetController c = new CDSheetController(parent, sheet) {
            public void callback(final int returncode) {
                try {
                    if (returncode == DEFAULT_OPTION) {
                        allowHost(host, actualHostKey, false);
                    }
                    if (returncode == ALTERNATE_OPTION) {
                        allowHost(host, actualHostKey, true); // always allow host
                    }
                    if (returncode == CANCEL_OPTION) {
                        log.info("Cannot continue without a valid host key");
                    }
                }
                catch (InvalidHostFileException e) {
                    log.error(e.getMessage());
                }
            }
        };
        c.beginSheet(true);
    }

    public void onUnknownHost(final String host,
                              final SshPublicKey actualHostKey) {
        NSWindow sheet = NSAlertPanel.criticalAlertPanel(NSBundle.localizedString("Unknown host key for", "") + " " + host, //title
                NSBundle.localizedString("The host is currently unknown to the system. The host key fingerprint is", "") + ": " + actualHostKey.getFingerprint() + ".",
                NSBundle.localizedString("Allow", ""), // default button
                NSBundle.localizedString("Deny", ""), // alternate button
                this.isHostFileWriteable() ? NSBundle.localizedString("Always", "") : null //other button
        );
        CDSheetController c = new CDSheetController(parent, sheet) {
            public void callback(final int returncode) {
                try {
                    if (returncode == DEFAULT_OPTION) {
                        allowHost(host, actualHostKey, false); // allow host (once)
                    }
                    if (returncode == ALTERNATE_OPTION) {
                        allowHost(host, actualHostKey, true); // allow host (always)
                    }
                    if (returncode == CANCEL_OPTION) {
                        log.info("Cannot continue without a valid host key");
                    }
                }
                catch (InvalidHostFileException e) {
                    log.error(e.getMessage());
                }
            }
        };
        c.beginSheet(true);
    }
}