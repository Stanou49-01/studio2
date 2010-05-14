package com.aptana.ide.syncing.tests;

import java.io.File;

import org.eclipse.core.runtime.Path;

import com.aptana.ide.core.io.LocalConnectionPoint;

public class LocalSyncingTests extends SyncingTests
{

	@Override
	protected void setUp() throws Exception
	{
		File baseTempFile = File.createTempFile("test", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
		baseTempFile.deleteOnExit();
		
		File baseDirectory = baseTempFile.getParentFile();
		
		LocalConnectionPoint lcp = new LocalConnectionPoint();
		lcp.setPath(new Path(baseDirectory.getAbsolutePath()));
		clientManager = lcp;

		LocalConnectionPoint scp = new LocalConnectionPoint();
		scp.setPath(new Path(baseDirectory.getAbsolutePath()));
		serverManager = scp;

		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception
	{
		// TODO Auto-generated method stub
		super.tearDown();
	}

}
