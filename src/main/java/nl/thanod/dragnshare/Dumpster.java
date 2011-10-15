/**
 * 
 */
package nl.thanod.dragnshare;


import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.*;

import nl.thanod.dragnshare.net.MulticastShare;
import nl.thanod.dragnshare.net.MulticastShare.AvailableFile;
import nl.thanod.dragnshare.net.Receiver;
import nl.thanod.dragnshare.net.Sender;
import nl.thanod.dragnshare.notify.Notifier;
import nl.thanod.dragnshare.ui.InteractiveList;
import nl.thanod.dragnshare.ui.TopLineBorder;
import nl.thanod.dragnshare.ui.Tray;
import nl.thanod.util.ScreenInfo;
import nl.thanod.util.Settings;

/**
 * @author nilsdijk
 * @author Koen Bollen <meneer@koenbollen>
 */
public class Dumpster extends JDialog implements MulticastShare.Listener {
	
	private static final long serialVersionUID = 7192301082111534982L;

	protected volatile int count = 0;

	protected final MulticastShare sharer;

	protected final InteractiveList<ShareInfo> list;

	protected Tray tray;

	private JButton clearall;

	public Dumpster() {
		super((Frame)null,"Drag'n Share");
		
		Notifier.Factory.dropZone = this;
		
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
		
		//this.setModal(true);
		this.setResizable(false);
		
		list = new InteractiveList<ShareInfo>();
		list.addMouseListener(new MouseAdapter() {
			/* (non-Javadoc)
			 * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
			 */
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2){
					if (Desktop.isDesktopSupported()){
						for (ShareInfo o:list.getSelector().getSelected()){
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
		});
		
		JLabel drop;
		list.setDrop(drop = new JLabel("drop here"));
		drop.setHorizontalAlignment(SwingConstants.CENTER);
		drop.setForeground(Color.LIGHT_GRAY);
		
		new FileDrop(this, new FileDrop.Listener() {
			@Override
			public void filesDropped(File[] files) {
				for (final File file : files) {
					Dumpster.this.sharer.share(file);
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

						@Override
						public boolean isReady() {
							return true;
						}

						@Override
						public void remove() {
							//TODO implement;
						}
					});
				}

			}
		});

		this.sharer.addMulticastListener(this);

		JScrollPane jsp = new JScrollPane(this.list);
		jsp.setBorder(null);
		
		FlowLayout fl;
		JPanel buttons = new JPanel(fl = new FlowLayout(FlowLayout.TRAILING));
		buttons.setBackground(Color.WHITE);
		fl.setVgap(2);
		buttons.setBorder(new TopLineBorder(Color.LIGHT_GRAY));
		buttons.add(this.clearall = new JButton("clear all"));
		this.clearall.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent paramActionEvent) {
				List<ShareInfo> l = Dumpster.this.list.getModel().getElements();
				for (ShareInfo si:l)
					si.removeFromList();
			}
		});
		
		JPanel container = new JPanel(new BorderLayout());
		container.add(jsp, BorderLayout.CENTER);
		container.add(buttons, BorderLayout.SOUTH);
		add(container);

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

		setResizable(false);
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
				List<ShareInfo> selected = Dumpster.this.list.getSelector().getSelected();
				final List<ShareInfo> dragged = new ArrayList<ShareInfo>();
				for (ShareInfo o:selected){
					if (o.getSharedFile().isReady()){
						files.add(o.getSharedFile().getFile());
						dragged.add(o);
					}
				}
						
				if (files.size() == 0)
					return;
						
				Transferable t = new FileTransferable(files);
				ds.startDrag(evt, DragSource.DefaultMoveDrop, t, new DragSourceAdapter() {
					@Override
					public void dragDropEnd(DragSourceDropEvent dsde) {
						if( dsde.getDropAction() == DnDConstants.ACTION_MOVE )
						{
							// delete all selected from model
							Dumpster.this.list.getModel().removeAll(dragged);
						}
						// hide window after dragging, if option is set:
						if( Settings.instance.getBool("hideDropZone") )
							Dumpster.this.setVisible(false);
					}
				});
			}
		});
	}
	
	private void setupTray()
	{
		this.tray = new Tray();
		
		this.addWindowFocusListener(new WindowFocusListener() { // TODO: Test on OSX
			@Override
			public void windowLostFocus(WindowEvent e)
			{
			}
			@Override
			public void windowGainedFocus(WindowEvent e)
			{
				Dumpster.this.tray.setDefaultIcon(); 
			}
		});
		
		this.tray.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if( e.getButton() == MouseEvent.BUTTON1 )
				{
					if( !Dumpster.this.isVisible() )
					{
						Dimension size = Dumpster.this.getSize();
						Point p = Settings.instance.getLocation();
						if( p != null && ScreenInfo.intersects(new Rectangle(p, size)) )
							Dumpster.this.setLocation(p);
					}
					Dumpster.this.setVisible(!Dumpster.this.isVisible());
				}
			}
		});
	}
	
	protected void addSharedFile(SharedFile shared) {
		ShareInfo info;
		this.list.getModel().add(0, info = new ShareInfo(shared));
		info.setMonitor(new ShareInfo.Monitor() {
			@Override
			public void onRemove(ShareInfo info) {
				Dumpster.this.list.getModel().remove(info);
				info.getSharedFile().remove();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * it.koen.dragnshare.net.MulticastShare.Listener#onReceive(it.koen.dragnshare
	 * .net.Receiver)
	 */
	@Override
	public void onReceive(final Receiver receiver) {
		if( !this.isFocused() ) // TODO: Test on OSX
			this.tray.setDecorator("add", true);
		addSharedFile(new ReceivedSharedFile(receiver));
	}

	@Override
	public void onSending(Sender sender)
	{
		//System.out.println("onSending");
	}

	@Override
	public void onSent(Sender sender)
	{
		//System.out.println("onSent");
	}

	public static void main(String... args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable ball) {
			ball.printStackTrace();
		}

		new Dumpster();
	}

	@Override
	public void onAvailable( UUID id, AvailableFile info )
	{
		// TODO: Show an available file in the DropZone and call this.sharer.accept( id ) when it's accepted by the user;
		final UUID idd = id;
		System.err.println("file larger then limit is available, this isn't implemented yet... Accepting in 5 sec..");
		Thread t = new Thread( new Runnable() {
			
			@Override
			public void run()
			{
				try
				{
					Thread.sleep(5000);
				} catch (InterruptedException e)
				{
				}
				Dumpster.this.sharer.accept(idd);
			}
		});
		t.setDaemon(true);
		t.start();
	}
	
}