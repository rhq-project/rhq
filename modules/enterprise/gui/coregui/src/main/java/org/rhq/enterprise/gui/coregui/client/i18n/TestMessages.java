/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.i18n;

import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;

/*
 * @PluralText takes an array of strings.  The even indexes represent plural form names, while the odd indexes
 * correspond to the text for the associated plural form.  Example:
 * 
 *    @PluralText( { "one", "There is a single item in your cart.",
 *                   "two", "There are two items in your cart.",
 *                   "few", "There are a few items in your cart.",
 *                   "many", "There are many items in your cart." } )
 *
 * As of GWT 2.0.4, the following plural forms exist:
 * 
 *    none  one     two   eighteleven
 *    few   paucal  many  other
 * 
 * However, not all of them are applicable to each locale.  When implementing a new localization, refer to the
 * appropriate subclass of "com.google.gwt.i18n.client.impl.plurals.DefaultRule" to obtain the necessary plural
 * form subset which must be implemented to correctly support that locale.
 * 
 * The rules implemented in these various subclasses are taken from:
 * 
 *    http://translate.sourceforge.net/wiki/l10n/pluralforms
 */
@DefaultLocale("en")
public interface TestMessages extends Messages {

    /**
     * @param subject the name of the user
     * @param cartItems the number of cart items
     * @return a message specifying the number of items in the user's cart
     */
    @DefaultMessage("{0}, there are {1,number} items in your cart")
    @PluralText( { "one", "{0}, there is {1,number} item in your cart" })
    String cartLabel(String subject, @PluralCount int cartItems);
}
