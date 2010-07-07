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

package com.aptana.ide.ui.io;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.aptana.ide.core.io.ConnectionPointType;
import com.aptana.ide.core.io.CoreIOPlugin;
import com.aptana.ide.core.io.IConnectionPoint;
import com.aptana.ide.core.io.IConnectionPointManager;
import com.aptana.ide.core.io.events.ConnectionPointEvent;
import com.aptana.ide.core.io.events.IConnectionPointListener;
import com.aptana.ide.core.ui.CoreUIUtils;
import com.aptana.ide.ui.io.navigator.internal.NavigatorDecoratorLoader;

/**
 * The activator class controls the plug-in life cycle
 */
public class IOUIPlugin extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.aptana.ide.ui.io"; //$NON-NLS-1$

    // The shared instance
    private static IOUIPlugin plugin;

    private IResourceChangeListener resourceListener = new IResourceChangeListener() {

        public void resourceChanged(IResourceChangeEvent event) {
            if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
                // only refreshes the direct parent of the files that are added
                // or removed
                IResourceDelta delta = event.getDelta();
                if (delta != null) {
                    List<IResource> list = new ArrayList<IResource>();
                    getResourcesNeededToRefresh(list, delta);
                    for (IResource resource : list) {
                        refreshNavigatorView(resource);
                    }
                }
            }
        }

        private void getResourcesNeededToRefresh(List<IResource> list, IResourceDelta delta) {
            IResourceDelta[] children = delta.getAffectedChildren();
            int kind;
            for (IResourceDelta child : children) {
                kind = child.getKind();
                if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
                    list.add(child.getResource().getParent());
                } else if (kind == IResourceDelta.CHANGED) {
                    getResourcesNeededToRefresh(list, child);
                }
            }
        }
    };

    private IConnectionPointListener connectionListener = new IConnectionPointListener() {

        public void connectionPointChanged(ConnectionPointEvent event) {
			IConnectionPoint connection = event.getConnectionPoint();
			IConnectionPointManager manager = CoreIOPlugin.getConnectionPointManager();
			ConnectionPointType type = manager.getType(connection);
			if (type == null) {
			    return;
			}
			
			switch (event.getKind()) {
			case ConnectionPointEvent.POST_ADD:
			    refreshNavigatorViewAndSelect(
			    		manager.getConnectionPointCategory(type.getCategory().getId()), connection);
			    break;
			case ConnectionPointEvent.POST_DELETE:
			    refreshNavigatorView(manager.getConnectionPointCategory(type.getCategory().getId()));
			    break;
			case ConnectionPointEvent.POST_CHANGE:
			    refreshNavigatorView(connection);
			}
        }

    };

    /**
     * The constructor
     */
    public IOUIPlugin() {
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener);
        CoreIOPlugin.getConnectionPointManager().addConnectionPointListener(connectionListener);
        NavigatorDecoratorLoader.init();
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
        CoreIOPlugin.getConnectionPointManager().removeConnectionPointListener(connectionListener);
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static IOUIPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path.
     * 
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
     * Returns the active workbench window
     * 
     * @return the active workbench window
     */
    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        return getDefault().getWorkbench().getActiveWorkbenchWindow();
    }

    /**
     * Returns the active workbench shell or <code>null</code> if none
     * 
     * @return the active workbench shell or <code>null</code> if none
     */
    public static Shell getActiveWorkbenchShell() {
        IWorkbenchWindow window = getActiveWorkbenchWindow();
        if (window != null) {
            return window.getShell();
        }
        return null;
    }

    /**
     * getActivePage
     * 
     * @return IWorkbenchPage
     */
    public static IWorkbenchPage getActivePage() {
        IWorkbenchWindow w = getActiveWorkbenchWindow();
        if (w != null) {
            return w.getActivePage();
        }
        return null;
    }

    public static void refreshNavigatorView(Object element) {
        refreshNavigatorViewAndSelect(element, null);
    }

    public static void refreshNavigatorViewAndSelect(final Object element, final Object selection) {
        CoreUIUtils.getDisplay().asyncExec(new Runnable() {

            public void run() {
                try {
                    IViewPart view = CoreUIUtils.findView("com.aptana.ide.ui.io.fileExplorerView"); //$NON-NLS-1$
                    if (view != null && view instanceof CommonNavigator) {
                        CommonViewer viewer = ((CommonNavigator) view).getCommonViewer();
                        if (element == null) {
                            // full refresh
                            viewer.refresh();
                        } else {
                            viewer.refresh(element);
                        }

                        if (selection != null) {
                            // ensures the category's new content are loaded
                            viewer.expandToLevel(element, 1);
                            viewer.setSelection(new StructuredSelection(selection));
                        }
                    }
                } catch (PartInitException e) {
                }
            }
        });
    }

    public static void log(String msg, Throwable e) {
        log(new Status(IStatus.INFO, PLUGIN_ID, IStatus.OK, msg, e));
    }

    private static void log(IStatus status) {
        getDefault().getLog().log(status);
    }

}
