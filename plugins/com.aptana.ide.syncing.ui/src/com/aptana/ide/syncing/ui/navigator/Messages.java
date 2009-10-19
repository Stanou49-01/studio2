package com.aptana.ide.syncing.ui.navigator;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

    private static final String BUNDLE_NAME = "com.aptana.ide.syncing.ui.navigator.messages"; //$NON-NLS-1$

    public static String ProjectSiteConnections_Name;

    public static String SiteConnections_LBL;

    public static String SiteConnections_TTP;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
