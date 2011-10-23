/**
 * 
 */
package nl.thanod.dragnshare.net;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author nilsdijk
 *
 */
public interface Sharer {
	interface Listener {
		void offered(Receiver receiver);
	}
	
	public static class Listeners implements Sharer.Listener {
		Set<Sharer.Listener> listeners = Collections.synchronizedSet(new HashSet<Sharer.Listener>());

		/* (non-Javadoc)
		 * @see nl.thanod.dragnshare.net.Sharer.Listener#offered(nl.thanod.dragnshare.net.Receiver)
		 */
		@Override
		public void offered(Receiver receiver) {
			Set<Sharer.Listener> listeners = new HashSet<Sharer.Listener>(this.listeners);
			for(Sharer.Listener l:listeners)
				l.offered(receiver);
		}
		
		public boolean addListener(Sharer.Listener listener){
			if (listener == null)
				return false;
			return this.listeners.add(listener);
		}
		
		public boolean removeListener(Sharer.Listener listener){
			return this.listeners.remove(listener);
		}
	}
	
	Sender share(File file) throws IOException;
	
	Thread start() throws IOException;

	Listeners listeners();
}
