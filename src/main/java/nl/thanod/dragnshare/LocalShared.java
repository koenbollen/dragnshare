/**
 * 
 */
package nl.thanod.dragnshare;

import java.io.File;
import java.util.Observable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import nl.thanod.dragnshare.net.Sender;
import nl.thanod.dragnshare.net.Sender.Listener;
import nl.thanod.util.Settings;

/**
 * @author nilsdijk
 */
public class LocalShared extends Observable implements SharedFile, Listener {

	private final Sender sender;

	private final AtomicInteger shareCount = new AtomicInteger();
	private final AtomicInteger currentCount = new AtomicInteger();

	protected ShareInfo view;

	private ScheduledFuture<?> remover;

	/**
	 * @param sender
	 */
	public LocalShared(Sender sender) {
		this.sender = sender;
		this.sender.listeners().addListener(this);
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getFile()
	 */
	@Override
	public File getFile() {
		return this.sender.getFile();
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getName()
	 */
	@Override
	public String getName() {
		return getFile().getName();
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getProgress()
	 */
	@Override
	public float getProgress() {
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getColorScheme()
	 */
	@Override
	public ColorScheme getColorScheme() {
		return ColorScheme.OFFERED;
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#isReady()
	 */
	@Override
	public boolean isReady() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#cancel()
	 */
	@Override
	public void cancel() {
		this.sender.cancel();
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#shouldStart()
	 */
	@Override
	public boolean shouldStart() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#start()
	 */
	@Override
	public void start() {
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getStatus()
	 */
	@Override
	public String getStatus() {
		int active = this.currentCount.get();
		return "Shared with " + this.shareCount.get() + " computers" + (active > 0 ? " (" + this.currentCount.get() + " active)" : "");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * nl.thanod.dragnshare.SharedFile#setView(nl.thanod.dragnshare.ShareInfo)
	 */
	@Override
	public void setView(ShareInfo view) {
		this.view = view;
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#remove()
	 */
	@Override
	public void remove() {
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * nl.thanod.dragnshare.net.Sender.Listener#onSending(nl.thanod.dragnshare
	 * .net.Sender)
	 */
	@Override
	public void onSending(Sender sender) {
		addOne();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * nl.thanod.dragnshare.net.Sender.Listener#onSent(nl.thanod.dragnshare.
	 * net.Sender)
	 */
	@Override
	public void onSent(Sender sender) {
		finishedOne();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * nl.thanod.dragnshare.net.Sender.Listener#onError(nl.thanod.dragnshare
	 * .net.Sender, java.lang.Exception)
	 */
	@Override
	public void onError(Sender sender, Exception ball) {
		finishedOne();
	}

	private void addOne() {
		if (this.remover != null){
			this.remover.cancel(true);
			this.remover = null;
		}
		this.shareCount.incrementAndGet();
		this.currentCount.incrementAndGet();

		setChanged();
		notifyObservers();
	}

	private void finishedOne() {
		int i = this.currentCount.decrementAndGet();
		if (i == 0 && Settings.instance.getBool("autoClear")) {
			int autoCleartimeout = Settings.instance.getInt("autoClearTimeout",10);
			if (this.remover != null){
				this.remover.cancel(true);
				this.remover = null;
			}
			this.remover = DropZone.executor.schedule(new Runnable() {

				@Override
				public void run() {
					LocalShared.this.view.remove();
				}
			}, autoCleartimeout, TimeUnit.SECONDS);
		}

		setChanged();
		notifyObservers();
	}

}
