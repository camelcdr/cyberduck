package ch.cyberduck.ui.cocoa;

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

import com.apple.cocoa.application.*;
import com.apple.cocoa.foundation.*;

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import org.apache.log4j.Logger;

import ch.cyberduck.core.*;

public class CDQueueController extends NSObject {
    private static Logger log = Logger.getLogger(CDQueueController.class);

    private static CDQueueController instance;

    private static NSMutableArray instances = new NSMutableArray();

    /**
     * The observer to notify when an upload is complete
     */
    private Observer callback;

    private NSToolbar toolbar;
	
    private CDQueueController() {
        instances.addObject(this);
    }

    public static CDQueueController instance() {
        if (null == instance) {
            instance = new CDQueueController();
            if (false == NSApplication.loadNibNamed("Queue", instance)) {
                log.fatal("Couldn't load Queue.nib");
            }
        }
        return instance;
    }
	
	/*
	 public int checkForRunningTransfers() {
		Iterator iter = CDQueueList.instance().iterator();
		while(iter.hasNext()) {
			ch.cyberduck.core.Queue q = (ch.cyberduck.core.Queue)iter.next();
			if(q.isRunning()) {
				NSAlertPanel.beginCriticalAlertSheet(NSBundle.localizedString("Transfers in progress", ""), //title
													 NSBundle.localizedString("Cancel", ""), // defaultbutton
													 NSBundle.localizedString("Quit", ""), //alternative button
													 null, //other button
													 this.window(), //window
													 this, //delegate
													 new NSSelector("checkForRunningTransfersSheetDidEnd",
																	new Class[]{NSWindow.class, int.class, Object.class}),
													 null, // dismiss selector
													 null, // context
													 NSBundle.localizedString("There are items in the queue currently being transferred. Quit anyway?", "") // message
													 );
				return NSApplication.TerminateLater; //break
			}
		}
		return NSApplication.TerminateNow;
	}
	
	public void checkForRunningTransfersSheetDidEnd(NSWindow sheet, int returncode, Object contextInfo) {
        sheet.orderOut(null);
        if (returncode == NSAlertPanel.AlternateReturn) {
			this.stopAllButtonClicked(null);
			NSApplication.sharedApplication().replyToApplicationShouldTerminate(true);
		}
        if (returncode == NSAlertPanel.DefaultReturn) {
			NSApplication.sharedApplication().replyToApplicationShouldTerminate(false);
		}
	}
	 */

	
    public boolean windowShouldClose(NSWindow sender) {
        log.debug("windowShouldClose" + sender);
        return true;
    }

    public void windowWillClose(NSNotification notification) {
        log.debug("windowWillClose:" + notification);
		//@todo this controller may still be the observer of a queue item
		instances.removeObject(this);
		instance = null;
    }

    private NSWindow window; // IBOutlet

    public void setWindow(NSWindow window) {
        this.window = window;
        this.window.setDelegate(this);
    }

    public NSWindow window() {
        return this.window;
    }

    private CDQueueTableDataSource queueModel;
    private NSTableView queueTable; // IBOutlet

    public void setQueueTable(NSTableView queueTable) {
        this.queueTable = queueTable;
        this.queueTable.setTarget(this);
        this.queueTable.setDoubleAction(new NSSelector("queueTableRowDoubleClicked", new Class[]{Object.class}));
        this.queueTable.setDataSource(this.queueModel = new CDQueueTableDataSource());
        this.queueTable.setDelegate(this.queueModel);
        // receive drag events from types
        // in fact we are not interested in file promises, but because the browser model can only initiate
        // a drag with tableView.dragPromisedFilesOfTypes(), we listens for those events
        // and then use the private pasteboard instead.
        this.queueTable.registerForDraggedTypes(new NSArray(new Object[]{"QueuePBoardType",
                                                                         NSPasteboard.StringPboardType,
                                                                         NSPasteboard.FilesPromisePboardType}));

        this.queueTable.setRowHeight(50f);

        NSTableColumn dataColumn = new NSTableColumn();
        dataColumn.setIdentifier("DATA");
        dataColumn.setMinWidth(200f);
        dataColumn.setWidth(350f);
        dataColumn.setMaxWidth(1000f);
        dataColumn.setEditable(false);
        dataColumn.setResizable(true);
        dataColumn.setDataCell(new CDQueueCell());
        this.queueTable.addTableColumn(dataColumn);

        NSTableColumn progressColumn = new NSTableColumn();
        progressColumn.setIdentifier("PROGRESS");
        progressColumn.setMinWidth(80f);
        progressColumn.setWidth(300f);
        progressColumn.setMaxWidth(1000f);
        progressColumn.setEditable(false);
        progressColumn.setResizable(true);
        progressColumn.setDataCell(new CDProgressCell());
        this.queueTable.addTableColumn(progressColumn);

        NSSelector setUsesAlternatingRowBackgroundColorsSelector =
                new NSSelector("setUsesAlternatingRowBackgroundColors", new Class[]{boolean.class});
        if (setUsesAlternatingRowBackgroundColorsSelector.implementedByClass(NSTableView.class)) {
            this.queueTable.setUsesAlternatingRowBackgroundColors(true);
        }
        NSSelector setGridStyleMaskSelector =
                new NSSelector("setGridStyleMask", new Class[]{int.class});
        if (setGridStyleMaskSelector.implementedByClass(NSTableView.class)) {
            this.queueTable.setGridStyleMask(NSTableView.SolidHorizontalGridLineMask);
        }
		
        //selection properties
        this.queueTable.setAllowsMultipleSelection(true);
        this.queueTable.setAllowsEmptySelection(true);
        this.queueTable.setAllowsColumnReordering(false);

        this.queueTable.sizeToFit();
    }
	
    public void startItem(Queue queue) {
        this.startItem(queue, false);
    }

    public void startItem(Queue queue, Observer callback) {
        this.callback = callback;
        this.startItem(queue, false);
    }

    public void startItem(Queue queue, boolean resumeRequested) {
        log.info("Starting item:" + queue);
//        QueueList.instance().save();
        this.queueTable.reloadData();
        this.queueTable.selectRow(QueueList.instance().indexOf(queue), false);
        this.queueTable.scrollRowToVisible(QueueList.instance().indexOf(queue));

        if (Preferences.instance().getProperty("queue.orderFrontOnTransfer").equals("true")) {
            this.window().makeKeyAndOrderFront(null);
        }

		//@todo reference to this.window() may become invalid
        queue.getRoot().getHost().getLogin().setController(new CDLoginController(this.window()));
        if (queue.getRoot().getHost().getProtocol().equals(Session.SFTP)) {
            try {
                queue.getRoot().getHost().setHostKeyVerificationController(new CDHostKeyController(this.window()));
            }
            catch (com.sshtools.j2ssh.transport.InvalidHostFileException e) {
                this.window().makeKeyAndOrderFront(null);
                //This exception is thrown whenever an exception occurs open or reading from the host file.
                NSAlertPanel.beginCriticalAlertSheet(NSBundle.localizedString("Error", ""), //title
                        NSBundle.localizedString("OK", ""), // defaultbutton
                        null, //alternative button
                        null, //other button
                        this.window(), //docWindow
                        null, //modalDelegate
                        null, //didEndSelector
                        null, // dismiss selector
                        null, // context
                        NSBundle.localizedString("Could not open or read the host file", "") + ": " + e.getMessage() // message
                );
            }
        }
        queue.start(new CDValidatorController(queue.kind(), resumeRequested));
    }
	
	public boolean isVisible() {
		return this.window() != null && this.window().isVisible();
	}

    public void update(Queue observable, Object arg) {
//		log.debug("update:"+observable+","+arg);
		if(this.isVisible()) {
			if (arg instanceof Message) {
				Message msg = (Message)arg;
				if (msg.getTitle().equals(Message.PROGRESS) || msg.getTitle().equals(Message.ERROR)) {
					if (this.window().isVisible()) {
						if (this.queueTable.visibleRect() != NSRect.ZeroRect) {
							int row = QueueList.instance().indexOf(observable);
							NSRect queueRect = this.queueTable.frameOfCellAtLocation(0, row);
							this.queueTable.setNeedsDisplay(queueRect);
						}
					}
				}
				else if (msg.getTitle().equals(Message.DATA)) {
					if (this.window().isVisible()) {
						if (this.queueTable.visibleRect() != NSRect.ZeroRect) {
							int row = QueueList.instance().indexOf(observable);
							NSRect progressRect = this.queueTable.frameOfCellAtLocation(1, row);
							this.queueTable.setNeedsDisplay(progressRect);
						}
					}
				}
				else if (msg.getTitle().equals(Message.QUEUE_START)) {
					this.toolbar.validateVisibleItems();
					QueueList.instance().save();
				}
				else if (msg.getTitle().equals(Message.QUEUE_STOP)) {
					this.toolbar.validateVisibleItems();
					Queue queue = (Queue)observable;
					if (queue.isComplete()) {
						if (Queue.KIND_DOWNLOAD == queue.kind()) {
							if (Preferences.instance().getProperty("queue.postProcessItemWhenComplete").equals("true")) {
								boolean success = NSWorkspace.sharedWorkspace().openFile(queue.getRoot().getLocal().toString());
								log.debug("Success opening file:" + success);
							}
						}
						if (Queue.KIND_UPLOAD == queue.kind()) {
							if (callback != null) {
								log.debug("Telling observable to refresh directory listing");
								callback.update(null, new Message(Message.REFRESH));
							}
						}
						if (Preferences.instance().getProperty("queue.removeItemWhenComplete").equals("true")) {
							this.queueTable.deselectAll(null);
							QueueList.instance().removeItem(queue);
							this.queueTable.reloadData();
						}
					}
					QueueList.instance().save();
				}
			}
		}
    }

    public void awakeFromNib() {
        log.debug("awakeFromNib");
        this.toolbar = new NSToolbar("Queue Toolbar");
        this.toolbar.setDelegate(this);
        this.toolbar.setAllowsUserCustomization(true);
        this.toolbar.setAutosavesConfiguration(true);
        this.window.setToolbar(toolbar);
    }

    // ----------------------------------------------------------
    // Toolbar Delegate
    // ----------------------------------------------------------

    public NSToolbarItem toolbarItemForItemIdentifier(NSToolbar toolbar, String itemIdentifier, boolean flag) {
        NSToolbarItem item = new NSToolbarItem(itemIdentifier);
        if (itemIdentifier.equals("Stop")) {
            item.setLabel(NSBundle.localizedString("Stop", ""));
            item.setPaletteLabel(NSBundle.localizedString("Stop", ""));
            item.setImage(NSImage.imageNamed("stop.tiff"));
            item.setTarget(this);
            item.setAction(new NSSelector("stopButtonClicked", new Class[]{Object.class}));
            return item;
        }
        if (itemIdentifier.equals("Resume")) {
            item.setLabel(NSBundle.localizedString("Resume", ""));
            item.setPaletteLabel(NSBundle.localizedString("Resume", ""));
            item.setImage(NSImage.imageNamed("resume.tiff"));
            item.setTarget(this);
            item.setAction(new NSSelector("resumeButtonClicked", new Class[]{Object.class}));
            return item;
        }
        if (itemIdentifier.equals("Reload")) {
            item.setLabel(NSBundle.localizedString("Reload", ""));
            item.setPaletteLabel(NSBundle.localizedString("Reload", ""));
            item.setImage(NSImage.imageNamed("reload.tiff"));
            item.setTarget(this);
            item.setAction(new NSSelector("reloadButtonClicked", new Class[]{Object.class}));
            return item;
        }
        if (itemIdentifier.equals("Show")) {
            item.setLabel(NSBundle.localizedString("Show", ""));
            item.setPaletteLabel(NSBundle.localizedString("Show in Finder", ""));
            item.setImage(NSImage.imageNamed("reveal.tiff"));
            item.setTarget(this);
            item.setAction(new NSSelector("revealButtonClicked", new Class[]{Object.class}));
            return item;
        }
        if (itemIdentifier.equals("Open")) {
            item.setLabel(NSBundle.localizedString("Open", ""));
            item.setPaletteLabel(NSBundle.localizedString("Open", ""));
            item.setImage(NSImage.imageNamed("open.tiff"));
            item.setTarget(this);
            item.setAction(new NSSelector("openButtonClicked", new Class[]{Object.class}));
            return item;
        }
        if (itemIdentifier.equals("Remove")) {
            item.setLabel(NSBundle.localizedString("Remove", ""));
            item.setPaletteLabel(NSBundle.localizedString("Remove", ""));
            item.setImage(NSImage.imageNamed("clean.tiff"));
            item.setTarget(this);
            item.setAction(new NSSelector("removeButtonClicked", new Class[]{Object.class}));
            return item;
        }
        if (itemIdentifier.equals("Clear")) {
            item.setLabel(NSBundle.localizedString("Clear", ""));
            item.setPaletteLabel(NSBundle.localizedString("Clear", ""));
            item.setImage(NSImage.imageNamed("cleanAll.tiff"));
            item.setTarget(this);
            item.setAction(new NSSelector("clearButtonClicked", new Class[]{Object.class}));
            return item;
        }
        // itemIdent refered to a toolbar item that is not provide or supported by us or cocoa.
        // Returning null will inform the toolbar this kind of item is not supported.
        return null;
    }

    public void queueTableRowDoubleClicked(Object sender) {
        if (this.queueTable.selectedRow() != -1) {
            Queue item = QueueList.instance().getItem(this.queueTable.selectedRow());
            if (item.isComplete()) {
                this.revealButtonClicked(sender);
            }
            else {
                this.resumeButtonClicked(sender);
            }
        }
    }

    public void stopButtonClicked(Object sender) {
        NSEnumerator enum = queueTable.selectedRowEnumerator();
        while (enum.hasMoreElements()) {
            Queue queue = QueueList.instance().getItem(((Integer)enum.nextElement()).intValue());
            if (queue.isRunning()) {
                queue.cancel();
            }
        }
    }

	public void stopAllButtonClicked(Object sender) {
		Iterator iter = CDQueueList.instance().iterator();
		while(iter.hasNext()) {
			Queue q = (Queue)iter.next();
			if(q.isRunning()) {
				q.cancel();
			}
		}
	}

    public void resumeButtonClicked(Object sender) {
        NSEnumerator enum = queueTable.selectedRowEnumerator();
        while (enum.hasMoreElements()) {
            Queue queue = QueueList.instance().getItem(((Integer)enum.nextElement()).intValue());
            if (!queue.isRunning()) {
                this.startItem(queue, true);
            }
        }
    }

    public void reloadButtonClicked(Object sender) {
        NSEnumerator enum = queueTable.selectedRowEnumerator();
        while (enum.hasMoreElements()) {
            Queue queue = QueueList.instance().getItem(((Integer)enum.nextElement()).intValue());
            if (!queue.isRunning()) {
                this.startItem(queue, false);
            }
        }
    }

    public void openButtonClicked(Object sender) {
        if (this.queueTable.selectedRow() != -1) {
            Queue item = QueueList.instance().getItem(this.queueTable.selectedRow());
            Path f = item.getRoot();
            String file = item.getRoot().getLocal().toString();
            if (!NSWorkspace.sharedWorkspace().openFile(file)) {
                if (item.isComplete()) {
                    NSAlertPanel.beginCriticalAlertSheet(NSBundle.localizedString("Could not open the file", ""), //title
														 NSBundle.localizedString("OK", ""), // defaultbutton
														 null, //alternative button
														 null, //other button
														 this.window(), //docWindow
														 null, //modalDelegate
														 null, //didEndSelector
														 null, // dismiss selector
														 null, // context
														 NSBundle.localizedString("Could not open the file", "") + " \""
														 + file
														 + "\". " + NSBundle.localizedString("It moved since you downloaded it.", "") // message
														 );
                }
                else {
                    NSAlertPanel.beginCriticalAlertSheet(NSBundle.localizedString("Could not open the file", ""), //title
														 NSBundle.localizedString("OK", ""), // defaultbutton
														 null, //alternative button
														 null, //other button
														 this.window(), //docWindow
														 null, //modalDelegate
														 null, //didEndSelector
														 null, // dismiss selector
														 null, // context
														 NSBundle.localizedString("Could not open the file", "") + " \""
														 + file
														 + "\". " + NSBundle.localizedString("The file has not yet been downloaded.", "") // message
														 );
                }
            }
        }
    }

    public void revealButtonClicked(Object sender) {
        if (this.queueTable.selectedRow() != -1) {
            Queue item = QueueList.instance().getItem(this.queueTable.selectedRow());
            Path f = item.getRoot();
            String file = item.getRoot().getLocal().toString();
            if (!NSWorkspace.sharedWorkspace().selectFile(file, "")) {
                if (item.isComplete()) {
                    NSAlertPanel.beginCriticalAlertSheet(NSBundle.localizedString("Could not show the file in the Finder", ""), //title
                            NSBundle.localizedString("OK", ""), // defaultbutton
                            null, //alternative button
                            null, //other button
                            this.window(), //docWindow
                            null, //modalDelegate
                            null, //didEndSelector
                            null, // dismiss selector
                            null, // context
                            NSBundle.localizedString("Could not show the file", "") + " \""
                            + file
                            + "\". " + NSBundle.localizedString("It moved since you downloaded it.", "") // message
                    );
                }
                else {
                    NSAlertPanel.beginCriticalAlertSheet(NSBundle.localizedString("Could not show the file in the Finder", ""), //title
                            NSBundle.localizedString("OK", ""), // defaultbutton
                            null, //alternative button
                            null, //other button
                            this.window(), //docWindow
                            null, //modalDelegate
                            null, //didEndSelector
                            null, // dismiss selector
                            null, // context
                            NSBundle.localizedString("Could not show the file", "") + " \""
                            + file
                            + "\". " + NSBundle.localizedString("The file has not yet been downloaded.", "") // message
                    );
                }
            }
        }
    }

    public void removeButtonClicked(Object sender) {
        NSEnumerator enum = queueTable.selectedRowEnumerator();
        int i = 0;
        while (enum.hasMoreElements()) {
            QueueList.instance().removeItem(((Integer)enum.nextElement()).intValue() - i);
            i++;
        }
        this.queueTable.reloadData();
    }

    public void clearButtonClicked(Object sender) {
        for (Iterator iter = QueueList.instance().iterator(); iter.hasNext();) {
            Queue q = (Queue)iter.next();
            if (q.getSize() == q.getCurrent() && q.getSize() > 0) {
                iter.remove();
            }
        }
        this.queueTable.reloadData();
    }

    public boolean validateMenuItem(_NSObsoleteMenuItemProtocol cell) {
        String sel = cell.action().name();
        log.debug("validateMenuItem:" + sel);
        return this.validateItem(sel);
    }

    public NSArray toolbarDefaultItemIdentifiers(NSToolbar toolbar) {
        return new NSArray(new Object[]{
            "Resume",
            "Reload",
            "Stop",
            "Remove",
            "Clear",
            NSToolbarItem.FlexibleSpaceItemIdentifier,
			"Open",
            "Show"
        });
    }

    public NSArray toolbarAllowedItemIdentifiers(NSToolbar toolbar) {
        return new NSArray(new Object[]{
            "Resume",
            "Reload",
            "Stop",
            "Remove",
            "Clear",
            "Show",
			"Open",
            NSToolbarItem.CustomizeToolbarItemIdentifier,
            NSToolbarItem.SpaceItemIdentifier,
            NSToolbarItem.SeparatorItemIdentifier,
            NSToolbarItem.FlexibleSpaceItemIdentifier
        });
    }

    public boolean validateToolbarItem(NSToolbarItem item) {
        String identifier = item.itemIdentifier();
        return this.validateItem(identifier);
    }

    private boolean validateItem(String identifier) {
        if (identifier.equals("Stop") || identifier.equals("stopButtonClicked:")) {
            if (this.queueTable.numberOfSelectedRows() < 1) {
                return false;
            }
            NSEnumerator enum = queueTable.selectedRowEnumerator();
            while (enum.hasMoreElements()) {
                Queue queue = QueueList.instance().getItem(((Integer)enum.nextElement()).intValue());
                if (!queue.isRunning()) {
                    return false;
                }
            }
            return true;
        }
        if (identifier.equals("Resume") || identifier.equals("resumeButtonClicked:")) {
            if (this.queueTable.numberOfSelectedRows() == 1) {
                Queue queue = QueueList.instance().getItem(this.queueTable.selectedRow());
                return !queue.isRunning() && !queue.isComplete();
            }
            return false;
            /*
if (this.queueTable.numberOfSelectedRows() < 1) {
return false;
}
NSEnumerator enum = queueTable.selectedRowEnumerator();
while (enum.hasMoreElements()) {
Queue queue = QueueList.instance().getItem(((Integer) enum.nextElement()).intValue());
//                if (!(queue.isCanceled() && !(queue.remainingJobs() == 0) && (queue.getRoot() instanceof FTPPath))) {
if (!(queue.isCanceled() && !(queue.remainingJobs() == 0))) {
return false;
}
}
return true;
            */
        }
        if (identifier.equals("Reload") || identifier.equals("reloadButtonClicked:")) {
            if (this.queueTable.numberOfSelectedRows() == 1) {
                Queue queue = QueueList.instance().getItem(this.queueTable.selectedRow());
                return !queue.isRunning();
            }
            return false;
            /*
if (this.queueTable.numberOfSelectedRows() < 1) {
return false;
}
NSEnumerator enum = queueTable.selectedRowEnumerator();
while (enum.hasMoreElements()) {
Queue queue = QueueList.instance().getItem(((Integer) enum.nextElement()).intValue());
if (queue.isRunning()) {
return false;
}
}
return true;
         */
        }
        if (identifier.equals("Show") || identifier.equals("revealButtonClicked:")) {
            return this.queueTable.numberOfSelectedRows() == 1;
        }
        if (identifier.equals("Open") || identifier.equals("openButtonClicked:")) {
            return this.queueTable.numberOfSelectedRows() == 1;
        }
        if (identifier.equals("Clear")) {
            return this.queueTable.numberOfRows() > 0;
        }
        if (identifier.equals("Remove") || identifier.equals("removeButtonClicked:")) {
            if (this.queueTable.numberOfSelectedRows() < 1) {
                return false;
            }
            NSEnumerator enum = queueTable.selectedRowEnumerator();
            while (enum.hasMoreElements()) {
                Queue queue = QueueList.instance().getItem(((Integer)enum.nextElement()).intValue());
                if (queue.isRunning()) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }
}