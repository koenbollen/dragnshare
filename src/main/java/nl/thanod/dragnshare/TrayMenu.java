package nl.thanod.dragnshare;

import java.awt.Desktop;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

public class TrayMenu extends PopupMenu
{
	private static final long serialVersionUID = -5944495892788958118L;
	
	protected final Dumpster parent;
	
	public TrayMenu( Dumpster parent )
	{
		this.parent = parent;
		MenuItem i;
		
		i = new MenuItem("About");
		i.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				if(Desktop.isDesktopSupported())
				{
					try
					{
						Desktop.getDesktop().browse(new URI("http://github.com/koenbollen/dragnshare"));
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		});
		this.add(i);
		this.addSeparator();
		i = new MenuItem("Settings");
		i.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				new SettingsPane().setVisible(true);
			}
		});
		this.add(i);
		this.addSeparator();
		i = new MenuItem("Exit");
		i.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				System.exit(0);
			}
		});
		this.add(i);
		
	}

}
