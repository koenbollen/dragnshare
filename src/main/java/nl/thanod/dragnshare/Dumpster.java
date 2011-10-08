/**
 * 
 */
package nl.thanod.dragnshare;

import it.koen.dragnshare.net.MulticastShare;
import it.koen.dragnshare.net.Receiver;

import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.*;

import nl.thanod.dragnshare.DumpsterListCellRenderer.ColorScheme;
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

	protected final DefaultListModel filelist;

	protected final JList list;

	protected TrayIcon trayIcon;
	protected BufferedImage defaultIcon;
	protected BufferedImage newIcon;

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

		this.filelist = new ObservingDefaultListModel();
		
		list = new JList(this.filelist);
		list.addMouseListener(new MouseAdapter() {
			/* (non-Javadoc)
			 * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
			 */
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2){
					if (Desktop.isDesktopSupported()){
						for (Object o:list.getSelectedValues()){
							if (o instanceof SharedFile){
								try {
									Desktop.getDesktop().open(((SharedFile)o).getFile());
								} catch (IOException ball) {
									ball.printStackTrace();
								}
							}
						}
					}
				}
			}
		});
		
		list.setCellRenderer(new DumpsterListCellRenderer());
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
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
							return 1;
						}

						@Override
						public ColorScheme getColorScheme() {
							return ColorScheme.OFFERED;
						}
					});
				}

			}
		});

		this.sharer.addMulticastListener(this);

		JScrollPane jsp = new JScrollPane(list);
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
		
		initDragable();
	}
	
	private void initDragable() {
		final DragSource ds = new DragSource();
		ds.createDefaultDragGestureRecognizer(this.list, DnDConstants.ACTION_COPY_OR_MOVE, new DragGestureListener() {

			@Override
			public void dragGestureRecognized(DragGestureEvent evt) {
				//get files
				List<File> files = new ArrayList<File>();
				final Object[] selected = Dumpster.this.list.getSelectedValues();
				for (Object o:selected)
					if (o instanceof SharedFile)
						files.add(((SharedFile)o).getFile());
						
				if (files.size() == 0)
					return;
						
				Transferable t = new FileTransferable(files);
				ds.startDrag(evt, DragSource.DefaultMoveDrop, t, new DragSourceAdapter() {
					@Override
					public void dragDropEnd(DragSourceDropEvent dsde) {
						if( dsde.getDropAction() == DnDConstants.ACTION_MOVE )
						{
							// delete all selected from model
							for (Object o:Dumpster.this.list.getSelectedValues())
								Dumpster.this.filelist.removeElement(o);
						}
						// hide window after dragging
						Dumpster.this.setVisible(false);
					}
				});
			}
		});
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
		this.filelist.add(this.filelist.size(), shared);
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