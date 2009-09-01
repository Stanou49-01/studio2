/**
 * This file Copyright (c) 2005-2008 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain Eclipse Public Licensed code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ide.core.ui.browser;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author Kevin Sawicki (ksawicki@aptana.com)
 */
public class BaseBrowserAdapter implements IBrowser
{

	private Browser browser;

	/**
	 * @see com.aptana.ide.core.ui.browser.IBrowser#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent)
	{
		browser = new Browser(parent, SWT.NONE);
	}

	/**
	 * @see com.aptana.ide.core.ui.browser.IBrowser#getControl()
	 */
	public Control getControl()
	{
		return browser;
	}

	/**
	 * @see com.aptana.ide.core.ui.browser.IBrowser#setURL(java.lang.String)
	 */
	public void setURL(String url)
	{
		browser.setUrl(url);
	}

	/**
	 * @see com.aptana.ide.core.ui.browser.IBrowser#back()
	 */
	public void back()
	{
		browser.back();
	}

	/**
	 * @see com.aptana.ide.core.ui.browser.IBrowser#dispose()
	 */
	public void dispose()
	{
		browser.dispose();
	}

	/**
	 * @see com.aptana.ide.core.ui.browser.IBrowser#forward()
	 */
	public void forward()
	{
		browser.forward();
	}

	/**
	 * @see com.aptana.ide.core.ui.browser.IBrowser#refresh()
	 */
	public void refresh()
	{
		browser.refresh();
	}

	/**
	 * @see com.aptana.ide.core.ui.browser.IBrowser#addLocationListener(org.eclipse.swt.browser.LocationListener)
	 */
	public void addLocationListener(LocationListener listener)
	{
	    browser.addLocationListener(listener);
	}

	/**
	 * @see com.aptana.ide.core.ui.browser.IBrowser#removeLocationListener(org.eclipse.swt.browser.LocationListener)
	 */
	public void removeLocationListener(LocationListener listener)
	{
	    browser.removeLocationListener(listener);
	}
}
