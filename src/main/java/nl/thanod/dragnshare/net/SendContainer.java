/**
 * 
 */
package nl.thanod.dragnshare.net;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

import nl.thanod.dragnshare.ColorScheme;
import nl.thanod.dragnshare.SharedFile;

/**
 * @author nilsdijk
 *
 */
public class SendContainer extends Observable implements SharedFile, MulticastShare.Listener {
	
	private final File file;
	private final List<Sender> sender = Collections.synchronizedList(new LinkedList<Sender>());
	private MulticastShare share;
	
	public SendContainer(MulticastShare share, File file){
		this.file = file;
		this.share = share;
		this.share.addMulticastListener(this);
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getFile()
	 */
	@Override
	public File getFile() {
		return this.file;
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getName()
	 */
	@Override
	public String getName() {
		return this.getFile().getName();
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getProgress()
	 */
	@Override
	public float getProgress() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getColorScheme()
	 */
	@Override
	public ColorScheme getColorScheme() {
		return ColorScheme.OFFERED;
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#isReady()
	 */
	@Override
	public boolean isReady() {
		return true;
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#remove()
	 */
	@Override
	public void remove() {
		for (Sender sender:this.sender)
			sender.interrupt();
		this.sender.clear();
		this.share.removeMulticastListener(this);
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#shouldStart()
	 */
	@Override
	public boolean shouldStart() {
		return false;
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#start()
	 */
	@Override
	public void start() {
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.SharedFile#getStatus()
	 */
	@Override
	public String getStatus() {
		int c = this.sender.size();
		return "sharing with " + c + " computer" + (c != 1 ? "s":"");
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.MulticastShare.Listener#onReceive(nl.thanod.dragnshare.net.Receiver)
	 */
	@Override
	public void onReceive(Receiver receiver) { }

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.MulticastShare.Listener#onSending(nl.thanod.dragnshare.net.Sender)
	 */
	@Override
	public void onSending(Sender sender) {
		if (!sender.getFile().equals(this.file))
			return;
		this.sender.add(sender);
		
		setChanged();
		notifyObservers();
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.MulticastShare.Listener#onSent(nl.thanod.dragnshare.net.Sender)
	 */
	@Override
	public void onSent(Sender sender) {
		if (!sender.getFile().equals(this.file))
			return;
		this.sender.remove(sender);
		
		setChanged();
		notifyObservers();
	}

}
