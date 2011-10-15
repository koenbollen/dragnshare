package nl.thanod.dragnshare;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
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
	private final Clipboard clipboard;
	private final List<FileDrop.Listener> listeners;

	public Clipper(Component watch, FileDrop.Listener listener)
	{
		this.listeners = new ArrayList<FileDrop.Listener>();
		this.clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		this.listeners.add( listener );
		watch.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e)
			{
				System.out.println(e);
				boolean meta = false;
				if (OS.isOSX() && e.isMetaDown())
					meta = true;
				else if (e.isControlDown())
					meta = true;
				if (meta && e.getKeyCode() == KeyEvent.VK_V)
					Clipper.this.paste();
			}
		});
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
