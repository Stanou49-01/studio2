/**
 * This file Copyright (c) 2005-2009 Aptana, Inc. This program is
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
package com.aptana.ide.syncing.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.aptana.ide.core.io.CoreIOPlugin;
import com.aptana.ide.core.io.IConnectionPoint;
import com.aptana.ide.core.io.events.ConnectionPointEvent;
import com.aptana.ide.core.io.events.IConnectionPointListener;
import com.aptana.ide.syncing.core.ISiteConnection;
import com.aptana.ide.syncing.core.SiteConnection;
import com.aptana.ide.syncing.core.SyncingPlugin;
import com.aptana.ide.syncing.core.events.ISiteConnectionListener;
import com.aptana.ide.syncing.core.events.SiteConnectionEvent;
import com.aptana.ide.syncing.ui.editors.EditorUtils;
import com.aptana.ide.syncing.ui.navigator.ProjectSitesManager;
import com.aptana.ide.syncing.ui.navigator.SiteConnections;
import com.aptana.ide.ui.io.IOUIPlugin;

/**
 * The activator class controls the plug-in life cycle
 */
public class SyncingUIPlugin extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.aptana.ide.syncing.ui"; //$NON-NLS-1$

    // The shared instance
    private static SyncingUIPlugin plugin;

    private static Map<String, Image> images = new HashMap<String, Image>();

    private ISiteConnectionListener connectionListener = new ISiteConnectionListener() {

        public void siteConnectionChanged(SiteConnectionEvent event) {
            ISiteConnection siteConnection = event.getSiteConnection();
            switch (event.getKind()) {
            case SiteConnectionEvent.POST_ADD:
                // opens the corresponding connection editor
                // EditorUtils.openConnectionEditor(siteConnection);
                break;
            case SiteConnectionEvent.POST_DELETE:
                // closes the corresponding connection editor
                EditorUtils.closeConnectionEditor(siteConnection);
                break;
            }

            IOUIPlugin.refreshNavigatorView(SiteConnections.getInstance());
            IConnectionPoint source = siteConnection.getSource();
            if (source != null) {
                IContainer container = (IContainer) source.getAdapter(IContainer.class);
                if (container != null) {
                    IOUIPlugin.refreshNavigatorView(ProjectSitesManager.getInstance()
                            .getProjectSites(container.getProject()));
                }
            }
        }
    };

    private IConnectionPointListener connectionPointListener = new IConnectionPointListener() {

        public void connectionPointChanged(ConnectionPointEvent event) {
            IConnectionPoint connectionPoint = event.getConnectionPoint();
            switch (event.getKind()) {
            case ConnectionPointEvent.POST_DELETE:
                // check if any site connection has the deleted connection point as either source or destination
                boolean affected = false;
                ISiteConnection[] siteConnections = SyncingPlugin.getSiteConnectionManager().getSiteConnections();
                for (ISiteConnection siteConnection : siteConnections) {
                    if (siteConnection.getSource() == connectionPoint) {
                        ((SiteConnection) siteConnection).setSource(null);
                        affected = true;
                    }
                    if (siteConnection.getDestination() == connectionPoint) {
                        ((SiteConnection) siteConnection).setDestination(null);
                        affected = true;
                    }
                }
                if (affected) {
                    IOUIPlugin.refreshNavigatorView(SiteConnections.getInstance());
                }
                break;
            }
        }
    };

    /**
     * The constructor
     */
    public SyncingUIPlugin() {
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        SyncingPlugin.getSiteConnectionManager().addListener(connectionListener);
        CoreIOPlugin.getConnectionPointManager().addConnectionPointListener(connectionPointListener);
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        SyncingPlugin.getSiteConnectionManager().removeListener(connectionListener);
        CoreIOPlugin.getConnectionPointManager().removeConnectionPointListener(connectionPointListener);
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static SyncingUIPlugin getDefault() {
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
     * Returns an image for the image file at the given plug-in relative path.
     * 
     * @param path
     *            the path
     * @return the image object
     */
    public static Image getImage(String path) {
        if (images.get(path) == null) {
            ImageDescriptor id = getImageDescriptor(path);
            if (id == null) {
                return null;
            }

            Image i = id.createImage();
            images.put(path, i);
            return i;
        }
        return images.get(path);
    }

    public static void log(String msg, Throwable e) {
        log(new Status(IStatus.INFO, PLUGIN_ID, IStatus.OK, msg, e));
    }

    private static void log(IStatus status) {
        getDefault().getLog().log(status);
    }
}
