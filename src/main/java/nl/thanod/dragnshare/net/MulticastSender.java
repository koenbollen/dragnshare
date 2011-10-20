/**
 * 
 */
package nl.thanod.dragnshare.net;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author nilsdijk
 */
public class MulticastSender implements Sender {

	public static int BUFFER_SIZE = 64 * 1024;

	class Sending implements Runnable {
		private InetSocketAddress addr;

		public Sending(InetSocketAddress addr) {
			this.addr = addr;
		}

		@Override
		public void run() {
			MulticastSender.this.listeners.onSending(MulticastSender.this);
			Socket s = new Socket();
			try {
				s.connect(addr);

				File f = MulticastSender.this.file;
				if (f.isFile())
					sendFile(s, null, f);
				else if (f.isDirectory())
					sendDir(s, f.getParentFile().toURI(), f);
				
				MulticastSender.this.listeners.onSent(MulticastSender.this);
			} catch (Exception ball) {
				ball.printStackTrace();
				MulticastSender.this.listeners.onError(MulticastSender.this, ball); 
			} finally {
				try {
					if (s != null)
						s.close();
				} catch (IOException ball) {
				}
			}
		}

		/**
		 * @param socket
		 * @param dir
		 * @throws IOException 
		 */
		private void sendDir(Socket socket, URI root, File dir) throws IOException {
			for (File f : dir.listFiles()) {
				if (f.isFile())
					sendFile(socket, root, f);
				if (f.isDirectory())
					sendDir(socket, root, f);
			}
		}

		/**
		 * @param socket
		 * @param file
		 * @throws IOException
		 */
		private void sendFile(Socket socket, URI root, File file) throws IOException {
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());

			String name = file.getName();
			if (root != null) {
				name = root.relativize(file.toURI()).getPath();
			}
			long length;
			InputStream in = null;
			try {
				in = new FileInputStream(file);
				
				out.writeUTF(name);
				out.writeLong(length = file.length());
				
				byte[] buffer = new byte[MulticastSender.BUFFER_SIZE];
				while (length > 0) {
					int read = in.read(buffer);
					if (read > 0) {
						out.write(buffer, 0, read);
						length -= read;
					}
					if (read < 0)
						throw new EOFException(name);
				}
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ball) {
					}
				}
			}
		}
	}

	private final Message message;
	protected final File file;
	protected final Listeners listeners = new Listeners();
	
	private final List<Thread> threads = Collections.synchronizedList(new LinkedList<Thread>()); 

	/**
	 * @param multicastSharer
	 * @param message
	 */
	public MulticastSender(MulticastSharer multicastSharer, File file, Message message) {
		this.file = file;
		this.message = message;
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Shared#getMessage()
	 */
	@Override
	public Message getMessage() {
		return this.message;
	}

	public void process(InetAddress addr, int port) {
		InetSocketAddress target = new InetSocketAddress(addr,port);
		Sending s = new Sending(target);
		Thread t = new Thread(s, "Sender " + this.file.getName() + " " + target);
		t.setDaemon(true);
		t.start();
		this.threads.add(t);
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Sender#cancel()
	 */
	@Override
	public void cancel() {
		for (Thread t:this.threads)
			t.interrupt();
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Sender#listeners()
	 */
	@Override
	public Listeners listeners() {
		return this.listeners ;
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Share#getFile()
	 */
	@Override
	public File getFile() {
		return this.file;
	}

}
