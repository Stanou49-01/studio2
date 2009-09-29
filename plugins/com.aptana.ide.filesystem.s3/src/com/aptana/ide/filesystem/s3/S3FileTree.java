package com.aptana.ide.filesystem.s3;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileTree;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileTree;

import com.amazon.s3.ListEntry;

/**
 * A more efficient way of accessing/querying the S3 file tree. Since when we query root for it's children we end up
 * with entire hierarchy below it (not just direct descendants), just store all the sub-tree entries and generate
 * IFileInfo and IFileStore's from it.
 * 
 * @author cwilliams
 */
public class S3FileTree extends FileTree implements IFileTree
{

	private List<ListEntry> entries;

	public S3FileTree(IFileStore treeRoot, List<ListEntry> entries)
	{
		super(treeRoot);
		this.entries = entries;
	}

	@Override
	public IFileInfo[] getChildInfos(IFileStore store)
	{
		if (!(store instanceof S3FileStore))
			return null;
		List<ListEntry> matches = getChildEntries(store);
		List<IFileInfo> infos = new ArrayList<IFileInfo>();
		for (ListEntry match : matches)
		{
			FileInfo fileInfo = generateFileInfo(match);
			// TODO these will always be files. What about "parent directories" that these keys imply?
			infos.add(fileInfo);
		}
		return infos.toArray(new IFileInfo[infos.size()]);
	}

	private FileInfo generateFileInfo(ListEntry match)
	{
		String name = match.key;
		int lastSlash = name.lastIndexOf("/");
		boolean isDirectory = false;
		if (lastSlash != -1)
		{
			if (lastSlash == (name.length() - 1))
			{
				name = name.substring(0, lastSlash);
				isDirectory = true;
				lastSlash = name.lastIndexOf("/");
				if (lastSlash != -1)
					name = name.substring(lastSlash + 1);
			}
			else
			{
				name = name.substring(lastSlash);
				if (name.startsWith("/"))
					name = name.substring(1);
			}
		}

		FileInfo fileInfo = new FileInfo(name);
		fileInfo.setExists(true);
		fileInfo.setLastModified(match.lastModified.getTime());
		fileInfo.setLength(match.size);
		fileInfo.setDirectory(isDirectory);
		return fileInfo;
	}

	private List<ListEntry> getChildEntries(IFileStore store)
	{
		S3FileStore s3Store = (S3FileStore) store;
		String key = s3Store.getKey();
		if (key.startsWith("/"))
			key = key.substring(1);
		// Find all the entries that have the key as prefix
		List<ListEntry> matches = new ArrayList<ListEntry>();
		for (ListEntry entry : entries)
		{
			String relative = entry.key;
			if (relative.startsWith("/"))
				relative = relative.substring(1);
			if (!(key.length() == 0 || relative.startsWith(key + "/")))
				continue;
			// Only limit to direct children!
			relative = relative.substring(key.length());
			if (relative.startsWith("/"))
				relative = relative.substring(1);
			if (relative.endsWith("/"))
				relative = relative.substring(0, relative.length() - 1);
			if (relative.length() == 0 || relative.indexOf("/") != -1)
				continue;
			matches.add(entry);
		}
		return matches;
	}

	@Override
	public IFileStore[] getChildStores(IFileStore store)
	{
		if (!(store instanceof S3FileStore))
			return null;
		S3FileStore s3Store = (S3FileStore) store;
		List<ListEntry> matches = getChildEntries(s3Store);
		List<IFileStore> childrenStores = new ArrayList<IFileStore>();
		for (ListEntry match : matches)
		{
			String childName = match.key;
			if (childName.endsWith("/"))
				childName = childName.substring(0, childName.length() - 1);
			childrenStores.add(s3Store.getChild(childName));
		}
		return childrenStores.toArray(new IFileStore[childrenStores.size()]);
	}

	@Override
	public IFileInfo getFileInfo(IFileStore store)
	{
		if (!(store instanceof S3FileStore))
			return null;
		S3FileStore s3Store = (S3FileStore) store;
		String key = s3Store.getKey();
		// generate an info from a ListEntry if we have a match!
		for (ListEntry entry : entries)
		{
			String entryKey = entry.key;
			if (entryKey.startsWith("/"))
				entryKey = entryKey.substring(1);
			if (entryKey.endsWith("/"))
				entryKey = entryKey.substring(0, entryKey.length() - 1);
			if (entryKey.equals(key))
				return generateFileInfo(entry);
		}
		return s3Store.fetchInfo();
	}

}
