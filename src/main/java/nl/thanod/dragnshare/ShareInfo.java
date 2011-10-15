/**
 * 
 */
package nl.thanod.dragnshare;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import nl.thanod.dragnshare.ui.InteractiveList.ListViewable;

/**
 * @author nilsdijk
 */
public class ShareInfo extends JPanel implements ListViewable {

	public interface Monitor {
		void onRemove(ShareInfo info);
	}

	public static final JFileChooser chooser = new JFileChooser();

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected JLabel label;

	protected SharedFile sf;
	private final JLabel status;

	private int index;
	private boolean focused;
	private boolean selected;

	private JLabel close;
	
	private List<JComponent> coloredComponents = new ArrayList<JComponent>();

	private Monitor monitor;

	public ShareInfo(SharedFile sf) {
		super(new BorderLayout());
		this.coloredComponents.add(this);
		
		setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 2));
		this.sf = sf;
		this.setPreferredSize(new Dimension(0, 50));

		Icon icon = chooser.getIcon(sf.getFile());
		this.add(label = new JLabel(sf.getName()), BorderLayout.NORTH);
		this.label.setIcon(icon);

		if (sf instanceof Observable) {
			final JProgressBar prog;
			this.add(prog = new JProgressBar(0, 100), BorderLayout.CENTER);
			((Observable) sf).addObserver(new Observer() {
				@Override
				public void update(Observable paramObservable, Object paramObject) {
					prog.setValue((int) (ShareInfo.this.sf.getProgress() * 100));
					if (ShareInfo.this.sf.getProgress() == 1f)
						ShareInfo.this.remove(prog);
					ShareInfo.this.updateView();
				}
			});
		}

		this.add(this.status = new JLabel("status here"), BorderLayout.SOUTH);
		this.status.setForeground(Color.LIGHT_GRAY);
		this.status.setBorder(BorderFactory.createEmptyBorder(0, icon.getIconWidth() + 4, 0, 0));

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		buttons.add(this.close = new JLabel(getIcon("cancel.png")));
		resizeLabel(this.close);
		this.coloredComponents.add(this.close);
		this.close.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() != 1 || e.getButton() != MouseEvent.BUTTON1)
					return;
				ShareInfo.this.removeFromList();
				e.consume();
			}
		});

		if (sf.canSave()) {
			final JLabel save;
			buttons.add(save = new JLabel(getIcon("disk.png")));
			resizeLabel(save);
			this.coloredComponents.add(save);
			save.addMouseListener(new MouseAdapter() {
				/*
				 * (non-Javadoc)
				 * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.
				 * MouseEvent)
				 */
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() != 1 || e.getButton() != MouseEvent.BUTTON1)
						return;
					if (!ShareInfo.this.sf.isReady())
						return;
					e.consume();
					chooser.setSelectedFile(new File(ShareInfo.this.sf.getFile().getName()));
					if (chooser.showSaveDialog(ShareInfo.this) == JFileChooser.APPROVE_OPTION) {
						File f = chooser.getSelectedFile();
						ShareInfo.this.sf.getFile().renameTo(f);
						removeFromList();
					}

				}
			});
		}
		this.add(buttons, BorderLayout.EAST);
		
		updateView();
	}

	/**
	 * @param close2
	 */
	private static void resizeLabel(JLabel l) {
		l.setPreferredSize(new Dimension(l.getIcon().getIconWidth(), l.getIcon().getIconHeight()));
	}

	/**
	 * 
	 */
	public void removeFromList() {
		if (this.monitor != null)
			this.monitor.onRemove(this);
	}

	/**
	 * @param name
	 * @return
	 */
	private static Icon getIcon(String name) {
		try {
			return new ImageIcon(ShareInfo.class.getClassLoader().getResource(name));
		} catch (Throwable ball) {
			ball.printStackTrace();
			return null;
		}
	}

	public SharedFile getSharedFile() {
		return this.sf;
	}

	/*
	 * (non-Javadoc)
	 * @see nl.thanod.dragnshare.ui.InteractiveList.ListViewable#getView()
	 */
	@Override
	public JComponent getView() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * nl.thanod.dragnshare.ui.InteractiveList.ListViewable#viewStateSelected
	 * (boolean)
	 */
	@Override
	public void viewStateSelected(boolean selected) {
		this.selected = selected;
		updateView();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * nl.thanod.dragnshare.ui.InteractiveList.ListViewable#viewStateFocus(boolean
	 * )
	 */
	@Override
	public void viewStateFocus(boolean focused) {
		this.focused = focused;
		updateView();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * nl.thanod.dragnshare.ui.InteractiveList.ListViewable#viewStateIndex(int)
	 */
	@Override
	public void viewStateIndex(int index) {
		this.index = index;
		updateView();
	}

	protected void updateView() {
		ColorScheme cs = this.sf.getColorScheme();
		if (selected)
			cs = ColorScheme.SELECTED;
		
		for (JComponent c:this.coloredComponents){
			c.setOpaque(true);
			c.setBackground(cs.getColor(index));
		}
		repaint();
	}

	/**
	 * @param object
	 */
	public void setMonitor(Monitor monitor) {
		this.monitor = monitor;
	}
}