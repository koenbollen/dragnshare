package nl.thanod.dragnshare.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import nl.thanod.dragnshare.ShareInfo;
import nl.thanod.util.Settings;

public class SettingsPane extends JDialog
{
	private static final long serialVersionUID = -2755952784476390645L;

	private Map<String, JPanel> panes;

	public SettingsPane(Window owner)
	{
		super(owner, "Drag'n Share - Preferences");
		
		setModal(true);
		setLayout(new BorderLayout());
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(owner);

		this.panes = new HashMap<String, JPanel>();
		JTabbedPane tabs = new JTabbedPane();
		tabs.setPreferredSize(new Dimension(550, 240));
		createInterfacePane(tabs);
		createNetworkingPane(tabs);
		createAdvancedPane(tabs);

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
		about.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
		buttons.add( about, BorderLayout.WEST );
		
		FlowLayout l = new FlowLayout(FlowLayout.TRAILING);
		l.setHgap(30);
		l.setVgap(20);
		JPanel closepane = new JPanel(l);
		JButton close = new JButton("Close");
		close.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				SettingsPane.this.setVisible(false);
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
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEADING));

		
		final JCheckBox quitconf = new JCheckBox("Confirm before quitting Drag'n Share.");
		quitconf.setSelected(Settings.instance.getBool("confirmQuit", true));
		quitconf.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Settings.instance.setBool("confirmQuit", quitconf.isSelected());
			}
		});
		p.add(quitconf);

		String msg = "Show a notification message when a file is received.";
		final JCheckBox showNotify = new JCheckBox(msg);
		showNotify.setSelected(Settings.instance.getBool("showNotifications", true));
		showNotify.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Settings.instance.setBool("showNotifications", showNotify.isSelected());
			}
		});
		p.add(showNotify);

		final JCheckBox hideDropZone = new JCheckBox("Hide window when an item is dragged from the drop zone.");
		hideDropZone.setSelected(Settings.instance.getBool("hideDropZone"));
		hideDropZone.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Settings.instance.setBool("hideDropZone", hideDropZone.isSelected());
			}
		});
		p.add(hideDropZone);
		
		int timeout = Settings.instance.getInt("autoClearTimeout", 10);
		final JCheckBox autoClear = new JCheckBox("Automatically clear shared files "+timeout+" seconds after completion.");
		autoClear.setSelected(Settings.instance.getBool("autoClear"));
		autoClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Settings.instance.setBool("autoClear", autoClear.isSelected());
			}
		});
		p.add(autoClear);

		tabs.addTab("General", p);
		this.panes.put("general", p);
	}

	private void createNetworkingPane(JTabbedPane tabs)
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEADING));

		final JLabel t1 = new JLabel("Automatically download files smaller then ");
		p.add(t1);
		final JTextField adl = new JTextField(Integer.toString(Settings.instance.getInt("automaticDownloadLimit", 100)));
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
		p.add(adl);
		final JLabel t2 = new JLabel(" MB.  (0 = no check)");
		p.add(t2);

		final JCheckBox bruteForceDiscover = new JCheckBox("Use a blunt technique to boardcast files shared across the network.");
		bruteForceDiscover.setSelected(Settings.instance.getBool("bruteForceDiscover"));
		bruteForceDiscover.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Settings.instance.setBool("bruteForceDiscover", bruteForceDiscover.isSelected());
			}
		});
		p.add(bruteForceDiscover);

		p.validate();
		tabs.addTab("Networking", p);
		this.panes.put("network", p);
	}

	private void createAdvancedPane(JTabbedPane tabs)
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEADING));
		p.add(new JLabel("Advanced stuff"));
		tabs.addTab("Advanced", p);
		this.panes.put("advanced", p);
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
		SettingsPane sp = new SettingsPane(null);
		//sp.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		sp.setVisible(true);
	}
}
