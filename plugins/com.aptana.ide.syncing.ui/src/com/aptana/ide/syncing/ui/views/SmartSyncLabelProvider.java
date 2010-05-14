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
 * with certain Eclipse Public Licensed code and certain additional terms
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
package com.aptana.ide.syncing.ui.views;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.aptana.ide.core.io.syncing.SyncState;
import com.aptana.ide.core.io.syncing.VirtualFileSyncPair;
import com.aptana.ide.core.ui.CoreUIPlugin;
import com.aptana.ide.syncing.ui.SyncingUIPlugin;

/**
 * Label and color provider for the smart sync viewer.
 * 
 * @author Kevin Sawicki (ksawicki@aptana.com)
 * @author Michael Xia (mxia@aptana.com)
 */
public class SmartSyncLabelProvider implements ITableLabelProvider, ITableColorProvider
{

	private FileLabelProvider fImages;

	private Color fNewColor;
	private Color fOverwriteColor;
	private Color fUpdateColor;
	private Color fDeleteColor;
	private Color fMixedColor;
	private Display fDisplay;

	private int fSyncDirection;
	private int fPresentationType;
	private boolean fDeleteRemoteFiles;
	private boolean fDeleteLocalFiles;

	/**
	 * Constructor.
	 */
	public SmartSyncLabelProvider(Display display)
	{
		fDisplay = display;
		fImages = new FileLabelProvider();
		fNewColor = new Color(fDisplay, new RGB(216, 191, 216));
		fOverwriteColor = new Color(fDisplay, new RGB(255, 165, 0));
		fUpdateColor = new Color(fDisplay, new RGB(173, 216, 230));
		fDeleteColor = fDisplay.getSystemColor(SWT.COLOR_RED);
		fMixedColor = new Color(fDisplay, new RGB(164, 164, 164));
	}

	/**
	 * Sets the sync direction.
	 * 
	 * @param direction
	 *            the direction for doing the sync
	 */
	public void setSyncDirection(int direction)
	{
		fSyncDirection = direction;
	}

	/**
	 * Sets the presentation type.
	 * 
	 * @param type
	 *            the type of presentation for the viewer
	 */
	public void setPresentationType(int type)
	{
		fPresentationType = type;
	}

	/**
	 * Sets the indication of if deleting remote files is selected.
	 * 
	 * @param delete
	 *            true if deleting remote files is selected, false otherwise
	 */
	public void setDeleteRemoteFiles(boolean delete)
	{
		fDeleteRemoteFiles = delete;
	}

	/**
	 * Sets the indication of if deleting local files is selected.
	 * 
	 * @param delete
	 *            true if deleting local files is selected, false otherwise
	 */
	public void setDeleteLocalFiles(boolean delete)
	{
		fDeleteLocalFiles = delete;
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener)
	{
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener)
	{
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property)
	{
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose()
	{
		fImages.dispose();
		disposeColor(fNewColor);
		disposeColor(fOverwriteColor);
		disposeColor(fUpdateColor);
		disposeColor(fMixedColor);
	}

	/**
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	public String getColumnText(Object element, int columnIndex)
	{
		if (element instanceof ISyncResource)
		{
			ISyncResource resource = (ISyncResource) element;
			switch (columnIndex)
			{
				case 0:
					if (fPresentationType == OptionsToolBar.TREE_VIEW)
					{
						return ((ISyncResource) element).getName();
					}
					if (fPresentationType == OptionsToolBar.FLAT_VIEW)
					{
						return ((ISyncResource) element).getPath().toString();
					}
					// if (fPresentationType == OptionsToolBar.COMPRESSED_VIEW)
					// {
					// if (resource instanceof SyncFile)
					// {
					// return ((ISyncResource) element).getName();
					// }
					// if (resource instanceof SyncFolder)
					// {
					// SyncFolder folder = (SyncFolder) resource;
					// if (folder.getParent() != null &&
					// !folder.getParent().containsFiles())
					// {
					// return ((ISyncResource)
					// element).getPath().toString();
					// }
					// return ((ISyncResource) element).getName();
					// }
					// }
				case 2:
					if (resource.isSkipped())
					{
						return Messages.SmartSyncDialog_Skipped;
					}
					if (fSyncDirection == DirectionToolBar.FORCE_UPLOAD || fSyncDirection == DirectionToolBar.UPLOAD)
					{
						return ""; //$NON-NLS-1$
					}

					switch (resource.getSyncState())
					{
						case SyncState.ServerItemIsNewer:
							if (fSyncDirection == DirectionToolBar.FORCE_DOWNLOAD)
							{
								return Messages.SmartSyncDialog_Overwrite;
							}
							return Messages.SmartSyncDialog_Update;
						case SyncFolder.MIXED:
							return Messages.SmartSyncDialog_Modified;
						case SyncState.ClientItemDeleted:
						case SyncState.ClientItemOnly:
							return fDeleteLocalFiles ? Messages.SmartSyncDialog_Delete : ""; //$NON-NLS-1$
						case SyncState.ServerItemOnly:
							if (fSyncDirection == DirectionToolBar.FORCE_DOWNLOAD
									|| fSyncDirection == DirectionToolBar.DOWNLOAD || !fDeleteRemoteFiles)
							{
								return Messages.SmartSyncDialog_New;
							}
						default:
							return ""; //$NON-NLS-1$
					}
				case 3:
					if (resource.isSkipped())
					{
						return Messages.SmartSyncDialog_Skipped;
					}
					if (fSyncDirection == DirectionToolBar.FORCE_DOWNLOAD
							|| fSyncDirection == DirectionToolBar.DOWNLOAD)
					{
						return ""; //$NON-NLS-1$
					}

					switch (resource.getSyncState())
					{
						case SyncState.ClientItemIsNewer:
							if (fSyncDirection == DirectionToolBar.FORCE_UPLOAD)
							{
								return Messages.SmartSyncDialog_Overwrite;
							}
							return Messages.SmartSyncDialog_Update;
						case SyncFolder.MIXED:
							return Messages.SmartSyncDialog_Modified;
						case SyncState.ServerItemDeleted:
						case SyncState.ServerItemOnly:
							return fDeleteRemoteFiles ? Messages.SmartSyncDialog_Delete : ""; //$NON-NLS-1$
						case SyncState.ClientItemOnly:
							if (fSyncDirection == DirectionToolBar.FORCE_UPLOAD
									|| fSyncDirection == DirectionToolBar.UPLOAD || !fDeleteLocalFiles)
							{
								return Messages.SmartSyncDialog_New;
							}
						default:
							return ""; //$NON-NLS-1$
					}
				case 4:
					VirtualFileSyncPair pair = resource.getPair();
					return pair == null ? "" : getModificationDate(pair.getSourceFile()); //$NON-NLS-1$
				case 5:
					pair = resource.getPair();
					return pair == null ? "" : getModificationDate(pair.getDestinationFile()); //$NON-NLS-1$
				default:
					return ""; //$NON-NLS-1$
			}
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	public Image getColumnImage(Object element, int columnIndex)
	{
		if (columnIndex == 0)
		{
			if (element instanceof ISyncResource)
			{
				ISyncResource resource = (ISyncResource) element;
				int state = resource.getTransferState();

				if (state == ISyncResource.ERROR)
				{
					return SyncingUIPlugin.getImage("icons/full/obj16/no.png"); //$NON-NLS-1$
				}
				if (state == ISyncResource.SYNCED)
				{
					return SyncingUIPlugin.getImage("icons/full/obj16/ok.png"); //$NON-NLS-1$
				}
				return SyncingUIPlugin.getImage("icons/full/obj16/empty.png"); //$NON-NLS-1$
			}
			if (element instanceof SyncFile)
			{
				SyncFile file = (SyncFile) element;
				if (file.getPair() != null)
				{
					if (file.getPair().getSourceFile() != null)
					{
						return fImages.getImage(file.getPair().getSourceFile());
					}
					if (file.getPair().getDestinationFile() != null)
					{
						return fImages.getImage(file.getPair().getDestinationFile());
					}
				}
			}
			else if (element instanceof SyncFolder)
			{
				return CoreUIPlugin.getImage("icons/full/obj16/folder.gif"); //$NON-NLS-1$
			}
		}
		else if (columnIndex == 1)
		{
			if (element instanceof ISyncResource)
			{
				ISyncResource resource = (ISyncResource) element;
				if (resource.isSkipped())
				{
					return SyncingUIPlugin.getImage("icons/full/obj16/checked.gif"); //$NON-NLS-1$
				}
				return SyncingUIPlugin.getImage("icons/full/obj16/unchecked.gif"); //$NON-NLS-1$
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ITableColorProvider#getBackground(java.lang.Object, int)
	 */
	public Color getBackground(Object element, int columnIndex)
	{
		if (element instanceof ISyncResource)
		{
			ISyncResource resource = (ISyncResource) element;
			Color color = fDisplay.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
			switch (columnIndex)
			{
				case 2:
					if (resource.isSkipped())
					{
						return color;
					}
					if (fSyncDirection == DirectionToolBar.FORCE_UPLOAD || fSyncDirection == DirectionToolBar.UPLOAD)
					{
						return null;
					}

					switch (resource.getSyncState())
					{
						case SyncState.ClientItemDeleted:
						case SyncState.ClientItemOnly:
							return fDeleteLocalFiles ? fDeleteColor : null;
						case SyncState.ServerItemOnly:
							if (fSyncDirection == DirectionToolBar.FORCE_DOWNLOAD
									|| fSyncDirection == DirectionToolBar.DOWNLOAD || !fDeleteRemoteFiles)
							{
								return fNewColor;
							}
							return null;
						case SyncState.ServerItemIsNewer:
							if (fSyncDirection == DirectionToolBar.FORCE_DOWNLOAD)
							{
								return fOverwriteColor;
							}
							return fUpdateColor;
						case SyncFolder.MIXED:
							return fMixedColor;
						default:
							return null;
					}
				case 3:
					if (resource.isSkipped())
					{
						return color;
					}
					if (fSyncDirection == DirectionToolBar.FORCE_DOWNLOAD
							|| fSyncDirection == DirectionToolBar.DOWNLOAD)
					{
						return null;
					}

					switch (resource.getSyncState())
					{
						case SyncState.ServerItemDeleted:
						case SyncState.ServerItemOnly:
							return fDeleteRemoteFiles ? fDeleteColor : null;
						case SyncState.ClientItemOnly:
							if (fSyncDirection == DirectionToolBar.FORCE_UPLOAD
									|| fSyncDirection == DirectionToolBar.UPLOAD || !fDeleteLocalFiles)
							{
								return fNewColor;
							}
							return null;
						case SyncState.ClientItemIsNewer:
							if (fSyncDirection == DirectionToolBar.FORCE_UPLOAD)
							{
								return fOverwriteColor;
							}
							return fUpdateColor;
						case SyncFolder.MIXED:
							return fMixedColor;
						default:
							return null;
					}
				default:
					return null;
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ITableColorProvider#getForeground(java.lang.Object, int)
	 */
	public Color getForeground(Object element, int columnIndex)
	{
		if (element instanceof ISyncResource)
		{
			ISyncResource resource = (ISyncResource) element;
			if (!resource.isSkipped())
			{
				Color color = fDisplay.getSystemColor(SWT.COLOR_WHITE);
				if (columnIndex == 2)
				{
					if ((resource.getSyncState() == SyncState.ClientItemOnly || resource.getSyncState() == SyncState.ClientItemDeleted)
							&& fDeleteLocalFiles
							&& (fSyncDirection == DirectionToolBar.BOTH || fSyncDirection == DirectionToolBar.DOWNLOAD || fSyncDirection == DirectionToolBar.FORCE_DOWNLOAD))
					{
						return color;
					}
				}
				else if (columnIndex == 3)
				{
					if ((resource.getSyncState() == SyncState.ServerItemOnly || resource.getSyncState() == SyncState.ServerItemDeleted)
							&& fDeleteRemoteFiles
							&& (fSyncDirection == DirectionToolBar.BOTH || fSyncDirection == DirectionToolBar.UPLOAD || fSyncDirection == DirectionToolBar.FORCE_UPLOAD))
					{
						return color;
					}
				}
			}
		}
		return null;
	}

	private static void disposeColor(Color color)
	{
		if (color != null && !color.isDisposed())
		{
			color.dispose();
		}
	}

	private static String getModificationDate(IFileStore file)
	{
		if (file == null)
		{
			return ""; //$NON-NLS-1$
		}
		Date d = new Date(file.fetchInfo().getLastModified());
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

		return df.format(d);
	}

}
