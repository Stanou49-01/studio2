package com.aptana.ide.core.io.efs.tests;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import junit.framework.TestCase;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.aptana.ide.core.io.efs.WorkspaceFileSystem;

public class WorkspaceFileSystemTest extends TestCase {

	protected IFileSystem wfs = null;
	protected IPath location;
	
	protected void setUp() throws Exception {
		super.setUp();
		wfs = WorkspaceFileSystem.getInstance();
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		location = root.getLocation();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testAttributes() {
		wfs.attributes();
	}

	public void testCanDelete() throws IOException {
		assertTrue(wfs.canDelete());
	}

	public void testCanWrite() throws IOException {
		assertTrue(wfs.canWrite());
	}

	public void testIsCaseSensitive() {
		// This is going to be OS-specific
		//assertFalse(wfs.isCaseSensitive());
	}

	public void testGetInstance() {
		assertNotNull(wfs);
	}

	public void testFromLocalFileFile() throws IOException {
		File baseTempFile = File.createTempFile("test", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
		IFileStore fs = wfs.fromLocalFile(baseTempFile);
		assertNull(fs); // File from temporary location will not be in workspace
		
	}

	public void testGetStoreIPath() throws IOException, CoreException {
		IFileStore fs = wfs.getStore(location);

		// Workspace file has workspace:/ protocol and trailing '/'
		URI uri = fs.toURI();
		String replaced = uri.toString().replaceAll("workspace:", "file:") + "/";
		assertEquals(location.toFile().toURI().toString(), replaced);
	}

	public void testGetStoreURI() throws IOException, CoreException {
		IFileStore fs = wfs.getStore(location.toFile().toURI());

		// Workspace file has workspace:/ protocol
		URI uri = fs.toURI();
		String replaced = uri.toString().replaceAll("workspace:", "file:");
		assertEquals(location.toFile().toURI().toString(), replaced);
	}

}
