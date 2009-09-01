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
 * with certain other free and open source software ("FOSS") code and certain additional terms
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

package com.aptana.ide.core.internal.ui;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.aptana.ide.core.ui.CoreUIPlugin;
import com.aptana.ide.core.ui.preferences.IPreferenceConstants;

/**
 * @author Max Stepanov
 *
 */
public class WebFilesPropertyTester extends PropertyTester {

	private Set<String> extensions;
	
	/**
	 * 
	 */
	public WebFilesPropertyTester() {
		super();
		loadExtensions();
		CoreUIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if ( IPreferenceConstants.PREF_FILE_EXPLORER_WEB_FILES.equals(event.getProperty()) ) {
					loadExtensions();
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.PropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if ( receiver instanceof IFile ) {
			if ( "isWebRunnable".equals(property) ) { //$NON-NLS-1$
				boolean value = true;
				if ( expectedValue != null && expectedValue instanceof Boolean ) {
					value = ((Boolean)expectedValue).booleanValue();
				}
				String ext = ((IFile)receiver).getFileExtension();
				if ( ext != null && ext.length() > 0 ) {
					return extensions.contains(ext) == value;
				}
			}
		}
		return false;
	}
	
	private void loadExtensions() {		
		IPreferenceStore store = CoreUIPlugin.getDefault().getPreferenceStore();
		String[] files = store.getString(IPreferenceConstants.PREF_FILE_EXPLORER_WEB_FILES).split(";"); //$NON-NLS-1$
		
		extensions = new HashSet<String>();
		for( int i = 0; i < files.length; ++i ) {
			String ext = files[i];
			int index = ext.lastIndexOf('.');
			if ( index >= 0 ) {
				ext = ext.substring(index+1);
			}
			extensions.add(ext);
		}
	}

}
