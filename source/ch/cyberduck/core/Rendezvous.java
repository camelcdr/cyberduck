package ch.cyberduck.core;

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

import java.util.Iterator;

/**
 * @version $Id:$
 */
public interface Rendezvous {
    /**
     * Start browsing for rendezvous servcices for all registered service types
     */
    void init();

    /**
     * Halt all service discvery browsers
     */
    void quit();

    void addListener(RendezvousListener listener);

    void removeListener(RendezvousListener listener);

    int numberOfServices();

    Host getService(int index);

    Iterator<Host> iterator();

    String getDisplayedName(int index);

    String getDisplayedName(String identifier);
}
