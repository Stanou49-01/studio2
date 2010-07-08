/**
 * This file Copyright (c) 2005-2010 Aptana, Inc. This program is
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

package com.aptana.ide.ui.io.actions;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IActionFilter;

import com.aptana.ide.core.io.IConnectionPoint;
import com.aptana.ide.core.io.IConnectionPointCategory;

/**
 * @author Max Stepanov
 *
 */
public class ConnectionActionFilter implements IActionFilter {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionFilter#testAttribute(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public boolean testAttribute(Object target, String name, String value) {
		if (target instanceof IConnectionPoint) {
			if ( "isConnected".equals(name)) { //$NON-NLS-1$
				return ((IConnectionPoint) target).isConnected() == toBoolean(value);
			}
			if ( "canDisconnect".equals(name)) { //$NON-NLS-1$
				return ((IConnectionPoint) target).canDisconnect() == toBoolean(value);
			}
			if ( "isLocal".equals(name)) { //$NON-NLS-1$
				try {
					return ((IConnectionPoint) target).getRoot().getAdapter(File.class) != null;
				} catch (CoreException ignore) {
				}
			}			
			if ( "isWorkspace".equals(name)) { //$NON-NLS-1$
				try {
					return ((IConnectionPoint) target).getRoot().getAdapter(IResource.class) != null;
				} catch (CoreException ignore) {
				}
			}			
		} else if (target instanceof IConnectionPointCategory) {
			if ( "isCategory".equals(name)) { //$NON-NLS-1$
				return String.valueOf(value).equals(((IConnectionPointCategory) target).getId());
			}			
		}
		return false;
	}

	private static boolean toBoolean(Object value) {
		if ( value instanceof Boolean ) {
			 return ((Boolean)value).booleanValue();
		} else if (value instanceof String) {
			return Boolean.parseBoolean((String) value);
		}
		return false;
	}

	public static class Factory implements IAdapterFactory {
		
		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
		 */
		@SuppressWarnings("unchecked")
		public Object getAdapter(Object adaptableObject, Class adapterType) {
			if (adapterType == IActionFilter.class) {
				return new ConnectionActionFilter();
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
		 */
		@SuppressWarnings("unchecked")
		public Class[] getAdapterList() {
			return new Class[] { IActionFilter.class };
		}
		
	}

}
