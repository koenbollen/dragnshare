package nl.thanod.dragnshare;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import nl.thanod.util.OS;

/**
 * @author Koen Bollen <meneer@koenbollen.nl>
 */
public class Clipper
{
	private final DropZone dropzone;
	private final Clipboard clipboard;
	private final List<FileDrop.Listener> listeners;

	public Clipper(DropZone dropzone, FileDrop.Listener listener)
	{
		this.dropzone = dropzone;
		this.listeners = new ArrayList<FileDrop.Listener>();
		this.clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		this.listeners.add( listener );
		this.dropzone.list.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e)
			{
				if( e.isConsumed() )
					return;
				
				boolean meta = false;
				if (OS.isOSX() && e.isMetaDown())
					meta = true;
				else if (!OS.isOSX() && e.isControlDown())
					meta = true;
				
				if (meta && e.getKeyCode() == KeyEvent.VK_V)
					Clipper.this.paste();
				if (meta && ( e.getKeyCode() == KeyEvent.VK_X || e.getKeyCode() == KeyEvent.VK_C ) )
					Clipper.this.copy();
			}
		});
	}

	protected void copy()
	{
		List<ShareInfo> selected = this.dropzone.list.getSelector().getSelected();
		List<File> files = new ArrayList<File>();
		for( ShareInfo si : selected )
			if( !files.contains(si.getSharedFile().getFile()) )
					files.add( si.getSharedFile().getFile() );
		System.out.println(files.toString());
		FileTransferable ft = new FileTransferable(files);
		this.clipboard.setContents(ft, null);
	}

	protected void paste()
	{
		Transferable tr = this.clipboard.getContents(null);
		if (tr == null)
			return;
		if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
		{
			try
			{
				java.util.List<?> fileList = (java.util.List<?>) tr.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);

				java.io.File[] filesTemp = new java.io.File[fileList.size()];
				fileList.toArray(filesTemp);
				final java.io.File[] files = filesTemp;

				for (FileDrop.Listener l : new ArrayList<FileDrop.Listener>(this.listeners))
					if (l != null)
						l.filesDropped(files);
			} catch (UnsupportedFlavorException e)
			{
				e.printStackTrace();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		} else
		{
			// linux
			for (DataFlavor df : tr.getTransferDataFlavors())
			{
				if (df.isRepresentationClassReader())
				{
					Reader reader;
					try
					{
						reader = df.getReaderForText(tr);
					} catch (Exception e)
					{
						continue;
					}

					BufferedReader br = new BufferedReader(reader);

					for (FileDrop.Listener l : this.listeners)
						if (l != null)
							l.filesDropped(FileDrop.createFileArray(br, null));

					break;
				}
			}
		}
	}

}
