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

import javax.swing.*;

import nl.thanod.dragnshare.net.MulticastShare;
import nl.thanod.dragnshare.net.Receiver;
import nl.thanod.dragnshare.net.Sender;
import nl.thanod.dragnshare.notify.Notifier;
import nl.thanod.dragnshare.notify.Notifier.Type;
import nl.thanod.dragnshare.ui.InteractiveList;
import nl.thanod.dragnshare.ui.InteractiveListModel;
import nl.thanod.dragnshare.ui.TopLineBorder;
import nl.thanod.dragnshare.ui.Tray;
import nl.thanod.util.ScreenInfo;
import nl.thanod.util.Settings;

/**
 * @author nilsdijk
 * @author Koen Bollen <meneer@koenbollen>
 */
public class DropZone extends JDialog implements MulticastShare.Listener {

	private static final long serialVersionUID = 7192301082111534982L;

	protected volatile int count = 0;

	protected final MulticastShare sharer;

	protected final InteractiveList<ShareInfo> list;

	protected Tray tray;

	private JButton clearall;
	
	protected volatile boolean dragging = false;

	public DropZone() {
		super((Frame) null, "Drag'n Share");

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
		this.list.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent paramKeyEvent) {}
			
			@Override
			public void keyReleased(KeyEvent paramKeyEvent) {}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE){
					DropZone.this.list.getModel().removeAll(DropZone.this.list.getSelector().getSelected());
				}
			}
		});

		this.list.getModel().addListener(new InteractiveListModel.Listener<ShareInfo>(){

			@Override
			public void addedToModel(int index, ShareInfo e) {}

			@Override
			public void removedFromModel(int index, ShareInfo e) {
				e.getSharedFile().remove();
			}
		});

		JLabel drop;
		this.list.setDrop(drop = new JLabel("drop here"));
		drop.setHorizontalAlignment(SwingConstants.CENTER);
		drop.setForeground(Color.LIGHT_GRAY);
		
		FileDrop.Listener dropper = new FileDrop.Listener() {
			@Override
			public void filesDropped(File[] files) {
				if (DropZone.this.dragging)
					return;
				
				for (final File file : files) {
					DropZone.this.sharer.share(file);
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

						@Override
						public boolean shouldStart() {
							return false;
						}

						@Override
						public void start() {}

						@Override
						public String getStatus() {
							return "sharing ...";
						}
					});
				}

			}
		};
		new FileDrop(this, dropper);
		new Clipper(this, dropper);

		this.sharer.addMulticastListener(this);

		JScrollPane jsp = new JScrollPane(this.list, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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
				DropZone.this.list.getModel().removeAll(DropZone.this.list.getModel().getElements());
			}
		});

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

		this.addWindowFocusListener(new WindowFocusListener() { // TODO: Test on OSX
			@Override
			public void windowLostFocus(WindowEvent e) {
			}

			@Override
			public void windowGainedFocus(WindowEvent e) {
				DropZone.this.tray.setDefaultIcon();
			}
		});
		
		this.tray.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent paramActionEvent) {
				System.err.println(paramActionEvent);
			}
		});
		this.tray.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					if (!DropZone.this.isVisible()) {
						Dimension size = DropZone.this.getSize();
						Point p = Settings.instance.getLocation();
						if (p != null && ScreenInfo.intersects(new Rectangle(p, size)))
							DropZone.this.setLocation(p);
					}
					DropZone.this.setVisible(!DropZone.this.isVisible());
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
				DropZone.this.list.getModel().remove(info);
			}

			@Override
			public void onAccept(ShareInfo info) {
				info.getSharedFile().start();
			}
		});
		
		if (shared.shouldStart())
			Notifier.Factory.notify(Type.RECEIVED, "Big file", "The file " + shared.getName() + " is offerd but exeeded the size for automatic download.");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * it.koen.dragnshare.net.MulticastShare.Listener#onReceive(it.koen.dragnshare
	 * .net.Receiver)
	 */
	@Override
	public void onReceive(final Receiver receiver) {
		if (!this.isFocused()) // TODO: Test on OSX
			this.tray.setDecorator("add", true);
		addSharedFile(new ReceivedSharedFile(receiver));
		
		// TODO: Wait for button press:
		//receiver.start();
	}

	@Override
	public void onSending(Sender sender) {
		//System.out.println("onSending");
	}

	@Override
	public void onSent(Sender sender) {
		//System.out.println("onSent");
	}

	public static void main(String... args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable ball) {
			ball.printStackTrace();
		}

		new DropZone();
	}
}