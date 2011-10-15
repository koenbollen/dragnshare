/**
 * 
 */
package nl.thanod.dragnshare;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

import nl.thanod.dragnshare.ui.InteractiveList.ListViewable;

/**
 * @author nilsdijk
 */
public class ShareInfo extends JPanel implements ListViewable {

	public static final JFileChooser chooser = new JFileChooser();
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected JLabel label;

	private SharedFile sf;

	private int index;

	private boolean focused;

	private boolean selected;

	public ShareInfo(SharedFile sf) {
		super(new FlowLayout(FlowLayout.LEADING));
		this.sf = sf;
		this.setPreferredSize(new Dimension(0, 50));

		Icon icon = chooser.getIcon(sf.getFile());
		this.add(label = new JLabel(sf.getName()));
		this.label.setIcon(icon);
		
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

	private void updateView() {
		ColorScheme cs = this.sf.getColorScheme();
		if (selected)
			cs = ColorScheme.SELECTED;
		setBackground(cs.getColor(this.index));
		repaint();
	}
}