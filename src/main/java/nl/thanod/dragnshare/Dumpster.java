/**
 * 
 */
package nl.thanod.dragnshare;

import it.koen.dragnshare.net.MulticastShare;
import it.koen.dragnshare.net.Receiver;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import nl.thanod.util.Settings;

/**
 * @author nilsdijk
 */
public class Dumpster extends JDialog implements MulticastShare.Listener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7192301082111534982L;

	protected volatile int count = 0;

	protected final MulticastShare sharer;

	private final JPanel filelist;

	private TrayIcon trayIcon;
	private BufferedImage defaultIcon;
	private BufferedImage newIcon;

	public Dumpster() {
		super((Frame)null,"Drag'n Share");
		this.sharer = new MulticastShare();
		try {
			this.sharer.connect();
		} catch (IOException ball) {
			ball.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not connect the sharer", "oh noos!", JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}
		this.sharer.start();
		
		this.setupTray();
		
		this.setModal(true);
		this.setResizable(false);

		this.filelist = new JPanel();
		filelist.setLayout(new BoxLayout(filelist, BoxLayout.Y_AXIS));
		new FileDrop(this, new FileDrop.Listener() {
			@Override
			public void filesDropped(File[] files) {
				for (final File file : files) {
					sharer.share(file);
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
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent e)
			{
				Settings.instance.setLocation(Dumpster.this.getLocation());
			}
		});
		
		Settings.instance.addListener(new Settings.Adapter(){
			@Override
			public void preStore(Settings instance)
			{
				instance.setLocation(Dumpster.this.getLocation());
			}
		});
		
		setAlwaysOnTop(true);
		setSize(400, 300);
		setLocationRelativeTo(null);
	}
	
	private void setupTray()
	{
		final SystemTray tray = SystemTray.getSystemTray();
		
		Dimension size = tray.getTrayIconSize();
		BufferedImage add = createImage("add.png", null);
		this.defaultIcon = createImage("dragn.png", size);
		this.newIcon = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = this.newIcon.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.drawImage(this.defaultIcon, 0, 0, size.width, size.height, null);
		graphics.drawImage(add,size.width-add.getWidth(), size.height-add.getHeight(), add.getWidth(), add.getHeight(), null);
		graphics.dispose();
		
		this.trayIcon = new TrayIcon(this.defaultIcon);
		this.trayIcon.setImageAutoSize(true);
		final TrayMenu menu = new TrayMenu(this);
		trayIcon.setPopupMenu(menu);

		this.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e)
			{
				Dumpster.this.trayIcon.setImage(defaultIcon);
			}
		});
		
		trayIcon.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if( e.getButton() == MouseEvent.BUTTON1 )
				{
					if( !Dumpster.this.isVisible() )
					{
						Dumpster.this.trayIcon.setImage(defaultIcon);
						Point p = Settings.instance.getLocation();
						if( p != null )
							Dumpster.this.setLocation(p);
					}
					Dumpster.this.setVisible(!Dumpster.this.isVisible());
				}
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
	private static BufferedImage createImage(String string, Dimension size) {
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
		if( !this.hasFocus() )
			this.trayIcon.setImage(newIcon);
		addSharedFile(new ReceivedSharedFile(receiver));
	}
	
	protected void addSharedFile(SharedFile shared) {
		this.filelist.add(new ShareInfo(shared, count++));
		this.filelist.revalidate();
	}

	public static void main(String... args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable ball) {
			ball.printStackTrace();
		}

		new Dumpster();
	}
	
}