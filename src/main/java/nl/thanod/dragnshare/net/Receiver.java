/**
 * 
 */
package nl.thanod.dragnshare.net;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author nilsdijk
 */
public interface Receiver extends Share {
	public interface Listener {
		void onProgress(Receiver receiver, long size, long received, long speed);

		void onStart(Receiver receiver);

		void onCompleted(Receiver receiver);

		void onError(Receiver receiver, Exception cause);
	}

	public static class Listeners implements Listener {
		private Set<Receiver.Listener> listeners = Collections.synchronizedSet(new HashSet<Receiver.Listener>());

		public boolean addListener(Receiver.Listener listener) {
			return this.listeners.add(listener);
		}

		public boolean removeListener(Receiver.Listener listener) {
			return this.listeners.remove(listener);
		}

		/*
		 * (non-Javadoc)
		 * @see nl.thanod.dragnshare.net.Receiver.Listener#onProgress(nl.thanod.
		 * dragnshare.net.Receiver, long, long, long)
		 */
		@Override
		public void onProgress(Receiver receiver, long size, long received, long speed) {
			Set<Receiver.Listener> listeners = new HashSet<Receiver.Listener>(this.listeners);
			for (Receiver.Listener l : listeners)
				l.onProgress(receiver, size, received, speed);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * nl.thanod.dragnshare.net.Receiver.Listener#onStart(nl.thanod.dragnshare
		 * .net.Receiver)
		 */
		@Override
		public void onStart(Receiver receiver) {
			Set<Receiver.Listener> listeners = new HashSet<Receiver.Listener>(this.listeners);
			for (Receiver.Listener l : listeners)
				l.onStart(receiver);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * nl.thanod.dragnshare.net.Receiver.Listener#onCompleted(nl.thanod.
		 * dragnshare.net.Receiver)
		 */
		@Override
		public void onCompleted(Receiver receiver) {
			Set<Receiver.Listener> listeners = new HashSet<Receiver.Listener>(this.listeners);
			for (Receiver.Listener l : listeners)
				l.onCompleted(receiver);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * nl.thanod.dragnshare.net.Receiver.Listener#onError(nl.thanod.dragnshare
		 * .net.Receiver, java.io.IOException)
		 */
		@Override
		public void onError(Receiver receiver, Exception cause) {
			Set<Receiver.Listener> listeners = new HashSet<Receiver.Listener>(this.listeners);
			for (Receiver.Listener l : listeners)
				l.onError(receiver, cause);
		}

	}

	Receiver.Listeners listeners();

	void accept() throws IOException;

	void cancel();

	boolean isAccepted();

	long getSize();

}
