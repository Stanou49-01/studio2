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
package com.aptana.ide.core;

import java.io.InputStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Assert;

/**
 * A storage for java.io.File. Previously org.eclipse.ui.internal.editors.text.JavaFileStorage, removed in 3.3
 * 
 * @since 3.2
 */
class BaseFileStorage implements IStorage
{

	private IFileStore fFileStore;
	private IPath fFullPath;

	/**
	 * Creates a new file storage
	 * 
	 * @param fileStore
	 */
	public BaseFileStorage(IFileStore fileStore)
	{
		Assert.isNotNull(fileStore);
		Assert.isTrue(EFS.SCHEME_FILE.equals(fileStore.getFileSystem().getScheme()));
		fFileStore = fileStore;
	}

	/**
	 * @see org.eclipse.core.resources.IStorage#getContents()
	 */
	public InputStream getContents() throws CoreException
	{
		return fFileStore.openInputStream(EFS.NONE, null);
	}

	/**
	 * @see org.eclipse.core.resources.IStorage#getFullPath()
	 */
	public IPath getFullPath()
	{
		if (fFullPath == null)
			fFullPath = new Path(fFileStore.toURI().getPath());
		return fFullPath;
	}

	/**
	 * @see org.eclipse.core.resources.IStorage#getName()
	 */
	public String getName()
	{
		return fFileStore.getName();
	}

	/**
	 * @see org.eclipse.core.resources.IStorage#isReadOnly()
	 */
	public boolean isReadOnly()
	{
		return fFileStore.fetchInfo().getAttribute(EFS.ATTRIBUTE_READ_ONLY);
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter)
	{
		return null;
	}
}
