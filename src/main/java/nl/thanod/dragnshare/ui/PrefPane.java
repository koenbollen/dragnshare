package nl.thanod.dragnshare.ui;

import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import nl.thanod.dragnshare.ShareInfo;
import nl.thanod.dragnshare.notify.GrowlNotifier;
import nl.thanod.dragnshare.notify.LibNotifyNotifier;
import nl.thanod.util.OS;
import nl.thanod.util.Settings;

public class PrefPane extends JDialog
{
	private static final long serialVersionUID = -2755952784476390645L;

	private Map<String, JPanel> panes;

	public PrefPane(Window owner)
	{
		super(owner, "Drag'n Share - Preferences");
		
		setModal(true);
		setLayout(new BorderLayout());
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(owner);

		this.panes = new HashMap<String, JPanel>();
		JTabbedPane tabs = new JTabbedPane();
		tabs.setFont(UIConstants.DefaultFont);
		tabs.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		tabs.setPreferredSize(new Dimension(550, 240));
		createInterfacePane(tabs);
		createNetworkingPane(tabs);

		this.add(tabs, BorderLayout.CENTER);
		
		
		JPanel buttons = new JPanel( new BorderLayout() );
		JLabel about = new JLabel(ShareInfo.getIcon("help.png"));
		about.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if( Desktop.isDesktopSupported() )
				{
					try
					{
						Desktop.getDesktop().browse( new URI( "http://koenbollen.github.com/dragnshare" ) );
					} catch (Exception ball)
					{
						ball.printStackTrace();
					}
				}
			}
		});
		about.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
		buttons.add( about, BorderLayout.WEST );
		
		FlowLayout l = new FlowLayout(FlowLayout.TRAILING);
		l.setHgap(30);
		l.setVgap(20);
		JPanel closepane = new JPanel(l);
		JButton close = new JButton("Close");
		close.setFont(UIConstants.DefaultFont);
		close.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				PrefPane.this.setVisible(false);
				PrefPane.this.dispose();
				Settings.instance.store();
			}
		});
		closepane.add( close );
		buttons.add( closepane, BorderLayout.EAST );
		
		this.add(buttons, BorderLayout.SOUTH);
		this.pack();
	}

	private void createInterfacePane(JTabbedPane tabs)
	{
		JPanel p = new JPanel(new BorderLayout());
		
		JPanel settings = new JPanel(new GridLayout(0,1, 5, 5));
		settings.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		final JCheckBox quitconf = new JCheckBox("Confirm before quitting Drag'n Share.");
		quitconf.setFont(UIConstants.DefaultFont);
		quitconf.setSelected(Settings.instance.getBool("confirmQuit", true));
		quitconf.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Settings.instance.setBool("confirmQuit", quitconf.isSelected());
			}
		});
		settings.add(quitconf);

		String msg = "Show a notification message when a file or directory is received.";
		final JCheckBox showNotify = new JCheckBox(msg);
		showNotify.setFont(UIConstants.DefaultFont);
		showNotify.setSelected(Settings.instance.getBool("showNotifications", true));
		showNotify.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Settings.instance.setBool("showNotifications", showNotify.isSelected());
			}
		});
		settings.add(showNotify);

		final JCheckBox hideDropZone = new JCheckBox("Hide the window when a file is dragged from the DropZone.");
		hideDropZone.setFont(UIConstants.DefaultFont);
		hideDropZone.setSelected(Settings.instance.getBool("hideDropZone"));
		hideDropZone.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Settings.instance.setBool("hideDropZone", hideDropZone.isSelected());
			}
		});
		settings.add(hideDropZone);
		
		int timeout = Settings.instance.getInt("autoClearTimeout", 10);
		final JCheckBox autoClear = new JCheckBox("Automatically clear shared files "+timeout+" seconds after the tranfer.");
		autoClear.setFont(UIConstants.DefaultFont);
		autoClear.setSelected(Settings.instance.getBool("autoClear"));
		autoClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Settings.instance.setBool("autoClear", autoClear.isSelected());
			}
		});
		settings.add(autoClear);

		p.add(settings, BorderLayout.NORTH);
		
		if( Settings.instance.getBool("showNotifications", true) )
		{
			String warn = null;
			if( OS.isOSX() && !GrowlNotifier.isAvailable() )
				warn = "Please install Growl for more ecstatically pleasing notification, see http://growl.info/ for download.";
			if( OS.isLinux() && !LibNotifyNotifier.isAvailable() )
				warn = "Please install libnotify-bin for more ecstatically pleasing notification (sudo apt-get install libnotify-bin).";
			if( warn != null )
			{
				JLabel l = new JLabel("<HTML>warning: " + warn +"</HTML>" );
				l.setFont(UIConstants.DefaultFont);
				l.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				p.add(l, BorderLayout.SOUTH);
			}
		}

		
		p.validate();
		tabs.addTab("Interface", p);
		this.panes.put("interface", p);
	}

	private void createNetworkingPane(JTabbedPane tabs)
	{
		
		JPanel p = new JPanel(new BorderLayout());
		JPanel settings = new JPanel(new GridLayout(0,1, 5, 5));
		settings.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		final JPanel limit = new JPanel(new FlowLayout(FlowLayout.LEADING));
		final JTextField adl = new JTextField(Integer.toString(Settings.instance.getInt("automaticDownloadLimit", 100)));
		adl.setFont(UIConstants.DefaultFont);
		adl.setPreferredSize(new Dimension(70, 25));
		adl.setHorizontalAlignment(SwingConstants.RIGHT);
		adl.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e)
			{
				if( !Character.isDigit(e.getKeyChar()) )
					e.consume();
			}
		});
		adl.addCaretListener(new CaretListener() {
			
			@Override
			public void caretUpdate(CaretEvent e)
			{
				int i = 0;
				try
				{
					i = Integer.parseInt(adl.getText());
				} catch( NumberFormatException n )
				{
				}
				Settings.instance.setInt("automaticDownloadLimit", i);
			}
		});
		final JLabel t1 = new JLabel("Automatically download files smaller then ");
		t1.setFont(UIConstants.DefaultFont);
		t1.setLabelFor(adl);
		final JLabel t2 = new JLabel(" MB. (0 = no check)");
		t2.setFont(UIConstants.DefaultFont);
		t2.setLabelFor(adl);
		MouseListener focusADL = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				adl.requestFocus();
				adl.selectAll();
			}
		};
		t1.addMouseListener(focusADL);
		t2.addMouseListener(focusADL);
		limit.add(t1);
		limit.add(adl);
		limit.add(t2);
		settings.add(limit);
		

		final JPanel yellowPagesPrefs = new JPanel(new FlowLayout(FlowLayout.LEADING, 0,0));
		
		final JTextField yellowServer = new JTextField("", 10);
		final JCheckBox beYellow = new JCheckBox("Use entry point to create a network:");
		final JCheckBox alsoBoardcast = new JCheckBox("Also use the default broadcast protocol");
		yellowServer.setToolTipText( "enter an ip-addresses or a hostnames (comma seperated)" );
		yellowServer.setFont(UIConstants.DefaultFont);
		beYellow.setFont(UIConstants.DefaultFont);
		alsoBoardcast.setFont(UIConstants.DefaultFont);
		yellowServer.setEnabled( Settings.instance.getBool("beYellow") );
		beYellow.setSelected( Settings.instance.getBool("beYellow") );
		alsoBoardcast.setSelected( Settings.instance.getBool("alsoBoardcast") );
		alsoBoardcast.setEnabled( Settings.instance.getBool("beYellow") );
		if( beYellow.isSelected() )
			yellowServer.setText( Settings.instance.getProperty( "yellowMaster" ) );
		yellowServer.addCaretListener( new CaretListener() {
			@Override
			public void caretUpdate( CaretEvent arg0 )
			{
				if(beYellow.isSelected())
					Settings.instance.setProperty( "yellowMaster", yellowServer.getText() );
			}
		} );
		beYellow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Settings.instance.setBool("beYellow", beYellow.isSelected());
				yellowServer.setEnabled( beYellow.isSelected() );
				alsoBoardcast.setEnabled( beYellow.isSelected() );
				if( beYellow.isSelected() )
				{	
					yellowServer.setText( Settings.instance.getProperty( "yellowMaster" ) );
					yellowServer.requestFocus();
					yellowServer.selectAll();
				}else
				{
					yellowServer.setText( "" );
				}
				
			}
		});
		beYellow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Settings.instance.setBool("alsoBoardcast", alsoBoardcast.isSelected());
			}
		});

		yellowPagesPrefs.add( beYellow );
		yellowPagesPrefs.add( yellowServer );
		yellowPagesPrefs.add( new JLabel(" (restart required)") );
		settings.add( yellowPagesPrefs );
		settings.add( alsoBoardcast );

		
		
		final JCheckBox bruteForceDiscover = new JCheckBox("Use a blunt technique to boardcast files shared across the network.");
		bruteForceDiscover.setFont(UIConstants.DefaultFont);
		bruteForceDiscover.setSelected(Settings.instance.getBool("bruteForceDiscover"));
		bruteForceDiscover.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Settings.instance.setBool("bruteForceDiscover", bruteForceDiscover.isSelected());
			}
		});
		settings.add(bruteForceDiscover);
		
		p.add(settings, BorderLayout.NORTH);

		p.validate();
		tabs.addTab("Networking", p);
		this.panes.put("network", p);
	}

	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable ball)
		{
			ball.printStackTrace();
		}
		PrefPane sp = new PrefPane(null);
		sp.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				System.exit(1);
			}
		});
		sp.setVisible(true);
	}
}
