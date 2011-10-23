/**
 * 
 */
package nl.thanod.dragnshare.net;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author nilsdijk
 *
 */
public interface Sender extends Share {
	public interface Listener {
		void onSending(Sender sender);
		void onSent(Sender sender);
		void onError(Sender sender, Exception ball);
	}
	
	public static class Listeners implements Listener {
		private Set<Sender.Listener> listeners = Collections.synchronizedSet(new HashSet<Sender.Listener>());
		
		public boolean addListener(Sender.Listener listener){
			return this.listeners.add(listener);
		}
		
		public boolean removeListener(Sender.Listener listener){
			return this.listeners.remove(listener);
		}
		
		@Override
		public void onSending(Sender sender) {
			Set<Sender.Listener> listeners = new HashSet<Sender.Listener>(this.listeners);
			for (Sender.Listener l:listeners)
				l.onSending(sender);
		}

		@Override
		public void onSent(Sender sender) {
			Set<Sender.Listener> listeners = new HashSet<Sender.Listener>(this.listeners);
			for (Sender.Listener l:listeners)
				l.onSent(sender);
		}

		@Override
		public void onError(Sender sender, Exception ball) {
			Set<Sender.Listener> listeners = new HashSet<Sender.Listener>(this.listeners);
			for (Sender.Listener l:listeners)
				l.onError(sender, ball);
		}
		
	}
	
	Sender.Listeners listeners();
	Message getMessage();
	void cancel();
	long getSize();
	
}
