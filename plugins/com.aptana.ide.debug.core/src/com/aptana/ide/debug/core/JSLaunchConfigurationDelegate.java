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
package com.aptana.ide.debug.core;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.SocketUtil;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.URLEncoder;
import com.aptana.ide.debug.internal.core.BrowserUtil;
import com.aptana.ide.debug.internal.core.LocalResourceMapper;
import com.aptana.ide.debug.internal.core.browsers.Firefox;
import com.aptana.ide.debug.internal.core.browsers.InternetExplorer;
import com.aptana.ide.debug.internal.core.model.DebugConnection;
import com.aptana.ide.debug.internal.core.model.HttpServerProcess;
import com.aptana.ide.debug.internal.core.model.JSDebugProcess;
import com.aptana.ide.debug.internal.core.model.JSDebugTarget;
import com.aptana.ide.server.ServerCore;
import com.aptana.ide.server.core.IHttpServerProviderAdapter;
import com.aptana.ide.server.core.IServer;

/**
 * @author Max Stepanov
 *
 */
public class JSLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

	protected static final int DEFAULT_PORT = 8999;
	
	/**
	 * launchBrowserPromptStatus
	 */
	protected static final IStatus launchBrowserPromptStatus = new Status(IStatus.INFO, JSDebugPlugin.ID, 302, StringUtils.EMPTY, null);  

	/**
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	// CHECKSTYLE:OFF
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException
	// CHECKSTYLE:ON
	{

		IStatusHandler prompter = DebugPlugin.getDefault().getStatusHandler(promptStatus);

		// TODO remove when multiple debug targets supported
		if ( "debug".equals(mode) ) { //$NON-NLS-1$
			IDebugTarget[] targets = DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
			IDebugTarget activeSession = null;
			for (int i = 0; i < targets.length; i++) {
				if (targets[i].getModelIdentifier().equals(IDebugConstants.ID_DEBUG_MODEL)) {
					if (!targets[i].isTerminated()){
						activeSession = targets[i];
						break;
					}
				}
			}
			if ( activeSession != null ) {
				Object result = prompter.handleStatus(launchBrowserPromptStatus, null);
				if ( (result instanceof Boolean) && (((Boolean) result).booleanValue()) ) {
					activeSession.terminate();
				} else {
					String errorMessage = Messages.JSLaunchConfigurationDelegate_MultipleJavaScriptDebugNotSupported +
							Messages.JSLaunchConfigurationDelegate_PleaseTerminateActiveSession;
					throw new CoreException(new Status(IStatus.ERROR, JSDebugPlugin.ID, Status.ERROR, errorMessage, null));
				}
			}
		} 

		/* Check browser */
		String browserExecutable = configuration.getAttribute(ILaunchConfigurationConstants.CONFIGURATION_BROWSER_EXECUTABLE,(String)null);
		if ( browserExecutable == null || !new File(browserExecutable).exists() ) {
			throw new CoreException( new Status(IStatus.ERROR, JSDebugPlugin.ID, IStatus.OK, StringUtils.format(Messages.JSLaunchConfigurationDelegate_WebBrowserDoesNotExist, browserExecutable), null));			
		}

		JSLaunchConfigurationHelper.initializeLaunchAttributes(configuration,launch);

		boolean debugCompatible = BrowserUtil.isBrowserDebugCompatible(browserExecutable);
		boolean debugAvailable = false;
		boolean debug = "debug".equals(mode); //$NON-NLS-1$
		boolean advancedRun = configuration.getAttribute(ILaunchConfigurationConstants.CONFIGURATION_ADVANCED_RUN_ENABLED,false);
		
		if ( debugCompatible && ("debug".equals(mode) || advancedRun ) ) { //$NON-NLS-1$
			monitor.subTask(Messages.JSLaunchConfigurationDelegate_CheckingBrowserForDebugger);
			debugAvailable = BrowserUtil.isBrowserDebugAvailable(browserExecutable);
			if ( !debugAvailable ) {
				if(!BrowserUtil.installDebugExtension(browserExecutable,prompter,monitor)) {
					monitor.setCanceled(true);
					return;					
				}
				debugAvailable = BrowserUtil.isBrowserDebugAvailable(browserExecutable);
			}
			if ( debug && !debugAvailable ) {
				throw new CoreException(new Status(IStatus.ERROR, JSDebugPlugin.ID, IStatus.OK, Messages.JSLaunchConfigurationDelegate_DebuggerExtensionNotInstalled, null));				
			}		
		}

		/* Initialize launch URL, optionally start local HTTP server */
		int serverType = configuration.getAttribute(ILaunchConfigurationConstants.CONFIGURATION_SERVER_TYPE,ILaunchConfigurationConstants.DEFAULT_SERVER_TYPE);
		int startActionType = configuration.getAttribute(ILaunchConfigurationConstants.CONFIGURATION_START_ACTION_TYPE,ILaunchConfigurationConstants.DEFAULT_START_ACTION_TYPE);
		boolean appendProjectName = configuration.getAttribute(ILaunchConfigurationConstants.CONFIGURATION_APPEND_PROJECT_NAME, false);

		LocalResourceMapper resourceMapper = null;
		HttpServerProcess httpServer = null;
		boolean launchHttpServer = false;
		boolean launchServerDebugger = false;
		URL baseURL = null;
		try {
			if ( serverType == ILaunchConfigurationConstants.SERVER_INTERNAL ) {
				if ( startActionType != ILaunchConfigurationConstants.START_ACTION_START_URL ) {
					launchHttpServer = true;
				} /* else => do not launch server for direct URLs */
			} else if (serverType == ILaunchConfigurationConstants.SERVER_EXTERNAL
					|| serverType == ILaunchConfigurationConstants.SERVER_MANAGED) {
				String externalBaseUrl;
				if (serverType == ILaunchConfigurationConstants.SERVER_EXTERNAL) {
					externalBaseUrl = configuration.getAttribute(ILaunchConfigurationConstants.CONFIGURATION_EXTERNAL_BASE_URL, StringUtils.EMPTY).trim();
				} else {
					String serverId = configuration.getAttribute(ILaunchConfigurationConstants.CONFIGURATION_SERVER_ID, (String)null);
					String host = null;
					IServer server = ServerCore.getServerManager().findServer(serverId);
					if (server != null) {
						host = server.getHost();
						if (host == null) {
							host = "localhost"; //$NON-NLS-1$
						}
						for (IServer associatedServer : server.getAssociatedServers()) {
							if ("Jaxer Server".equals(associatedServer.getDescription())) { //$NON-NLS-1$
								launchServerDebugger = true;
							}							
						}
					}
					if (host == null) {
						throw new CoreException( new Status(IStatus.ERROR, JSDebugPlugin.ID, IStatus.OK, Messages.JSLaunchConfigurationDelegate_Host_Not_Specified, null));															
					}
					externalBaseUrl = StringUtils.format("http://{0}/", host);				 //$NON-NLS-1$
				}
				if (externalBaseUrl.length() == 0) {
					throw new CoreException( new Status(IStatus.ERROR, JSDebugPlugin.ID, IStatus.OK, Messages.JSLaunchConfigurationDelegate_Empty_URL, null));					
				}
				if ( externalBaseUrl.charAt(externalBaseUrl.length()-1) != '/' ) {
					externalBaseUrl = externalBaseUrl + '/';
				}
				baseURL = new URL(externalBaseUrl);		
				resourceMapper = new LocalResourceMapper();
				resourceMapper.addMapping(baseURL,ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile());
			} else {
				throw new CoreException( new Status(IStatus.ERROR, JSDebugPlugin.ID, IStatus.OK, Messages.JSLaunchConfigurationDelegate_No_Server_Type, null));				
			}
		} catch (MalformedURLException e) {
			throw new CoreException( new Status(IStatus.ERROR, JSDebugPlugin.ID, IStatus.OK, Messages.JSLaunchConfigurationDelegate_MalformedServerURL, e));
		}

		try {
			URL launchURL = null;
			try {
				if ( startActionType == ILaunchConfigurationConstants.START_ACTION_START_URL ) {
					if ( resourceMapper != null ) {
						JSLaunchConfigurationHelper.setResourceMapping(configuration, baseURL, resourceMapper, httpServer);
					}
					launchURL = new URL(configuration.getAttribute(ILaunchConfigurationConstants.CONFIGURATION_START_PAGE_URL, StringUtils.EMPTY));
				} else {
					IResource resource = null;
					if ( startActionType == ILaunchConfigurationConstants.START_ACTION_CURRENT_PAGE ) {
						resource = getCurrentEditorResource();
					} else if ( startActionType == ILaunchConfigurationConstants.START_ACTION_SPECIFIC_PAGE ) {
						String resourcePath = configuration.getAttribute(ILaunchConfigurationConstants.CONFIGURATION_START_PAGE_PATH,(String)null);
						if ( resourcePath != null && resourcePath.length() > 0 ) {
							resource = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(resourcePath));
						}
					}
					if ( resource != null ) {
						if ( baseURL == null && launchHttpServer ) {
							monitor.subTask(Messages.JSLaunchConfigurationDelegate_LaunchingHTTPServer);
							IHttpServerProviderAdapter httpServerProvider = (IHttpServerProviderAdapter) getContributedAdapter(IHttpServerProviderAdapter.class);
							IServer server = null;
							if (httpServerProvider != null) {
								server = httpServerProvider.getServer(resource);
								if ( server != null) {
									for (IServer associatedServer : server.getAssociatedServers()) {
										if ("Jaxer Server".equals(associatedServer.getDescription())) { //$NON-NLS-1$
											launchServerDebugger = true;
										}							
									}
									IPath documentRoot = server.getDocumentRoot();
									if (documentRoot != null && documentRoot.equals(ResourcesPlugin.getWorkspace().getRoot().getLocation())) {
										appendProjectName = true;
									}
								}
							}
							
							File root = resource.getProject().getLocation().toFile();
							if (server != null) {
								baseURL = new URL(StringUtils.format("http://{0}/", server.getHost())); //$NON-NLS-1$
							} else {
								httpServer = new HttpServerProcess(launch);
								httpServer.setServerRoot(root);
								baseURL = httpServer.getBaseURL();
							}
							if ( appendProjectName ) {
								IProject project = resource.getProject();
								baseURL = new URL(baseURL, project.getName()+'/');
							}
							
							resourceMapper = new LocalResourceMapper();
							resourceMapper.addMapping(baseURL,root);
							JSLaunchConfigurationHelper.setResourceMapping(configuration, baseURL, resourceMapper, httpServer);
							
							launchURL = resourceMapper.resolveLocalURI(resource.getLocationURI()).toURL();
							//launchURL = new URL(baseURL, resource.getProjectRelativePath().makeRelative().toPortableString());
						} else if ( baseURL != null ) {
							IProject project = resource.getProject();
							if ( appendProjectName ) {
								baseURL = new URL(baseURL, project.getName()+'/');
							}
							resourceMapper.addMapping(baseURL,project.getLocation().toFile());
							JSLaunchConfigurationHelper.setResourceMapping(configuration, baseURL, resourceMapper, httpServer);
							
							launchURL = resourceMapper.resolveLocalURI(resource.getLocationURI()).toURL();
							//launchURL = new URL(baseURL, resource.getProjectRelativePath().makeRelative().toPortableString());
						} else {
							launchURL = resource.getLocation().toFile().toURI().toURL();
						}
					} else if ( startActionType == ILaunchConfigurationConstants.START_ACTION_CURRENT_PAGE ) {
						IPath path = getCurrentEditorPath();
						if ( path != null ) {
							if (debug && InternetExplorer.isBrowserExecutable(browserExecutable)) {
								String errorMessage = Messages.JSLaunchConfigurationDelegate_Only_Project_Debugging_Supported;
								throw new CoreException(new Status(IStatus.ERROR, JSDebugPlugin.ID, Status.ERROR, errorMessage, null));				
							}
							launchURL = path.toFile().toURI().toURL();
						} else {
							launchURL = getCurrentEditorURL();
							if (launchURL == null) {
								monitor.setCanceled(true);
								return;
							}
						}
					}
				}

				if ( launchURL == null ) {
					throw new CoreException( new Status(IStatus.ERROR, JSDebugPlugin.ID, IStatus.OK, Messages.JSLaunchConfigurationDelegate_LaunchURLNotDefined, null));
				}

				// XXX: temporary solution for IE
				if( launchURL.toExternalForm().endsWith(".js") && InternetExplorer.isBrowserExecutable(browserExecutable)) { //$NON-NLS-1$
					String errorMessage = Messages.JSLaunchConfigurationDelegate_Cannot_Debug_JS_File;
					throw new CoreException(new Status(IStatus.ERROR, JSDebugPlugin.ID, Status.ERROR, errorMessage, null));				
				}

				String httpGetQuery = configuration.getAttribute(ILaunchConfigurationConstants.CONFIGURATION_HTTP_GET_QUERY,StringUtils.EMPTY);
				if ( httpGetQuery != null && httpGetQuery.length() > 0
						&& launchURL.getQuery() == null && launchURL.getRef() == null ) {
					if ( httpGetQuery.charAt(0) != '?') {
						httpGetQuery = '?'+httpGetQuery;
					}
					launchURL = new URL(launchURL,launchURL.getFile()+httpGetQuery);
				}
				launchURL = new URL(launchURL, URLEncoder.encode(launchURL.getPath(), launchURL.getQuery(), launchURL.getRef()));
			} catch (MalformedURLException e) {
				throw new CoreException( new Status(IStatus.ERROR, JSDebugPlugin.ID, IStatus.OK, Messages.JSLaunchConfigurationDelegate_MalformedLaunchURL, e));
			}
								
			monitor.subTask(Messages.JSLaunchConfigurationDelegate_LaunchingBrowser);

			Process process = null;
			ArrayList<String> browserArgs = new ArrayList<String>();
			String browserCmdLine = configuration.getAttribute(ILaunchConfigurationConstants.CONFIGURATION_BROWSER_COMMAND_LINE, StringUtils.EMPTY);
			if ( browserCmdLine != null && browserCmdLine.length() > 0 ) {
				String[] args = browserCmdLine.split(StringUtils.SPACE);
				for( int i = 0; i < args.length; ++i ) {
					if ( args[i].trim().length() > 0 ) {
						browserArgs.add(args[i].trim());
					}
				}
			}
			ArrayList<String> args = new ArrayList<String>();
			
			if ( debugAvailable ) {
				
				int port = SocketUtil.findFreePort();
				if ( "true".equals(Platform.getDebugOption("com.aptana.ide.debug.core/debugger_debug")) ) { //$NON-NLS-1$ //$NON-NLS-2$
					port = 2525;
				}
				if ( port == -1 ) {
					port = DEFAULT_PORT;
				}

				ServerSocket listenSocket = null;
				try {
					listenSocket = new ServerSocket(port);
					if ( !"true".equals(Platform.getDebugOption("com.aptana.ide.debug.core/debugger_debug")) ) { //$NON-NLS-1$ //$NON-NLS-2$
						listenSocket.setSoTimeout(DebugConnection.SOCKET_TIMEOUT);
					}
				} catch (IOException e) {
					throw new CoreException( new Status(IStatus.ERROR, JSDebugPlugin.ID, IStatus.OK, Messages.JSLaunchConfigurationDelegate_SocketConnectionError, e));
				}
				
				String debuggerLaunchUrl = BrowserUtil.DEBUGGER_LAUNCH_URL + Integer.toString(port);

				/*TODO: temporary Jaxer solution - change before 1.1 release */
				if ((launchServerDebugger || "true".equals(Platform.getDebugOption("com.aptana.ide.debug.core/external_server_is_jaxer"))) //$NON-NLS-1$ //$NON-NLS-2$
						&& "true".equals(Platform.getDebugOption("com.aptana.ide.debug.core/jaxer_debugger"))) { //$NON-NLS-1$ //$NON-NLS-2$
					debuggerLaunchUrl = launchURL.toExternalForm();
					if(launchURL.getQuery() == null) {
						debuggerLaunchUrl += '?';
					}
					debuggerLaunchUrl += "__JAXER_DEBUGGER=" + Integer.toString(port); //$NON-NLS-1$
				}
				
				try {
					if ( Platform.OS_MACOSX.equals(Platform.getOS()) ) {
						args.add("/usr/bin/open"); //$NON-NLS-1$
						if ( System.getProperty("os.version",StringUtils.EMPTY).startsWith("10.3.") ) { //$NON-NLS-1$ //$NON-NLS-2$
							/*
							 * Workaround for MaxOSX systems prior 10.4
							 * where open command doesn't have -b option
							 */
							args.add("-a"); //$NON-NLS-1$
							args.add(browserExecutable);
						} else {
							args.add("-b"); //$NON-NLS-1$
							args.add(BrowserUtil.getMacOSXApplicationIdentifier(browserExecutable));
						}
						args.add(debuggerLaunchUrl);

					} else if ( InternetExplorer.isBrowserExecutable(browserExecutable) ) {
						args.add(browserExecutable);
						args.add(debuggerLaunchUrl);
					} else {
						args.add(browserExecutable);
						args.add(debuggerLaunchUrl);
					}
					if ( "true".equals(Platform.getDebugOption("com.aptana.ide.debug.core/debugger_debug")) ) { //$NON-NLS-1$ //$NON-NLS-2$
						args = null;
					}
					
					if ( args != null ) {
						args.addAll(browserArgs);
						process = Runtime.getRuntime().exec( (String[]) args.toArray(new String[args.size()]));
					}
				} catch (IOException e) {
					if ( listenSocket != null ) {
						try {
							listenSocket.close();
						} catch (IOException ignore) {
						}
						listenSocket = null;
					}
					throw new CoreException( new Status(IStatus.ERROR, JSDebugPlugin.ID, IStatus.OK, Messages.JSLaunchConfigurationDelegate_LaunchProcessError, e));
				}
				
				// TODO: use separate thread
				Socket socket = null;
				try {
					monitor.subTask(StringUtils.format(Messages.JSLaunchConfigurationDelegate_OpeningSocketOnPort, port));
					socket = listenSocket.accept();
				} catch (IOException e) {
					BrowserUtil.resetBrowserCache(browserExecutable);
					if ( debug ) {
						throw new CoreException( new Status(IStatus.ERROR, JSDebugPlugin.ID, IStatus.OK, Messages.JSLaunchConfigurationDelegate_SocketConnectionError, e));
					}
				} finally {
					if ( listenSocket != null ) {
						try {
							listenSocket.close();
						} catch (IOException ignore) {
						}
					}
				}
				if ( socket != null ) {
					monitor.subTask(Messages.JSLaunchConfigurationDelegate_InitializingDebugger);
					JSDebugTarget debugTarget = null;
					try {
						JSDebugProcess debugProcess = new JSDebugProcess(launch, browserExecutable, null);
						DebugConnection controller = DebugConnection.createConnection(socket);
						debugTarget = new JSDebugTarget(launch, debugProcess, httpServer, resourceMapper, controller, debug);
						monitor.subTask(StringUtils.format(Messages.JSLaunchConfigurationDelegate_OpeningPage, launchURL));
						debugTarget.openURL(launchURL);
					} catch (CoreException e) {
						JSDebugPlugin.log(e);
						if ( debugTarget != null ) {
							debugTarget.terminate();
						} else {
							try {
								socket.close();
							} catch (IOException ignore) {
							}
						}
						throw e;
					}
				} else {
					DebugPlugin.newProcess(launch,process,browserExecutable);
				}
			} else if ( "run".equals(mode) ) { //$NON-NLS-1$
				try {
					String launchPage = launchURL.toExternalForm();				
					if ( Platform.OS_MACOSX.equals(Platform.getOS()) ) {
						args.add("/usr/bin/open"); //$NON-NLS-1$
						if ( System.getProperty("os.version",StringUtils.EMPTY).startsWith("10.3.") ) { //$NON-NLS-1$ //$NON-NLS-2$
							args.add("-a"); //$NON-NLS-1$
							args.add(browserExecutable);
						} else {
							args.add("-b"); //$NON-NLS-1$
							args.add(BrowserUtil.getMacOSXApplicationIdentifier(browserExecutable));
						}
						args.add(launchPage);
					}
					else 
					{
						args.add(browserExecutable);
						if (debugCompatible && Firefox.isBrowserExecutable(browserExecutable)) {
							if ( advancedRun ) {
								args.add(Firefox.NEW_WINDOW);
								browserArgs.remove(Firefox.NEW_WINDOW);
								browserArgs.remove(Firefox.NEW_TAB);
							} else {
								if (browserArgs.contains(Firefox.NEW_WINDOW)) {
									args.add(Firefox.NEW_WINDOW);
								} else {
									args.add(Firefox.NEW_TAB);
								}
								browserArgs.remove(Firefox.NEW_WINDOW);
								browserArgs.remove(Firefox.NEW_TAB);
							}
						}
						args.add(launchPage);
					}
					
					args.addAll(browserArgs);
					process = Runtime.getRuntime().exec( (String[]) args.toArray(new String[args.size()]));

				} catch (IOException e) {
					throw new CoreException( new Status(IStatus.ERROR, JSDebugPlugin.ID, IStatus.OK, Messages.JSLaunchConfigurationDelegate_LaunchProcessError, e));
				}
				DebugPlugin.newProcess(launch,process,browserExecutable);

			} else {
				throw new CoreException( new Status(IStatus.ERROR, JSDebugPlugin.ID, IStatus.OK, StringUtils.format(Messages.JSLaunchConfigurationDelegate_ConfiguredBrowserDoesNotSupportDebugging, browserExecutable), null));
			}
		} catch (CoreException e) {
			/* Shutdown HTTP server on error if launched */
			if ( httpServer != null ) {
				launch.removeProcess(httpServer);
				try {
					httpServer.terminate();
				} catch (DebugException e1) {
					IdeLog.logError(JSDebugPlugin.getDefault(), StringUtils.EMPTY, e1);
				}
			}
			throw e;
		}
	}
			
	protected IResource getCurrentEditorResource() throws MalformedURLException {
		IActiveResourcePathGetterAdapter adapter = (IActiveResourcePathGetterAdapter) getContributedAdapter(IActiveResourcePathGetterAdapter.class);
		if ( adapter != null ) {
			return adapter.getActiveResource();
		}
		return null;
	}

	protected IPath getCurrentEditorPath() throws MalformedURLException {
		IActiveResourcePathGetterAdapter adapter = (IActiveResourcePathGetterAdapter) getContributedAdapter(IActiveResourcePathGetterAdapter.class);
		if ( adapter != null ) {
			return adapter.getActiveResourcePath();
		}
		return null;
	}

	protected URL getCurrentEditorURL() throws MalformedURLException {
		IActiveResourcePathGetterAdapter adapter = (IActiveResourcePathGetterAdapter) getContributedAdapter(IActiveResourcePathGetterAdapter.class);
		if ( adapter != null ) {
			return adapter.getActiveResourceURL();
		}
		return null;
	}

	protected Object getContributedAdapter( Class clazz ) {
		Object adapter = null;
		IAdapterManager manager = Platform.getAdapterManager();
		if (manager.hasAdapter(this, clazz.getName())) {
			adapter = manager.getAdapter(this,clazz.getName());
			if ( adapter == null ) {
				adapter = manager.loadAdapter(this, clazz.getName());
			}
		}
		return adapter;
	}	
}
