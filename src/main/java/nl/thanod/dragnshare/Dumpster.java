/**
 * 
 */
package nl.thanod.dragnshare;

import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.*;

import nl.thanod.DebugUtil;

/**
 * @author nilsdijk
 */
public class Dumpster extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7192301082111534982L;

	protected int count = 0;

	public Dumpster(final FileShare share) {
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		new FileDrop(this, new FileDrop.Listener() {
			@Override
			public void filesDropped(File[] files) {
				for (final File file : files) {
					if (share != null)
						share.share(file);
					panel.add(new ShareInfo(new AbstractSharedFile() {
						@Override
						public File getFile() {
							return file;
						}

						@Override
						public void safeTo(File file) {
							System.out.println("saving to " + file);
						}

						@Override
						public String getName() {
							return file.getName();
						}

						@Override
						public float getProgress() {
							return 0;
						}
					}, count++));
					panel.revalidate();
				}

			}
		});

		JPanel container = new JPanel(new BorderLayout());
		container.setOpaque(false);
		container.add(panel,BorderLayout.NORTH);
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
		
		final TrayIcon trayIcon = new TrayIcon(createImage("tray.gif"));
        final SystemTray tray = SystemTray.getSystemTray();
        
        trayIcon.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent paramActionEvent) {
				
			}
		});
        
        trayIcon.addMouseMotionListener(DebugUtil.getPrintingImplementation(MouseMotionListener.class));
        
        trayIcon.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent paramMouseEvent) {
				dump.setVisible(true);
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
	private static Image createImage(String string) {
		try {
			URL url = Dumpster.class.getClassLoader().getResource(string);
			return ImageIO.read(url);
		} catch (Throwable ball) {
			ball.printStackTrace();
			return null;
		}
	}
}