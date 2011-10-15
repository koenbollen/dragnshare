/**
 * 
 */
package nl.thanod.dragnshare;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
	
	private Monitor monitor;

	public ShareInfo(SharedFile sf) {
		super(new BorderLayout());
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

		this.add(this.close = new JLabel(getIcon("cancel.png")), BorderLayout.EAST);
		this.close.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent paramMouseEvent) {
				ShareInfo.this.removeFromList();
			}
		});
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
		setBackground(cs.getColor(this.index));
		repaint();
	}

	/**
	 * @param object
	 */
	public void setMonitor(Monitor monitor) {
		this.monitor = monitor;
	}
}