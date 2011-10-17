/**
 * 
 */
package nl.thanod.dragnshare;


import java.io.File;
import java.io.IOException;
import java.util.Observable;

import nl.thanod.dragnshare.net.Receiver;
import nl.thanod.dragnshare.notify.Notifier;
import nl.thanod.dragnshare.ui.Tray;
import nl.thanod.util.FileUtils;

/**
 * @author nilsdijk
 */
public class ReceivedSharedFile extends Observable implements SharedFile, Receiver.Listener {

	private Receiver receiver;
	private File file;
	private volatile float progress;
	private ColorScheme colorScheme = ColorScheme.RECEIVED;
	private boolean ready;
	private String speed = "";
	private long speedHist = 0;
	private String remain = "";
	private String error = null;

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
	public void onProgress(File result, String filename, long filesize, long received, long speed) {
		if( speed == 0 )
		{
			this.speed = "";
			this.remain = "";
		}
		else
		{
			this.speedHist = (long)(this.speedHist * .5 + speed * .5);
			speed = this.speedHist;
			this.speed = FileUtils.humanizeBytes( speed ) + "/s";
			
			long time = (filesize - received) / speed;
			
			StringBuffer sb = new StringBuffer(" ( ");
			sb.append( FileUtils.humanizeTime(time) );
			sb.append( " left )" );
			this.remain = sb.toString();
		}
		
		
		
		this.updateProgress((float) received / (float) filesize);
	}

	/*
	 * (non-Javadoc)
	 * @see it.koen.dragnshare.net.Receiver.Listener#onCompleted(java.io.File,
	 * java.lang.String, long)
	 */
	@Override
	public void onCompleted(File result, String filename, long filesize) {
		Notifier.Factory.notify(Notifier.Type.RECEIVED, "Received", "You received " + result.getName());
		Tray.findTray().setDecorator("add");
		this.updateProgress(1f);
	}

	/**
	 * @param f
	 */
	private void updateProgress(float progress) {
		if (progress < 1){
			this.colorScheme = ColorScheme.RECEIVED;
			this.ready = false;
		}else{
			this.colorScheme = ColorScheme.DEFAULT;
			this.ready = true;
		}
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
		this.colorScheme = ColorScheme.ERROR;
		this.ready = false;
		this.error  = e.getMessage();
		Tray t = Tray.findTray();
		t.setDecorator("exclamation");
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

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#isReady()
	 */
	@Override
	public boolean isReady() {
		return this.ready;
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#remove()
	 */
	@Override
	public void remove() {
		this.receiver.interrupt();
		// TODO: Make sure directories are removed.
		FileUtils.clean( this.receiver.getTarget() );
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#shouldStart()
	 */
	@Override
	public boolean shouldStart() {
		return !this.receiver.isStarted();
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#start()
	 */
	@Override
	public void start() {
		this.receiver.start();
		setChanged();
		notifyObservers();
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getStatus()
	 */
	@Override
	public String getStatus() {
		if( this.error != null )
			return "error: " + this.error;
		if (!this.receiver.isStarted())
			return "waiting for accept";
		if (this.getProgress() < 1f)
			return "downloading " + this.speed + this.remain;
		return "done";
	}
}
