/**
 * 
 */
package nl.thanod.dragnshare;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FileTransferable implements Transferable
{
	final private List<File> files;
	final private DataFlavor[] flavors;

	public static final DataFlavor TEXT_URI_FLAVOR;

	static
	{
		DataFlavor tmp = null;
		try
		{
			tmp = new DataFlavor("text/uri-list;class=" + String.class.getName());
		} catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		;
		TEXT_URI_FLAVOR = tmp;
	}

	/**
	 * A drag-and-drop object for transfering a file.
	 * 
	 * @param file
	 *            file to transfer -- this file should already exist, otherwise
	 *            it may not be accepted by drag targets.
	 */
	public FileTransferable(Collection<File> files)
	{
		this.files = Collections.unmodifiableList(new ArrayList<File>(files));
		this.flavors = new DataFlavor[] { DataFlavor.javaFileListFlavor, TEXT_URI_FLAVOR };
	}

	static FileTransferable createFileInTempDirectory(String filename) throws IOException
	{
		File dir = new File(System.getProperty("java.io.tmpdir"));
		File f = new File(dir, filename);
		f.createNewFile();
		return new FileTransferable(Collections.singletonList(f));
	}

	public List<File> getFiles()
	{
		return this.files;
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
	{
		if (!isDataFlavorSupported(flavor))
			return null;
		if (DataFlavor.javaFileListFlavor.equals(flavor))
			return this.files;
		if (flavor.equals(TEXT_URI_FLAVOR))
		{
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < this.files.size(); i++)
			{
				File file = this.files.get(i);
				sb.append(file.toURI());
				if (i != this.files.size() - 1)
					sb.append("\r\n");
			}

			return sb.toString();
		}
		return null;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors()
	{
		return this.flavors;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
		for (DataFlavor f : this.flavors)
			if (f != null && f.equals(flavor))
				return true;
		return false;

	}
}