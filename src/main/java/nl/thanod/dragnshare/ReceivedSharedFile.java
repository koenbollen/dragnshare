/**
 * 
 */
package nl.thanod.dragnshare;


import java.io.File;
import java.io.IOException;
import java.util.Observable;

import nl.thanod.dragnshare.DumpsterListCellRenderer.ColorScheme;
import nl.thanod.dragnshare.net.Receiver;

/**
 * @author nilsdijk
 */
public class ReceivedSharedFile extends Observable implements SharedFile, Receiver.Listener {

	private Receiver receiver;
	private File file;
	private volatile float progress;
	private ColorScheme colorScheme = ColorScheme.RECEIVED;

	/**
	 * @param receiver
	 */
	public ReceivedSharedFile(Receiver receiver) {
		this.receiver = receiver;
		this.receiver.addCompletionListener(this);

		this.file = this.receiver.getTarget();
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getFile()
	 */
	@Override
	public File getFile() {
		return this.file;
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getName()
	 */
	@Override
	public String getName() {
		return this.file.getName();
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getProgress()
	 */
	@Override
	public float getProgress() {
		return this.progress;
	}

	/*
	 * (non-Javadoc)
	 * @see it.koen.dragnshare.net.Receiver.Listener#onStart(java.io.File,
	 * java.lang.String, long)
	 */
	@Override
	public void onStart(File result, String filename, long filesize) {
	}

	/*
	 * (non-Javadoc)
	 * @see it.koen.dragnshare.net.Receiver.Listener#onProgress(java.io.File,
	 * java.lang.String, long, long)
	 */
	@Override
	public void onProgress(File result, String filename, long filesize, long received) {
		this.updateProgress((float) received / (float) filesize);
	}

	/*
	 * (non-Javadoc)
	 * @see it.koen.dragnshare.net.Receiver.Listener#onCompleted(java.io.File,
	 * java.lang.String, long)
	 */
	@Override
	public void onCompleted(File result, String filename, long filesize) {
		DragnShareNotifier.notify(DragnShareNotifier.Type.RECEIVED, "Received", "You received " + result.getName());
		this.updateProgress(1f);
	}

	/**
	 * @param f
	 */
	private void updateProgress(float progress) {
		if (progress < 1)
			colorScheme = ColorScheme.RECEIVED;
		else
			colorScheme = ColorScheme.DEFAULT;
		this.progress = progress;

		this.setChanged();
		this.notifyObservers();
	}

	/*
	 * (non-Javadoc)
	 * @see it.koen.dragnshare.net.Receiver.Listener#onError(java.io.File,
	 * java.lang.String, long, java.io.IOException)
	 */
	@Override
	public void onError(File target, String filename, long filesize, IOException e) {
		e.printStackTrace();
		updateProgress(1f);
		colorScheme = ColorScheme.ERROR;
		setChanged();
		notifyObservers();
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getColorScheme()
	 */
	@Override
	public ColorScheme getColorScheme() {
		return this.colorScheme ;
	}
}
