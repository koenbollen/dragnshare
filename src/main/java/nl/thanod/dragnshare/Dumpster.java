/**
 * 
 */
package nl.thanod.dragnshare;

import it.koen.dragnshare.net.MulticastShare;
import it.koen.dragnshare.net.Receiver;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * @author nilsdijk
 */
public class Dumpster extends JFrame implements MulticastShare.Listener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7192301082111534982L;

	protected volatile int count = 0;

	protected final MulticastShare sharer;

	private final JPanel filelist;

	public Dumpster(final FileShare share) {
		this.sharer = new MulticastShare();
		try {
			this.sharer.connect();
		} catch (IOException ball) {
			ball.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not connect the sharer", "oh noos!", JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}
		this.sharer.start();

		this.filelist = new JPanel();
		filelist.setLayout(new BoxLayout(filelist, BoxLayout.Y_AXIS));
		new FileDrop(this, new FileDrop.Listener() {
			@Override
			public void filesDropped(File[] files) {
				for (final File file : files) {
					sharer.share(file);
					if (share != null)
						share.share(file);
					addSharedFile(new SharedFile() {
						@Override
						public File getFile() {
							return file;
						}

						@Override
						public String getName() {
							return file.getName();
						}

						@Override
						public float getProgress() {
							return 0;
						}
					});
				}

			}
		});

		this.sharer.addMulticastListener(this);

		JPanel container = new JPanel(new BorderLayout());
		container.setOpaque(false);
		container.add(this.filelist, BorderLayout.NORTH);
		JScrollPane jsp = new JScrollPane(container);
		jsp.setBorder(null);
		add(jsp);

		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

		setAlwaysOnTop(true);
		setSize(400, 300);
		setLocationRelativeTo(null);
	}

	public static void main(String... args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable ball) {
			ball.printStackTrace();
		}

		final Dumpster dump = new Dumpster(new FileShare() {
			@Override
			public void share(File file) {
				System.out.println("Share " + file);
			}
		});

		final SystemTray tray = SystemTray.getSystemTray();
		final TrayIcon trayIcon = new TrayIcon(createImage("dragn.png", tray.getTrayIconSize()));
		final TrayMenu menu = new TrayMenu(dump);
		trayIcon.setPopupMenu(menu);
		
		trayIcon.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if( e.getButton() == MouseEvent.BUTTON1 )
					dump.setVisible(!dump.isVisible());
			}
		});

		try {
			tray.add(trayIcon);
		} catch (AWTException ball) {
			ball.printStackTrace();
		}
	}

	/**
	 * @param string
	 * @return
	 */
	private static Image createImage(String string, Dimension size) {
		try {
			URL url = Dumpster.class.getClassLoader().getResource(string);
			BufferedImage img = ImageIO.read(url);
			if( size != null )
			{
				BufferedImage scaled = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = scaled.createGraphics();
				graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				graphics.drawImage(img, 0, 0, size.width, size.height, null);
				graphics.dispose();
				return scaled;
			}
			return img;
		} catch (Throwable ball) {
			ball.printStackTrace();
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * it.koen.dragnshare.net.MulticastShare.Listener#onReceive(it.koen.dragnshare
	 * .net.Receiver)
	 */
	@Override
	public void onReceive(final Receiver receiver) {
		addSharedFile(new ReceivedSharedFile(receiver));
	}
	
	protected void addSharedFile(SharedFile shared) {
		this.filelist.add(new ShareInfo(shared, count++));
		this.filelist.revalidate();
	}
}