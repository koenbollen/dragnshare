package nl.thanod.dragnshare.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import nl.thanod.util.Settings;

public class SettingsPane extends JFrame
{
	private static final long serialVersionUID = -2755952784476390645L;
	
	private Map<String, JPanel> panes;

	public SettingsPane()
	{
		super("Drag'n Share - Settings");
		
		this.panes = new HashMap<String, JPanel>();
		JTabbedPane tabs = new JTabbedPane();
		tabs.setPreferredSize(new Dimension(550, 240));
		createInterfacePane(tabs);
		createNetworkingPane(tabs);
		createAdvancedPane(tabs);
		
		this.add(tabs);
		this.pack();
	}

	private void createInterfacePane(JTabbedPane tabs)
	{
		JPanel p = new JPanel();
		
		//JTextArea groupKey = new JTextArea("<group key>");
		//groupKey.setEditable(false); // TODO: Enable when implemented.
		//p.add( groupKey );
		
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
		
		tabs.addTab( "General", p );
		this.panes.put( "general", p );
	}

	private void createNetworkingPane(JTabbedPane tabs)
	{
		JPanel p = new JPanel();

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
		
		tabs.addTab( "Networking", p );
		this.panes.put( "network", p );
	}

	private void createAdvancedPane(JTabbedPane tabs)
	{
		JPanel p = new JPanel();
		p.add(new JLabel( "Advanced stuff" ));
		tabs.addTab( "Advanced", p );
		this.panes.put( "advanced", p );
	}
	
	public static void main(String[] args)
	{
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable ball) {
			ball.printStackTrace();
		}
		SettingsPane sp = new SettingsPane();
		sp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		sp.setVisible(true);
	}
}