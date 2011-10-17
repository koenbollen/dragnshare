/**
 * 
 */
package nl.thanod.dragnshare.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.*;

import nl.thanod.dragnshare.ui.InteractiveList.ListViewable;

/**
 * @author nilsdijk
 */
public class InteractiveList<E extends ListViewable> extends JPanel implements InteractiveListModel.Listener<E> {
	
	public static interface ListViewable {
		JComponent getView();
		void viewStateSelected(boolean selected);
		void viewStateFocus(boolean focused);
		void viewStateIndex(int index);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8186548336695395541L;
	private final InteractiveListModel<E> model;

	protected final JPanel list;
	private InteractiveListSelector<E> selector;
	private final List<E> elements = new ArrayList<E>();
	
	private JComponent drop = null;

	public InteractiveList() {
		super(new BorderLayout());
		
		setFocusable(true);
		this.model = new InteractiveListModel<E>();
		this.model.addListener(this);

		this.list = new JPanel(new GridLayout(0, 1));
		this.list.setOpaque(false);

		this.setBackground(Color.WHITE);

		this.setSelector(new InteractiveListSelector<E>());
		
		updateDrop();
	}
	
	public void setDrop(JComponent drop){
		this.drop = drop;
		updateDrop();
	}

	public void setSelector(InteractiveListSelector<E> selector) {
		if (this.selector != null){
			this.removeMouseListener(this.selector);
			this.removeFocusListener(this.selector);
			this.removeKeyListener(this.selector);
		}
		this.selector = selector;
		this.selector.initialize(this);
		
		this.addMouseListener(this.selector);
		this.addFocusListener(this.selector);
		this.addKeyListener(this.selector);
	}
	
	public InteractiveListSelector<E> getSelector(){
		return this.selector;
	}

	public InteractiveListModel<E> getModel() {
		return this.model;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * nl.thanod.dragnshare.ui.InteractiveListModel.Listener#addedToModel(int,
	 * java.lang.Object)
	 */
	@Override
	public void addedToModel(int index, E e) {
		this.list.add(e.getView(), index);
		this.elements.add(index, e);
		updateIndexes(index);
		updateDrop();
		this.revalidate();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * nl.thanod.dragnshare.ui.InteractiveListModel.Listener#removedFromModel
	 * (int, java.lang.Object)
	 */
	@Override
	public void removedFromModel(int index, E e) {
		this.selector.unselect(e);
		this.list.remove(index);
		this.elements.remove(index);
		updateIndexes(index);
		updateDrop();
		this.revalidate();
	}

	/**
	 * @param point
	 */
	@SuppressWarnings("unchecked")
	public E getElementAt(Point point) {
		try {
			return (E)this.list.getComponentAt(point);
		} catch(ClassCastException ball){
			return null;
		}
	}
	
	public int indexOf(E e){
		return this.elements.indexOf(e);
	}

	/**
	 * @param i
	 */
	public E elementAt(int i) {
		return this.elements.get(i);
	}
	
	private void updateIndexes(int from){		
		int f = Math.max(from, 0);
		for (int i=f; i<this.elements.size(); i++)
			this.elements.get(i).viewStateIndex(i);
	}
	
	private void updateDrop(){
		if (this.drop != null && elements.size() == 0){
			this.remove(this.list);
			this.add(drop);
		}else{
			if (drop != null)
				this.remove(this.drop);
			this.add(this.list, BorderLayout.NORTH);
		}
		repaint();
	}
	
	static class StringView extends JPanel implements ListViewable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2822839565240917129L;
		
		private int index=0;

		public StringView(String toView) {
			super(new BorderLayout());
			setPreferredSize(new Dimension(50, 50));
			this.add(new JLabel(toView));
			
			setOpaque(true);
		}

		/*
		 * (non-Javadoc)
		 * @see nl.thanod.dragnshare.ui.InteractiveList.ListViewable#getView()
		 */
		@Override
		public JComponent getView() {
			return this;
		}

		public static List<StringView> makeViews(String... strings) {
			List<StringView> list = new LinkedList<InteractiveList.StringView>();
			for (String s : strings)
				list.add(new StringView(s));
			return list;
		}

		/* (non-Javadoc)
		 * @see nl.thanod.dragnshare.ui.InteractiveList.ListViewable#viewStateSelected(boolean)
		 */
		@Override
		public void viewStateSelected(boolean selected) {
			if (selected)
				setBackground(Color.BLUE);
			else{
				if (this.index %2 == 0)
					setBackground(Color.WHITE);
				else
					setBackground(Color.lightGray);
			}
			repaint();
		}

		/* (non-Javadoc)
		 * @see nl.thanod.dragnshare.ui.InteractiveList.ListViewable#viewStateFocus(boolean)
		 */
		@Override
		public void viewStateFocus(boolean focused) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see nl.thanod.dragnshare.ui.InteractiveList.ListViewable#viewStateIndex(int)
		 */
		@Override
		public void viewStateIndex(int index) {
			this.index = index;
			
			if (this.index %2 == 0)
				setBackground(Color.WHITE);
			else
				setBackground(Color.lightGray);
			
			repaint();
		}
	}

	public static void main(String... args) {
		JFrame frame = new JFrame("test de lijst");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(400, 300);

		final InteractiveList<StringView> list;
		frame.add(new JScrollPane(list = new InteractiveList<StringView>()));
		list.getModel().addAll(StringView.makeViews("hello world my name is nils dijk and your mom sucks".split(" ")));
		
		list.addMouseListener(new MouseAdapter() {
			/* (non-Javadoc)
			 * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
			 */
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.isConsumed())
					return;
				if (e.getClickCount() == 2){
					list.getModel().removeAll(list.getSelector().getSelected());
					e.consume();
				}
			}
		});

		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	public void ensureVisible(E e, int index){
		this.scrollRectToVisible(getBounds(index));
	}

	/**
	 * @param e
	 * @param index
	 * @return
	 */
	public Rectangle getBounds(int index) {
		return this.list.getComponent(index).getBounds();
	}

}
