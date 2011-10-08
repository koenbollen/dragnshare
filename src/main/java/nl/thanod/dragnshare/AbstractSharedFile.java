/**
 * 
 */
package nl.thanod.dragnshare;

import java.util.LinkedList;
import java.util.List;

/**
 * @author nilsdijk
 *
 */
public abstract class AbstractSharedFile implements SharedFile {
	private List<ShareListener> listeners = new LinkedList<ShareListener>();
	
	
	protected void invokeProgressOnListeners(float progress){
		List<ShareListener> listeners = new LinkedList<ShareListener>(this.listeners);
		for (ShareListener listener:listeners)
			listener.progress(progress);
	}
	
	@Override
	public void addShareListener(ShareListener listener){
		if (this.listeners.contains(listener))
			return;
		this.listeners.add(listener);
	}

	@Override
	public void removeShareListener(ShareListener listener){
		this.listeners.remove(listener);
	}
}
