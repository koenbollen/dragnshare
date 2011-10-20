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
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;
import javax.swing.border.Border;

import nl.thanod.dragnshare.ui.DottedLineBorder;
import nl.thanod.dragnshare.ui.InteractiveList.ListViewable;

/**
 * @author nilsdijk
 */
public class ShareInfo extends JPanel implements ListViewable, Observer {

	public interface Monitor {
		void onRemove(ShareInfo info);
		void onAccept(ShareInfo info);
	}

	public static final JFileChooser chooser = new JFileChooser();
	private static final Border NONFOCUSED = BorderFactory.createEmptyBorder(7, 7, 7, 7);
	private static final Border FOCUSED = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1), BorderFactory.createCompoundBorder(new DottedLineBorder(Color.BLACK), BorderFactory.createEmptyBorder(5, 5, 5, 5)));

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected JLabel label;

	protected final SharedFile sf;
	private final JLabel status;

	private int index;
	private boolean focused;
	private boolean selected;

	private JLabel close;

	private List<JComponent> coloredComponents = new ArrayList<JComponent>();

	protected Monitor monitor;
	private JProgressBar progress;
	private JPanel container;
	private JLabel start;
	private JPanel buttons;

	public ShareInfo(SharedFile sf) {
		super(new BorderLayout());
		setBorder(NONFOCUSED);
		
		sf.setView(this);
		
		this.coloredComponents.add(this);
		
		this.container = new JPanel(new BorderLayout());
		this.coloredComponents.add(this.container);

		this.sf = sf;
		this.setPreferredSize(new Dimension(0, 60));

		Icon icon = chooser.getIcon(sf.getFile());
		this.container.add(this.label = new JLabel(sf.getName()), BorderLayout.NORTH);
		this.label.setIcon(icon);

		if (sf instanceof Observable) {
			((Observable)sf).addObserver(this);
		}
		
		this.container.add(this.status = new JLabel(this.sf.getStatus()), BorderLayout.SOUTH);
		this.status.setForeground(Color.LIGHT_GRAY);
		this.status.setBorder(BorderFactory.createEmptyBorder(0, icon.getIconWidth() + 4, 0, 0));

		int vgap = ((this.getPreferredSize().height - icon.getIconHeight()) / 2 ) - 7;
		this.buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 2, vgap));
		this.coloredComponents.add(this.buttons);
		
		if (sf.shouldStart()){
			this.start = new JLabel(getIcon("play.png"));
			this.start.setToolTipText("start download");
			this.coloredComponents.add(this.start);
			this.buttons.add(this.start);
			this.start.addMouseListener(new MouseAdapter() {
				/* (non-Javadoc)
				 * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
				 */
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() != 1 || e.getButton() != MouseEvent.BUTTON1)
						return;					
					if (ShareInfo.this.monitor != null)
						ShareInfo.this.monitor.onAccept(ShareInfo.this);
					e.consume();
				}
			});
		}
		
		this.buttons.add(Box.createHorizontalStrut(5));
		this.buttons.add(this.close = new JLabel(getIcon("cancel.png")));
		this.coloredComponents.add(this.close);
		this.close.setToolTipText("stop & remove");
		this.close.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() != 1 || e.getButton() != MouseEvent.BUTTON1)
					return;
				if (ShareInfo.this.monitor != null)
					ShareInfo.this.monitor.onRemove(ShareInfo.this);
				e.consume();
			}
		});

		this.add(this.container, BorderLayout.CENTER);
		this.add(this.buttons, BorderLayout.EAST);

		updateView();
	}

	/**
	 * @param name
	 * @return
	 */
	public static Icon getIcon(String name) {
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
		if (this.selected)
			cs = ColorScheme.SELECTED;

		if (this.focused)
			setBorder(FOCUSED);
		else
			setBorder(NONFOCUSED);

		for (JComponent c : this.coloredComponents) {
			c.setOpaque(true);
			c.setBackground(cs.getColor(this.index));
		}
		repaint();
	}

	/**
	 * @param object
	 */
	public void setMonitor(Monitor monitor) {
		this.monitor = monitor;
	}

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable paramObservable, Object paramObject) {
		float p = this.sf.getProgress();
		if (p > 0f && p < 1f){
			if (this.progress == null){
				this.progress = new JProgressBar(0,100);
				this.container.add(this.progress);
				this.container.revalidate();
			}
			this.progress.setValue((int)(100*p));
		} else {
			if (this.progress != null){
				this.container.remove(this.progress);
				this.progress = null;
				this.container.revalidate();
			}
		}
		
		if (this.start != null && !this.sf.shouldStart()){
			this.buttons.remove(this.start);
			this.start = null;
		}
		
		this.status.setText(this.sf.getStatus());
		updateView();
	}

	public void remove()
	{
		if (ShareInfo.this.monitor != null)
			ShareInfo.this.monitor.onRemove(ShareInfo.this);
	}
}