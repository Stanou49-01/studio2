/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.aptana.ide.internal.ui.dialogs;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.ui.preferences.IWorkingCopyManager;

import org.osgi.service.prefs.BackingStoreException;

/**
 * 
 */
public class PreferencesAccess {
	
	/**
	 * @return s
	 */
	public static PreferencesAccess getOriginalPreferences() {
		return new PreferencesAccess();
	}
	
	/**
	 * @param workingCopyManager
	 * @return s
	 */
	public static PreferencesAccess getWorkingCopyPreferences(IWorkingCopyManager workingCopyManager) {
		return new WorkingCopyPreferencesAccess(workingCopyManager);
	}
		
	private PreferencesAccess() {
		// can only extends in this file
	}
	
	/**
	 * @return s
	 */
	public IScopeContext getDefaultScope() {
		return new DefaultScope();
	}
	
	/**
	 * @return s
	 */
	public IScopeContext getInstanceScope() {
		return new InstanceScope();
	}
	
	/**
	 * @param project
	 * @return s
	 */
	public IScopeContext getProjectScope(IProject project) {
		return new ProjectScope(project);
	}
	
	/**
	 * @throws BackingStoreException
	 */
	public void applyChanges() throws BackingStoreException {
	}
	
	
	private static class WorkingCopyPreferencesAccess extends PreferencesAccess {
		
		private final IWorkingCopyManager fWorkingCopyManager;

		private WorkingCopyPreferencesAccess(IWorkingCopyManager workingCopyManager) {
			fWorkingCopyManager= workingCopyManager;
		}
		
		private final IScopeContext getWorkingCopyScopeContext(IScopeContext original) {
			return new WorkingCopyScopeContext(fWorkingCopyManager, original);
		}
		
		/**
		 * @see com.aptana.ide.internal.ui.dialogs.PreferencesAccess#getDefaultScope()
		 */
		public IScopeContext getDefaultScope() {
			return getWorkingCopyScopeContext(super.getDefaultScope());
		}
		
		/**
		 * @see com.aptana.ide.internal.ui.dialogs.PreferencesAccess#getInstanceScope()
		 */
		public IScopeContext getInstanceScope() {
			return getWorkingCopyScopeContext(super.getInstanceScope());
		}
		
		/**
		 * @see com.aptana.ide.internal.ui.dialogs.PreferencesAccess#getProjectScope(org.eclipse.core.resources.IProject)
		 */
		public IScopeContext getProjectScope(IProject project) {
			return getWorkingCopyScopeContext(super.getProjectScope(project));
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.preferences.PreferencesAccess#applyChanges()
		 */
		/**
		 * @see com.aptana.ide.internal.ui.dialogs.PreferencesAccess#applyChanges()
		 */
		public void applyChanges() throws BackingStoreException {
			fWorkingCopyManager.applyChanges();
		}
	}
	
	
	private static class WorkingCopyScopeContext implements IScopeContext {
		
		private final IWorkingCopyManager fWorkingCopyManager;
		private final IScopeContext fOriginal;

		/**
		 * @param workingCopyManager
		 * @param original
		 */
		public WorkingCopyScopeContext(IWorkingCopyManager workingCopyManager, IScopeContext original) {
			fWorkingCopyManager= workingCopyManager;
			fOriginal= original;	
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.preferences.IScopeContext#getName()
		 */
		/**
		 * @see org.eclipse.core.runtime.preferences.IScopeContext#getName()
		 */
		public String getName() {
			return fOriginal.getName();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.preferences.IScopeContext#getNode(java.lang.String)
		 */
		/**
		 * @see org.eclipse.core.runtime.preferences.IScopeContext#getNode(java.lang.String)
		 */
		public IEclipsePreferences getNode(String qualifier) {
			return fWorkingCopyManager.getWorkingCopy(fOriginal.getNode(qualifier));
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.preferences.IScopeContext#getLocation()
		 */
		/**
		 * @see org.eclipse.core.runtime.preferences.IScopeContext#getLocation()
		 */
		public IPath getLocation() {
			return fOriginal.getLocation();
		}
	}


}
