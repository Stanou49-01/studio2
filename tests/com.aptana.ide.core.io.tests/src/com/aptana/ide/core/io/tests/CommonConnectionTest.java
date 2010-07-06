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

package com.aptana.ide.core.io.tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileTree;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;

import com.aptana.ide.core.io.IBaseRemoteConnectionPoint;
import com.aptana.ide.core.io.vfs.ExtendedFileInfo;
import com.aptana.ide.core.io.vfs.IExtendedFileInfo;
import com.aptana.ide.core.io.vfs.IExtendedFileStore;

/**
 * @author Max Stepanov
 */
public abstract class CommonConnectionTest extends BaseConnectionTest
{
	public final void testURI() throws CoreException
	{
		assertEquals(cp.getRootURI(), cp.getRoot().toURI());
		IFileStore fs = cp.getRoot().getFileStore(new Path("/some/path/some.file")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileStore fs2 = EFS.getStore(cp.getRootURI().resolve("/some/path/some.file")); //$NON-NLS-1$
		assertEquals(fs, fs2);
	}

	public final void testConnectDisconnect() throws CoreException
	{
		cp.connect(null);
		assertTrue(cp.isConnected());
		assertTrue(cp.canDisconnect());
		cp.disconnect(null);
		assertFalse(cp.isConnected());
		assertFalse(cp.canDisconnect());
	}

	public final void testIncorrectPaths() throws CoreException
	{
		if (!(cp instanceof IBaseRemoteConnectionPoint))
		{
			return;
		}

		IBaseRemoteConnectionPoint ftpcp = (IBaseRemoteConnectionPoint) cp;
		IPath basePath = ftpcp.getPath();

		ftpcp.setPath(null);
		ftpcp.connect(null);

		ftpcp.setPath(basePath);
	}

	public final void testFetchRootInfo() throws CoreException
	{
		IFileStore fs = cp.getRoot();
		assertNotNull(fs);
		assertFalse(cp.isConnected());
		IFileInfo fi = fs.fetchInfo();
		assertTrue(cp.isConnected());
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertTrue(fi.isDirectory());
		assertEquals(Path.ROOT.toPortableString(), fi.getName());
	}

	public final void testFetchInfoWillConnectIfDisconnected() throws CoreException
	{
		IFileStore fs = cp.getRoot();
		assertNotNull(fs);
		if (cp.isConnected())
		{
			cp.disconnect(null);
		}
		assertFalse(cp.isConnected());
		IFileInfo fi = fs.fetchInfo();
		assertTrue(cp.isConnected());
		assertNotNull(fi);
	}

	public final void testNonexisting() throws CoreException
	{
		IFileStore fs = cp.getRoot().getFileStore(new Path("/some/path/nonexisting.file")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
		assertFalse(fi.isDirectory());
		assertEquals(0, fi.getLength());
		assertEquals(0, fi.getLastModified());
		assertEquals("nonexisting.file", fi.getName()); //$NON-NLS-1$
		try
		{
			assertEquals(null, fs.openInputStream(EFS.NONE, null));
			assertFalse("<unreachable>", true); //$NON-NLS-1$
		}
		catch (CoreException e)
		{
			assertEquals(FileNotFoundException.class, e.getCause().getClass());
			assertEquals("/some/path/nonexisting.file", ((FileNotFoundException) e.getCause()).getMessage()); //$NON-NLS-1$
		}
		fs.delete(EFS.NONE, null);
	}

	public final void testParent() throws CoreException
	{
		IFileStore fs = cp.getRoot().getFileStore(new Path("/some/path/some.file")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertEquals("some.file", fi.getName()); //$NON-NLS-1$

		IFileStore pfs = fs.getParent();
		assertNotNull(pfs);
		fi = pfs.fetchInfo();
		assertNotNull(fi);
		assertEquals("path", fi.getName()); //$NON-NLS-1$
		assertTrue(pfs.isParentOf(fs));

		IFileStore ppfs = pfs.getParent();
		assertNotNull(ppfs);
		fi = ppfs.fetchInfo();
		assertNotNull(fi);
		assertEquals("some", fi.getName()); //$NON-NLS-1$
		assertTrue(ppfs.isParentOf(pfs));
		assertTrue(ppfs.isParentOf(fs));

		assertEquals(cp.getRoot(), ppfs.getParent());
		assertTrue(cp.getRoot().isParentOf(ppfs));
		assertTrue(cp.getRoot().isParentOf(pfs));
		assertTrue(cp.getRoot().isParentOf(fs));

		assertEquals(null, cp.getRoot().getParent());
	}

	public final void testCreateEmptyFile() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/emptyfile.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
		assertEquals("emptyfile.txt", fi.getName()); //$NON-NLS-1$
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.close();
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(0, fi.getLength());
	}

	public final void testCreateEmptyDotFile() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/.emptyfile.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
		assertEquals(".emptyfile.txt", fi.getName()); //$NON-NLS-1$
		try
		{
			OutputStream out = fs.openOutputStream(EFS.NONE, null);
			out.close();
		}
		catch (CoreException e)
		{
			assertEquals(FileNotFoundException.class, e.getCause().getClass());
			assertEquals(
					testPath.append(".emptyfile.txt").toPortableString(), ((FileNotFoundException) e.getCause()).getMessage()); //$NON-NLS-1$
			fi = fs.fetchInfo();
			assertNotNull(fi);
			assertFalse(fi.exists());
			return;
		}
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(0, fi.getLength());
	}

	public final void testCreateEmptyFileRecursive() throws CoreException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/nonexisting/emptyfile.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
		assertEquals("emptyfile.txt", fi.getName()); //$NON-NLS-1$
		try
		{
			OutputStream out = fs.openOutputStream(EFS.NONE, null);
			out.close();
			assertFalse("<unreachable>", true); //$NON-NLS-1$
		}
		catch (Exception e)
		{
			assertEquals(FileNotFoundException.class, e.getCause().getClass());
			assertEquals(
					testPath.append("nonexisting/emptyfile.txt").toPortableString(), ((FileNotFoundException) e.getCause()).getMessage()); //$NON-NLS-1$
		}
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
	}

	public final void testCreateFolder() throws CoreException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/newfolder")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
		assertEquals("newfolder", fi.getName()); //$NON-NLS-1$
		fs.mkdir(EFS.SHALLOW, null);
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertTrue(fi.isDirectory());
		fs.mkdir(EFS.SHALLOW, null); // retry to show no errors
	}

	public final void testCreateFolderRecursive() throws CoreException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/leve1/level2/level3")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
		IFileStore fs2 = fs.getChild("newfolder"); //$NON-NLS-1$
		try
		{
			fs2.mkdir(EFS.SHALLOW, null);
			assertFalse("<unreachable>", true); //$NON-NLS-1$
		}
		catch (Exception e)
		{
			assertEquals(FileNotFoundException.class, e.getCause().getClass());
			assertEquals(
					testPath.append("leve1/level2/level3/newfolder").toPortableString(), ((FileNotFoundException) e.getCause()).getMessage()); //$NON-NLS-1$
		}
		fi = fs2.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());

		fs2.mkdir(EFS.NONE, null);
		fi = fs2.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
	}

	public final void testWriteReadBinFile() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/rwfile.bin")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.write(BYTES);
		out.close();
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(BYTES.length, fi.getLength());
		InputStream in = fs.openInputStream(EFS.NONE, null);
		ByteArrayOutputStream bout = new ByteArrayOutputStream(BYTES.length);
		byte[] buffer = new byte[256];
		int count;
		while ((count = in.read(buffer)) > 0)
		{
			bout.write(buffer, 0, count);
		}
		in.close();
		bout.close();
		assertTrue(Arrays.equals(BYTES, bout.toByteArray()));
	}

	public final void testWriteReadTextFile() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/rwfile.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
		Writer w = new OutputStreamWriter(fs.openOutputStream(EFS.NONE, null));
		w.write(TEXT);
		w.close();
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(TEXT.length(), fi.getLength());
		Reader r = new InputStreamReader(fs.openInputStream(EFS.NONE, null));
		StringWriter sw = new StringWriter(TEXT.length());
		char[] buffer = new char[256];
		int count;
		while ((count = r.read(buffer)) > 0)
		{
			sw.write(buffer, 0, count);
		}
		r.close();
		sw.close();
		assertTrue(Arrays.equals(TEXT.toCharArray(), sw.toString().toCharArray()));
	}

	public final void testWriteReadExistingFile() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/rwfile.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.write(new byte[] { 'a', 'b', 'c', 'd' });
		out.close();
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());

		Writer w = new OutputStreamWriter(fs.openOutputStream(EFS.NONE, null));
		w.write(TEXT);
		w.close();
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(TEXT.length(), fi.getLength());
		Reader r = new InputStreamReader(fs.openInputStream(EFS.NONE, null));
		StringWriter sw = new StringWriter(TEXT.length());
		char[] buffer = new char[256];
		int count;
		while ((count = r.read(buffer)) > 0)
		{
			sw.write(buffer, 0, count);
		}
		r.close();
		sw.close();
		assertTrue(Arrays.equals(TEXT.toCharArray(), sw.toString().toCharArray()));
	}

	public final void testWriteReadTextFileSimultanesously() throws CoreException, IOException
	{
		IFileStore[] fslist = new IFileStore[4];
		for (int i = 0; i < fslist.length; ++i)
		{
			IFileStore fs = fslist[i] = cp.getRoot().getFileStore(
					testPath.append(MessageFormat.format("/rwfile{0}.txt", i))); //$NON-NLS-1$
			assertNotNull(fs);
			IFileInfo fi = fs.fetchInfo();
			assertNotNull(fi);
			assertFalse(fi.exists());
		}
		Writer[] writers = new Writer[fslist.length];
		for (int i = 0; i < fslist.length; ++i)
		{
			writers[i] = new OutputStreamWriter(fslist[i].openOutputStream(EFS.NONE, null));
		}
		for (int i = 0; i < writers.length; ++i)
		{
			writers[i].write(TEXT);
		}
		for (int i = 0; i < writers.length; ++i)
		{
			writers[i].close();
		}
		for (int i = 0; i < fslist.length; ++i)
		{
			IFileInfo fi = fslist[i].fetchInfo();
			assertNotNull(fi);
			assertTrue(fi.exists());
			assertEquals(TEXT.length(), fi.getLength());
		}
		Reader[] readers = new Reader[fslist.length];
		for (int i = 0; i < fslist.length; ++i)
		{
			readers[i] = new InputStreamReader(fslist[i].openInputStream(EFS.NONE, null));
		}
		for (int i = 0; i < readers.length; ++i)
		{
			StringWriter sw = new StringWriter(TEXT.length());
			char[] buffer = new char[256];
			int count;
			while ((count = readers[i].read(buffer)) > 0)
			{
				sw.write(buffer, 0, count);
			}
			sw.close();
			assertTrue(Arrays.equals(TEXT.toCharArray(), sw.toString().toCharArray()));
		}
		for (int i = 0; i < readers.length; ++i)
		{
			readers[i].close();
		}
	}

	public final void testDeleteFile() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/deleteme.ext")); //$NON-NLS-1$
		assertNotNull(fs);
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.close();
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		fs.delete(EFS.NONE, null);
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
	}

	public final void testDeleteFolder() throws CoreException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/deleteme")); //$NON-NLS-1$
		assertNotNull(fs);
		fs.mkdir(EFS.SHALLOW, null);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		fs.delete(EFS.NONE, null);
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
	}

	public final void testDeleteFolderRecursive() throws CoreException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/delete.me/level1/level2/level3")); //$NON-NLS-1$
		assertNotNull(fs);
		fs.mkdir(EFS.NONE, null);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		fs = cp.getRoot().getFileStore(testPath.append("/delete.me")); //$NON-NLS-1$
		fs.delete(EFS.NONE, null);
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
	}

	public final void testListFiles() throws CoreException, IOException
	{
		String[] NAMES = new String[] { "file1.txt", "file2.txt", "file3.txt" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		IFileStore fs = cp.getRoot().getFileStore(testPath);
		assertNotNull(fs);
		for (int i = 0; i < NAMES.length; ++i)
		{
			OutputStream out = fs.getChild(NAMES[i]).openOutputStream(EFS.NONE, null);
			out.close();
		}

		String[] names = fs.childNames(EFS.NONE, null);
		Arrays.sort(names);
		assertEquals(NAMES.length, names.length);
		for (int i = 0; i < names.length; ++i)
		{
			assertEquals(NAMES[i], names[i]);
		}

		IFileStore[] fslist = fs.childStores(EFS.NONE, null);
		Arrays.sort(fslist, new Comparator<IFileStore>()
		{
			public int compare(IFileStore o1, IFileStore o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		});
		assertEquals(NAMES.length, fslist.length);
		for (int i = 0; i < fslist.length; ++i)
		{
			assertEquals(NAMES[i], fslist[i].getName());
		}

		IFileInfo[] filist = fs.childInfos(EFS.NONE, null);
		Arrays.sort(filist, new Comparator<IFileInfo>()
		{
			public int compare(IFileInfo o1, IFileInfo o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		});
		assertEquals(NAMES.length, filist.length);
		for (int i = 0; i < filist.length; ++i)
		{
			assertEquals(NAMES[i], filist[i].getName());
			assertFalse(filist[i].isDirectory());
		}
	}

	public final void testListFolders() throws CoreException
	{
		String[] NAMES = new String[] { "folder1", "folder2", "folder3" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		IFileStore fs = cp.getRoot().getFileStore(testPath);
		assertNotNull(fs);
		for (int i = 0; i < NAMES.length; ++i)
		{
			fs.getChild(NAMES[i]).mkdir(EFS.SHALLOW, null);
		}

		String[] names = fs.childNames(EFS.NONE, null);
		Arrays.sort(names);
		assertEquals(NAMES.length, names.length);
		for (int i = 0; i < names.length; ++i)
		{
			assertEquals(NAMES[i], names[i]);
		}

		IFileStore[] fslist = fs.childStores(EFS.NONE, null);
		Arrays.sort(fslist, new Comparator<IFileStore>()
		{
			public int compare(IFileStore o1, IFileStore o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		});
		assertEquals(NAMES.length, fslist.length);
		for (int i = 0; i < fslist.length; ++i)
		{
			assertEquals(NAMES[i], fslist[i].getName());
		}

		IFileInfo[] filist = fs.childInfos(EFS.NONE, null);
		Arrays.sort(filist, new Comparator<IFileInfo>()
		{
			public int compare(IFileInfo o1, IFileInfo o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		});
		assertEquals(NAMES.length, filist.length);
		for (int i = 0; i < filist.length; ++i)
		{
			assertEquals(NAMES[i], filist[i].getName());
			assertTrue(filist[i].isDirectory());
		}
	}

	public final void testPutInfoFileBase() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/file.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.close();
		IFileInfo fi = fs.fetchInfo(IExtendedFileStore.DETAILED, null);
		assertNotNull(fi);
		assertTrue(fi.exists());

		long lastModified = fi.getLastModified();
		if (supportsSetModificationTime())
		{
			lastModified -= new Random().nextInt(7 * 24 * 60) * 1000;
			lastModified -= lastModified % 1000; // remove milliseconds
		}
		IFileInfo pfi = new FileInfo();
		pfi.setLastModified(lastModified);
		fs.putInfo(pfi, EFS.SET_LAST_MODIFIED, null);

		fi = fs.fetchInfo(IExtendedFileStore.DETAILED, null);
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(lastModified, fi.getLastModified());
	}

	public final void testPutInfoFolderBase() throws CoreException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/newfolder")); //$NON-NLS-1$
		assertNotNull(fs);
		fs.mkdir(EFS.SHALLOW, null);
		IFileInfo fi = fs.fetchInfo(IExtendedFileStore.DETAILED, null);
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertTrue(fi.isDirectory());

		long lastModified = fi.getLastModified();
		if (supportsSetModificationTime())
		{
			lastModified -= new Random().nextInt(7 * 24) * 60000;
			lastModified -= lastModified % 60000; // remove seconds/milliseconds
		}
		IFileInfo pfi = new FileInfo();
		pfi.setLastModified(lastModified);
		fs.putInfo(pfi, EFS.SET_LAST_MODIFIED, null);

		fi = fs.fetchInfo(IExtendedFileStore.DETAILED, null);
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertTrue(fi.isDirectory());
		assertEquals(lastModified, fi.getLastModified());
	}


	public final void testPutInfoPermissions() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/file.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.close();
		IExtendedFileInfo fi = (IExtendedFileInfo) fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());

		long permissions = fi.getPermissions();
		if (supportsChangePermissions())
		{
			permissions &= ~IExtendedFileInfo.PERMISSION_OTHERS_READ;
			permissions &= ~IExtendedFileInfo.PERMISSION_OWNER_WRITE;
			assertFalse(permissions == fi.getPermissions());
		}

		IExtendedFileInfo pfi = new ExtendedFileInfo();
		pfi.setPermissions(permissions);
		fs.putInfo(pfi, IExtendedFileInfo.SET_PERMISSIONS, null);

		fi = (IExtendedFileInfo) fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(permissions, fi.getPermissions());
	}


	public final void testPutInfoGroup() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/file.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.close();
		IExtendedFileInfo fi = (IExtendedFileInfo) fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());

		String owner = fi.getOwner();
		String group = fi.getGroup();
		if (supportsChangeGroup())
		{
			group = "staff"; //$NON-NLS-1$
			assertFalse(group.equals(fi.getGroup()));
		}

		IExtendedFileInfo pfi = new ExtendedFileInfo();
		pfi.setGroup(group);
		fs.putInfo(pfi, IExtendedFileInfo.SET_GROUP, null);

		fi = (IExtendedFileInfo) fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(owner, fi.getOwner());
		assertEquals(group, fi.getGroup());
	}

	public final void testMoveFileSameFolder() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/file.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.write(BYTES);
		out.close();
		assertTrue(fs.fetchInfo().exists());
		IFileStore fs2 = fs.getParent().getChild("file2.txt"); //$NON-NLS-1$
		assertNotNull(fs2);
		assertFalse(fs2.fetchInfo().exists());
		fs.move(fs2, EFS.NONE, null);

		assertFalse(fs.fetchInfo().exists());

		IFileInfo fi = fs2.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(BYTES.length, fi.getLength());
	}

	public final void testMoveFileAnotherFolder() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/file.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.write(BYTES);
		out.close();
		assertTrue(fs.fetchInfo().exists());
		IFileStore fs2 = fs.getParent().getFileStore(new Path("folder/file2.txt")); //$NON-NLS-1$
		assertNotNull(fs2);
		assertFalse(fs2.fetchInfo().exists());
		try
		{
			fs.move(fs2, EFS.NONE, null);
			assertFalse("<unreachable>", true); //$NON-NLS-1$
		}
		catch (CoreException e)
		{
			assertEquals(FileNotFoundException.class, e.getCause().getClass());
			assertEquals(
					testPath.append("folder/file2.txt").toPortableString(), ((FileNotFoundException) e.getCause()).getMessage()); //$NON-NLS-1$
		}

		fs2.getParent().mkdir(EFS.SHALLOW, null);
		assertTrue(fs2.getParent().fetchInfo().exists());
		fs.move(fs2, EFS.NONE, null);
		assertFalse(fs.fetchInfo().exists());

		IFileInfo fi = fs2.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(BYTES.length, fi.getLength());
	}

	public final void testMoveFileToExisting() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/file.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.write(BYTES);
		out.close();
		assertTrue(fs.fetchInfo().exists());
		IFileStore fs2 = fs.getParent().getChild("file2.txt"); //$NON-NLS-1$
		assertNotNull(fs2);
		assertFalse(fs2.fetchInfo().exists());
		fs2.openOutputStream(EFS.NONE, null).close();
		assertTrue(fs2.fetchInfo().exists());
		try
		{
			fs.move(fs2, EFS.NONE, null);
			assertFalse("<unreachable>", true); //$NON-NLS-1$
		}
		catch (CoreException e)
		{
			assertEquals(FileNotFoundException.class, e.getCause().getClass());
			assertEquals(
					testPath.append("file2.txt").toPortableString(), ((FileNotFoundException) e.getCause()).getMessage()); //$NON-NLS-1$
		}

		fs.move(fs2, EFS.OVERWRITE, null);
		assertFalse(fs.fetchInfo().exists());

		IFileInfo fi = fs2.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(BYTES.length, fi.getLength());
	}

	public final void testMoveFolderSameFolder() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/fromfolder")); //$NON-NLS-1$
		assertNotNull(fs);
		fs.mkdir(EFS.SHALLOW, null);
		assertTrue(fs.fetchInfo().exists());
		OutputStream out = fs.getChild("file.txt").openOutputStream(EFS.NONE, null); //$NON-NLS-1$
		out.write(BYTES);
		out.close();
		assertTrue(fs.getChild("file.txt").fetchInfo().exists()); //$NON-NLS-1$
		IFileStore fs2 = fs.getParent().getFileStore(new Path("tofolder")); //$NON-NLS-1$
		assertNotNull(fs2);
		assertFalse(fs2.fetchInfo().exists());
		fs.move(fs2, EFS.NONE, null);
		assertFalse(fs.fetchInfo().exists());

		IFileInfo fi = fs2.getChild("file.txt").fetchInfo(); //$NON-NLS-1$
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(BYTES.length, fi.getLength());
	}

	public final void testMoveFolderAnotherFolder() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/fromfolder")); //$NON-NLS-1$
		assertNotNull(fs);
		fs.mkdir(EFS.SHALLOW, null);
		assertTrue(fs.fetchInfo().exists());
		OutputStream out = fs.getChild("file.txt").openOutputStream(EFS.NONE, null); //$NON-NLS-1$
		out.write(BYTES);
		out.close();
		assertTrue(fs.getChild("file.txt").fetchInfo().exists()); //$NON-NLS-1$
		IFileStore fs2 = fs.getParent().getFileStore(new Path("to/folder")); //$NON-NLS-1$
		assertNotNull(fs2);
		assertFalse(fs2.fetchInfo().exists());
		try
		{
			fs.move(fs2, EFS.NONE, null);
			assertFalse("<unreachable>", true); //$NON-NLS-1$
		}
		catch (CoreException e)
		{
			assertEquals(FileNotFoundException.class, e.getCause().getClass());
			assertEquals(
					testPath.append("to/folder").toPortableString(), ((FileNotFoundException) e.getCause()).getMessage()); //$NON-NLS-1$
		}

		fs2.getParent().mkdir(EFS.SHALLOW, null);
		assertTrue(fs2.getParent().fetchInfo().exists());
		fs.move(fs2, EFS.NONE, null);
		assertFalse(fs.fetchInfo().exists());

		IFileInfo fi = fs2.getChild("file.txt").fetchInfo(); //$NON-NLS-1$
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(BYTES.length, fi.getLength());
	}

	public final void testMoveFolderToExisting() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/fromfolder")); //$NON-NLS-1$
		assertNotNull(fs);
		fs.mkdir(EFS.SHALLOW, null);
		assertTrue(fs.fetchInfo().exists());
		OutputStream out = fs.getChild("file.txt").openOutputStream(EFS.NONE, null); //$NON-NLS-1$
		out.write(BYTES);
		out.close();
		assertTrue(fs.getChild("file.txt").fetchInfo().exists()); //$NON-NLS-1$
		IFileStore fs2 = fs.getParent().getFileStore(new Path("tofolder")); //$NON-NLS-1$
		assertNotNull(fs2);
		assertFalse(fs2.fetchInfo().exists());
		fs2.mkdir(EFS.SHALLOW, null);
		assertTrue(fs2.fetchInfo().exists());
		try
		{
			fs.move(fs2, EFS.NONE, null);
			assertFalse("<unreachable>", true); //$NON-NLS-1$
		}
		catch (CoreException e)
		{
			assertEquals(FileNotFoundException.class, e.getCause().getClass());
			assertEquals(
					testPath.append("tofolder").toPortableString(), ((FileNotFoundException) e.getCause()).getMessage()); //$NON-NLS-1$
		}

		fs.move(fs2, EFS.OVERWRITE, null);
		assertFalse(fs.fetchInfo().exists());

		IFileInfo fi = fs2.getChild("file.txt").fetchInfo(); //$NON-NLS-1$
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(BYTES.length, fi.getLength());
	}

	public final void testMoveFileToLocal() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/file.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.write(BYTES);
		out.close();
		assertTrue(fs.fetchInfo().exists());
		File file = File.createTempFile("testMoveFileToLocal", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
		file.delete();
		file.deleteOnExit();
		IFileStore fs2 = EFS.getLocalFileSystem().fromLocalFile(file);
		assertNotNull(fs2);
		assertFalse(fs2.fetchInfo().exists());
		fs.move(fs2, EFS.NONE, null);

		assertFalse(fs.fetchInfo().exists());

		IFileInfo fi = fs2.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(BYTES.length, fi.getLength());
	}

	public final void testMoveFolderToLocal() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/fromfolder")); //$NON-NLS-1$
		assertNotNull(fs);
		fs.mkdir(EFS.SHALLOW, null);
		assertTrue(fs.fetchInfo().exists());
		OutputStream out = fs.getChild("file.txt").openOutputStream(EFS.NONE, null); //$NON-NLS-1$
		out.write(BYTES);
		out.close();
		assertTrue(fs.getChild("file.txt").fetchInfo().exists()); //$NON-NLS-1$
		File file = File.createTempFile("testMoveFolderToLocal", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
		file.delete();
		file.deleteOnExit();
		IFileStore fs2 = EFS.getLocalFileSystem().fromLocalFile(file);
		assertNotNull(fs2);
		assertFalse(fs2.fetchInfo().exists());
		fs.move(fs2, EFS.NONE, null);
		assertFalse(fs.fetchInfo().exists());

		IFileInfo fi = fs2.getChild("file.txt").fetchInfo(); //$NON-NLS-1$
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(BYTES.length, fi.getLength());
	}

	public final void testFetchTree() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath);
		assertNotNull(fs);
		long lastModified = System.currentTimeMillis();
		lastModified -= lastModified % 1000; // remove milliseconds
		IFileStore parent = fs.getChild("folder1/folder2/folder3/folder4/folder5"); //$NON-NLS-1$
		parent.getChild("folder6").mkdir(EFS.NONE, null); //$NON-NLS-1$
		for (int j = 1; j < 7; ++j)
		{
			for (int i = 1; i < 6; ++i)
			{
				IFileStore child = parent.getChild("file" + i); //$NON-NLS-1$
				OutputStream out = child.openOutputStream(EFS.NONE, null);
				out.close();
				if (supportsSetModificationTime())
				{
					IFileInfo fi = new FileInfo();
					fi.setLastModified(lastModified);
					child.putInfo(fi, EFS.SET_LAST_MODIFIED, null);
				}
			}
			parent = parent.getParent();
		}

		IFileTree ft = fs.getFileSystem().fetchFileTree(fs, null);
		assertNotNull(ft);
		assertEquals(fs, ft.getTreeRoot());
		fs = ft.getTreeRoot();
		for (int i = 1; i < 6; ++i)
		{
			IFileStore[] fslist = ft.getChildStores(fs);
			assertEquals(6, fslist.length);
			IFileInfo[] filist = ft.getChildInfos(fs);
			assertEquals(6, filist.length);
			fs = null;
			for (int j = 0; j < filist.length; ++j)
			{
				IFileInfo fi = filist[j];
				assertTrue(fi.exists());
				if (fi.isDirectory())
				{
					assertEquals("folder" + i, fi.getName()); //$NON-NLS-1$
					assertEquals("folder" + i, fslist[j].getName()); //$NON-NLS-1$
					fs = fslist[j];
				}
				else
				{
					assertTrue(fi.getName().startsWith("file")); //$NON-NLS-1$
					assertTrue(fslist[j].getName().startsWith("file")); //$NON-NLS-1$
					assertEquals(0, fi.getLength());
					if (supportsSetModificationTime())
					{
						assertEquals(lastModified, fi.getLastModified());
					}
				}
			}
			assertNotNull(fs);
		}
	}

	protected final IFileStore populateRemoteFolder(String folderName, int numFiles) throws CoreException, IOException
	{

		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/" + folderName)); //$NON-NLS-1$
		assertNotNull(fs);
		fs.mkdir(EFS.SHALLOW, null);
		assertTrue(fs.fetchInfo().exists());
		for (int i = 0; i < numFiles; i++)
		{
			OutputStream out = fs.getChild("file" + i + ".txt").openOutputStream(EFS.NONE, null); //$NON-NLS-1$
			out.write(BYTES);
			out.close();
			assertTrue(fs.getChild("file" + i + ".txt").fetchInfo().exists()); //$NON-NLS-1$			
		}
		return fs;
	}
	
	public final void testSymlinks() throws CoreException, IOException, InterruptedException
	{
		String targetName = "symlinkTargetFolder";
		String linkName = "symlinkFolder";
		
		if(getRemoteFileDirectory() != null && cp instanceof IBaseRemoteConnectionPoint) {
			IBaseRemoteConnectionPoint brcp = (IBaseRemoteConnectionPoint)cp;
			Path targetPath = (Path)new Path(getRemoteFileDirectory()).append(brcp.getPath()).append(testPath).append("/" + targetName);
			Path linkPath = (Path)new Path(getRemoteFileDirectory()).append(brcp.getPath()).append(testPath).append("/" + linkName);

			// create target folder
			File target = new File(targetPath.toPortableString());
			assertTrue(target.mkdirs());
			target.deleteOnExit();

			Process process = Runtime.getRuntime().exec( new String[] { "ln", "-s", targetPath.toPortableString(), linkPath.toPortableString() } );
			process.waitFor();
			process.destroy();	

			IFileInfo[] children = cp.getRoot().getFileStore(testPath).childInfos(EFS.NONE, null);
			assertEquals(2, children.length);

			process = Runtime.getRuntime().exec( new String[] { "rm", linkPath.toPortableString() } );
			process.waitFor();
			process.destroy();	

		}
	}
}
