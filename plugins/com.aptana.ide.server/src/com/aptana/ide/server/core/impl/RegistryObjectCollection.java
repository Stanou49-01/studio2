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
package com.aptana.ide.server.core.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 * collection of objects that are created based on contribution elements from extension registry
 * 
 * @author Pavel Petrochenko
 */
public abstract class RegistryObjectCollection
{
	private final String id;

	private Map<String, RegistryLazyObject> extensions;

	/**
	 * @param id
	 */
	public RegistryObjectCollection(String id)
	{
		this.id = id;
	}

	/**
	 * @param id
	 * @return object for a given id
	 */
	public RegistryLazyObject getObject(String id)
	{
		checkLoad();
		return extensions.get(id);
	}

	/**
	 * @return all objects for a given extension
	 */
	public RegistryLazyObject[] getAll()
	{
		checkLoad();
		RegistryLazyObject[] result = new RegistryLazyObject[extensions.size()];
		extensions.values().toArray(result);
		return result;
	}

	private void checkLoad()
	{
		if (extensions == null)
		{
			extensions = new HashMap<String, RegistryLazyObject>();
			IConfigurationElement[] configurationElementsFor = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(id);
			for (int a = 0; a < configurationElementsFor.length; a++)
			{
				IConfigurationElement configurationElement = configurationElementsFor[a];
				RegistryLazyObject registryLazyObject = createObject(configurationElement);
				extensions.put(registryLazyObject.getId(), registryLazyObject);
			}
		}
	}

	/**
	 * creates object for a given configuration element
	 * 
	 * @param configurationElement
	 * @return object of required type
	 */
	protected abstract RegistryLazyObject createObject(IConfigurationElement configurationElement);

}
