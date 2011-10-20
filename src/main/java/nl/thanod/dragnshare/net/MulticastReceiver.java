/**
 * 
 */
package nl.thanod.dragnshare.net;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import nl.thanod.dragnshare.net.Message.Type;
import nl.thanod.util.FileUtils;

/**
 * @author nilsdijk
 */
public class MulticastReceiver implements Receiver, Runnable {

	public final Message message;
	private final MulticastSharer sharer;
	private InetAddress source;
	private ServerSocket socket;
	private Receiver.Listeners listeners = new Receiver.Listeners();
	private long speed = -1;
	private int total;
	private long starttime;
	private Thread thread;
	private final File file;
	private File temp;

	/**
	 * @param source
	 * @param multicastSharer
	 * @param m
	 * @throws IOException 
	 */
	public MulticastReceiver(MulticastSharer sharer, InetAddress source, Message message) throws IOException {
		this.message = message;
		this.source = source;
		this.sharer = sharer;

		this.temp = FileUtils.createDragnShareTemp();
		
		this.file = new File(this.temp, this.message.name);
		if (message.type == Type.FILE){
			this.file.getParentFile().mkdirs();
			this.file.createNewFile();
		} else if (message.type == Type.DIRECTORY){
			this.file.mkdirs();
		}
		
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Receiver#start()
	 */
	@Override
	public void accept() throws IOException {
		if (this.thread != null)
			throw new IllegalStateException("already started");
		// make thread,
		this.socket = new ServerSocket(0, 1);
		this.socket.setSoTimeout(5000);

		this.thread = new Thread(this, "Receiver " + this.message.name + " " + this.source);
		this.thread.setDaemon(true);
		this.thread.start();

		this.sharer.send(Message.accept(this.message.id, this.socket.getLocalPort()), this.source);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		Socket s = null;
		try {
			s = this.socket.accept();
			this.socket.close();
			this.socket = null;

			this.total = 0;
			if (this.message.type == Type.FILE)
				readFile(s, this.temp);
			else if (this.message.type == Type.DIRECTORY)
				readDir(s, this.temp);

			if (this.message.size != this.total)
				throw new IOException("didn't receive enough bytes (expected: " + this.message.size + " got: " + this.total + ").");

			this.listeners.onProgress(this, this.message.size, this.total, 0);
			this.listeners.onCompleted(this);

		} catch (Exception ball) {
			ball.printStackTrace();
			this.listeners.onError(this, ball);
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (IOException ball) {
				}
			}

			if (this.socket != null) {
				try {
					this.socket.close();
				} catch (IOException ball) {
				}
			}
		}
	}

	/**
	 * @param s
	 * @param tmp
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private void readDir(Socket s, File root) throws IOException, InterruptedException {
		while (s.isConnected()) {
			readFile(s, root);
			this.listeners.onProgress(this, this.message.size, this.total, this.speed);
			if (this.total == this.message.size) {
				// received all
				break;
			}
		}
	}

	/**
	 * @param s
	 * @param root
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private long readFile(Socket s, File root) throws IOException, InterruptedException {
		DataInputStream in = new DataInputStream(s.getInputStream());

		String name = in.readUTF();
		long length = in.readLong();

		File target = new File(root, name);
		
		System.out.println(name + " saved to " + target);
		target.getParentFile().mkdirs();

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(target);

			byte[] buffer = new byte[MulticastSender.BUFFER_SIZE];

			final long updateTime = 500;
			long nextUpdate = System.currentTimeMillis() + updateTime;
			while (length > 0) {
				int len = (int) Math.min(buffer.length, length);
				int read = in.read(buffer, 0, len);
				if (read > 0) {
					out.write(buffer, 0, read);

					length -= read;
					this.total += read;
				}
				if (read < 0)
					throw new IOException("other side canceled");
				
				if (nextUpdate <= System.currentTimeMillis()) {
					updateSpeed();
					this.listeners.onProgress(this, this.message.size, this.total, this.speed);
					nextUpdate += updateTime;
				}

				if (read < buffer.length)
					Thread.sleep(1);
			}
			updateSpeed();
			this.listeners.onProgress(this, this.message.size, this.total, this.speed);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException ball) {
				}
			}
		}
		return total;
	}

	/**
	 * 
	 */
	private void updateSpeed() {
		long now = System.currentTimeMillis();
		if (this.starttime <= 0)
			this.starttime = now;
		long took = (now - this.starttime) / 1000;
		if (took == 0) {
			this.speed = 0;
		} else {
			this.speed = this.total / took;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Receiver#cancel()
	 */
	@Override
	public void cancel() {
		if (this.thread != null)
			this.thread.interrupt();
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Receiver#listeners()
	 */
	@Override
	public Receiver.Listeners listeners() {
		return this.listeners;
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Share#getFile()
	 */
	@Override
	public File getFile() {
		return this.file;
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Receiver#isAccepted()
	 */
	@Override
	public boolean isAccepted() {
		return this.thread != null;
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Receiver#getSize()
	 */
	@Override
	public long getSize() {
		return this.message.size;
	}
}
