/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.aptana.ide.ui.editors.preferences.formatter;


import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import com.aptana.ide.internal.ui.dialogs.PreferencesAccess;


/**
 * 
 * The page to configure the code formatter options.
 */
public abstract class CodeFormatterPreferencePage extends ProfilePreferencePage {

	/**
	 * @param editor
	 * @param store
	 */
	public CodeFormatterPreferencePage(String editor,IPreferenceStore store) {
		super(store);
		// only used when page is shown programatically
		setTitle("Code Formatter"); //$NON-NLS-1$
	}
	
	/**
	 * @see com.aptana.ide.ui.editors.preferences.formatter.ProfilePreferencePage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	
	public void createControl(Composite parent) {
	    super.createControl(parent);
    	PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), ""); //$NON-NLS-1$
	}

	/**
	 * @see com.aptana.ide.ui.editors.preferences.formatter.ProfilePreferencePage#createConfigurationBlock(com.aptana.ide.internal.ui.dialogs.PreferencesAccess)
	 */
	protected abstract ProfileConfigurationBlock createConfigurationBlock(PreferencesAccess access);
	


	

}
