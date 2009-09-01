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
package com.aptana.ide.internal.core.resources;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.internal.resources.IMarkerSetElement;
import org.eclipse.core.internal.resources.MarkerTypeDefinitionCache;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;

import com.aptana.ide.core.AptanaCorePlugin;
import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.resources.IUniformResource;
import com.aptana.ide.core.resources.IUniformResourceChangeListener;
import com.aptana.ide.internal.core.events.UniformResourceChangeEvent;

/**
 * @author Max Stepanov
 *
 */
public final class MarkerManager {

	private static final MarkerInfo[] NO_MARKER_INFO = new MarkerInfo[0];
	private static MarkerManager instance;

	private MarkerTypeDefinitionCache cache = new MarkerTypeDefinitionCache();
	private long nextMarkerId = 0;
	private Map resources = new HashMap();
	private ListenerList listeners = new ListenerList();
	private IMarker rootMarker;
	private Map currentDeltas = null;
	private Object lock = new Object();

	/**
	 * getInstance
	 *
	 * @return MarkerManager
	 */
	public static MarkerManager getInstance() {
		if ( instance == null ) {
			instance = new MarkerManager();
		}
		return instance;
	}
	
	private MarkerManager() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		try {
			rootMarker = workspace.getRoot().createMarker("com.aptana.ide.internal.core.resources.ExternalResourcesMarker"); //$NON-NLS-1$
		} catch (CoreException e) {
			IdeLog.logError(AptanaCorePlugin.getDefault(),StringUtils.EMPTY,e);
		}
		if ( rootMarker != null ) {
			workspace.addResourceChangeListener( new IResourceChangeListener() {
				public void resourceChanged(IResourceChangeEvent event) {
					handleResourceChanged();
				}
			}, IResourceChangeEvent.PRE_BUILD);			
		}
	}

	/**
	 * findMarkerInfo
	 *
	 * @param resource
	 * @param id
	 * @return MarkerInfo
	 */
	public MarkerInfo findMarkerInfo(IUniformResource resource, long id) {
		ResourceInfo info = getResourceInfo(resource);
		if ( info == null ) {
			return null;
		}
		MarkerSet markers = info.getMarkers(false);
		if ( markers == null ) {
			return null;
		}
		return (MarkerInfo) markers.get(id);
	}

	/**
	 * add
	 *
	 * @param resource
	 * @param marker
	 * @throws CoreException
	 */
	public void add(IUniformResource resource, MarkerInfo marker) throws CoreException {
		ResourceInfo info = getResourceInfo(resource);
		if ( info == null ) {
			info = createResourceInfo(resource);
		}
		MarkerSet markers = info.getMarkers(true);
		if ( markers == null ) {
			markers = new MarkerSet(1);
		}
		
		basicAdd(resource,markers,marker);
		if ( !markers.isEmpty() ) {
			info.setMarkers(markers);
		}
		
		IMarkerSetElement[] changes = new IMarkerSetElement[1];
		changes[0] = new MarkerDelta(IResourceDelta.ADDED,resource,marker);
		changedMarkers(resource,changes);
	}
	
	/**
	 * isPersistent
	 *
	 * @param info
	 * @return boolean
	 */
	public boolean isPersistent(MarkerInfo info) {
		if ( !cache.isPersistent(info.getType()) ) {
			return false;
		}
		Object isTransient = info.getAttribute(IMarker.TRANSIENT);
		return isTransient == null || !(isTransient instanceof Boolean) || !((Boolean) isTransient).booleanValue();
	}

	/**
	 * removeMarker
	 *
	 * @param resource
	 * @param id
	 * @throws CoreException
	 */
	public void removeMarker(IUniformResource resource, long id) throws CoreException {
		MarkerInfo marker = findMarkerInfo(resource,id);
		if ( marker == null ) {
			return;
		}
		ResourceInfo info = getResourceInfo(resource);
		MarkerSet markers = info.getMarkers(true);
		int size = markers.size();
		markers.remove(marker);
		info.setMarkers(markers.size() == 0 ? null : markers);
		if ( markers.size() != size ) {
			/* TODO: store persistent marker state */
			IMarkerSetElement[] changes = new IMarkerSetElement[] { new MarkerDelta(IResourceDelta.REMOVED,resource,marker) };
			changedMarkers(resource,changes);
		}
	}

	/**
	 * changedMarkers
	 *
	 * @param resource
	 * @param changes
	 * @throws CoreException
	 */
	public void changedMarkers(IUniformResource resource, IMarkerSetElement[] changes) throws CoreException {
		if (changes == null || changes.length == 0) {
			return;
		}
		URI uri = resource.getURI();
		synchronized (lock) {
			if ( currentDeltas == null ) {
				currentDeltas = new HashMap();
			}
			MarkerSet previousChanges = (MarkerSet) currentDeltas.get(uri);
			MarkerSet result = MarkerDelta.merge(previousChanges, changes);
			if (result.size() == 0) {
				currentDeltas.remove(uri);
			} else {
				currentDeltas.put(uri, result);
			}
		}
		
		if ( rootMarker != null ) {
			rootMarker.setAttribute("updateId",rootMarker.getAttribute("updateId",0)+1); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/**
	 * isSubtype
	 *
	 * @param type
	 * @param superType
	 * @return boolean
	 */
	public boolean isSubtype(String type, String superType) {
		return cache.isSubtype(type,superType);
	}

	/**
	 * findMarkersInfo
	 *
	 * @param resource
	 * @param type
	 * @param includeSubtypes
	 * @return MarkerInfo[]
	 */
	public MarkerInfo[] findMarkersInfo(IUniformResource resource, String type, boolean includeSubtypes) {
		ArrayList result = new ArrayList();
		ResourceInfo info = getResourceInfo(resource);
		if ( info == null ) {
			return NO_MARKER_INFO;
		}
		
		MarkerSet markers = info.getMarkers(false);
		if ( markers == null ) {
			return NO_MARKER_INFO;
		}
		
		IMarkerSetElement[] elements = markers.elements();
		for( int i = 0; i < elements.length; ++i ) {
			MarkerInfo marker = (MarkerInfo) elements[i];
			if ( type == null ) {
				result.add(marker);
			} else {
				if (includeSubtypes) {
					if (isSubtype(marker.getType(), type)) {
						result.add(marker);
					}
				} else {
					if (marker.getType().equals(type)) {
						result.add(marker);
					}
				}				
			}			
		}
		if (result.size() == 0) {
			return NO_MARKER_INFO;
		}
		return (MarkerInfo[]) result.toArray(new MarkerInfo[result.size()]);
	}
	
	private ResourceInfo getResourceInfo(IUniformResource resource) {
		return (ResourceInfo) resources.get(resource.getURI());
	}

	private ResourceInfo createResourceInfo(IUniformResource resource) {
		ResourceInfo info = new ResourceInfo();
		resources.put(resource.getURI(),info);
		return info;
	}

	private void basicAdd( IUniformResource resource, MarkerSet markers, MarkerInfo newMarker) throws CoreException {
		if (newMarker.getId() != MarkerInfo.UNDEFINED_ID) {
			throw new CoreException( new Status(IStatus.ERROR, AptanaCorePlugin.ID, IStatus.OK, Messages.MarkerManager_MarkerIDIsDefined, null));
		}
		newMarker.setId(nextMarkerId());
		markers.add(newMarker);
		/* TODO: store persistent marker state*/
	}

	private long nextMarkerId() {
		return nextMarkerId++;
	}

	/**
	 * addResourceChangeListener
	 *
	 * @param listener
	 */
	public void addResourceChangeListener( IUniformResourceChangeListener listener ) {
		listeners.add(listener);
	}

	/**
	 * removeResourceChangeListener
	 *
	 * @param listener
	 */
	public void removeResourceChangeListener( IUniformResourceChangeListener listener ) {
		listeners.remove(listener);
	}
	
	/**
	 * Notifies manager that external resource is changed.
	 * @param resource - resource that is changed.
	 */
	public void externalResourceChanged(IUniformResource resource)
	{
	    handleResourceChanged();
	}

	private void handleResourceChanged() {
		if ( currentDeltas == null ) {
			return;
		}
		MarkerSet[] markers;
		synchronized (lock) {
			markers = (MarkerSet[]) currentDeltas.values().toArray(new MarkerSet[currentDeltas.size()]);
			currentDeltas = null;
		}
		Object[] list = listeners.getListeners();
		for( int j = 0; j < markers.length; ++j ) {
			IMarkerDelta[] deltas = new IMarkerDelta[markers[j].size()];
			markers[j].copyInto(deltas);
			IUniformResource resource = null;
			if ( deltas.length > 0 && deltas[0] instanceof MarkerDelta ) {
				resource = ((MarkerDelta)deltas[0]).getUniformResource();
			}
			UniformResourceChangeEvent event = new UniformResourceChangeEvent(this,resource,deltas);
			for( int i = 0; i < list.length; ++i ) {
				try {
					((IUniformResourceChangeListener)list[i]).resourceChanged(event);
				} catch( Exception e ) {
					IdeLog.logError(AptanaCorePlugin.getDefault(),StringUtils.EMPTY,e);
				}
			}	
		}
	}

}
