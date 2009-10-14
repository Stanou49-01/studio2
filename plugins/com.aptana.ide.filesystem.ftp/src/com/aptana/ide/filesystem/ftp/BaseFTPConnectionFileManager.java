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

package com.aptana.ide.filesystem.ftp;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import com.aptana.ide.core.IdeLog;
import com.aptana.ide.core.StringUtils;
import com.aptana.ide.core.URLEncoder;
import com.aptana.ide.core.io.CoreIOPlugin;
import com.aptana.ide.core.io.InfiniteProgressMonitor;
import com.aptana.ide.core.io.vfs.ExtendedFileInfo;
import com.aptana.ide.core.io.vfs.IConnectionFileManager;
import com.aptana.ide.core.io.vfs.IExtendedFileInfo;
import com.aptana.ide.core.io.vfs.IExtendedFileStore;

/**
 * @author Max Stepanov
 *
 */
public abstract class BaseFTPConnectionFileManager implements IConnectionFileManager {

	protected static final int TIMEOUT = 20000;
	protected static final int RETRY = 3;
	protected static final int RETRY_DELAY = 5000;
	protected static final int KEEPALIVE_INTERVAL = 15000;
	protected static final int TRANSFER_BUFFER_SIZE = 32768;
	protected static final int CHECK_CONNECTION_TIMEOUT = 30000;
	protected static final int CACHE_TTL = 60000; /* 1min */
	protected static final String TMP_UPLOAD_PREFIX = ".tmp_upload."; //$NON-NLS-1$
	protected static final Pattern PASS_COMMAND_PATTERN = Pattern.compile("^(.*PASS ).+$"); //$NON-NLS-1$

	protected String host;
	protected int port;
	protected String login;
	protected char[] password;
	protected IPath basePath;
	protected String authId;
	
	private long lastOperationTime;

	private Map<IPath, ExtendedFileInfo> fileInfoCache;
	private Map<IPath, ExtendedFileInfo[]> fileInfosCache;

	protected void promptPassword(String title, String message) {
		password = CoreIOPlugin.getAuthenticationManager().promptPassword(
						authId, login, title, message);
		if (password == null) {
			throw new OperationCanceledException();
		}
	}

	protected void getOrPromptPassword(String title, String message) {
		password = CoreIOPlugin.getAuthenticationManager().getPassword(authId);
		if (password == null) {
			promptPassword(title, message);
		}
	}
	
	protected synchronized void setCaching(boolean enabled) {
		if ((fileInfoCache != null) == enabled) {
			return;
		}
		if (enabled) {
			fileInfoCache = new ExpiringMap<IPath, ExtendedFileInfo>(CACHE_TTL);
			fileInfosCache = new ExpiringMap<IPath, ExtendedFileInfo[]>(CACHE_TTL);
		} else {
			fileInfoCache = null;
			fileInfosCache = null;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#fetchInfo(org.eclipse.core.runtime.IPath, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized IExtendedFileInfo fetchInfo(IPath path, int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(StringUtils.format(Messages.BaseFTPConnectionFileManager_gethering_details, path.toPortableString()), 2);
		try {
			ExtendedFileInfo fileInfo = getCachedFileInfo(path);
			if (fileInfo == null) {
				testOrConnect(monitor);
				try {
					fileInfo = fetchAndCacheFileInfo(path, options, monitor);
				} finally {
					setLastOperationTime();
				}
			}
			return (IExtendedFileInfo) fileInfo.clone();
		} finally {
			monitor.done();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#childInfos(org.eclipse.core.runtime.IPath, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized IExtendedFileInfo[] childInfos(IPath path, int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(StringUtils.format(Messages.BaseFTPConnectionFileManager_gethering_details, path.toPortableString()), 2);
		options = (options & IExtendedFileStore.DETAILED);
		try {
			ExtendedFileInfo[] fileInfos = getCachedFileInfos(path);
			if (fileInfos == null) {
				testOrConnect(monitor);
				try {
					fileInfos = cache(path, fetchFiles(basePath.append(path), options, monitor));
					for (ExtendedFileInfo fileInfo : fileInfos) {
						postProcessFileInfo(fileInfo, path, options, monitor);
						cache(path.append(fileInfo.getName()), fileInfo);
					}
				} catch (FileNotFoundException e) {
					return new IExtendedFileInfo[0];
				} finally {
					setLastOperationTime();
				}
			}
			return fileInfos.clone();
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#childNames(org.eclipse.core.runtime.IPath, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized String[] childNames(IPath path, int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(StringUtils.format(Messages.BaseFTPConnectionFileManager_listing_directory, path.toPortableString()), 2);
		try {
			ExtendedFileInfo[] fileInfos = getCachedFileInfos(path);
			if (fileInfos != null) {
				List<String> list = new ArrayList<String>();
				for (ExtendedFileInfo fileInfo : fileInfos) {
					list.add(fileInfo.getName());
				}
				return list.toArray(new String[list.size()]);
			}
			testOrConnect(monitor);
			try {
				return listDirectory(basePath.append(path), monitor);
			} finally {
				setLastOperationTime();
			}
		} catch (FileNotFoundException e) {
			return new String[0];
		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#openInputStream(org.eclipse.core.runtime.IPath, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized InputStream openInputStream(IPath path, int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(StringUtils.format(Messages.BaseFTPConnectionFileManager_opening_file, path.toPortableString()), 3);
		testOrConnect(monitor);
		try {
			ExtendedFileInfo fileInfo = fetchAndCacheFileInfo(path, Policy.subMonitorFor(monitor, 1));
			if (!fileInfo.exists()) {
				throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
						Messages.BaseFTPConnectionFileManager_no_such_file, new FileNotFoundException(path.toPortableString())));
			}
			if (fileInfo.isDirectory()) {
				throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
						Messages.BaseFTPConnectionFileManager_file_is_directory, new FileNotFoundException(path.toPortableString())));				
			}
			if (fileInfo.getLength() == 0) {
				return new ByteArrayInputStream(new byte[0]);
			}
			return readFile(basePath.append(path), Policy.subMonitorFor(monitor, 1));			
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
					Messages.BaseFTPConnectionFileManager_no_such_file, new FileNotFoundException(path.toPortableString())));
		} finally {
			setLastOperationTime();
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#openOutputStream(org.eclipse.core.runtime.IPath, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized OutputStream openOutputStream(IPath path, int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(StringUtils.format(Messages.BaseFTPConnectionFileManager_opening_file, path.toPortableString()), 3);
		testOrConnect(monitor);
		try {
			ExtendedFileInfo fileInfo = fetchAndCacheFileInfo(path, Policy.subMonitorFor(monitor, 1));
			if (fileInfo.exists() && fileInfo.isDirectory()) {
				throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
						Messages.BaseFTPConnectionFileManager_file_is_directory, new FileNotFoundException(path.toPortableString())));				
			}
			clearCache(path);
			return writeFile(basePath.append(path), Policy.subMonitorFor(monitor, 1));
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
					Messages.BaseFTPConnectionFileManager_parent_doesnt_exist, new FileNotFoundException(path.toPortableString())));
		} finally {
			setLastOperationTime();
			monitor.done();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#delete(org.eclipse.core.runtime.IPath, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized void delete(IPath path, int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		monitor = new InfiniteProgressMonitor(monitor);
		monitor.beginTask(Messages.BaseFTPConnectionFileManager_deleting, 20);
		testOrConnect(monitor);
		try {
			ExtendedFileInfo fileInfo = getCachedFileInfo(path);
			if (fileInfo == null) {
				fileInfo = fetchAndCacheFileInfo(path, Policy.subMonitorFor(monitor, 1));
			}
			if (!fileInfo.exists()) {
				return;
			}
			Policy.checkCanceled(monitor);
			try {
				if (fileInfo.isDirectory()) {
					deleteDirectory(basePath.append(path), monitor);
				} else {
					deleteFile(basePath.append(path), monitor);				
				}
			} catch (FileNotFoundException ignore) {
			} finally {
				clearCache(path);
			}
		} finally {
			setLastOperationTime();
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#mkdir(org.eclipse.core.runtime.IPath, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized void mkdir(IPath path, int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(StringUtils.format(Messages.BaseFTPConnectionFileManager_creating_folder, path.toPortableString()), 3);
		testOrConnect(monitor);
		try {
			ExtendedFileInfo fileInfo = fetchAndCacheFileInfo(path, Policy.subMonitorFor(monitor, 1));
			if (fileInfo.exists()) {
				if (!fileInfo.isDirectory()) {
					throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
							Messages.BaseFTPConnectionFileManager_file_already_exists, new FileNotFoundException(path.toPortableString())));				
				}
				return;
			}
			if ((options & EFS.SHALLOW) != 0 && path.segmentCount() > 1) {
				fileInfo = fetchAndCacheFileInfo(path.removeLastSegments(1), Policy.subMonitorFor(monitor, 1));
				if (!fileInfo.exists()) {
					throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
							Messages.BaseFTPConnectionFileManager_parent_doesnt_exist, new FileNotFoundException(path.toPortableString())));					
				}
				if (!fileInfo.isDirectory()) {
					throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
							Messages.BaseFTPConnectionFileManager_parent_is_not_directory, new FileNotFoundException(path.toPortableString())));				
				}
				createDirectory(basePath.append(path), Policy.subMonitorFor(monitor, 1));
			} else if (path.segmentCount() == 1) {
				createDirectory(basePath.append(path), Policy.subMonitorFor(monitor, 1));
			} else {
				IProgressMonitor subMonitor = Policy.subMonitorFor(monitor, 1);
				subMonitor.beginTask(Messages.BaseFTPConnectionFileManager_creating_folders, path.segmentCount());
				for (int i = path.segmentCount() - 1; i >= 0; --i) {
					createDirectory(basePath.append(path).removeLastSegments(i), subMonitor);
					subMonitor.worked(1);
				}
				subMonitor.done();
			}
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
					Messages.BaseFTPConnectionFileManager_parent_doesnt_exist, e));
		} finally {
			setLastOperationTime();
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#putInfo(org.eclipse.core.runtime.IPath, org.eclipse.core.filesystem.IFileInfo, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized void putInfo(IPath path, IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(StringUtils.format(Messages.BaseFTPConnectionFileManager_putting_changes, path.toPortableString()), 5);
		testOrConnect(monitor);
		try {
			if ((options & EFS.SET_LAST_MODIFIED) != 0) {
				setModificationTime(basePath.append(path), info.getLastModified(), Policy.subMonitorFor(monitor, 1));
			}
			if ((options & EFS.SET_ATTRIBUTES) != 0 && (options & IExtendedFileInfo.SET_PERMISSIONS) == 0) {
				ExtendedFileInfo fileInfo = fetchAndCacheFileInfo(path, Policy.subMonitorFor(monitor, 1));
				if (fileInfo.exists()) {
					long permissions = fileInfo.getPermissions();
					if (!info.getAttribute(EFS.ATTRIBUTE_READ_ONLY)) {
						permissions |= IExtendedFileInfo.PERMISSION_OWNER_WRITE;
					} else {
						permissions &= ~IExtendedFileInfo.PERMISSION_OWNER_WRITE;
					}
					if (info.getAttribute(EFS.ATTRIBUTE_EXECUTABLE)) {
						permissions |= IExtendedFileInfo.PERMISSION_OWNER_EXECUTE;
					} else {
						permissions &= ~IExtendedFileInfo.PERMISSION_OWNER_EXECUTE;
					}
					changeFilePermissions(basePath.append(path), permissions, Policy.subMonitorFor(monitor, 1));
				}
			}
			if (info instanceof IExtendedFileInfo) {
				IExtendedFileInfo extInfo = (IExtendedFileInfo) info;
				if ((options & IExtendedFileInfo.SET_PERMISSIONS) != 0) {
					changeFilePermissions(basePath.append(path), extInfo.getPermissions(), Policy.subMonitorFor(monitor, 1));
				}
				if ((options & IExtendedFileInfo.SET_GROUP) != 0) {
					changeFileGroup(basePath.append(path), extInfo.getGroup(), Policy.subMonitorFor(monitor, 1));
				}
			}
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
					Messages.BaseFTPConnectionFileManager_no_such_file, new FileNotFoundException(path.toPortableString())));
		} finally {
			clearCache(path);
			setLastOperationTime();
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#move(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath, int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized void move(IPath sourcePath, IPath destinationPath, int options, IProgressMonitor monitor) throws CoreException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(StringUtils.format(Messages.BaseFTPConnectionFileManager_moving, sourcePath.toPortableString()), 5);
		testOrConnect(monitor);
		try {
			ExtendedFileInfo fileInfo = fetchAndCacheFileInfo(sourcePath, Policy.subMonitorFor(monitor, 1));
			if (!fileInfo.exists()) {
				throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
						Messages.BaseFTPConnectionFileManager_no_such_file, new FileNotFoundException(sourcePath.toPortableString())));
			}
			boolean isDirectory = fileInfo.isDirectory();
			fileInfo = fetchAndCacheFileInfo(destinationPath, Policy.subMonitorFor(monitor, 1));
			if (fileInfo.exists()) {
				if ((options & EFS.OVERWRITE) == 0) {
					throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
							Messages.BaseFTPConnectionFileManager_file_already_exists, new FileNotFoundException(destinationPath.toPortableString())));
				}
				if (fileInfo.isDirectory() != isDirectory) {
					throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
							Messages.BaseFTPConnectionFileManager_cant_move));				
				}
			} else {
				fileInfo = fetchAndCacheFileInfo(destinationPath.removeLastSegments(1), Policy.subMonitorFor(monitor, 1));
				if (!fileInfo.exists()) {
					throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
							Messages.BaseFTPConnectionFileManager_parent_doesnt_exist, new FileNotFoundException(destinationPath.toPortableString())));					
				}

			}
			clearCache(sourcePath);
			clearCache(destinationPath);
			renameFile(basePath.append(sourcePath), basePath.append(destinationPath), Policy.subMonitorFor(monitor, 2));
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, FTPPlugin.PLUGIN_ID,
					Messages.BaseFTPConnectionFileManager_no_such_file, new FileNotFoundException(sourcePath.toPortableString())));
		} finally {
			setLastOperationTime();
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.vfs.IConnectionFileManager#getCanonicalURI(org.eclipse.core.runtime.IPath)
	 */
	public URI getCanonicalURI(IPath path) {
		// TODO:max - trace links here
		return getRootCanonicalURI().resolve(URLEncoder.encode(basePath.append(path).toPortableString(), null, null));
	}

	protected abstract void checkConnected() throws Exception;
	protected abstract URI getRootCanonicalURI();

	// all methods here accept absolute path
	protected abstract ExtendedFileInfo fetchFile(IPath path, int options, IProgressMonitor monitor) throws CoreException, FileNotFoundException;
	protected abstract ExtendedFileInfo[] fetchFiles(IPath path, int options, IProgressMonitor monitor) throws CoreException, FileNotFoundException;
	protected abstract String[] listDirectory(IPath path, IProgressMonitor monitor) throws CoreException, FileNotFoundException;
	protected abstract InputStream readFile(IPath path, IProgressMonitor monitor) throws CoreException, FileNotFoundException;
	protected abstract OutputStream writeFile(IPath path, IProgressMonitor monitor) throws CoreException, FileNotFoundException;
	protected abstract void deleteFile(IPath path, IProgressMonitor monitor) throws CoreException, FileNotFoundException;
	protected abstract void deleteDirectory(IPath path, IProgressMonitor monitor) throws CoreException, FileNotFoundException;
	protected abstract void createDirectory(IPath path, IProgressMonitor monitor) throws CoreException, FileNotFoundException;
	protected abstract void renameFile(IPath sourcePath, IPath destinationPath, IProgressMonitor monitor) throws CoreException, FileNotFoundException;

	protected abstract void setModificationTime(IPath path, long modificationTime, IProgressMonitor monitor) throws CoreException, FileNotFoundException;
	protected abstract void changeFilePermissions(IPath path, long permissions, IProgressMonitor monitor) throws CoreException, FileNotFoundException;
	protected abstract void changeFileGroup(IPath path, String group, IProgressMonitor monitor) throws CoreException, FileNotFoundException;

	private ExtendedFileInfo fetchAndCacheFileInfo(IPath path, IProgressMonitor monitor) throws CoreException {
		return fetchAndCacheFileInfo(path, EFS.NONE, monitor);
	}

	private ExtendedFileInfo fetchAndCacheFileInfo(IPath path, int options, IProgressMonitor monitor) throws CoreException {
		ExtendedFileInfo fileInfo;
		try {
			fileInfo = fetchFile(basePath.append(path), options, monitor);
		} catch (FileNotFoundException e) {
			fileInfo = new ExtendedFileInfo(path.segmentCount() > 0 ? path.lastSegment() : Path.ROOT.toPortableString());
			fileInfo.setExists(false);
			return fileInfo;
		}
		if (path.segmentCount() == 0) {
			fileInfo.setName(Path.ROOT.toPortableString());
		}
		postProcessFileInfo(fileInfo, path, options, monitor);
		return cache(path, fileInfo);
	}
	
	private void postProcessFileInfo(ExtendedFileInfo fileInfo, IPath dirPath, int options, IProgressMonitor monitor) throws CoreException {
		if (fileInfo.getAttribute(EFS.ATTRIBUTE_SYMLINK)) {
			ExtendedFileInfo targetFileInfo = resolveSymlink(dirPath, fileInfo.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), options, monitor);
			fileInfo.setExists(targetFileInfo.exists());
			if (targetFileInfo.exists()) {
				fileInfo.setDirectory(targetFileInfo.isDirectory());
				fileInfo.setLength(targetFileInfo.getLength());
				fileInfo.setLastModified(targetFileInfo.getLastModified());
				fileInfo.setOwner(targetFileInfo.getOwner());
				fileInfo.setGroup(targetFileInfo.getGroup());
				fileInfo.setPermissions(targetFileInfo.getPermissions());
			}
		}
		long permissions = fileInfo.getPermissions();
		fileInfo.setAttribute(EFS.ATTRIBUTE_READ_ONLY, (permissions & IExtendedFileInfo.PERMISSION_OWNER_WRITE) == 0);
		fileInfo.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, (permissions & IExtendedFileInfo.PERMISSION_OWNER_EXECUTE) != 0);
	}
	
	private ExtendedFileInfo resolveSymlink(IPath dirPath, String linkTarget, int options, IProgressMonitor monitor) throws CoreException {
		Set<IPath> visited = new HashSet<IPath>();
		visited.add(dirPath);
		while (linkTarget != null && linkTarget.length() > 0) {
			IPath targetPath = Path.fromPortableString(linkTarget);
			if (!targetPath.isAbsolute()) {
				targetPath = dirPath.append(targetPath);
			}
			if (visited.contains(targetPath)) {
				break;
			}
			visited.add(targetPath);
			ExtendedFileInfo targetFileInfo = getCachedFileInfo(targetPath);
			if (targetFileInfo == null) {
				try {
					Policy.checkCanceled(monitor);
					targetFileInfo = fetchFile(targetPath, options, Policy.subMonitorFor(monitor, 1));
				} catch (FileNotFoundException e) {
					targetFileInfo = new ExtendedFileInfo();
				}
			}
			cache(targetPath, targetFileInfo);
			if (targetFileInfo.getAttribute(EFS.ATTRIBUTE_SYMLINK)) {
				linkTarget = targetFileInfo.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET);
				dirPath = targetPath.removeLastSegments(1);
				continue;
			}
			return targetFileInfo;
		}
		return new ExtendedFileInfo();
	}
	
	private ExtendedFileInfo getCachedFileInfo(IPath path) {
		return fileInfoCache != null ? fileInfoCache.get(path) : null;
	}

	private ExtendedFileInfo[] getCachedFileInfos(IPath path) {
		return fileInfosCache !=  null ? fileInfosCache.get(path) : null;
	}

	protected ExtendedFileInfo cache(IPath path, ExtendedFileInfo fileInfo) {
		if (fileInfoCache != null && fileInfo.exists()) {
			fileInfoCache.put(path, fileInfo);
		}
		return fileInfo;
	}

	protected ExtendedFileInfo[] cache(IPath path, ExtendedFileInfo[] fileInfos) {
		if (fileInfosCache != null) {
			fileInfosCache.put(path, fileInfos);
		}
		return fileInfos;
	}

	protected void clearCache(IPath path) {
		int segments = path.segmentCount();
		if (fileInfoCache !=  null) {
			for (IPath p : new ArrayList<IPath>(fileInfoCache.keySet())) {
				if (p.segmentCount() >= segments && path.matchingFirstSegments(p) == segments) {
					fileInfoCache.remove(p);
				}
			}
		}
		if (fileInfosCache != null) {
			for (IPath p : new ArrayList<IPath>(fileInfosCache.keySet())) {
				if (p.segmentCount() >= segments && path.matchingFirstSegments(p) == segments) {
					fileInfosCache.remove(p);
				}
			}
		}
	}

	protected void cleanup() {
		if (fileInfoCache != null) {
			fileInfoCache.clear();
		}
		if (fileInfosCache != null) {
			fileInfosCache.clear();
		}
	}
	
	private void testOrConnect(IProgressMonitor monitor) throws CoreException {
		Policy.checkCanceled(monitor);
		testConnection();
		if (!isConnected()) {
			connect(Policy.subMonitorFor(monitor, 1));
			Policy.checkCanceled(monitor);
		}

	}
	
	private void testConnection() {
		if (!isConnected()) {
			return;
		}
		if (System.currentTimeMillis() - lastOperationTime > CHECK_CONNECTION_TIMEOUT) {
			try {
				checkConnected();
			} catch (Exception e) {
				IdeLog.logImportant(FTPPlugin.getDefault(), Messages.BaseFTPConnectionFileManager_connection_check_failed, e);
			}
		}
		if (isConnected()) {
			setLastOperationTime();
		}
	}
	
	private void setLastOperationTime() {
		lastOperationTime = System.currentTimeMillis();		
	}

}
