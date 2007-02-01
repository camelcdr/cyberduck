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

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPTransferType;

import ch.cyberduck.core.*;
import ch.cyberduck.ui.cocoa.odb.Editor;

import com.apple.cocoa.application.*;
import com.apple.cocoa.foundation.*;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @version $Id$
 */
public class CDPreferencesController extends CDWindowController {
    private static Logger log = Logger.getLogger(CDPreferencesController.class);

    private static CDPreferencesController instance;

    public static CDPreferencesController instance() {
        synchronized(NSApplication.sharedApplication()) {
            if (null == instance) {
                instance = new CDPreferencesController();
                if (!NSApplication.loadNibNamed("Preferences", instance)) {
                    log.fatal("Couldn't load Preferences.nib");
                }
            }
            return instance;
        }
    }

    private CDPreferencesController() {
        ;
    }

    private NSTabView tabView;

    public void setTabView(NSTabView tabView) {
        this.tabView = tabView;
    }

    private NSView panelGeneral;
    private NSView panelInterface;
    private NSView panelTransfer;
    private NSView panelFTP;
    private NSView panelFTPTLS;
    private NSView panelSFTP;
    private NSView panelAdvanced;
    private NSView panelUpdate;

    public void setPanelUpdate(NSView panelUpdate) {
        this.panelUpdate = panelUpdate;
    }

    public void setPanelAdvanced(NSView panelAdvanced) {
        this.panelAdvanced = panelAdvanced;
    }

    public void setPanelSFTP(NSView panelSFTP) {
        this.panelSFTP = panelSFTP;
    }

    public void setPanelFTPTLS(NSView panelFTPTLS) {
        this.panelFTPTLS = panelFTPTLS;
    }

    public void setPanelFTP(NSView panelFTP) {
        this.panelFTP = panelFTP;
    }

    public void setPanelTransfer(NSView panelTransfer) {
        this.panelTransfer = panelTransfer;
    }

    public void setPanelInterface(NSView panelInterface) {
        this.panelInterface = panelInterface;
    }

    public void setPanelGeneral(NSView panelGeneral) {
        this.panelGeneral = panelGeneral;
    }

    public void windowWillClose(NSNotification notification) {
        super.windowWillClose(notification);
        instance = null;
    }

    public void setWindow(NSWindow window) {
//        String file = null;
//        if(null == NSBundle.mainBundle().objectForInfoDictionaryKey("application.preferences.path")) {
//            file = NSPathUtilities.stringByExpandingTildeInPath("~/Library/Preferences")
//                    +"/"+NSBundle.mainBundle().infoDictionary().objectForKey("CFBundleIdentifier")+".plist";
//        }
//        else {
//            file = NSPathUtilities.stringByExpandingTildeInPath(
//                    (String)NSBundle.mainBundle().objectForInfoDictionaryKey("application.preferences.path"));
//        }
//        window.setRepresentedFilename(file);
        window.setExcludedFromWindowsMenu(true);
        super.setWindow(window);
    }

    public void awakeFromNib() {
        NSSelector setShowsToolbarButtonSelector
                = new NSSelector("setShowsToolbarButton", new Class[]{boolean.class});
        if (setShowsToolbarButtonSelector.implementedByClass(NSWindow.class)) {
            this.window.setShowsToolbarButton(false);
        }
        this.window.center();

        this.transfermodeComboboxClicked(this.transfermodeCombobox);
        {
            Permission p = new Permission(Preferences.instance().getProperty("queue.upload.permissions.default"));
            boolean[] ownerPerm = p.getOwnerPermissions();
            boolean[] groupPerm = p.getGroupPermissions();
            boolean[] otherPerm = p.getOtherPermissions();

            uownerr.setState(ownerPerm[Permission.READ] ? NSCell.OnState : NSCell.OffState);
            uownerw.setState(ownerPerm[Permission.WRITE] ? NSCell.OnState : NSCell.OffState);
            uownerx.setState(ownerPerm[Permission.EXECUTE] ? NSCell.OnState : NSCell.OffState);

            ugroupr.setState(groupPerm[Permission.READ] ? NSCell.OnState : NSCell.OffState);
            ugroupw.setState(groupPerm[Permission.WRITE] ? NSCell.OnState : NSCell.OffState);
            ugroupx.setState(groupPerm[Permission.EXECUTE] ? NSCell.OnState : NSCell.OffState);

            uotherr.setState(otherPerm[Permission.READ] ? NSCell.OnState : NSCell.OffState);
            uotherw.setState(otherPerm[Permission.WRITE] ? NSCell.OnState : NSCell.OffState);
            uotherx.setState(otherPerm[Permission.EXECUTE] ? NSCell.OnState : NSCell.OffState);
        }
        {
            Permission p = new Permission(Preferences.instance().getProperty("queue.download.permissions.default"));
            boolean[] ownerPerm = p.getOwnerPermissions();
            boolean[] groupPerm = p.getGroupPermissions();
            boolean[] otherPerm = p.getOtherPermissions();

            downerr.setState(ownerPerm[Permission.READ] ? NSCell.OnState : NSCell.OffState);
            downerw.setState(ownerPerm[Permission.WRITE] ? NSCell.OnState : NSCell.OffState);
            downerx.setState(ownerPerm[Permission.EXECUTE] ? NSCell.OnState : NSCell.OffState);

            dgroupr.setState(groupPerm[Permission.READ] ? NSCell.OnState : NSCell.OffState);
            dgroupw.setState(groupPerm[Permission.WRITE] ? NSCell.OnState : NSCell.OffState);
            dgroupx.setState(groupPerm[Permission.EXECUTE] ? NSCell.OnState : NSCell.OffState);

            dotherr.setState(otherPerm[Permission.READ] ? NSCell.OnState : NSCell.OffState);
            dotherw.setState(otherPerm[Permission.WRITE] ? NSCell.OnState : NSCell.OffState);
            dotherx.setState(otherPerm[Permission.EXECUTE] ? NSCell.OnState : NSCell.OffState);
        }

        boolean chmodDownloadDefaultEnabled = Preferences.instance().getBoolean("queue.download.changePermissions")
                && Preferences.instance().getBoolean("queue.download.permissions.useDefault");
        this.downerr.setEnabled(chmodDownloadDefaultEnabled);
        this.downerr.setTarget(this);
        this.downerr.setAction(new NSSelector("defaultPermissionsDownloadChanged", new Class[]{NSButton.class}));
        this.downerw.setEnabled(chmodDownloadDefaultEnabled);
        this.downerw.setTarget(this);
        this.downerw.setAction(new NSSelector("defaultPermissionsDownloadChanged", new Class[]{NSButton.class}));
        this.downerx.setEnabled(chmodDownloadDefaultEnabled);
        this.downerx.setTarget(this);
        this.downerx.setAction(new NSSelector("defaultPermissionsDownloadChanged", new Class[]{NSButton.class}));

        this.dgroupr.setEnabled(chmodDownloadDefaultEnabled);
        this.dgroupr.setTarget(this);
        this.dgroupr.setAction(new NSSelector("defaultPermissionsDownloadChanged", new Class[]{NSButton.class}));
        this.dgroupw.setEnabled(chmodDownloadDefaultEnabled);
        this.dgroupw.setTarget(this);
        this.dgroupw.setAction(new NSSelector("defaultPermissionsDownloadChanged", new Class[]{NSButton.class}));
        this.dgroupx.setEnabled(chmodDownloadDefaultEnabled);
        this.dgroupx.setTarget(this);
        this.dgroupx.setAction(new NSSelector("defaultPermissionsDownloadChanged", new Class[]{NSButton.class}));

        this.dotherr.setEnabled(chmodDownloadDefaultEnabled);
        this.dotherr.setTarget(this);
        this.dotherr.setAction(new NSSelector("defaultPermissionsDownloadChanged", new Class[]{NSButton.class}));
        this.dotherw.setEnabled(chmodDownloadDefaultEnabled);
        this.dotherw.setTarget(this);
        this.dotherw.setAction(new NSSelector("defaultPermissionsDownloadChanged", new Class[]{NSButton.class}));
        this.dotherx.setEnabled(chmodDownloadDefaultEnabled);
        this.dotherx.setTarget(this);
        this.dotherx.setAction(new NSSelector("defaultPermissionsDownloadChanged", new Class[]{NSButton.class}));

        boolean chmodUploadDefaultEnabled = Preferences.instance().getBoolean("queue.upload.changePermissions")
                && Preferences.instance().getBoolean("queue.upload.permissions.useDefault");
        this.uownerr.setEnabled(chmodUploadDefaultEnabled);
        this.uownerr.setTarget(this);
        this.uownerr.setAction(new NSSelector("defaultPermissionsUploadChanged", new Class[]{NSButton.class}));
        this.uownerw.setEnabled(chmodUploadDefaultEnabled);
        this.uownerw.setTarget(this);
        this.uownerw.setAction(new NSSelector("defaultPermissionsUploadChanged", new Class[]{NSButton.class}));
        this.uownerx.setEnabled(chmodUploadDefaultEnabled);
        this.uownerx.setTarget(this);
        this.uownerx.setAction(new NSSelector("defaultPermissionsUploadChanged", new Class[]{NSButton.class}));

        this.ugroupr.setEnabled(chmodUploadDefaultEnabled);
        this.ugroupr.setTarget(this);
        this.ugroupr.setAction(new NSSelector("defaultPermissionsUploadChanged", new Class[]{NSButton.class}));
        this.ugroupw.setEnabled(chmodUploadDefaultEnabled);
        this.ugroupw.setTarget(this);
        this.ugroupw.setAction(new NSSelector("defaultPermissionsUploadChanged", new Class[]{NSButton.class}));
        this.ugroupx.setEnabled(chmodUploadDefaultEnabled);
        this.ugroupx.setTarget(this);
        this.ugroupx.setAction(new NSSelector("defaultPermissionsUploadChanged", new Class[]{NSButton.class}));

        this.uotherr.setEnabled(chmodUploadDefaultEnabled);
        this.uotherr.setTarget(this);
        this.uotherr.setAction(new NSSelector("defaultPermissionsUploadChanged", new Class[]{NSButton.class}));
        this.uotherw.setEnabled(chmodUploadDefaultEnabled);
        this.uotherw.setTarget(this);
        this.uotherw.setAction(new NSSelector("defaultPermissionsUploadChanged", new Class[]{NSButton.class}));
        this.uotherx.setEnabled(chmodUploadDefaultEnabled);
        this.uotherx.setTarget(this);
        this.uotherx.setAction(new NSSelector("defaultPermissionsUploadChanged", new Class[]{NSButton.class}));

        tabView.tabViewItemAtIndex(0).setView(panelGeneral);
        tabView.tabViewItemAtIndex(1).setView(panelInterface);
        tabView.tabViewItemAtIndex(2).setView(panelTransfer);
        tabView.tabViewItemAtIndex(3).setView(panelFTP);
        tabView.tabViewItemAtIndex(4).setView(panelFTPTLS);
        tabView.tabViewItemAtIndex(5).setView(panelSFTP);
        tabView.tabViewItemAtIndex(6).setView(panelAdvanced);
        tabView.tabViewItemAtIndex(7).setView(panelUpdate);
    }

    private static final String CONNECTMODE_ACTIVE = NSBundle.localizedString("Active", "");
    private static final String CONNECTMODE_PASSIVE = NSBundle.localizedString("Passive", "");

    private static final String TRANSFERMODE_AUTO = NSBundle.localizedString("Auto", "");
    private static final String TRANSFERMODE_BINARY = NSBundle.localizedString("Binary", "");
    private static final String TRANSFERMODE_ASCII = NSBundle.localizedString("ASCII", "");

    private static final String UNIX_LINE_ENDINGS = NSBundle.localizedString("Unix Line Endings (LF)", "");
    private static final String MAC_LINE_ENDINGS = NSBundle.localizedString("Mac Line Endings (CR)", "");
    private static final String WINDOWS_LINE_ENDINGS = NSBundle.localizedString("Windows Line Endings (CRLF)", "");

    private static final String PROTOCOL_FTP = "FTP";
    private static final String PROTOCOL_FTP_TLS = "FTP-TLS";
    private static final String PROTOCOL_SFTP = "SFTP";

    private static final String ASK_ME_WHAT_TO_DO = NSBundle.localizedString("Ask me what to do", "");
    private static final String OVERWRITE_EXISTING_FILE = NSBundle.localizedString("Overwrite existing file", "");
    private static final String TRY_TO_RESUME_TRANSFER = NSBundle.localizedString("Try to resume transfer", "");
    private static final String USE_A_SIMILAR_NAME = NSBundle.localizedString("Use similar name", "");

    // ----------------------------------------------------------
    // Outlets
    // ----------------------------------------------------------

    private NSPopUpButton editorCombobox; //IBOutlet

    public void setEditorCombobox(NSPopUpButton editorCombobox) {
        this.editorCombobox = editorCombobox;
        this.editorCombobox.setAutoenablesItems(false);
        this.editorCombobox.removeAllItems();
        java.util.Map editors = Editor.SUPPORTED_EDITORS;
        java.util.Iterator editorNames = editors.keySet().iterator();
        java.util.Iterator editorIdentifiers = editors.values().iterator();
        while (editorNames.hasNext()) {
            String editor = (String) editorNames.next();
            String identifier = (String) editorIdentifiers.next();
            this.editorCombobox.addItem(editor);
            boolean enabled = NSWorkspace.sharedWorkspace().absolutePathForAppBundleWithIdentifier(
                    identifier) != null;
            this.editorCombobox.itemWithTitle(editor).setEnabled(enabled);
            if (enabled) {
                NSImage icon = NSWorkspace.sharedWorkspace().iconForFile(
                        NSWorkspace.sharedWorkspace().absolutePathForAppBundleWithIdentifier(identifier)
                );
                icon.setSize(new NSSize(16f, 16f));
                this.editorCombobox.itemWithTitle(editor).setImage(icon);
            }
        }
        this.editorCombobox.setTarget(this);
        this.editorCombobox.setAction(new NSSelector("editorComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.editorCombobox.selectItemWithTitle(Preferences.instance().getProperty("editor.name"));
    }

    public void editorComboboxClicked(NSPopUpButton sender) {
        Preferences.instance().setProperty("editor.name", sender.titleOfSelectedItem());
        Preferences.instance().setProperty("editor.bundleIdentifier", (String) Editor.SUPPORTED_EDITORS.get(sender.titleOfSelectedItem()));
        CDBrowserController.validateToolbarItems();
    }

    private NSButton openUntitledBrowserCheckbox; //IBOutlet

    public void setOpenUntitledBrowserCheckbox(NSButton openUntitledBrowserCheckbox) {
        this.openUntitledBrowserCheckbox = openUntitledBrowserCheckbox;
        this.openUntitledBrowserCheckbox.setTarget(this);
        this.openUntitledBrowserCheckbox.setAction(new NSSelector("openUntitledBrowserCheckboxClicked", new Class[]{NSButton.class}));
        this.openUntitledBrowserCheckbox.setState(Preferences.instance().getBoolean("browser.openUntitled") ? NSCell.OnState : NSCell.OffState);
    }

    public void openUntitledBrowserCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("browser.openUntitled", enabled);
    }

    private NSButton browserSerializeCheckbox; //IBOutlet

    public void setBrowserSerializeCheckbox(NSButton browserSerializeCheckbox) {
        this.browserSerializeCheckbox = browserSerializeCheckbox;
        this.browserSerializeCheckbox.setTarget(this);
        this.browserSerializeCheckbox.setAction(new NSSelector("browserSerializeCheckboxClicked", new Class[]{NSButton.class}));
        this.browserSerializeCheckbox.setState(Preferences.instance().getBoolean("browser.serialize") ? NSCell.OnState : NSCell.OffState);
    }

    public void browserSerializeCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("browser.serialize", enabled);
    }

    private NSPopUpButton defaultBookmarkCombobox; //IBOutlet

    private Map bookmarks = new HashMap();

    public void setDefaultBookmarkCombobox(NSPopUpButton defaultBookmarkCombobox) {
        this.defaultBookmarkCombobox = defaultBookmarkCombobox;
        this.defaultBookmarkCombobox.setToolTip(NSBundle.localizedString("Bookmarks", ""));
        this.defaultBookmarkCombobox.removeAllItems();
        this.defaultBookmarkCombobox.addItem(NSBundle.localizedString("None", ""));
        this.defaultBookmarkCombobox.menu().addItem(new NSMenuItem().separatorItem());
        Iterator iter = HostCollection.instance().iterator();
        while(iter.hasNext()) {
            Host bookmark = (Host) iter.next();
            this.defaultBookmarkCombobox.addItem(bookmark.getNickname());
            this.defaultBookmarkCombobox.itemWithTitle(bookmark.getNickname()).setImage(NSImage.imageNamed("bookmark16.tiff"));
            this.bookmarks.put(bookmark.getNickname(), bookmark);
        }
        HostCollection.instance().addListener(new CollectionListener() {
            public void collectionItemAdded(Object item) {
                Host bookmark = (Host) item;
                CDPreferencesController.this.defaultBookmarkCombobox.addItem(bookmark.getNickname());
                CDPreferencesController.this.defaultBookmarkCombobox.itemWithTitle(bookmark.getNickname()).setImage(NSImage.imageNamed("bookmark16.tiff"));
                bookmarks.put(bookmark.getNickname(), bookmark);
            }

            public void collectionItemRemoved(Object item) {
                Host bookmark = (Host) item;
                if(CDPreferencesController.this.defaultBookmarkCombobox.titleOfSelectedItem().equals(bookmark.getNickname())) {
                    Preferences.instance().deleteProperty("browser.defaultBookmark");
                }
                CDPreferencesController.this.defaultBookmarkCombobox.removeItemWithTitle(bookmark.getNickname());
                bookmarks.remove(bookmark.getNickname());
            }

            public void collectionItemChanged(Object item) {
                ;
            }
        });
        this.defaultBookmarkCombobox.setTarget(this);
        final NSSelector action = new NSSelector("defaultBookmarkComboboxClicked", new Class[]{NSPopUpButton.class});
        this.defaultBookmarkCombobox.setAction(action);
        String defaultBookmark = Preferences.instance().getProperty("browser.defaultBookmark");
        if(null == defaultBookmark) {
            this.defaultBookmarkCombobox.selectItemWithTitle(NSBundle.localizedString("None", ""));

        }
        else {
            if(null == this.bookmarks.get(defaultBookmark)) {
                this.defaultBookmarkCombobox.selectItemWithTitle(NSBundle.localizedString("None", ""));
            }
            else {
                this.defaultBookmarkCombobox.selectItemWithTitle(defaultBookmark);
            }
        }
    }

    public void defaultBookmarkComboboxClicked(NSPopUpButton sender) {
        if(NSBundle.localizedString("None", "").equals(sender.titleOfSelectedItem())) {
            Preferences.instance().deleteProperty("browser.defaultBookmark");
        }
        else {
            Preferences.instance().setProperty("browser.defaultBookmark", sender.titleOfSelectedItem());
        }
    }

    private NSPopUpButton encodingCombobox; //IBOutlet

    public void setEncodingCombobox(NSPopUpButton encodingCombobox) {
        this.encodingCombobox = encodingCombobox;
        this.encodingCombobox.setTarget(this);
        this.encodingCombobox.setAction(new NSSelector("encodingComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.encodingCombobox.removeAllItems();
        this.encodingCombobox.addItemsWithTitles(new NSArray(((CDMainController)NSApplication.sharedApplication().delegate()).availableCharsets()));
        this.encodingCombobox.selectItemWithTitle(Preferences.instance().getProperty("browser.charset.encoding"));
    }

    public void encodingComboboxClicked(NSPopUpButton sender) {
        Preferences.instance().setProperty("browser.charset.encoding", sender.titleOfSelectedItem());
    }

    private NSButton listCheckbox; //IBOutlet

    public void setListCheckbox(NSButton listCheckbox) {
        this.listCheckbox = listCheckbox;
        this.listCheckbox.setTarget(this);
        this.listCheckbox.setAction(new NSSelector("listCheckboxClicked", new Class[]{NSButton.class}));
        this.listCheckbox.setState(Preferences.instance().getBoolean("ftp.sendExtendedListCommand") ? NSCell.OnState : NSCell.OffState);
    }

    public void listCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("ftp.sendExtendedListCommand", enabled);
    }

    private NSButton systCheckbox; //IBOutlet

    public void setSystCheckbox(NSButton systCheckbox) {
        this.systCheckbox = systCheckbox;
        this.systCheckbox.setTarget(this);
        this.systCheckbox.setAction(new NSSelector("systCheckboxClicked", new Class[]{NSButton.class}));
        this.systCheckbox.setState(Preferences.instance().getBoolean("ftp.sendSystemCommand") ? NSCell.OnState : NSCell.OffState);
    }

    public void systCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("ftp.sendSystemCommand", enabled);
    }

    private NSButton chmodUploadCheckbox; //IBOutlet

    public void setChmodUploadCheckbox(NSButton chmodUploadCheckbox) {
        this.chmodUploadCheckbox = chmodUploadCheckbox;
        this.chmodUploadCheckbox.setTarget(this);
        this.chmodUploadCheckbox.setAction(new NSSelector("chmodUploadCheckboxClicked", new Class[]{NSButton.class}));
        this.chmodUploadCheckbox.setState(Preferences.instance().getBoolean("queue.upload.changePermissions") ? NSCell.OnState : NSCell.OffState);
    }

    public void chmodUploadCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.upload.changePermissions", enabled);
        this.chmodUploadDefaultCheckbox.setEnabled(enabled);
        this.chmodUploadCustomCheckbox.setEnabled(enabled);
        boolean chmodUploadDefaultChecked = this.chmodUploadDefaultCheckbox.state() == NSCell.OnState;
        this.uownerr.setEnabled(enabled && chmodUploadDefaultChecked);
        this.uownerw.setEnabled(enabled && chmodUploadDefaultChecked);
        this.uownerx.setEnabled(enabled && chmodUploadDefaultChecked);
        this.ugroupr.setEnabled(enabled && chmodUploadDefaultChecked);
        this.ugroupw.setEnabled(enabled && chmodUploadDefaultChecked);
        this.ugroupx.setEnabled(enabled && chmodUploadDefaultChecked);
        this.uotherr.setEnabled(enabled && chmodUploadDefaultChecked);
        this.uotherw.setEnabled(enabled && chmodUploadDefaultChecked);
        this.uotherx.setEnabled(enabled && chmodUploadDefaultChecked);
    }

    private NSButton chmodUploadDefaultCheckbox; //IBOutlet

    public void setChmodUploadDefaultCheckbox(NSButton chmodUploadDefaultCheckbox) {
        this.chmodUploadDefaultCheckbox = chmodUploadDefaultCheckbox;
        this.chmodUploadDefaultCheckbox.setTarget(this);
        this.chmodUploadDefaultCheckbox.setAction(new NSSelector("chmodUploadDefaultCheckboxClicked", new Class[]{NSButton.class}));
        this.chmodUploadDefaultCheckbox.setState(Preferences.instance().getBoolean("queue.upload.permissions.useDefault") ? NSCell.OnState : NSCell.OffState);
        this.chmodUploadDefaultCheckbox.setEnabled(Preferences.instance().getBoolean("queue.upload.changePermissions"));
    }

    public void chmodUploadDefaultCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.upload.permissions.useDefault", enabled);
        this.uownerr.setEnabled(enabled);
        this.uownerw.setEnabled(enabled);
        this.uownerx.setEnabled(enabled);
        this.ugroupr.setEnabled(enabled);
        this.ugroupw.setEnabled(enabled);
        this.ugroupx.setEnabled(enabled);
        this.uotherr.setEnabled(enabled);
        this.uotherw.setEnabled(enabled);
        this.uotherx.setEnabled(enabled);
        this.chmodUploadCustomCheckbox.setState(!enabled ? NSCell.OnState : NSCell.OffState);
    }

    private NSButton chmodUploadCustomCheckbox; //IBOutlet

    public void setChmodUploadCustomCheckbox(NSButton chmodUploadCustomCheckbox) {
        this.chmodUploadCustomCheckbox = chmodUploadCustomCheckbox;
        this.chmodUploadCustomCheckbox.setTarget(this);
        this.chmodUploadCustomCheckbox.setAction(new NSSelector("chmodUploadCustomCheckboxClicked", new Class[]{NSButton.class}));
        this.chmodUploadCustomCheckbox.setState(!Preferences.instance().getBoolean("queue.upload.permissions.useDefault") ? NSCell.OnState : NSCell.OffState);
        this.chmodUploadCustomCheckbox.setEnabled(Preferences.instance().getBoolean("queue.upload.changePermissions"));
    }

    public void chmodUploadCustomCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.upload.permissions.useDefault", !enabled);
        this.uownerr.setEnabled(!enabled);
        this.uownerw.setEnabled(!enabled);
        this.uownerx.setEnabled(!enabled);
        this.ugroupr.setEnabled(!enabled);
        this.ugroupw.setEnabled(!enabled);
        this.ugroupx.setEnabled(!enabled);
        this.uotherr.setEnabled(!enabled);
        this.uotherw.setEnabled(!enabled);
        this.uotherx.setEnabled(!enabled);
        this.chmodUploadDefaultCheckbox.setState(!enabled ? NSCell.OnState : NSCell.OffState);
    }

    private NSButton chmodDownloadCheckbox; //IBOutlet

    public void setChmodDownloadCheckbox(NSButton chmodDownloadCheckbox) {
        this.chmodDownloadCheckbox = chmodDownloadCheckbox;
        this.chmodDownloadCheckbox.setTarget(this);
        this.chmodDownloadCheckbox.setAction(new NSSelector("chmodDownloadCheckboxClicked", new Class[]{NSButton.class}));
        this.chmodDownloadCheckbox.setState(Preferences.instance().getBoolean("queue.download.changePermissions") ? NSCell.OnState : NSCell.OffState);
    }

    public void chmodDownloadCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.download.changePermissions", enabled);
        this.chmodDownloadDefaultCheckbox.setEnabled(enabled);
        this.chmodDownloadCustomCheckbox.setEnabled(enabled);
        boolean chmodDownloadDefaultChecked = this.chmodDownloadDefaultCheckbox.state() == NSCell.OnState;
        this.downerr.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.downerw.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.downerx.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.dgroupr.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.dgroupw.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.dgroupx.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.dotherr.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.dotherw.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.dotherx.setEnabled(enabled && chmodDownloadDefaultChecked);
    }

    private NSButton chmodDownloadDefaultCheckbox; //IBOutlet

    public void setChmodDownloadDefaultCheckbox(NSButton chmodDownloadDefaultCheckbox) {
        this.chmodDownloadDefaultCheckbox = chmodDownloadDefaultCheckbox;
        this.chmodDownloadDefaultCheckbox.setTarget(this);
        this.chmodDownloadDefaultCheckbox.setAction(new NSSelector("chmodDownloadDefaultCheckboxClicked", new Class[]{NSButton.class}));
        this.chmodDownloadDefaultCheckbox.setState(Preferences.instance().getBoolean("queue.download.permissions.useDefault") ? NSCell.OnState : NSCell.OffState);
        this.chmodDownloadDefaultCheckbox.setEnabled(Preferences.instance().getBoolean("queue.download.changePermissions"));
    }

    public void chmodDownloadDefaultCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.download.permissions.useDefault", enabled);
        this.downerr.setEnabled(enabled);
        this.downerw.setEnabled(enabled);
        this.downerx.setEnabled(enabled);
        this.dgroupr.setEnabled(enabled);
        this.dgroupw.setEnabled(enabled);
        this.dgroupx.setEnabled(enabled);
        this.dotherr.setEnabled(enabled);
        this.dotherw.setEnabled(enabled);
        this.dotherx.setEnabled(enabled);
        this.chmodDownloadCustomCheckbox.setState(!enabled ? NSCell.OnState : NSCell.OffState);
    }

    private NSButton chmodDownloadCustomCheckbox; //IBOutlet

    public void setChmodDownloadCustomCheckbox(NSButton chmodDownloadCustomCheckbox) {
        this.chmodDownloadCustomCheckbox = chmodDownloadCustomCheckbox;
        this.chmodDownloadCustomCheckbox.setTarget(this);
        this.chmodDownloadCustomCheckbox.setAction(new NSSelector("chmodDownloadCustomCheckboxClicked", new Class[]{NSButton.class}));
        this.chmodDownloadCustomCheckbox.setState(!Preferences.instance().getBoolean("queue.download.permissions.useDefault") ? NSCell.OnState : NSCell.OffState);
        this.chmodDownloadCustomCheckbox.setEnabled(Preferences.instance().getBoolean("queue.download.changePermissions"));
    }

    public void chmodDownloadCustomCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.download.permissions.useDefault", !enabled);
        this.downerr.setEnabled(!enabled);
        this.downerw.setEnabled(!enabled);
        this.downerx.setEnabled(!enabled);
        this.dgroupr.setEnabled(!enabled);
        this.dgroupw.setEnabled(!enabled);
        this.dgroupx.setEnabled(!enabled);
        this.dotherr.setEnabled(!enabled);
        this.dotherw.setEnabled(!enabled);
        this.dotherx.setEnabled(!enabled);
        this.chmodDownloadDefaultCheckbox.setState(!enabled ? NSCell.OnState : NSCell.OffState);
    }

    public NSButton downerr; //IBOutlet
    public NSButton downerw; //IBOutlet
    public NSButton downerx; //IBOutlet
    public NSButton dgroupr; //IBOutlet
    public NSButton dgroupw; //IBOutlet
    public NSButton dgroupx; //IBOutlet
    public NSButton dotherr; //IBOutlet
    public NSButton dotherw; //IBOutlet
    public NSButton dotherx; //IBOutlet

    public void defaultPermissionsDownloadChanged(final Object sender) {
        boolean[][] p = new boolean[3][3];

        p[Permission.OWNER][Permission.READ] = (downerr.state() == NSCell.OnState);
        p[Permission.OWNER][Permission.WRITE] = (downerw.state() == NSCell.OnState);
        p[Permission.OWNER][Permission.EXECUTE] = (downerx.state() == NSCell.OnState);

        p[Permission.GROUP][Permission.READ] = (dgroupr.state() == NSCell.OnState);
        p[Permission.GROUP][Permission.WRITE] = (dgroupw.state() == NSCell.OnState);
        p[Permission.GROUP][Permission.EXECUTE] = (dgroupx.state() == NSCell.OnState);

        p[Permission.OTHER][Permission.READ] = (dotherr.state() == NSCell.OnState);
        p[Permission.OTHER][Permission.WRITE] = (dotherw.state() == NSCell.OnState);
        p[Permission.OTHER][Permission.EXECUTE] = (dotherx.state() == NSCell.OnState);

        Permission permission = new Permission(p);
        Preferences.instance().setProperty("queue.download.permissions.default", permission.getMask());
    }

    public NSButton uownerr; //IBOutlet
    public NSButton uownerw; //IBOutlet
    public NSButton uownerx; //IBOutlet
    public NSButton ugroupr; //IBOutlet
    public NSButton ugroupw; //IBOutlet
    public NSButton ugroupx; //IBOutlet
    public NSButton uotherr; //IBOutlet
    public NSButton uotherw; //IBOutlet
    public NSButton uotherx; //IBOutlet

    public void defaultPermissionsUploadChanged(final Object sender) {
        boolean[][] p = new boolean[3][3];

        p[Permission.OWNER][Permission.READ] = (uownerr.state() == NSCell.OnState);
        p[Permission.OWNER][Permission.WRITE] = (uownerw.state() == NSCell.OnState);
        p[Permission.OWNER][Permission.EXECUTE] = (uownerx.state() == NSCell.OnState);

        p[Permission.GROUP][Permission.READ] = (ugroupr.state() == NSCell.OnState);
        p[Permission.GROUP][Permission.WRITE] = (ugroupw.state() == NSCell.OnState);
        p[Permission.GROUP][Permission.EXECUTE] = (ugroupx.state() == NSCell.OnState);

        p[Permission.OTHER][Permission.READ] = (uotherr.state() == NSCell.OnState);
        p[Permission.OTHER][Permission.WRITE] = (uotherw.state() == NSCell.OnState);
        p[Permission.OTHER][Permission.EXECUTE] = (uotherx.state() == NSCell.OnState);

        Permission permission = new Permission(p);
        Preferences.instance().setProperty("queue.upload.permissions.default", permission.getMask());
    }

    private NSButton preserveModificationDownloadCheckbox; //IBOutlet

    public void setPreserveModificationDownloadCheckbox(NSButton preserveModificationDownloadCheckbox) {
        this.preserveModificationDownloadCheckbox = preserveModificationDownloadCheckbox;
        this.preserveModificationDownloadCheckbox.setTarget(this);
        this.preserveModificationDownloadCheckbox.setAction(new NSSelector("preserveModificationDownloadCheckboxClicked", new Class[]{NSButton.class}));
        this.preserveModificationDownloadCheckbox.setState(Preferences.instance().getBoolean("queue.download.preserveDate") ? NSCell.OnState : NSCell.OffState);
    }

    public void preserveModificationDownloadCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.download.preserveDate", enabled);
    }

    private NSButton preserveModificationUploadCheckbox; //IBOutlet

    public void setPreserveModificationUploadCheckbox(NSButton preserveModificationUploadCheckbox) {
        this.preserveModificationUploadCheckbox = preserveModificationUploadCheckbox;
        this.preserveModificationUploadCheckbox.setTarget(this);
        this.preserveModificationUploadCheckbox.setAction(new NSSelector("preserveModificationUploadCheckboxClicked", new Class[]{NSButton.class}));
        this.preserveModificationUploadCheckbox.setState(Preferences.instance().getBoolean("queue.upload.preserveDate") ? NSCell.OnState : NSCell.OffState);
    }

    public void preserveModificationUploadCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.upload.preserveDate", enabled);
    }

    private NSButton horizontalLinesCheckbox; //IBOutlet

    public void setHorizontalLinesCheckbox(NSButton horizontalLinesCheckbox) {
        this.horizontalLinesCheckbox = horizontalLinesCheckbox;
        this.horizontalLinesCheckbox.setTarget(this);
        this.horizontalLinesCheckbox.setAction(new NSSelector("horizontalLinesCheckboxClicked", new Class[]{NSButton.class}));
        this.horizontalLinesCheckbox.setState(Preferences.instance().getBoolean("browser.horizontalLines") ? NSCell.OnState : NSCell.OffState);
    }

    public void horizontalLinesCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("browser.horizontalLines", enabled);
        CDBrowserController.updateBrowserTableAttributes();
    }

    private NSButton verticalLinesCheckbox; //IBOutlet

    public void setVerticalLinesCheckbox(NSButton verticalLinesCheckbox) {
        this.verticalLinesCheckbox = verticalLinesCheckbox;
        this.verticalLinesCheckbox.setTarget(this);
        this.verticalLinesCheckbox.setAction(new NSSelector("verticalLinesCheckboxClicked", new Class[]{NSButton.class}));
        this.verticalLinesCheckbox.setState(Preferences.instance().getBoolean("browser.verticalLines") ? NSCell.OnState : NSCell.OffState);
    }

    public void verticalLinesCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("browser.verticalLines", enabled);
        CDBrowserController.updateBrowserTableAttributes();
    }

    private NSButton alternatingRowBackgroundCheckbox; //IBOutlet

    public void setAlternatingRowBackgroundCheckbox(NSButton alternatingRowBackgroundCheckbox) {
        this.alternatingRowBackgroundCheckbox = alternatingRowBackgroundCheckbox;
        this.alternatingRowBackgroundCheckbox.setTarget(this);
        this.alternatingRowBackgroundCheckbox.setAction(new NSSelector("alternatingRowBackgroundCheckboxClicked", new Class[]{NSButton.class}));
        this.alternatingRowBackgroundCheckbox.setState(Preferences.instance().getBoolean("browser.alternatingRows") ? NSCell.OnState : NSCell.OffState);
    }

    public void alternatingRowBackgroundCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("browser.alternatingRows", enabled);
        CDBrowserController.updateBrowserTableAttributes();
    }

    private NSButton infoWindowAsInspectorCheckbox; //IBOutlet

    public void setInfoWindowAsInspectorCheckbox(NSButton infoWindowAsInspectorCheckbox) {
        this.infoWindowAsInspectorCheckbox = infoWindowAsInspectorCheckbox;
        this.infoWindowAsInspectorCheckbox.setTarget(this);
        this.infoWindowAsInspectorCheckbox.setAction(new NSSelector("infoWindowAsInspectorCheckboxClicked", new Class[]{NSButton.class}));
        this.infoWindowAsInspectorCheckbox.setState(Preferences.instance().getBoolean("browser.info.isInspector") ? NSCell.OnState : NSCell.OffState);
    }

    public void infoWindowAsInspectorCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("browser.info.isInspector", enabled);
    }

    // public-key algorithms
    private static final String SSH_DSS = "ssh-dss";
    private static final String SSH_RSA = "ssh-rsa";

    private NSPopUpButton publickeyCombobox; //IBOutlet

    public void setPublickeyCombobox(NSPopUpButton publickeyCombobox) {
        this.publickeyCombobox = publickeyCombobox;
        this.publickeyCombobox.setTarget(this);
        this.publickeyCombobox.setAction(new NSSelector("publickeyComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.publickeyCombobox.removeAllItems();
        this.publickeyCombobox.addItemsWithTitles(new NSArray(new String[]{
                //NSBundle.localizedString("Default", ""),
                SSH_DSS,
                SSH_RSA
        }));

        publickeyCombobox.selectItemWithTitle(Preferences.instance().getProperty("ssh.publickey"));
    }

    public void publickeyComboboxClicked(NSPopUpButton sender) {
        Preferences.instance().setProperty("ssh.publickey", sender.titleOfSelectedItem());
    }

    //encryption ciphers
    private static final String des_cbc = "3des-cbc";
    private static final String blowfish_cbc = "blowfish-cbc";
    private static final String twofish256_cbc = "twofish256-cbc";
    private static final String twofish192_cbc = "twofish192-cbc";
    private static final String twofish128_cbc = "twofish128-cbc";
    private static final String aes256_cbc = "aes256-cbc";
    private static final String aes192_cbc = "aes192-cbc";
    private static final String aes128_cbc = "aes128-cbc";
    private static final String cast128_cbc = "cast128-cbc";

    private NSPopUpButton csEncryptionCombobox; //IBOutlet

    public void setCsEncryptionCombobox(NSPopUpButton csEncryptionCombobox) {
        this.csEncryptionCombobox = csEncryptionCombobox;
        this.csEncryptionCombobox.setTarget(this);
        this.csEncryptionCombobox.setAction(new NSSelector("csEncryptionComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.csEncryptionCombobox.removeAllItems();
        this.csEncryptionCombobox.addItemsWithTitles(new NSArray(new String[]{
                des_cbc,
                blowfish_cbc,
                twofish256_cbc,
                twofish192_cbc,
                twofish128_cbc,
                aes256_cbc,
                aes192_cbc,
                aes128_cbc,
                cast128_cbc
        }));
        this.csEncryptionCombobox.selectItemWithTitle(Preferences.instance().getProperty("ssh.CSEncryption"));
    }

    public void csEncryptionComboboxClicked(NSPopUpButton sender) {
        Preferences.instance().setProperty("ssh.CSEncryption", sender.titleOfSelectedItem());
    }

    private NSPopUpButton scEncryptionCombobox; //IBOutlet

    public void setScEncryptionCombobox(NSPopUpButton scEncryptionCombobox) {
        this.scEncryptionCombobox = scEncryptionCombobox;
        this.scEncryptionCombobox.setTarget(this);
        this.scEncryptionCombobox.setAction(new NSSelector("scEncryptionComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.scEncryptionCombobox.removeAllItems();
        this.scEncryptionCombobox.addItemsWithTitles(new NSArray(new String[]{
                des_cbc,
                blowfish_cbc,
                twofish256_cbc,
                twofish192_cbc,
                twofish128_cbc,
                aes256_cbc,
                aes192_cbc,
                aes128_cbc,
                cast128_cbc
        }));

        this.scEncryptionCombobox.selectItemWithTitle(Preferences.instance().getProperty("ssh.SCEncryption"));
    }

    public void scEncryptionComboboxClicked(NSPopUpButton sender) {
        Preferences.instance().setProperty("ssh.SCEncryption", sender.titleOfSelectedItem());
    }


    //authentication algorithms
    private static final String hmac_sha1 = "hmac-sha1";
    private static final String hmac_sha1_96 = "hmac-sha1-96";
    private static final String hmac_md5 = "hmac-md5";
    private static final String hmac_md5_96 = "hmac-md5-96";

    private NSPopUpButton scAuthenticationCombobox; //IBOutlet

    public void setScAuthenticationCombobox(NSPopUpButton scAuthenticationCombobox) {
        this.scAuthenticationCombobox = scAuthenticationCombobox;
        this.scAuthenticationCombobox.setTarget(this);
        this.scAuthenticationCombobox.setAction(new NSSelector("scAuthenticationComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.scAuthenticationCombobox.removeAllItems();
        this.scAuthenticationCombobox.addItemsWithTitles(new NSArray(new String[]{
                hmac_sha1,
                hmac_sha1_96,
                hmac_md5,
                hmac_md5_96
        }));

        this.scAuthenticationCombobox.selectItemWithTitle(Preferences.instance().getProperty("ssh.SCAuthentication"));
    }

    public void scAuthenticationComboboxClicked(NSPopUpButton sender) {
        Preferences.instance().setProperty("ssh.SCAuthentication", sender.titleOfSelectedItem());
    }


    private NSPopUpButton csAuthenticationCombobox; //IBOutlet

    public void setCsAuthenticationCombobox(NSPopUpButton csAuthenticationCombobox) {
        this.csAuthenticationCombobox = csAuthenticationCombobox;
        this.csAuthenticationCombobox.setTarget(this);
        this.csAuthenticationCombobox.setAction(new NSSelector("csAuthenticationComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.csAuthenticationCombobox.removeAllItems();
        this.csAuthenticationCombobox.addItemsWithTitles(new NSArray(new String[]{
                hmac_sha1,
                hmac_sha1_96,
                hmac_md5,
                hmac_md5_96
        }));

        this.csAuthenticationCombobox.selectItemWithTitle(Preferences.instance().getProperty("ssh.CSAuthentication"));
    }

    public void csAuthenticationComboboxClicked(NSPopUpButton sender) {
        Preferences.instance().setProperty("ssh.CSAuthentication", sender.titleOfSelectedItem());
    }

    private static final String ZLIB = "zlib";

    private NSPopUpButton compressionCombobox; //IBOutlet

    public void setCompressionCombobox(NSPopUpButton compressionCombobox) {
        this.compressionCombobox = compressionCombobox;
        this.compressionCombobox.setTarget(this);
        this.compressionCombobox.setAction(new NSSelector("compressionComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.compressionCombobox.removeAllItems();
        this.compressionCombobox.addItemsWithTitles(new NSArray(new String[]{
                NSBundle.localizedString("None", ""),
                ZLIB
        }));

        this.compressionCombobox.selectItemWithTitle(Preferences.instance().getProperty("ssh.compression"));
    }

    public void compressionComboboxClicked(NSPopUpButton sender) {
        if (sender.titleOfSelectedItem().equals(ZLIB))
            Preferences.instance().setProperty("ssh.compression", ZLIB);
        else
            Preferences.instance().setProperty("ssh.compression", "none");
    }

    private NSPopUpButton downloadPathPopup; //IBOutlet

    private static final String CHOOSE = NSBundle.localizedString("Choose", "")+"...";

    public void setDownloadPathPopup(NSPopUpButton downloadPathPopup) {
        this.downloadPathPopup = downloadPathPopup;
        this.downloadPathPopup.setTarget(this);
        final NSSelector action = new NSSelector("downloadPathPopupClicked", new Class[]{NSPopUpButton.class});
        this.downloadPathPopup.setAction(action);
        this.downloadPathPopup.removeAllItems();
        // The currently set download folder
        final String CUSTOM = Preferences.instance().getProperty("queue.download.folder");
        this.downloadPathPopup.menu().addItem(NSPathUtilities.displayNameAtPath(
                NSPathUtilities.stringByExpandingTildeInPath(CUSTOM)
        ), action, "");
        this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems()-1).setTarget(this);
        this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems()-1).setImage(NSImage.imageNamed("folder16.tiff"));
        this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems()-1).setRepresentedObject(
                NSPathUtilities.stringByExpandingTildeInPath(CUSTOM)
        );
        this.downloadPathPopup.menu().addItem(new NSMenuItem().separatorItem());
        // Shortcut to the Desktop
        final String DESKTOP = "~/Desktop";
        this.downloadPathPopup.menu().addItem(NSPathUtilities.displayNameAtPath(
                NSPathUtilities.stringByExpandingTildeInPath(DESKTOP)
        ), action, "");
        this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems()-1).setTarget(this);
        this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems()-1).setImage(DESKTOP_ICON);
        this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems()-1).setRepresentedObject(
                NSPathUtilities.stringByExpandingTildeInPath(DESKTOP));
        if(CUSTOM.equals(DESKTOP)) {
            this.downloadPathPopup.selectItemAtIndex(this.downloadPathPopup.numberOfItems()-1);
        }
        // Shortcut to user home
        final String HOME = "~";
        this.downloadPathPopup.menu().addItem(NSPathUtilities.displayNameAtPath(
                NSPathUtilities.stringByExpandingTildeInPath(HOME)
        ), action, "");
        this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems()-1).setTarget(this);
        this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems()-1).setImage(HOME_ICON);
        this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems()-1).setRepresentedObject(
                NSPathUtilities.stringByExpandingTildeInPath(HOME));
        if(CUSTOM.equals(HOME)) {
            this.downloadPathPopup.selectItemAtIndex(this.downloadPathPopup.numberOfItems()-1);
        }
        // Choose another folder
        this.downloadPathPopup.menu().addItem(new NSMenuItem().separatorItem());
        this.downloadPathPopup.menu().addItem(CHOOSE, action, "");
        this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems()-1).setTarget(this);
    }

    private NSOpenPanel downloadPathPanel;

    public void downloadPathPopupClicked(final NSMenuItem sender) {
        if(sender.title().equals(CHOOSE)) {
            downloadPathPanel = NSOpenPanel.openPanel();
            downloadPathPanel.setCanChooseFiles(false);
            downloadPathPanel.setCanChooseDirectories(true);
            downloadPathPanel.setAllowsMultipleSelection(false);
            downloadPathPanel.setCanCreateDirectories(true);
            downloadPathPanel.beginSheetForDirectory(null, null, null, this.window, this, new NSSelector("downloadPathPanelDidEnd", new Class[]{NSOpenPanel.class, int.class, Object.class}), null);
        }
        else {
            Preferences.instance().setProperty("queue.download.folder", NSPathUtilities.stringByAbbreviatingWithTildeInPath(
                    sender.representedObject().toString()));
        }
    }

    public void downloadPathPanelDidEnd(NSOpenPanel sheet, int returncode, Object contextInfo) {
        if(returncode == CDSheetCallback.DEFAULT_OPTION) {
            NSArray selected = sheet.filenames();
            String filename;
            if ((filename = (String) selected.lastObject()) != null) {
                Preferences.instance().setProperty("queue.download.folder",
                        NSPathUtilities.stringByAbbreviatingWithTildeInPath(filename));
            }
        }
        String custom = NSPathUtilities.stringByExpandingTildeInPath(
                Preferences.instance().getProperty("queue.download.folder"));
        this.downloadPathPopup.itemAtIndex(0).setTitle(NSPathUtilities.displayNameAtPath(custom));
        this.downloadPathPopup.itemAtIndex(0).setRepresentedObject(custom);
        this.downloadPathPopup.selectItemAtIndex(0);
        this.downloadPathPanel = null;
    }

    private NSButton defaultBufferButton; //IBOutlet

    public void setDefaultBufferButton(NSButton defaultBufferButton) {
        this.defaultBufferButton = defaultBufferButton;
        this.defaultBufferButton.setTarget(this);
        this.defaultBufferButton.setAction(new NSSelector("defaultBufferButtonClicked", new Class[]{NSButton.class}));
    }

    public void defaultBufferButtonClicked(final NSButton sender) {
        Preferences.instance().setProperty("connection.buffer", Preferences.instance().getProperty("connection.buffer.default"));
        try {
            int bytes = Preferences.instance().getInteger("connection.buffer");
            int kbit = bytes / 1024 * 8;
            this.bufferField.setStringValue("" + kbit);
        }
        catch (NumberFormatException e) {
            log.error(e.getMessage());
        }
    }

    private NSPopUpButton transferPopup; //IBOutlet

    public void setTransferPopup(NSPopUpButton transferPopup) {
        this.transferPopup = transferPopup;
        this.transferPopup.setTarget(this);
        this.transferPopup.setAction(new NSSelector("transferPopupClicked", new Class[]{NSPopUpButton.class}));
        this.transferPopup.selectItemAtIndex(
                Preferences.instance().getInteger("connection.host.max") == 1 ? USE_BROWSER_SESSION_INDEX : USE_QUEUE_SESSION_INDEX);
    }

    private final int USE_QUEUE_SESSION_INDEX = 0;
    private final int USE_BROWSER_SESSION_INDEX = 1;

    public void transferPopupClicked(final NSPopUpButton sender) {
        if(sender.indexOfSelectedItem() == USE_BROWSER_SESSION_INDEX) {
            Preferences.instance().setProperty("connection.host.max", 1);
        }
        else if(sender.indexOfSelectedItem() == USE_QUEUE_SESSION_INDEX) {
            Preferences.instance().setProperty("connection.host.max", -1);
        }
    }

    private NSTextField bufferField; //IBOutlet

    public void setBufferField(NSTextField bufferField) {
        this.bufferField = bufferField;
        try {
            int bytes = Preferences.instance().getInteger("connection.buffer");
            int kbit = bytes / 1024 * 8;
            this.bufferField.setStringValue("" + kbit);
        }
        catch (NumberFormatException e) {
            log.error(e.getMessage());
        }
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("bufferFieldDidChange", new Class[]{NSNotification.class}),
                NSControl.ControlTextDidChangeNotification,
                this.bufferField);
    }

    public void bufferFieldDidChange(NSNotification sender) {
        try {
            int kbit = Integer.parseInt(this.bufferField.stringValue());
            int MIN_VALUE = 32;
            if(kbit < MIN_VALUE) {
                this.bufferField.setStringValue(String.valueOf(MIN_VALUE));
            }
            Preferences.instance().setProperty("connection.buffer", kbit / 8 * 1024); //Bytes
        }
        catch (NumberFormatException e) {
            log.error(e.getMessage());
        }
    }

    private NSButton keepAliveCheckbox; //IBOutlet

    public void setKeepAliveCheckbox(NSButton keepAliveCheckbox) {
        this.keepAliveCheckbox = keepAliveCheckbox;
        this.keepAliveCheckbox.setTarget(this);
        this.keepAliveCheckbox.setAction(new NSSelector("keepAliveCheckboxClicked", new Class[]{NSButton.class}));
        this.keepAliveCheckbox.setState(Preferences.instance().getBoolean("connection.keepalive") ? NSCell.OnState : NSCell.OffState);
    }

    public void keepAliveCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("connection.keepalive", enabled);
    }

    private NSTextField keepAliveIntervalField; //IBOutlet

    public void setKeepAliveIntervalField(NSTextField keepAliveIntervalField) {
        this.keepAliveIntervalField = keepAliveIntervalField;
        try {
            int i = Preferences.instance().getInteger("connection.keepalive.interval");
            this.keepAliveIntervalField.setStringValue("" + i);
        }
        catch (NumberFormatException e) {
            log.error(e.getMessage());
        }
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("keepAliveIntervalFieldDidChange", new Class[]{NSNotification.class}),
                NSControl.ControlTextDidChangeNotification,
                this.keepAliveIntervalField);
    }

    public void keepAliveIntervalFieldDidChange(NSNotification sender) {
        try {
            int i = Integer.parseInt(this.keepAliveIntervalField.stringValue());
            Preferences.instance().setProperty("connection.keepalive.interval", i);
        }
        catch (NumberFormatException e) {
            log.error(e.getMessage());
        }
    }

    private NSTextField timeoutField; //IBOutlet

    public void setTimeoutField(NSTextField timeoutField) {
        this.timeoutField = timeoutField;
        this.timeoutField.setStringValue(String.valueOf(Preferences.instance().getInteger("connection.timeout")/1000));
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("timeoutFieldDidChange", new Class[]{NSNotification.class}),
                NSControl.ControlTextDidChangeNotification,
                this.timeoutField);
    }

    public void timeoutFieldDidChange(NSNotification sender) {
        try {
            int timeout = Integer.parseInt(this.timeoutField.stringValue())*1000;
            int MIN_VALUE = 10000;
            if(timeout < MIN_VALUE) {
                this.timeoutField.setStringValue(String.valueOf(MIN_VALUE/1000));
            }
            Preferences.instance().setProperty("connection.timeout", timeout);
        }
        catch (NumberFormatException e) {
            log.error(e.getMessage());
        }
    }

    private NSTextField anonymousField; //IBOutlet

    public void setAnonymousField(NSTextField anonymousField) {
        this.anonymousField = anonymousField;
        this.anonymousField.setStringValue(Preferences.instance().getProperty("ftp.anonymous.pass"));
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("anonymousFieldDidChange", new Class[]{NSNotification.class}),
                NSControl.ControlTextDidChangeNotification,
                this.anonymousField);
    }

    public void anonymousFieldDidChange(NSNotification sender) {
        Preferences.instance().setProperty("ftp.anonymous.pass", this.anonymousField.stringValue());
    }

    private NSTextField textFileTypeRegexField; //IBOutlet

    public void setTextFileTypeRegexField(NSTextField textFileTypeRegexField) {
        this.textFileTypeRegexField = textFileTypeRegexField;
        this.textFileTypeRegexField.setStringValue(Preferences.instance().getProperty("filetype.text.regex"));
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("textFileTypeRegexFieldDidChange", new Class[]{NSNotification.class}),
                NSControl.ControlTextDidChangeNotification,
                this.textFileTypeRegexField);
    }

    public void textFileTypeRegexFieldDidChange(NSNotification sender) {
        String value = this.textFileTypeRegexField.stringValue().trim();
        try {
            Pattern compiled = Pattern.compile(value);
            Preferences.instance().setProperty("filetype.text.regex",
                    compiled.pattern());
        }
        catch(PatternSyntaxException e) {
            log.warn("Invalid regex:"+e.getMessage());
        }
    }

    private NSTextField binaryFileTypeRegexField; //IBOutlet

    public void setBinaryFileTypeRegexField(NSTextField binaryFileTypeRegexField) {
        this.binaryFileTypeRegexField = binaryFileTypeRegexField;
        this.binaryFileTypeRegexField.setStringValue(Preferences.instance().getProperty("filetype.binary.regex"));
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("binaryFileTypeRegexFieldDidChange", new Class[]{NSNotification.class}),
                NSControl.ControlTextDidChangeNotification,
                this.binaryFileTypeRegexField);
    }

    public void binaryFileTypeRegexFieldDidChange(NSNotification sender) {
        String value = this.binaryFileTypeRegexField.stringValue().trim();
        try {
            Pattern compiled = Pattern.compile(value);
            Preferences.instance().setProperty("filetype.binary.regex",
                    compiled.pattern());
        }
        catch(PatternSyntaxException e) {
            log.warn("Invalid regex:"+e.getMessage());
        }
    }

    private NSButton downloadSkipButton; //IBOutlet

    public void setDownloadSkipButton(NSButton downloadSkipButton) {
        this.downloadSkipButton = downloadSkipButton;
        this.downloadSkipButton.setTarget(this);
        this.downloadSkipButton.setAction(new NSSelector("downloadSkipButtonClicked", new Class[]{NSButton.class}));
        this.downloadSkipButton.setState(Preferences.instance().getBoolean("queue.download.skip.enable") ? NSCell.OnState : NSCell.OffState);
    }

    public void downloadSkipButtonClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.download.skip.enable", enabled);
    }

    private NSTextView downloadSkipRegexField; //IBOutlet

    public void setDownloadSkipRegexField(NSTextView downloadSkipRegexField) {
        this.downloadSkipRegexField = downloadSkipRegexField;
        this.downloadSkipRegexField.textStorage().appendAttributedString(
                new NSAttributedString(Preferences.instance().getProperty("queue.download.skip.regex"), FIXED_WITH_FONT_ATTRIBUTES));
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("downloadSkipRegexFieldDidChange", new Class[]{NSNotification.class}),
                NSText.TextDidChangeNotification,
                this.downloadSkipRegexField);
    }

    public void downloadSkipRegexFieldDidChange(NSNotification sender) {
        String value = this.downloadSkipRegexField.textStorage().stringReference().string().trim();
        try {
            Pattern compiled = Pattern.compile(value);
            Preferences.instance().setProperty("queue.download.skip.regex",
                    compiled.pattern());
            this.mark(this.downloadSkipRegexField, null);
        }
        catch(PatternSyntaxException e) {
            this.mark(this.downloadSkipRegexField, e);
        }
    }

    private NSButton uploadSkipButton; //IBOutlet

    public void setUploadSkipButton(NSButton uploadSkipButton) {
        this.uploadSkipButton = uploadSkipButton;
        this.uploadSkipButton.setTarget(this);
        this.uploadSkipButton.setAction(new NSSelector("uploadSkipButtonClicked", new Class[]{NSButton.class}));
        this.uploadSkipButton.setState(Preferences.instance().getBoolean("queue.upload.skip.enable") ? NSCell.OnState : NSCell.OffState);
    }

    public void uploadSkipButtonClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.upload.skip.enable", enabled);
    }

    private NSTextView uploadSkipRegexField; //IBOutlet

    public void setUploadSkipRegexField(NSTextView uploadSkipRegexField) {
        this.uploadSkipRegexField = uploadSkipRegexField;
        this.uploadSkipRegexField.textStorage().appendAttributedString(
                new NSAttributedString(Preferences.instance().getProperty("queue.upload.skip.regex"), FIXED_WITH_FONT_ATTRIBUTES));
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("uploadSkipRegexFieldDidChange", new Class[]{NSNotification.class}),
                NSText.TextDidChangeNotification,
                this.uploadSkipRegexField);
    }

    public void uploadSkipRegexFieldDidChange(NSNotification sender) {
        String value = this.uploadSkipRegexField.textStorage().stringReference().string().trim();
        try {
            Pattern compiled = Pattern.compile(value);
            Preferences.instance().setProperty("queue.upload.skip.regex",
                    compiled.pattern());
            this.mark(this.uploadSkipRegexField, null);
        }
        catch(PatternSyntaxException e) {
            this.mark(this.uploadSkipRegexField, e);
        }
    }

    protected static NSDictionary RED_FONT = new NSDictionary(
            new Object[]{NSColor.redColor()},
            new Object[]{NSAttributedString.ForegroundColorAttributeName}
    );

    private void mark(NSTextView view, PatternSyntaxException e) {
        if(null == e) {
            view.textStorage().removeAttributeInRange(
                    NSAttributedString.ForegroundColorAttributeName,
                    new NSRange(0, view.textStorage().length()));
            return;
        }
        int index = e.getIndex(); //The approximate index in the pattern of the error
        NSRange range = null;
        if(-1 == index) {
            range = new NSRange(0, view.textStorage().length());
        }
        if(index < view.textStorage().length()) {
            //Initializes the NSRange with the range elements of location and length;
            range = new NSRange(index, 1);
        }
        view.textStorage().addAttributesInRange(RED_FONT, range);
    }

    private NSTextField loginField; //IBOutlet

    public void setLoginField(NSTextField loginField) {
        this.loginField = loginField;
        this.loginField.setStringValue(Preferences.instance().getProperty("connection.login.name"));
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("loginFieldDidChange", new Class[]{NSNotification.class}),
                NSControl.ControlTextDidChangeNotification,
                this.loginField);
    }

    public void loginFieldDidChange(NSNotification sender) {
        Preferences.instance().setProperty("connection.login.name", this.loginField.stringValue());
    }

    private NSButton keychainCheckbox; //IBOutlet

    public void setKeychainCheckbox(NSButton keychainCheckbox) {
        this.keychainCheckbox = keychainCheckbox;
        this.keychainCheckbox.setTarget(this);
        this.keychainCheckbox.setAction(new NSSelector("keychainCheckboxClicked", new Class[]{NSButton.class}));
        this.keychainCheckbox.setState(Preferences.instance().getBoolean("connection.login.useKeychain") ? NSCell.OnState : NSCell.OffState);
    }

    public void keychainCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("connection.login.useKeychain", enabled);
    }

    private NSButton doubleClickCheckbox; //IBOutlet

    public void setDoubleClickCheckbox(NSButton doubleClickCheckbox) {
        this.doubleClickCheckbox = doubleClickCheckbox;
        this.doubleClickCheckbox.setTarget(this);
        this.doubleClickCheckbox.setAction(new NSSelector("doubleClickCheckboxClicked", new Class[]{NSButton.class}));
        this.doubleClickCheckbox.setState(Preferences.instance().getBoolean("browser.doubleclick.edit") ? NSCell.OnState : NSCell.OffState);
    }

    public void doubleClickCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("browser.doubleclick.edit", enabled);
    }

    private NSButton showHiddenCheckbox; //IBOutlet

    public void setShowHiddenCheckbox(NSButton showHiddenCheckbox) {
        this.showHiddenCheckbox = showHiddenCheckbox;
        this.showHiddenCheckbox.setTarget(this);
        this.showHiddenCheckbox.setAction(new NSSelector("showHiddenCheckboxClicked", new Class[]{NSButton.class}));
        this.showHiddenCheckbox.setState(Preferences.instance().getBoolean("browser.showHidden") ? NSCell.OnState : NSCell.OffState);
    }

    public void showHiddenCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("browser.showHidden", enabled);
    }

    private NSButton bringQueueToFrontCheckbox; //IBOutlet

    public void setBringQueueToFrontCheckbox(NSButton bringQueueToFrontCheckbox) {
        this.bringQueueToFrontCheckbox = bringQueueToFrontCheckbox;
        this.bringQueueToFrontCheckbox.setTarget(this);
        this.bringQueueToFrontCheckbox.setAction(new NSSelector("bringQueueToFrontCheckboxClicked", new Class[]{NSButton.class}));
        this.bringQueueToFrontCheckbox.setState(Preferences.instance().getBoolean("queue.orderFrontOnStart") ? NSCell.OnState : NSCell.OffState);
    }

    public void bringQueueToFrontCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.orderFrontOnStart", enabled);
    }

    private NSButton bringQueueToBackCheckbox; //IBOutlet

    public void setBringQueueToBackCheckbox(NSButton bringQueueToBackCheckbox) {
        this.bringQueueToBackCheckbox = bringQueueToBackCheckbox;
        this.bringQueueToBackCheckbox.setTarget(this);
        this.bringQueueToBackCheckbox.setAction(new NSSelector("bringQueueToBackCheckboxClicked", new Class[]{NSButton.class}));
        this.bringQueueToBackCheckbox.setState(Preferences.instance().getBoolean("queue.orderBackOnStop") ? NSCell.OnState : NSCell.OffState);
    }

    public void bringQueueToBackCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.orderBackOnStop", enabled);
    }

    private NSButton removeFromQueueCheckbox; //IBOutlet

    public void setRemoveFromQueueCheckbox(NSButton removeFromQueueCheckbox) {
        this.removeFromQueueCheckbox = removeFromQueueCheckbox;
        this.removeFromQueueCheckbox.setTarget(this);
        this.removeFromQueueCheckbox.setAction(new NSSelector("removeFromQueueCheckboxClicked", new Class[]{NSButton.class}));
        this.removeFromQueueCheckbox.setState(Preferences.instance().getBoolean("queue.removeItemWhenComplete") ? NSCell.OnState : NSCell.OffState);
    }

    public void removeFromQueueCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.removeItemWhenComplete", enabled);
    }

    private NSButton openAfterDownloadCheckbox; //IBOutlet

    public void setOpenAfterDownloadCheckbox(NSButton openAfterDownloadCheckbox) {
        this.openAfterDownloadCheckbox = openAfterDownloadCheckbox;
        this.openAfterDownloadCheckbox.setTarget(this);
        this.openAfterDownloadCheckbox.setAction(new NSSelector("openAfterDownloadCheckboxClicked", new Class[]{NSButton.class}));
        this.openAfterDownloadCheckbox.setState(Preferences.instance().getBoolean("queue.postProcessItemWhenComplete") ? NSCell.OnState : NSCell.OffState);
    }

    public void openAfterDownloadCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("queue.postProcessItemWhenComplete", enabled);
    }

    private NSPopUpButton duplicateDownloadCombobox; //IBOutlet

    public void setDuplicateDownloadCombobox(NSPopUpButton duplicateDownloadCombobox) {
        this.duplicateDownloadCombobox = duplicateDownloadCombobox;
        this.duplicateDownloadCombobox.setTarget(this);
        this.duplicateDownloadCombobox.setAction(new NSSelector("duplicateDownloadComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.duplicateDownloadCombobox.removeAllItems();
        this.duplicateDownloadCombobox.addItemsWithTitles(new NSArray(new String[]{ASK_ME_WHAT_TO_DO, OVERWRITE_EXISTING_FILE, TRY_TO_RESUME_TRANSFER, USE_A_SIMILAR_NAME}));
        if (Preferences.instance().getProperty("queue.download.fileExists").equals(Validator.ASK)) {
            this.duplicateDownloadCombobox.selectItemWithTitle(ASK_ME_WHAT_TO_DO);
        }
        if (Preferences.instance().getProperty("queue.download.fileExists").equals(Validator.OVERWRITE)) {
            this.duplicateDownloadCombobox.selectItemWithTitle(OVERWRITE_EXISTING_FILE);
        }
        else if (Preferences.instance().getProperty("queue.download.fileExists").equals(Validator.RESUME)) {
            this.duplicateDownloadCombobox.selectItemWithTitle(TRY_TO_RESUME_TRANSFER);
        }
        else if (Preferences.instance().getProperty("queue.download.fileExists").equals(Validator.SIMILAR)) {
            this.duplicateDownloadCombobox.selectItemWithTitle(USE_A_SIMILAR_NAME);
        }
    }

    public void duplicateDownloadComboboxClicked(NSPopUpButton sender) {
        if (sender.selectedItem().title().equals(ASK_ME_WHAT_TO_DO)) {
            Preferences.instance().setProperty("queue.download.fileExists", Validator.ASK);
        }
        else if (sender.selectedItem().title().equals(OVERWRITE_EXISTING_FILE)) {
            Preferences.instance().setProperty("queue.download.fileExists", Validator.OVERWRITE);
        }
        else if (sender.selectedItem().title().equals(TRY_TO_RESUME_TRANSFER)) {
            Preferences.instance().setProperty("queue.download.fileExists", Validator.RESUME);
        }
        else if (sender.selectedItem().title().equals(USE_A_SIMILAR_NAME)) {
            Preferences.instance().setProperty("queue.download.fileExists", Validator.SIMILAR);
        }
        if(!Preferences.instance().getProperty("queue.download.reload.fileExists").equals(Validator.OVERWRITE)) {
            if (sender.selectedItem().title().equals(ASK_ME_WHAT_TO_DO)) {
                Preferences.instance().setProperty("queue.download.reload.fileExists", Validator.ASK);
            }
            else if (sender.selectedItem().title().equals(OVERWRITE_EXISTING_FILE)) {
                Preferences.instance().setProperty("queue.download.reload.fileExists", Validator.OVERWRITE);
            }
            else if (sender.selectedItem().title().equals(TRY_TO_RESUME_TRANSFER)) {
                Preferences.instance().setProperty("queue.download.reload.fileExists", Validator.RESUME);
            }
            else if (sender.selectedItem().title().equals(USE_A_SIMILAR_NAME)) {
                Preferences.instance().setProperty("queue.download.reload.fileExists", Validator.SIMILAR);
            }
        }
    }

    private NSButton duplicateDownloadOverwriteButton;

    public void setDuplicateDownloadOverwriteButton(NSButton duplicateDownloadOverwriteButton) {
        this.duplicateDownloadOverwriteButton = duplicateDownloadOverwriteButton;
        this.duplicateDownloadOverwriteButton.setTarget(this);
        this.duplicateDownloadOverwriteButton.setAction(new NSSelector("duplicateDownloadOverwriteButtonClicked", new Class[]{NSButton.class}));
        this.duplicateDownloadOverwriteButton.setState(
                Preferences.instance().getProperty("queue.download.reload.fileExists").equals(Validator.OVERWRITE) ? NSCell.OnState : NSCell.OffState);
    }

    public void duplicateDownloadOverwriteButtonClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        if(enabled) {
            Preferences.instance().setProperty("queue.download.reload.fileExists", Validator.OVERWRITE);
        }
        else {
            Preferences.instance().setProperty("queue.download.reload.fileExists",
                    Preferences.instance().getProperty("queue.download.fileExists"));
        }
    }

    private NSPopUpButton duplicateUploadCombobox; //IBOutlet

    public void setDuplicateUploadCombobox(NSPopUpButton duplicateUploadCombobox) {
        this.duplicateUploadCombobox = duplicateUploadCombobox;
        this.duplicateUploadCombobox.setTarget(this);
        this.duplicateUploadCombobox.setAction(new NSSelector("duplicateUploadComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.duplicateUploadCombobox.removeAllItems();
        this.duplicateUploadCombobox.addItemsWithTitles(new NSArray(new String[]{ASK_ME_WHAT_TO_DO, OVERWRITE_EXISTING_FILE, TRY_TO_RESUME_TRANSFER, USE_A_SIMILAR_NAME}));
        if (Preferences.instance().getProperty("queue.upload.fileExists").equals(Validator.ASK)) {
            this.duplicateUploadCombobox.selectItemWithTitle(ASK_ME_WHAT_TO_DO);
        }
        if (Preferences.instance().getProperty("queue.upload.fileExists").equals(Validator.OVERWRITE)) {
            this.duplicateUploadCombobox.selectItemWithTitle(OVERWRITE_EXISTING_FILE);
        }
        else if (Preferences.instance().getProperty("queue.upload.fileExists").equals(Validator.RESUME)) {
            this.duplicateUploadCombobox.selectItemWithTitle(TRY_TO_RESUME_TRANSFER);
        }
        else if (Preferences.instance().getProperty("queue.upload.fileExists").equals(Validator.SIMILAR)) {
            this.duplicateUploadCombobox.selectItemWithTitle(USE_A_SIMILAR_NAME);
        }
    }

    public void duplicateUploadComboboxClicked(NSPopUpButton sender) {
        if (sender.selectedItem().title().equals(ASK_ME_WHAT_TO_DO)) {
            Preferences.instance().setProperty("queue.upload.fileExists", Validator.ASK);
        }
        else if (sender.selectedItem().title().equals(OVERWRITE_EXISTING_FILE)) {
            Preferences.instance().setProperty("queue.upload.fileExists", Validator.OVERWRITE);
        }
        else if (sender.selectedItem().title().equals(TRY_TO_RESUME_TRANSFER)) {
            Preferences.instance().setProperty("queue.upload.fileExists", Validator.RESUME);
        }
        else if (sender.selectedItem().title().equals(USE_A_SIMILAR_NAME)) {
            Preferences.instance().setProperty("queue.upload.fileExists", Validator.SIMILAR);
        }
        if(!Preferences.instance().getProperty("queue.upload.reload.fileExists").equals(Validator.OVERWRITE)) {
            if (sender.selectedItem().title().equals(ASK_ME_WHAT_TO_DO)) {
                Preferences.instance().setProperty("queue.upload.reload.fileExists", Validator.ASK);
            }
            else if (sender.selectedItem().title().equals(OVERWRITE_EXISTING_FILE)) {
                Preferences.instance().setProperty("queue.upload.reload.fileExists", Validator.OVERWRITE);
            }
            else if (sender.selectedItem().title().equals(TRY_TO_RESUME_TRANSFER)) {
                Preferences.instance().setProperty("queue.upload.reload.fileExists", Validator.RESUME);
            }
            else if (sender.selectedItem().title().equals(USE_A_SIMILAR_NAME)) {
                Preferences.instance().setProperty("queue.upload.reload.fileExists", Validator.SIMILAR);
            }
        }
    }

    private NSButton duplicateUploadOverwriteButton;

    public void setDuplicateUploadOverwriteButton(NSButton duplicateUploadOverwriteButton) {
        this.duplicateUploadOverwriteButton = duplicateUploadOverwriteButton;
        this.duplicateUploadOverwriteButton.setTarget(this);
        this.duplicateUploadOverwriteButton.setAction(new NSSelector("duplicateUploadOverwriteButtonClicked", new Class[]{NSButton.class}));
        this.duplicateUploadOverwriteButton.setState(
                Preferences.instance().getProperty("queue.upload.reload.fileExists").equals(Validator.OVERWRITE) ? NSCell.OnState : NSCell.OffState);
    }

    public void duplicateUploadOverwriteButtonClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        if(enabled) {
            Preferences.instance().setProperty("queue.upload.reload.fileExists", Validator.OVERWRITE);
        }
        else {
            Preferences.instance().setProperty("queue.upload.reload.fileExists",
                    Preferences.instance().getProperty("queue.upload.fileExists"));
        }
    }

    private NSPopUpButton lineEndingCombobox; //IBOutlet

    public void setLineEndingCombobox(NSPopUpButton lineEndingCombobox) {
        this.lineEndingCombobox = lineEndingCombobox;
        this.lineEndingCombobox.setTarget(this);
        this.lineEndingCombobox.setAction(new NSSelector("lineEndingComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.lineEndingCombobox.removeAllItems();
        this.lineEndingCombobox.addItemsWithTitles(new NSArray(new String[]{UNIX_LINE_ENDINGS, MAC_LINE_ENDINGS, WINDOWS_LINE_ENDINGS}));
        if (Preferences.instance().getProperty("ftp.line.separator").equals("unix")) {
            this.lineEndingCombobox.selectItemWithTitle(UNIX_LINE_ENDINGS);
        }
        else if (Preferences.instance().getProperty("ftp.line.separator").equals("mac")) {
            this.lineEndingCombobox.selectItemWithTitle(MAC_LINE_ENDINGS);
        }
        else if (Preferences.instance().getProperty("ftp.line.separator").equals("win")) {
            this.lineEndingCombobox.selectItemWithTitle(WINDOWS_LINE_ENDINGS);
        }
    }

    public void lineEndingComboboxClicked(NSPopUpButton sender) {
        if (sender.selectedItem().title().equals(UNIX_LINE_ENDINGS)) {
            Preferences.instance().setProperty("ftp.line.separator", "unix");
        }
        else if (sender.selectedItem().title().equals(MAC_LINE_ENDINGS)) {
            Preferences.instance().setProperty("ftp.line.separator", "mac");
        }
        else if (sender.selectedItem().title().equals(WINDOWS_LINE_ENDINGS)) {
            Preferences.instance().setProperty("ftp.line.separator", "win");
        }
    }


    private NSPopUpButton transfermodeCombobox; //IBOutlet

    public void setTransfermodeCombobox(NSPopUpButton transfermodeCombobox) {
        this.transfermodeCombobox = transfermodeCombobox;
        this.transfermodeCombobox.setTarget(this);
        this.transfermodeCombobox.setAction(new NSSelector("transfermodeComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.transfermodeCombobox.removeAllItems();
        this.transfermodeCombobox.addItemsWithTitles(new NSArray(new String[]{TRANSFERMODE_AUTO, TRANSFERMODE_BINARY, TRANSFERMODE_ASCII}));
        if(Preferences.instance().getProperty("ftp.transfermode").equals(FTPTransferType.BINARY.toString())) {
            this.transfermodeCombobox.selectItemWithTitle(TRANSFERMODE_BINARY);
        }
        else if(Preferences.instance().getProperty("ftp.transfermode").equals(FTPTransferType.ASCII.toString())) {
            this.transfermodeCombobox.selectItemWithTitle(TRANSFERMODE_ASCII);
        }
        else if(Preferences.instance().getProperty("ftp.transfermode").equals(FTPTransferType.AUTO.toString())) {
            this.transfermodeCombobox.selectItemWithTitle(TRANSFERMODE_AUTO);
        }
    }

    public void transfermodeComboboxClicked(NSPopUpButton sender) {
        if (sender.selectedItem().title().equals(TRANSFERMODE_BINARY)) {
            Preferences.instance().setProperty("ftp.transfermode", FTPTransferType.BINARY.toString());
            this.lineEndingCombobox.setEnabled(false);
            this.textFileTypeRegexField.setEnabled(false);
        }
        else if (sender.selectedItem().title().equals(TRANSFERMODE_ASCII)) {
            Preferences.instance().setProperty("ftp.transfermode", FTPTransferType.ASCII.toString());
            this.lineEndingCombobox.setEnabled(true);
            this.textFileTypeRegexField.setEnabled(false);
        }
        else if (sender.selectedItem().title().equals(TRANSFERMODE_AUTO)) {
            Preferences.instance().setProperty("ftp.transfermode", FTPTransferType.AUTO.toString());
            this.lineEndingCombobox.setEnabled(true);
            this.textFileTypeRegexField.setEnabled(true);
        }
    }

    private NSPopUpButton connectmodeCombobox; //IBOutlet

    public void setConnectmodeCombobox(NSPopUpButton connectmodeCombobox) {
        this.connectmodeCombobox = connectmodeCombobox;
        this.connectmodeCombobox.setTarget(this);
        this.connectmodeCombobox.setAction(new NSSelector("connectmodeComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.connectmodeCombobox.removeAllItems();
        this.connectmodeCombobox.addItemsWithTitles(new NSArray(new String[]{CONNECTMODE_ACTIVE, CONNECTMODE_PASSIVE}));
        if (Preferences.instance().getProperty("ftp.connectmode").equals("passive")) {
            this.connectmodeCombobox.selectItemWithTitle(CONNECTMODE_PASSIVE);
        }
        else {
            this.connectmodeCombobox.selectItemWithTitle(CONNECTMODE_ACTIVE);
        }
    }

    public void connectmodeComboboxClicked(NSPopUpButton sender) {
        if (sender.selectedItem().title().equals(CONNECTMODE_ACTIVE)) {
            Preferences.instance().setProperty("ftp.connectmode", FTPConnectMode.ACTIVE.toString());
        }
        else if (sender.selectedItem().title().equals(CONNECTMODE_PASSIVE)) {
            Preferences.instance().setProperty("ftp.connectmode", FTPConnectMode.PASV.toString());
        }
    }

    private NSPopUpButton protocolCombobox; //IBOutlet

    public void setProtocolCombobox(NSPopUpButton protocolCombobox) {
        this.protocolCombobox = protocolCombobox;
        this.protocolCombobox.setTarget(this);
        this.protocolCombobox.setAction(new NSSelector("protocolComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.protocolCombobox.removeAllItems();
        this.protocolCombobox.addItemsWithTitles(new NSArray(new String[]{PROTOCOL_FTP,
                PROTOCOL_FTP_TLS,
                PROTOCOL_SFTP}));
        if (Preferences.instance().getProperty("connection.protocol.default").equals(Session.FTP)) {
            this.protocolCombobox.selectItemWithTitle(PROTOCOL_FTP);
        }
        if (Preferences.instance().getProperty("connection.protocol.default").equals(Session.FTP_TLS)) {
            this.protocolCombobox.selectItemWithTitle(PROTOCOL_FTP_TLS);
        }
        if (Preferences.instance().getProperty("connection.protocol.default").equals(Session.SFTP)) {
            this.protocolCombobox.selectItemWithTitle(PROTOCOL_SFTP);
        }
    }

    public void protocolComboboxClicked(NSPopUpButton sender) {
        if (sender.selectedItem().title().equals(PROTOCOL_FTP)) {
            Preferences.instance().setProperty("connection.protocol.default", Session.FTP);
            Preferences.instance().setProperty("connection.port.default", Session.FTP_PORT);
        }
        if (sender.selectedItem().title().equals(PROTOCOL_FTP_TLS)) {
            Preferences.instance().setProperty("connection.protocol.default", Session.FTP_TLS);
            Preferences.instance().setProperty("connection.port.default", Session.FTP_PORT);
        }
        if (sender.selectedItem().title().equals(PROTOCOL_SFTP)) {
            Preferences.instance().setProperty("connection.protocol.default", Session.SFTP);
            Preferences.instance().setProperty("connection.port.default", Session.SSH_PORT);
        }
    }

    private NSButton confirmDisconnectCheckbox; //IBOutlet

    public void setConfirmDisconnectCheckbox(NSButton confirmDisconnectCheckbox) {
        this.confirmDisconnectCheckbox = confirmDisconnectCheckbox;
        this.confirmDisconnectCheckbox.setTarget(this);
        this.confirmDisconnectCheckbox.setAction(new NSSelector("confirmDisconnectCheckboxClicked", new Class[]{NSButton.class}));
        this.confirmDisconnectCheckbox.setState(Preferences.instance().getBoolean("browser.confirmDisconnect") ? NSCell.OnState : NSCell.OffState);
    }

    public void confirmDisconnectCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("browser.confirmDisconnect", enabled);
    }

    private NSButton acceptAnyCertificateCheckbox; //IBOutlet

    public void setAcceptAnyCertificateCheckbox(NSButton acceptAnyCertificateCheckbox) {
        this.acceptAnyCertificateCheckbox = acceptAnyCertificateCheckbox;
        this.acceptAnyCertificateCheckbox.setTarget(this);
        this.acceptAnyCertificateCheckbox.setAction(new NSSelector("acceptAnyCertificateCheckboxClicked", new Class[]{NSButton.class}));
        this.acceptAnyCertificateCheckbox.setState(
                Preferences.instance().getBoolean("ftp.tls.acceptAnyCertificate") ? NSCell.OnState : NSCell.OffState);
    }

    public void acceptAnyCertificateCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("ftp.tls.acceptAnyCertificate", enabled);
    }

    private NSButton secureDataChannelCheckbox; //IBOutlet

    public void setSecureDataChannelCheckbox(NSButton secureDataChannelCheckbox) {
        this.secureDataChannelCheckbox = secureDataChannelCheckbox;
        this.secureDataChannelCheckbox.setTarget(this);
        this.secureDataChannelCheckbox.setAction(new NSSelector("secureDataChannelCheckboxClicked", new Class[]{NSButton.class}));
        this.secureDataChannelCheckbox.setState(
                Preferences.instance().getProperty("ftp.tls.datachannel").equals("P") ? NSCell.OnState : NSCell.OffState);
    }

    public void secureDataChannelCheckboxClicked(final NSButton sender) {
        if (sender.state() ==  NSCell.OnState) {
            Preferences.instance().setProperty("ftp.tls.datachannel", "P");
        }
        if (sender.state() ==  NSCell.OffState) {
            Preferences.instance().setProperty("ftp.tls.datachannel", "C");
        }
    }

    private NSButton failInsecureDataChannelCheckbox; //IBOutlet

    public void setFailInsecureDataChannelCheckbox(NSButton failInsecureDataChannelCheckbox) {
        this.failInsecureDataChannelCheckbox = failInsecureDataChannelCheckbox;
        this.failInsecureDataChannelCheckbox.setTarget(this);
        this.failInsecureDataChannelCheckbox.setAction(new NSSelector("failInsecureDataChannelCheckboxClicked", new Class[]{NSButton.class}));
        this.failInsecureDataChannelCheckbox.setState(
                Preferences.instance().getBoolean("ftp.tls.datachannel.failOnError") ? NSCell.OffState : NSCell.OnState);
    }

    public void failInsecureDataChannelCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.OnState;
        Preferences.instance().setProperty("ftp.tls.datachannel.failOnError", !enabled);
    }

    private NSPopUpButton sshTransfersCombobox; //IBOutlet

    public void setSshTransfersCombobox(NSPopUpButton sshTransfersCombobox) {
        this.sshTransfersCombobox = sshTransfersCombobox;
        this.sshTransfersCombobox.setTarget(this);
        this.sshTransfersCombobox.setAction(new NSSelector("sshTransfersComboboxClicked", new Class[]{NSPopUpButton.class}));
        this.sshTransfersCombobox.selectItemWithTitle(Preferences.instance().getProperty("ssh.transfers").toUpperCase());
    }

    public void sshTransfersComboboxClicked(NSPopUpButton sender) {
        Preferences.instance().setProperty("ssh.transfers", sender.titleOfSelectedItem().toLowerCase());
    }
}
