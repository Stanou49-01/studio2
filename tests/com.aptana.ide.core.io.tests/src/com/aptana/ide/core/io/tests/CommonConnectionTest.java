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

package com.aptana.ide.core.io.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.Random;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileTree;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.After;
import org.junit.Test;

import com.aptana.ide.core.io.ConnectionContext;
import com.aptana.ide.core.io.CoreIOPlugin;
import com.aptana.ide.core.io.IConnectionPoint;
import com.aptana.ide.core.io.efs.LocalFile;
import com.aptana.ide.core.io.vfs.ExtendedFileInfo;
import com.aptana.ide.core.io.vfs.IExtendedFileInfo;
import com.aptana.ide.core.io.vfs.IExtendedFileStore;

/**
 * @author Max Stepanov
 */
public abstract class CommonConnectionTest
{
	protected boolean supportsChangeGroup = false;
	protected boolean supportsChangePermissions = false;

	protected static final long ALLOWED_TIME_DIFFERENCE = 15 * 60 * 1000; /* 15 minute difference */

	protected static final String TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse nunc tellus, condimentum quis luctus fermentum, tincidunt eget dui. Sed bibendum iaculis ligula, fringilla ullamcorper justo ullamcorper non. Curabitur tristique mi a magna vestibulum fermentum. Praesent sed neque feugiat purus egestas tristique. Sed non nisi velit. Maecenas placerat, nisi quis iaculis porta, nisi mauris facilisis est, at rutrum lacus sem non ante. Morbi et cursus nibh. Aliquam tincidunt urna quis quam semper ut congue est auctor. Curabitur malesuada, diam ut congue elementum, orci eros rhoncus felis, vel elementum felis velit id eros. Quisque eros diam, malesuada nec tincidunt eget, gravida iaculis tortor. Donec sollicitudin ultricies ante ac facilisis. In egestas malesuada erat id vehicula.\n" + //$NON-NLS-1$
			"Integer non urna nunc, et rhoncus eros. Suspendisse tincidunt laoreet enim vel pretium. Nam bibendum sodales risus nec adipiscing. Pellentesque fringilla interdum odio posuere consectetur. Nullam venenatis augue sed felis tempus eu posuere quam facilisis. Pellentesque commodo rutrum bibendum. Ut sit amet sapien in purus vestibulum sodales. Integer pharetra mi in dui auctor in tristique erat malesuada. Integer nec ipsum quam. Quisque non enim et quam consequat mollis id ac sem. Nunc ut elit ac odio adipiscing pretium vel eget mauris. Aenean diam diam, porttitor sit amet lobortis a, accumsan at ante. Phasellus ut nulla enim. In nec diam magna. In molestie vulputate viverra. Etiam at justo tellus, sed rutrum erat.\r\n"
			+ //$NON-NLS-1$
			"Duis consectetur ornare ante, sit amet ultricies leo aliquam vitae. In fermentum nisi non dolor viverra non hendrerit nulla malesuada. Mauris adipiscing aliquet fringilla. Curabitur porttitor tristique massa, et semper nulla semper et. Phasellus a ipsum eu lectus pulvinar aliquam eget viverra velit. Sed commodo ultrices pulvinar. In at felis sollicitudin lorem semper scelerisque. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Proin vel purus id odio malesuada gravida. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Quisque metus mi, eleifend consectetur varius vitae, euismod eget nulla. Morbi justo felis, accumsan vel tempor non, rutrum at augue. Curabitur nulla lorem, ultricies a lobortis in, semper vitae diam. Pellentesque nec orci non turpis dignissim mollis. Quisque quis sapien vitae ligula iaculis dapibus sed at quam. Nullam ut nisl id eros sagittis rutrum a vitae risus. Suspendisse lacinia lacinia rutrum. Fusce molestie pellentesque dapibus. Quisque eu orci dolor, eget venenatis velit.\n"
			+ //$NON-NLS-1$
			"Nam rhoncus gravida ultrices. Maecenas hendrerit diam pharetra mauris commodo eleifend. Etiam ullamcorper aliquet arcu, sit amet luctus risus scelerisque at. Praesent nibh eros, rutrum in imperdiet eget, dignissim ornare nisl. Fusce sollicitudin, turpis id volutpat tincidunt, diam nibh euismod eros, eget tempor justo nulla ut magna. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Vivamus eu neque ac ante varius imperdiet. Vestibulum blandit neque lacus, a suscipit mi. Maecenas aliquet, lorem ut interdum bibendum, velit tellus feugiat quam, non posuere leo justo eget ante. Aliquam mattis augue est, et malesuada libero. Suspendisse nisl tellus, tempus sit amet luctus quis, vulputate eu turpis. Morbi lobortis vulputate odio at faucibus. Cras ut nisi ipsum."; //$NON-NLS-1$
	protected static final byte[] BYTES;

	static
	{
		BYTES = new byte[65536 + 20];
		for (int i = 0; i < BYTES.length; ++i)
		{
			BYTES[i] = (byte) i;
		}
	}

	protected IConnectionPoint cp;
	private IPath testPath;

	private static Properties cachedProperties;

	protected static final Properties getConfig() {
		if (cachedProperties == null) {
			cachedProperties = new Properties();
			String propertiesFile = System.getenv("junit.properties");
			if (propertiesFile != null && new File(propertiesFile).length() > 0) {
				try {
					cachedProperties.load(new FileInputStream(propertiesFile));
				} catch (IOException ignore) {
				}
			}
		}
		return cachedProperties;
	}

	protected void initialize() throws CoreException, FileNotFoundException, IOException
	{
		ConnectionContext context = new ConnectionContext();
		context.put(ConnectionContext.COMMAND_LOG, System.out);
		CoreIOPlugin.setConnectionContext(cp, context);

		testPath = Path.ROOT.append(getClass().getSimpleName() + System.currentTimeMillis());
		IFileStore fs = cp.getRoot().getFileStore(testPath);
		assertNotNull(fs);
		fs.mkdir(EFS.NONE, null);
		cp.disconnect(null);
		assertFalse(cp.isConnected());
	}

	@After
	public final void finish() throws CoreException
	{
		try
		{
			IFileStore fs = cp.getRoot().getFileStore(testPath);
			if (fs.fetchInfo().exists())
			{
				fs.delete(EFS.NONE, null);
				assertFalse(fs.fetchInfo().exists());
			}
		}
		finally
		{
			if (cp.isConnected())
			{
				cp.disconnect(null);
			}
		}
	}

	@Test
	public final void testURI() throws CoreException
	{
		assertEquals(cp.getRootURI(), cp.getRoot().toURI());
		IFileStore fs = cp.getRoot().getFileStore(new Path("/some/path/some.file")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileStore fs2 = EFS.getStore(cp.getRootURI().resolve("/some/path/some.file")); //$NON-NLS-1$
		assertEquals(fs, fs2);
	}

	@Test
	public final void testConnectDisconnect() throws CoreException
	{
		cp.connect(null);
		assertTrue(cp.isConnected());
		assertTrue(cp.canDisconnect());
		cp.disconnect(null);
		assertFalse(cp.isConnected());
		assertFalse(cp.canDisconnect());
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
	public final void testCreateEmptyFile() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/emptyfile.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
		assertEquals("emptyfile.txt", fi.getName()); //$NON-NLS-1$
		long beforeTime = System.currentTimeMillis() - ALLOWED_TIME_DIFFERENCE;
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.close();
		long afterTime = System.currentTimeMillis() + ALLOWED_TIME_DIFFERENCE;
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(0, fi.getLength());
		assertTrue(fi.getLastModified() > beforeTime && fi.getLastModified() < afterTime);
	}

	@Test
	public final void testCreateEmptyFileRecursive() throws CoreException, IOException
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

	@Test
	public final void testCreateFolder() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/newfolder")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
		assertEquals("newfolder", fi.getName()); //$NON-NLS-1$
		long beforeTime = System.currentTimeMillis() - ALLOWED_TIME_DIFFERENCE;
		fs.mkdir(EFS.SHALLOW, null);
		long afterTime = System.currentTimeMillis() + ALLOWED_TIME_DIFFERENCE;
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertTrue(fi.isDirectory());
		assertTrue(fi.getLastModified() > beforeTime && fi.getLastModified() < afterTime);
		fs.mkdir(EFS.SHALLOW, null); // retry to show no errors
	}

	@Test
	public final void testCreateFolderRecursive() throws CoreException, IOException
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

		long beforeTime = System.currentTimeMillis() - ALLOWED_TIME_DIFFERENCE;
		fs2.mkdir(EFS.NONE, null);
		long afterTime = System.currentTimeMillis() + ALLOWED_TIME_DIFFERENCE;
		fi = fs2.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertTrue(fi.getLastModified() > beforeTime && fi.getLastModified() < afterTime);
	}

	@Test
	public final void testWriteReadBinFile() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/rwfile.bin")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
		long beforeTime = System.currentTimeMillis() - ALLOWED_TIME_DIFFERENCE;
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.write(BYTES);
		out.close();
		long afterTime = System.currentTimeMillis() + ALLOWED_TIME_DIFFERENCE;
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(BYTES.length, fi.getLength());
		assertTrue(fi.getLastModified() > beforeTime && fi.getLastModified() < afterTime);
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

	@Test
	public final void testWriteReadTextFile() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/rwfile.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertFalse(fi.exists());
		long beforeTime = System.currentTimeMillis() - ALLOWED_TIME_DIFFERENCE;
		Writer w = new OutputStreamWriter(fs.openOutputStream(EFS.NONE, null));
		w.write(TEXT);
		w.close();
		long afterTime = System.currentTimeMillis() + ALLOWED_TIME_DIFFERENCE;
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(TEXT.length(), fi.getLength());
		assertTrue(fi.getLastModified() > beforeTime && fi.getLastModified() < afterTime);
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

	@Test
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

		long beforeTime = System.currentTimeMillis() - ALLOWED_TIME_DIFFERENCE;
		Writer w = new OutputStreamWriter(fs.openOutputStream(EFS.NONE, null));
		w.write(TEXT);
		w.close();
		long afterTime = System.currentTimeMillis() + ALLOWED_TIME_DIFFERENCE;
		fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(TEXT.length(), fi.getLength());
		assertTrue(fi.getLastModified() > beforeTime && fi.getLastModified() < afterTime);
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

	@Test
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

	@Test
	public final void testDeleteFolder() throws CoreException, IOException
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

	@Test
	public final void testDeleteFolderRecursive() throws CoreException, IOException
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

	@Test
	public final void testListFiles() throws CoreException, IOException
	{
		String[] NAMES = new String[] { "file1.txt", "file2.txt", "file3.txt" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		IFileStore fs = cp.getRoot().getFileStore(testPath);
		assertNotNull(fs);
		long beforeTime = System.currentTimeMillis() - ALLOWED_TIME_DIFFERENCE;
		for (int i = 0; i < NAMES.length; ++i)
		{
			OutputStream out = fs.getChild(NAMES[i]).openOutputStream(EFS.NONE, null);
			out.close();
		}
		long afterTime = System.currentTimeMillis() + ALLOWED_TIME_DIFFERENCE;

		String[] names = fs.childNames(EFS.NONE, null);
		Arrays.sort(names);
		assertEquals(NAMES.length, names.length);
		assertArrayEquals(NAMES, names);

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
			assertTrue(filist[i].getLastModified() > beforeTime && filist[i].getLastModified() < afterTime);
		}
	}

	@Test
	public final void testListFolders() throws CoreException, IOException
	{
		String[] NAMES = new String[] { "folder1", "folder2", "folder3" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		IFileStore fs = cp.getRoot().getFileStore(testPath);
		assertNotNull(fs);
		long beforeTime = System.currentTimeMillis() - ALLOWED_TIME_DIFFERENCE;
		for (int i = 0; i < NAMES.length; ++i)
		{
			fs.getChild(NAMES[i]).mkdir(EFS.SHALLOW, null);
		}
		long afterTime = System.currentTimeMillis() + ALLOWED_TIME_DIFFERENCE;

		String[] names = fs.childNames(EFS.NONE, null);
		Arrays.sort(names);
		assertEquals(NAMES.length, names.length);
		assertArrayEquals(NAMES, names);

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
			assertTrue(filist[i].getLastModified() > beforeTime && filist[i].getLastModified() < afterTime);
		}
	}

	@Test
	public final void testPutInfoFileBase() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/file.txt")); //$NON-NLS-1$
		assertNotNull(fs);
		OutputStream out = fs.openOutputStream(EFS.NONE, null);
		out.close();
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());

		long lastModified = System.currentTimeMillis();
		lastModified -= lastModified % 1000; // remove milliseconds
		IFileInfo pfi = new FileInfo();
		pfi.setLastModified(lastModified);
		fs.putInfo(pfi, EFS.SET_LAST_MODIFIED, null);

		fi = fs.fetchInfo(IExtendedFileStore.DETAILED, null);
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(lastModified, fi.getLastModified());
	}

	@Test
	public final void testPutInfoFolderBase() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath.append("/newfolder")); //$NON-NLS-1$
		assertNotNull(fs);
		fs.mkdir(EFS.SHALLOW, null);
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertTrue(fi.isDirectory());

		long lastModified = System.currentTimeMillis();
		lastModified -= new Random().nextInt(7 * 24 * 60) * 60000;
		lastModified -= lastModified % 60000; // remove seconds/milliseconds
		IFileInfo pfi = new FileInfo();
		pfi.setLastModified(lastModified);
		fs.putInfo(pfi, EFS.SET_LAST_MODIFIED, null);

		fi = fs.fetchInfo(IExtendedFileStore.DETAILED, null);
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertTrue(fi.isDirectory());
		assertEquals(lastModified, fi.getLastModified());
	}

	protected boolean supportsChangePermissions()
	{
		return supportsChangePermissions;
	}

	@Test
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

	protected boolean supportsChangeGroup()
	{
		return supportsChangeGroup;
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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
		IFileStore fs2 = new LocalFile(file);
		assertNotNull(fs2);
		assertFalse(fs2.fetchInfo().exists());
		fs.move(fs2, EFS.NONE, null);

		assertFalse(fs.fetchInfo().exists());

		IFileInfo fi = fs2.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(BYTES.length, fi.getLength());
	}

	@Test
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
		IFileStore fs2 = new LocalFile(file);
		assertNotNull(fs2);
		assertFalse(fs2.fetchInfo().exists());
		fs.move(fs2, EFS.NONE, null);
		assertFalse(fs.fetchInfo().exists());

		IFileInfo fi = fs2.getChild("file.txt").fetchInfo(); //$NON-NLS-1$
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertEquals(BYTES.length, fi.getLength());
	}

	@Test
	public final void testFetchTree() throws CoreException, IOException
	{
		IFileStore fs = cp.getRoot().getFileStore(testPath);
		assertNotNull(fs);
		long lastModified = System.currentTimeMillis();
		lastModified -= lastModified % 1000; // remove milliseconds
		IFileStore parent = fs.getChild("folder1/folder2/folder3/folder4/folder5");
		parent.getChild("folder6").mkdir(EFS.NONE, null);
		for (int j = 1; j < 7; ++j)
		{
			for (int i = 1; i < 6; ++i)
			{
				IFileStore child = parent.getChild("file" + i);
				OutputStream out = child.openOutputStream(EFS.NONE, null);
				out.close();
				IFileInfo fi = new FileInfo();
				fi.setLastModified(lastModified);
				child.putInfo(fi, EFS.SET_LAST_MODIFIED, null);
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
					assertEquals("folder" + i, fi.getName());
					assertEquals("folder" + i, fslist[j].getName());
					fs = fslist[j];
				}
				else
				{
					assertTrue(fi.getName().startsWith("file"));
					assertTrue(fslist[j].getName().startsWith("file"));
					assertEquals(0, fi.getLength());
					assertEquals(lastModified, fi.getLastModified());
				}
			}
			assertNotNull(fs);
		}
	}

}
