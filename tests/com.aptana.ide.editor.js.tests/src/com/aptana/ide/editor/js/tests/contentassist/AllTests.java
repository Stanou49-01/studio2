package com.aptana.ide.editor.js.tests.contentassist;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite("Test for com.aptana.ide.editor.js.tests.contentassist");
		//$JUnit-BEGIN$
		suite.addTestSuite(JSCompletionProposalTest.class);
		suite.addTestSuite(JSContentAssistProcessorTest.class);
		//$JUnit-END$
		return suite;
	}

}
