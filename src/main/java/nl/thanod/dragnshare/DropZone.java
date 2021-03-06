/**
 * 
 */
package nl.thanod.dragnshare;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import nl.thanod.dragnshare.net.MulticastSharer;
import nl.thanod.dragnshare.net.Receiver;
import nl.thanod.dragnshare.net.Sender;
import nl.thanod.dragnshare.net.Sharer;
import nl.thanod.dragnshare.net.Sharer.Listener;
import nl.thanod.dragnshare.notify.Notifier;
import nl.thanod.dragnshare.notify.Notifier.Type;
import nl.thanod.dragnshare.ui.InteractiveList;
import nl.thanod.dragnshare.ui.InteractiveListModel;
import nl.thanod.dragnshare.ui.PrefPane;
import nl.thanod.dragnshare.ui.TopLineBorder;
import nl.thanod.dragnshare.ui.Tray;
import nl.thanod.dragnshare.ui.UIConstants;
import nl.thanod.util.ScreenInfo;
import nl.thanod.util.Settings;

/**
 * @author nilsdijk
 * @author Koen Bollen <meneer@koenbollen>
 */
public class DropZone extends JDialog implements Listener {

	private static final long serialVersionUID = 7192301082111534982L;

	protected volatile int count = 0;

	protected final Sharer sharer;

	protected final InteractiveList<ShareInfo> list;

	protected Tray tray;

	private JLabel clearall;
	
	protected volatile boolean dragging = false;

	public final static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	
	public DropZone() {
		super((Frame) null, "Drag'n Share");
		try
		{
			this.setIconImage(ImageIO.read(DropZone.class.getClassLoader().getResource("dragn-logo.png")));
		} catch (Exception e1)
		{
			e1.printStackTrace();
		}

		Notifier.Factory.dropZone = this;

		this.sharer = new MulticastSharer();
		try {
			this.sharer.start();
		} catch (IOException ball) {
			ball.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not connect the sharer", "oh noos!", JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}

		//this.setModal(true);
		this.setResizable(false);
		
		this.list = new InteractiveList<ShareInfo>();
		this.list.addMouseListener(new MouseAdapter() {
			/* (non-Javadoc)
			 * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
			 */
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2){
					if (Desktop.isDesktopSupported()){
						for (ShareInfo o:DropZone.this.list.getSelector().getSelected()){
							if (o.getSharedFile().shouldStart()){
								o.getSharedFile().start();
							} else {
								if (!o.getSharedFile().isReady())
									continue;
								try {
									Desktop.getDesktop().open(o.getSharedFile().getFile());
								} catch (IOException ball) {
									ball.printStackTrace();
								}
							}
						}
					}
				}
			}
		});
		
		this.list.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE){
					List<ShareInfo> elements = DropZone.this.list.getSelector().getSelected();
					DropZone.this.list.getModel().removeAll(elements);
					for (ShareInfo info:elements)
						info.getSharedFile().remove();
				}
			}
		});

		this.list.getModel().addListener(new InteractiveListModel.Listener<ShareInfo>(){

			@Override
			public void addedToModel(int index, ShareInfo e) {}

			@Override
			public void removedFromModel(int index, ShareInfo e) {
				e.getSharedFile().cancel();
			}
		});

		JLabel drop;
		this.list.setDrop(drop = new JLabel("drop here"));
		drop.setFont(UIConstants.DropHereFont);
		drop.setHorizontalAlignment(SwingConstants.CENTER);
		drop.setForeground(Color.LIGHT_GRAY);
		
		FileDrop.Listener dropper = new FileDrop.Listener() {
			@Override
			public void filesDropped(File[] files) {
				if (DropZone.this.dragging)
					return;
				
				for (final File file : files) {
					try {
						addSharedFile(DropZone.this.sharer.share(file));
					} catch (IOException ball) {
						// TODO graphical warning
						ball.printStackTrace();
					}
				}

			}
		};
		new FileDrop(this, dropper);
		new Clipper(this, dropper);

		this.sharer.listeners().addListener(this);

		JScrollPane jsp = new JScrollPane(this.list, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		jsp.setViewportBorder(null);
		jsp.setBorder(null);

		BorderLayout fl;
		JPanel buttons = new JPanel(fl = new BorderLayout());
		buttons.setBackground(Color.WHITE);
		fl.setVgap(2);
		buttons.setBorder(new TopLineBorder(Color.LIGHT_GRAY));
		JPanel clearpane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		clearpane.setBackground(Color.WHITE);
		clearpane.add(this.clearall = new JLabel(ShareInfo.getIcon("bin.png")));
		this.clearall.setBackground(Color.WHITE);
		this.clearall.setToolTipText("clear all");
		this.clearall.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e)
			{
				List<ShareInfo> elements = DropZone.this.list.getModel().getElements();
				DropZone.this.list.getModel().removeAll(elements);
				
				for (ShareInfo info:elements)
					info.getSharedFile().remove();
			}
		});
		buttons.add(clearpane, BorderLayout.EAST);
		
		JPanel quitpref = new JPanel(new FlowLayout(FlowLayout.LEADING));
		final JLabel quit;
		quitpref.add(quit = new JLabel(ShareInfo.getIcon("stop.png")));
		quit.setBackground(Color.WHITE);
		quit.setToolTipText("Quit Drag'n Share");
		quit.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e)
			{
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run()
					{
						if( !Settings.instance.getBool("confirmQuit", true) )
							System.exit(0);
						
						JCheckBox checkbox = new JCheckBox("Do not show this message again.");  
						JLabel message = new JLabel("You are about to quit Drag'n Share completely, doing so will leave you unable to receive files.");
						JLabel question = new JLabel("Are you sure you want to quit?");
						message.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
						Object[] params = {message, question, checkbox};  
						int r = JOptionPane.showConfirmDialog(DropZone.this, params, "Drag'n Quit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);  
						boolean dontShow = checkbox.isSelected();
						if( r == JOptionPane.YES_OPTION )
						{  
							if( dontShow )
								Settings.instance.setBool( "confirmQuit", false );
							System.exit(0);	
						}
					}
				});
			}
		});
		final JLabel pref;
		quitpref.add(pref = new JLabel(ShareInfo.getIcon("pref.png")));
		pref.setToolTipText("Preferences");
		pref.setBackground(Color.WHITE);
		pref.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e)
			{
				new PrefPane(DropZone.this).setVisible(true);
			}
		});
		quitpref.setBackground(Color.WHITE);
		buttons.add( quitpref, BorderLayout.WEST);

		JPanel container = new JPanel(new BorderLayout());
		container.add(jsp, BorderLayout.CENTER);
		container.add(buttons, BorderLayout.SOUTH);
		add(container);

		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent e) {
				Settings.instance.setLocation(DropZone.this.getLocation());
			}
		});

		Settings.instance.addListener(new Settings.Adapter() {
			@Override
			public void preStore(Settings instance) {
				instance.setLocation(DropZone.this.getLocation());
			}
		});

		setSize(new Dimension(400, 300));
		setResizable(false);
		setAlwaysOnTop(true);
		if( Settings.instance.getProperty("location_x") == null )
			setLocationRelativeTo(null);

		initDragable();
		this.setupTray();
		
	}

	/**
	 * @param share
	 */
	protected void addSharedFile(final Sender share) {
		addSharedFile(new LocalShared(share));
	}

	private void initDragable() {
		final DragSource ds = new DragSource();
		ds.createDefaultDragGestureRecognizer(this.list, DnDConstants.ACTION_COPY_OR_MOVE, new DragGestureListener() {

			@Override
			public void dragGestureRecognized(DragGestureEvent evt) {
				//get files
				List<File> files = new ArrayList<File>();
				List<ShareInfo> selected = DropZone.this.list.getSelector().getSelected();
				final List<ShareInfo> dragged = new ArrayList<ShareInfo>();
				for (ShareInfo o : selected) {
					if (o.getSharedFile().isReady()) {
						files.add(o.getSharedFile().getFile());
						dragged.add(o);
					}
				}

				if (files.size() == 0)
					return;
				
				DropZone.this.dragging = true;

				Transferable t = new FileTransferable(files);
				ds.startDrag(evt, DragSource.DefaultMoveDrop, t, new DragSourceAdapter() {
					@Override
					public void dragDropEnd(DragSourceDropEvent dsde) {
						DropZone.this.dragging = false;
						if (dsde.getDropAction() == DnDConstants.ACTION_MOVE) {
							// delete all selected from model
							DropZone.this.list.getModel().removeAll(dragged);
						}
						// hide window after dragging, if option is set:
						if (Settings.instance.getBool("hideDropZone"))
							DropZone.this.setVisible(false);
					}
				});
			}
		});
	}

	private void setupTray() {
		this.tray = new Tray();

		this.addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowLostFocus(WindowEvent e) {
			}

			@Override
			public void windowGainedFocus(WindowEvent e) {
				DropZone.this.tray.setDefaultIcon();
			}
		});
		
		this.tray.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mousePressed(MouseEvent e) {
				if (!DropZone.this.isVisible()) {
					Dimension size = DropZone.this.getSize();
					Point p = Settings.instance.getLocation();
					if (p != null && ScreenInfo.intersects(new Rectangle(p, size)))
						DropZone.this.setLocation(p);
				}
				DropZone.this.setVisible(!DropZone.this.isVisible());
			}
		});
	}

	protected void addSharedFile(SharedFile shared) {
		ShareInfo info;
		this.list.getModel().add(0, info = new ShareInfo(shared));
		info.setMonitor(new ShareInfo.Monitor() {
			@Override
			public void onRemove(ShareInfo info) {
				info.getSharedFile().remove();
				DropZone.this.list.getModel().remove(info);
			}

			@Override
			public void onAccept(ShareInfo info) {
				info.getSharedFile().start();
			}
		});
	}

	/* (non-Javadoc)
	 * @see nl.thanod.dragnshare.net.Sharer.Listener#offered(nl.thanod.dragnshare.net.Receiver)
	 */
	@Override
	public void offered(Receiver receiver) {
		long limit = Settings.instance.getInt("automaticDownloadLimit",100) * 1024 * 1024;
		if( limit == 0 || receiver.getSize() <= limit ) {
			try {
				receiver.accept();
			} catch (IOException ball) {
				// TODO show #whatskeburt
				if (!this.isFocused())
					this.tray.setDecorator("exclamation");
			}
		} else {
			if (!this.isFocused()){
				Notifier.Factory.notify(Type.RECEIVED, "Large file available", "The file " + receiver.getFile().getName() + " is offered but exceeded the filesize for automatic download.");
				this.tray.setDecorator("accept", true);
			}
		}
		addSharedFile(new ReceivedSharedFile(this,receiver));
	}

	public static void main(String... args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable ball) {
			ball.printStackTrace();
		}
		Settings.singleInstance();

		new DropZone();
	}

}