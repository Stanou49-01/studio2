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
package com.aptana.ide.server.resources;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import com.aptana.ide.core.IdeLog;
import com.aptana.ide.server.core.ServerCorePlugin;
import com.aptana.ide.server.http.HttpContentTypes;
import com.aptana.ide.server.http.HttpServer;

/**
 * @author Kevin Lindsey
 */
public class WorkspaceHttpResource implements IHttpResource
{
	/*
	 * Fields
	 */
	IFile _resource;

	/*
	 * Constructors
	 */

	/**
	 * WorkspaceHttpResource
	 * 
	 * @param resource
	 */
	public WorkspaceHttpResource(IFile resource)
	{
		this._resource = resource;
	}

	/**
	 * Returns the input stream for the current contents
	 * 
	 * @param server
	 * @return The InputStream representing the resource
	 * @throws IOException
	 *             If the resource cannot be returned
	 */
	public InputStream getContentInputStream(HttpServer server) throws IOException
	{
		try
		{
			return _resource.getContents();
		}
		catch (CoreException e)
		{
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Returns the length of the content
	 * 
	 * @return The length of the content
	 */
	public long getContentLength()
	{
		try
		{
			IFileStore store = EFS.getStore(_resource.getLocationURI());
			IFileInfo info = store.fetchInfo();
			return info.getLength();
		}
		catch (CoreException e)
		{
			IdeLog.logError(ServerCorePlugin.getDefault(), e.getMessage(), e);
		}
		return -1;
	}

	/**
	 * Returns the type of the content
	 * 
	 * @return The type of the content
	 */
	public String getContentType()
	{
		String fileExtension = this._resource.getFileExtension();
		String contentType = HttpContentTypes.getContentType("." + fileExtension); //$NON-NLS-1$

		if (contentType == null)
		{
			contentType = "text/plain"; //$NON-NLS-1$
		}

		return contentType;
	}
}
