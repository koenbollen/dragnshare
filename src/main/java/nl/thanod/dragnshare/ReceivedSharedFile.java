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
	private final DropZone dropzone;

	/**
	 * @param receiver
	 */
	public ReceivedSharedFile(DropZone dropzone, Receiver receiver) {
		this.dropzone = dropzone;
		this.receiver = receiver;
		this.receiver.listeners().addListener(this);

		this.file = this.receiver.getFile();
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
		this.receiver.cancel();
		FileUtils.clean( this.receiver.getFile() );
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#shouldStart()
	 */
	@Override
	public boolean shouldStart() {
		return !this.receiver.isAccepted();
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#start()
	 */
	@Override
	public void start() {
		try {
			this.receiver.accept();
		} catch (IOException ball) {
			ball.printStackTrace();
		}
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
		if (!this.receiver.isAccepted())
			return "waiting for accept";
		if (this.getProgress() < 1f)
			return "downloading " + this.speed + this.remain;
		return "done";
	}

	@Override
	public void setView(ShareInfo view)
	{
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Receiver.Listener#onProgress(nl.thanod.dragnshare.net.Receiver, long, long, long)
	 */
	@Override
	public void onProgress(Receiver receiver, long size, long received, long speed) {
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
			
			long time = (size - received) / speed;
			
			StringBuffer sb = new StringBuffer(" ( ");
			sb.append( FileUtils.humanizeTime(time) );
			sb.append( " left )" );
			this.remain = sb.toString();
		}
		
		
		
		this.updateProgress((float) received / (float) size);
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Receiver.Listener#onStart(nl.thanod.dragnshare.net.Receiver)
	 */
	@Override
	public void onStart(Receiver receiver) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Receiver.Listener#onCompleted(nl.thanod.dragnshare.net.Receiver)
	 */
	@Override
	public void onCompleted(Receiver receiver) {
		Notifier.Factory.notify(Notifier.Type.RECEIVED, "Received", "You received " + receiver.getFile().getName());
		if (!this.dropzone.isFocused())
			this.dropzone.tray.setDecorator("add");
		this.updateProgress(1f);
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Receiver.Listener#onError(nl.thanod.dragnshare.net.Receiver, java.lang.Exception)
	 */
	@Override
	public void onError(Receiver receiver, Exception cause) {
		cause.printStackTrace();
		updateProgress(1f);
		this.colorScheme = ColorScheme.ERROR;
		this.ready = false;
		this.error  = cause.getMessage();
		Tray t = Tray.findTray();
		t.setDecorator("exclamation");
		setChanged();
		notifyObservers();
	}
}
