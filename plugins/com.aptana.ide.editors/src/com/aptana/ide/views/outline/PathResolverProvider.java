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
package com.aptana.ide.views.outline;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import com.aptana.ide.core.StreamUtils;
import com.aptana.ide.core.ui.CoreUIUtils;

/**
 * @author Pavel Petrochenko
 */
public final class PathResolverProvider
{
	/**
	 * 
	 * @author Pavel Petrochenko
	 * resolver which is not able to resolve anything 
	 */
	private static final class NullResolver implements IPathResolver
	{
		/**
		 * @see com.aptana.ide.views.outline.IPathResolver#addChangeListener(java.lang.String, org.eclipse.jface.util.IPropertyChangeListener)
		 */
		public void addChangeListener(String path, IPropertyChangeListener listener)
		{
		}

		/**
		 * @see com.aptana.ide.views.outline.IPathResolver#removeChangeListener(java.lang.String, org.eclipse.jface.util.IPropertyChangeListener)
		 */
		public void removeChangeListener(String path, IPropertyChangeListener listener)
		{
		}

		/**
		 * @see com.aptana.ide.views.outline.IPathResolver#resolveEditorInput(java.lang.String)
		 */
		public IEditorInput resolveEditorInput(String attribute)
		{
			return null;
		}

		/**
		 * @see com.aptana.ide.views.outline.IPathResolver#resolveSource(java.lang.String)
		 */
		public String resolveSource(String path) throws Exception
		{
			return null;
		}
	}

	/**
	 * resolver doing resolving basing on current directory in the file system
	 * @author Pavel Petrochenko
	 */
	private static final class FilePathResolver implements IPathResolver
	{

		private File file;

		/**
		 * @param path
		 */
		public FilePathResolver(IPath path)
		{
			this.file = path.toFile();
			// TODO Auto-generated constructor stub
		}

		/**
		 * @see com.aptana.ide.views.outline.IPathResolver#addChangeListener(java.lang.String,
		 *      org.eclipse.jface.util.IPropertyChangeListener)
		 */
		public void addChangeListener(String path, IPropertyChangeListener listener)
		{
			// do nothing here
		}

		/**
		 * @see com.aptana.ide.views.outline.IPathResolver#removeChangeListener(java.lang.String,
		 *      org.eclipse.jface.util.IPropertyChangeListener)
		 */
		public void removeChangeListener(String path, IPropertyChangeListener listener)
		{
			// do nothing here
		}

		private File resolveToFile(String path)
		{
			if (path.charAt(0) == '/')
			{
				// absolute path (project relative);
				return null;
			}
			Path p = new Path(path);
			File append = file.getParentFile();
			for (int a = 0; a < p.segmentCount(); a++)
			{
				String segment = p.segment(a).trim();
				if (segment.equals(".")) { //$NON-NLS-1$
					continue;
				}
				else if (segment.equals("..")) { //$NON-NLS-1$
					append = file.getParentFile();
				}
				else
				{
					append = new File(append, segment);
				}
			}
			return append;
		}

		/**
		 * @see com.aptana.ide.views.outline.IPathResolver#resolveSource(java.lang.String)
		 */
		public String resolveSource(String path) throws Exception
		{
			File file = resolveToFile(path);
			if (!file.exists())
			{
				return null;
			}
			FileInputStream fs = new FileInputStream(file);
			return StreamUtils.readContent(fs, Charset.defaultCharset().name());
		}

		/**
		 * @param path
		 * @return path
		 * @throws Exception
		 * @see com.aptana.ide.views.outline.IPathResolver#resolvePath(java.lang.String)
		 */
		public IPath resolvePath(String path) throws Exception
		{
			return new Path(resolveToFile(path).getAbsolutePath());
		}

		/**
		 * @see com.aptana.ide.views.outline.IPathResolver#resolveEditorInput(java.lang.String)
		 */
		public IEditorInput resolveEditorInput(String attribute)
		{
			File resolveToFile = resolveToFile(attribute);
			if (resolveToFile != null)
			{
				return CoreUIUtils.createJavaFileEditorInput(resolveToFile);
			}
			return null;
		}

	}

	/**
	 * resolver for resources in the workspace
	 * @author Pavel Petrochenko
	 */
	private static final class IFilePathResolver implements IPathResolver
	{

		private IProject project;

		private IPath path;

		static HashMap listeners = new HashMap();

		//listening to the workspace delta here
		static
		{
			ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener()
			{

				public void resourceChanged(IResourceChangeEvent event)
				{
					IResourceDelta delta = event.getDelta();
					if (delta != null)
					{
						processDelta(delta);
					}
				}

				private void processDelta(IResourceDelta delta)
				{
					IResourceDelta[] affectedChildren = delta.getAffectedChildren();
					for (int a = 0; a < affectedChildren.length; a++)
					{
						IResource resource = affectedChildren[a].getResource();
						if (resource instanceof IFile)
						{
							IPath fullPath = resource.getFullPath();
							fireChange(fullPath);
						}
						processDelta(affectedChildren[a]);
					}
				}

			});
		}

		/**
		 * fires change in the resource on a given path
		 * @param fullPath
		 */
		private static synchronized void fireChange(IPath fullPath)
		{
			List object = (List) listeners.get(fullPath);
			if (object == null)
			{
				return;
			}
			for (int i = 0; i < object.size(); i++)
			{
				IPropertyChangeListener listener = (IPropertyChangeListener) object.get(i);
				listener.propertyChange(new PropertyChangeEvent(IFilePathResolver.class, "content", null, fullPath //$NON-NLS-1$
						.toOSString()));
			}
		}

		/**
		 * @param project
		 * @param path
		 */
		public IFilePathResolver(IProject project, IPath path)
		{
			this.project = project;
			this.path = path;
		}

		/**
		 * @see com.aptana.ide.views.outline.IPathResolver#addChangeListener(java.lang.String,
		 *      org.eclipse.jface.util.IPropertyChangeListener)
		 */
		public synchronized void addChangeListener(String path, IPropertyChangeListener listener)
		{
			IFile resolveToIFile = resolveToIFile(path);
			if (resolveToIFile == null)
			{
				return;
			}
			IPath fullPath = resolveToIFile.getFullPath();
			List object = (List) listeners.get(fullPath);
			if (object == null)
			{
				object = new ArrayList();
				listeners.put(fullPath, object);
			}
			object.add(listener);
		}

		/**
		 * @see com.aptana.ide.views.outline.IPathResolver#removeChangeListener(java.lang.String,
		 *      org.eclipse.jface.util.IPropertyChangeListener)
		 */
		public synchronized void removeChangeListener(String path, IPropertyChangeListener listener)
		{
			IFile resolveToIFile = resolveToIFile(path);
			if (resolveToIFile == null)
			{
				return;
			}
			IPath fullPath = resolveToIFile.getFullPath();
			List object = (List) listeners.get(fullPath);
			if (object == null)
			{
				return;
			}
			object.remove(listener);
			if (object.isEmpty())
			{
				listeners.remove(fullPath);
			}
		}

		/**
		 * @see com.aptana.ide.views.outline.IPathResolver#resolveSource(java.lang.String)
		 */
		public String resolveSource(String path) throws Exception
		{
			IFile file = resolveToIFile(path);
			if (file == null)
			{
				return null;
			}
			if (!file.exists())
			{
				return null;
			}
			InputStream contents = file.getContents(true);
			try
			{
				String charset = file.getCharset(true);
				return StreamUtils.readContent(contents, charset);
			}
			finally
			{
				contents.close();
			}
		}

		private IFile resolveToIFile(String path)
		{
			if (path.charAt(0) == '/')
			{
				// absolute path (project relative);
				return project.getFile(path);
			}
			Path p = new Path(path);
			IPath append = this.path.removeLastSegments(1);
			for (int a = 0; a < p.segmentCount(); a++)
			{
				String segment = p.segment(a).trim();
				if (segment.equals(".")) { //$NON-NLS-1$
					continue;
				}
				else if (segment.equals("..")) { //$NON-NLS-1$
					if (append.segmentCount() == 0)
					{
						return null;
					}
					append = append.removeLastSegments(1);
				}
				else
				{
					append = append.append(segment);
				}
			}
			return project.getFile(append);
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			result = prime * result + ((project == null) ? 0 : project.hashCode());
			return result;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			if (obj == null)
			{
				return false;
			}
			if (getClass() != obj.getClass())
			{
				return false;
			}
			final IFilePathResolver other = (IFilePathResolver) obj;
			if (path == null)
			{
				if (other.path != null)
				{
					return false;
				}
			}
			else if (!path.equals(other.path))
			{
				return false;
			}
			if (project == null)
			{
				if (other.project != null)
				{
					return false;
				}
			}
			else if (!project.equals(other.project))
			{
				return false;
			}
			return true;
		}

		/**
		 * @param path
		 * @return
		 * @throws Exception
		 * @see com.aptana.ide.views.outline.IPathResolver#resolvePath(java.lang.String)
		 */
		public IPath resolvePath(String path) throws Exception
		{
			IFile resolveToIFile = resolveToIFile(path);
			if (resolveToIFile != null)
			{
				return resolveToIFile.getFullPath();
			}
			return null;
		}

		/**
		 * @see com.aptana.ide.views.outline.IPathResolver#resolveEditorInput(java.lang.String)
		 */
		public IEditorInput resolveEditorInput(String attribute)
		{
			IFile resolveToIFile = resolveToIFile(attribute);
			if (resolveToIFile != null)
			{
				return new FileEditorInput(resolveToIFile);
			}
			return null;
		}
	}

	private PathResolverProvider()
	{

	}

	/**
	 * returns path resolver for a given editor input
	 * @param input
	 * @return path resolver for a given editor input
	 */
	public static IPathResolver getResolver(IEditorInput input)
	{
		if (input instanceof FileEditorInput)
		{
			FileEditorInput fi = (FileEditorInput) input;
			return new IFilePathResolver(fi.getFile().getProject(), fi.getFile().getProjectRelativePath());
		}
		if (input instanceof IPathEditorInput)
		{
			IPathEditorInput fInput = (IPathEditorInput) (input);
			return new FilePathResolver(fInput.getPath());
		}
		if (input instanceof IURIEditorInput) {
			URI uri = ((IURIEditorInput) input).getURI();
			if ("file".equals(uri.getScheme())) {
				return new FilePathResolver(Path.fromOSString(new File(uri).getAbsolutePath()));
			}
		}
		IPathEditorInput adapter = (IPathEditorInput) input.getAdapter(IPathEditorInput.class);
		if (adapter != null)
		{
			return new FilePathResolver(adapter.getPath());
		}
		return new NullResolver();
	}

}
