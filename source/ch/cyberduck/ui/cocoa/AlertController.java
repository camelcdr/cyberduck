package ch.cyberduck.ui.cocoa;

/*
 * Copyright (c) 2002-2009 David Kocher. All rights reserved.
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

import ch.cyberduck.core.Preferences;
import ch.cyberduck.ui.cocoa.application.*;
import ch.cyberduck.ui.cocoa.foundation.NSEnumerator;
import ch.cyberduck.ui.cocoa.foundation.NSObject;

import org.rococoa.Foundation;
import org.rococoa.ID;
import org.rococoa.Rococoa;
import org.rococoa.cocoa.foundation.NSRect;

import org.apache.log4j.Logger;

public abstract class AlertController extends SheetController {
    protected static Logger log = Logger.getLogger(AlertController.class);

    /**
     * If using alert and no custom window
     */
    protected NSAlert alert;

    /**
     * @param parent
     * @param alert
     */
    public AlertController(final WindowController parent, NSAlert alert) {
        this(parent, alert, NSAlert.NSWarningAlertStyle);
    }

    /**
     * @param parent
     * @param alert
     * @param style
     */
    public AlertController(final WindowController parent, NSAlert alert, int style) {
        super(parent);
        this.alert = alert;
        this.alert.setAlertStyle(style);
        this.alert.setDelegate(this.id());
    }

    public void setAccessoryView(NSView view) {
        view.setFrame(new NSRect(300, view.frame().size.height.floatValue()));
        alert.setAccessoryView(view);
    }

    @Override
    public void beginSheet() {
        super.beginSheet();
        this.focus();
    }

    @Override
    protected void beginSheetImpl() {
        parent.window().makeKeyAndOrderFront(null);
        alert.layout();
        NSEnumerator buttons = alert.buttons().objectEnumerator();
        NSObject button;
        while(((button = buttons.nextObject()) != null)) {
            final NSButton b = Rococoa.cast(button, NSButton.class);
            b.setTarget(this.id());
            b.setAction(Foundation.selector("closeSheet:"));
        }
        alert.beginSheet(parent.window(), this.id(), Foundation.selector("alertDidEnd:returnCode:contextInfo:"), null);
        sheetRegistry.add(this);
    }

    protected void focus() {
        ;
    }

    protected void setTitle(String title) {
        alert.setMessageText(title);
    }

    protected void setMessage(String message) {
        alert.setInformativeText(message);
    }

    /**
     * Message the alert sends to modalDelegate after the user responds but before the sheet is dismissed.
     *
     * @param alert
     * @param returnCode
     * @param contextInfo
     */
    public void alertDidEnd_returnCode_contextInfo(NSAlert alert, int returnCode, ID contextInfo) {
        this.sheetDidClose_returnCode_contextInfo(alert.window(), returnCode, contextInfo);
    }

    @Override
    protected int getCallbackOption(NSButton selected) {
        if(selected.tag() == NSPanel.NSAlertDefaultReturn) {
            return SheetCallback.DEFAULT_OPTION;
        }
        if(selected.tag() == NSPanel.NSAlertAlternateReturn) {
            return SheetCallback.ALTERNATE_OPTION;
        }
        if(selected.tag() == NSPanel.NSAlertOtherReturn) {
            return SheetCallback.CANCEL_OPTION;
        }
        throw new RuntimeException("Unexpected tag:" + selected.tag());
    }

    /**
     * Open help page.
     */
    protected void help() {
        openUrl(Preferences.instance().getProperty("website.help"));
    }

    /**
     * When the help button is pressed, the alert delegate (delegate) is first sent a alertShowHelp: message.
     *
     * @param alert
     * @return True if help request was handled.
     */
    public boolean alertShowHelp(NSAlert alert) {
        this.help();
        return true;
    }

    @Override
    public NSWindow window() {
        return alert.window();
    }
}