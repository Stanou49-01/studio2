package com.aptana.commons.spelling.engine;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

    private static final String BUNDLE_NAME = "com.aptana.commons.spelling.engine.messages"; //$NON-NLS-1$

    public static String NoCompletionsProposal_DisplayText;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
