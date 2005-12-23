package ch.cyberduck.core;

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

import java.util.ArrayList;
import java.util.Vector;

/**
 * @version $Id$
 */
public class Collection extends ArrayList implements CollectionListener {

    public int indexOf(Object elem) {
        for (int i = 0; i < this.size(); i++) {
            if (this.get(i).equals(elem))
                return i;
        }
        return -1;
    }

    public int lastIndexOf(Object elem) {
        for (int i = this.size() - 1; i >= 0; i--) {
            if (this.get(i).equals(elem))
                return i;
        }
        return -1;
    }

    private Vector listeners = new Vector();

    public void addListener(CollectionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CollectionListener listener) {
        listeners.remove(listener);
    }

    public boolean add(Object object) {
        super.add(object);
        this.collectionItemAdded(object);
        return true;
    }

    public void add(int row, Object object) {
        super.add(row, object);
        this.collectionItemAdded(object);
    }

    public Object remove(int row) {
        Object previous = super.remove(row);
        this.collectionItemRemoved(previous);
        return previous;
    }

    public void collectionItemAdded(Object item) {
        CollectionListener[] l = (CollectionListener[])listeners.toArray(new CollectionListener[]{});
        for(int i = 0; i < l.length; i++) {
            l[i].collectionItemAdded(item);
        }
    }

    public void collectionItemRemoved(Object item) {
        CollectionListener[] l = (CollectionListener[])listeners.toArray(new CollectionListener[]{});
        for(int i = 0; i < l.length; i++) {
            l[i].collectionItemRemoved(item);
        }
    }

    public void collectionItemChanged(Object item) {
        CollectionListener[] l = (CollectionListener[])listeners.toArray(new CollectionListener[]{});
        for(int i = 0; i < l.length; i++) {
            l[i].collectionItemChanged(item);
        }
    }
}
