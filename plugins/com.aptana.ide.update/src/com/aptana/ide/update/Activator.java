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
package com.aptana.ide.update;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.aptana.ide.internal.update.manager.PluginManagerLoader;
import com.aptana.ide.update.manager.IPluginManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin
{

	/**
	 * The plug-in ID
	 */
	public static final String PLUGIN_ID = "com.aptana.ide.update"; //$NON-NLS-1$

	/**
	 * FEATURE_EXTENSION_POINT
	 */
	public static final String FEATURE_EXTENSION_POINT = PLUGIN_ID + ".feature"; //$NON-NLS-1$

	/**
	 * MANAGER_EXTENSION_POINT
	 */
	public static final String MANAGER_EXTENSION_POINT = PLUGIN_ID + ".manager"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private static BundleContext context;
	private IPluginManager pluginManager;

	/**
	 * The constructor
	 */
	public Activator()
	{
		plugin = this;
	}

	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		Activator.context = context;
	}

	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		Activator.context = null;
		pluginManager = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault()
	{
		return plugin;
	}

	public IPluginManager getPluginManager()
	{
		if (pluginManager == null) {
			pluginManager = PluginManagerLoader.getPluginManager();
		}
		return pluginManager;
	}

	public static BundleContext getContext()
	{
		return context;
	}

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path.
     * 
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path)
    {
        return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public static void log(int status, String message, Throwable exception)
    {
        plugin.getLog().log(new Status(status, plugin.getBundle().getSymbolicName(), IStatus.OK, message, exception));
    }

    public void checkForUpdates()
    {
        getPluginManager().checkForUpdates(true);
    }
}
