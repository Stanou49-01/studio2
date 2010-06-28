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
package com.aptana.ide.core.resources;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import com.aptana.ide.internal.core.resources.MarkerInfo;
import com.aptana.ide.internal.core.resources.MarkerManager;
import com.aptana.ide.internal.core.resources.UniformResourceMarker;

/**
 * @author Max Stepanov
 *
 */
public final class MarkerUtils {

	private static final IMarker[] NO_MARKERS = new IMarker[0];
	
	private MarkerUtils() {
	}

	/**
	 * createMarker
	 *
	 * @param resource
	 * @param attributes
	 * @param markerType
	 * @return IMarker
	 * @throws CoreException
	 */
	public static IMarker createMarker( IUniformResource resource, Map attributes, String markerType ) throws CoreException {
		MarkerInfo info = new MarkerInfo();
		info.setType(markerType);
		info.setCreationTime(System.currentTimeMillis());
		if ( attributes != null )
		{
			info.setAttributes(attributes);
		}
		getMarkerManager().add(resource, info);
		return new UniformResourceMarker(resource, info.getId());
	}
	
	/**
     * Creates marker for external resource.
     *
     * @param resource - resource.
     * @param attributes - marker attributes.
     * @param markerType - marker type.
     * @return marker.
     * @throws CoreException IF exception occurs.
     */
    public static IMarker createMarkerForExternalResource(
            IUniformResource resource, Map attributes, String markerType ) throws CoreException {
        IMarker marker = createMarker(resource, attributes, markerType);
        getMarkerManager().externalResourceChanged(resource);
        return marker;
    }
	
	/**
	 * findMarkers
	 *
	 * @param resource
	 * @param type
	 * @param includeSubtypes
	 * @return IMarker[]
	 */
	public static IMarker[] findMarkers(IUniformResource resource, String type, boolean includeSubtypes) {
		MarkerManager markerManager = getMarkerManager();
		MarkerInfo[] list = markerManager.findMarkersInfo(resource,type,includeSubtypes);
		ArrayList result = new ArrayList();
		for( int i = 0; i < list.length; ++i ) {
			result.add( new UniformResourceMarker(resource,list[i].getId()));
		}
		if (result.size() == 0) {
			return NO_MARKERS;
		}
		return (IMarker[]) result.toArray(new IMarker[result.size()]);
	}

	private static MarkerManager getMarkerManager() {
		return MarkerManager.getInstance();
	}
	
	/**
	 * addResourceChangeListener
	 *
	 * @param listener
	 */
	public static void addResourceChangeListener( IUniformResourceChangeListener listener ) {
		getMarkerManager().addResourceChangeListener(listener);
	}

	/**
	 * removeResourceChangeListener
	 *
	 * @param listener
	 */
	public static void removeResourceChangeListener( IUniformResourceChangeListener listener ) {
		getMarkerManager().removeResourceChangeListener(listener);
	}
	
	/**
	 * Sets char end.
	 * @param attributes - attributes.
	 * @param charEnd - char end.
	 */
    public static void setCharEnd(Map attributes, int charEnd) {
        attributes.put(IMarker.CHAR_END, new Integer(charEnd));
    }
    
    /**
     * Sets char end.
     * @param attributes - attributes.
     * @param charEnd - char end.
     */
    public static void setCharStart(Map attributes, int charStart) {
        attributes.put(IMarker.CHAR_START, new Integer(charStart));
    }
    
    /**
     * Sets message.
     * @param attributes - attributes.
     * @param message - message.
     */
    public static void setMessage(Map attributes, String message) {
        attributes.put(IMarker.MESSAGE, message);
    }
    
    /**
     * Sets line number.
     * @param attributes - attributes.
     * @param line - line number.
     */
    public static void setLineNumber(Map attributes, int line) {
        attributes.put(IMarker.LINE_NUMBER, new Integer(line));
    }
}
