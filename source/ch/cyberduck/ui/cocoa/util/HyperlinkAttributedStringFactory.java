package ch.cyberduck.ui.cocoa.util;

import com.apple.cocoa.application.NSColor;
import com.apple.cocoa.foundation.NSAttributedString;
import com.apple.cocoa.foundation.NSDictionary;
import com.apple.cocoa.foundation.NSMutableAttributedString;
import com.apple.cocoa.foundation.NSRange;
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

/**
 * From http://developer.apple.com/qa/qa2006/qa1487.html
 *
 * @version $Id:$
 */
public class HyperlinkAttributedStringFactory {

    /**
     * @param hyperlink
     */
    public static NSAttributedString create(final NSMutableAttributedString value, final String hyperlink) {
        NSRange range = new NSRange(0, value.length());
        value.beginEditing();
        value.addAttributeInRange(NSMutableAttributedString.LinkAttributeName,
                hyperlink, range);
        // make the text appear in blue
        value.addAttributeInRange(NSMutableAttributedString.ForegroundColorAttributeName,
                NSColor.blueColor(), range);

        // next make the text appear with an underline
        value.addAttributeInRange(NSMutableAttributedString.UnderlineStyleAttributeName,
                NSMutableAttributedString.SingleUnderlineStyle, range);

        value.endEditing();
        return value;
    }
}