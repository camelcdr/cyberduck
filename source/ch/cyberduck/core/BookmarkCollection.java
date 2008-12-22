package ch.cyberduck.core;

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

import com.apple.cocoa.application.NSImage;

import ch.cyberduck.ui.cocoa.CDIconCache;

/**
 * @version $Id$
 */
public abstract class BookmarkCollection extends Collection<Host> {

    public BookmarkCollection() {
        super();
    }

    public BookmarkCollection(java.util.Collection<Host> c) {
        super(c);
    }

    /**
     * Add new bookmark to the collection
     *
     * @return
     */
    public boolean allowsAdd() {
        return true;
    }

    /**
     * Remove a bookmark from the collection
     *
     * @return
     */
    public boolean allowsDelete() {
        return true;
    }

    /**
     * Edit the bookmark configuration
     *
     * @return
     */
    public boolean allowsEdit() {
        return true;
    }

    public NSImage getIcon(Host host) {
        return CDIconCache.instance().iconForName(host.getProtocol().icon(),
                Preferences.instance().getBoolean("browser.bookmarkDrawer.smallItems") ? 16 : 32);
    }
}