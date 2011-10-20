/**
 * 
 */
package nl.thanod.dragnshare.ui;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import nl.thanod.dragnshare.ui.InteractiveList.ListViewable;
import nl.thanod.util.OS;

/**
 * @author nilsdijk
 */
public class InteractiveListSelector<E extends ListViewable> extends MouseAdapter implements FocusListener, KeyListener {

	enum SelectMode {
		RANGE {
			@Override
			public boolean basedOnMouseEvent(MouseEvent me) {
				return me.isShiftDown();
			}
		},
		ADD {
			@Override
			public boolean basedOnMouseEvent(MouseEvent me) {
				if (OS.isOSX())
					return me.isMetaDown();
				return me.isControlDown();
			}
		},
		SINGLE {
			@Override
			public boolean basedOnMouseEvent(MouseEvent me) {
				return true;
			}
		};

		public abstract boolean basedOnMouseEvent(MouseEvent me);

		public static SelectMode getSelectionMode(MouseEvent me) {
			for (SelectMode sm : SelectMode.values())
				if (sm.basedOnMouseEvent(me))
					return sm;
			return SelectMode.SINGLE;
		}
	}

	private InteractiveList<E> list;
	private List<E> selected = new ArrayList<E>();
	private int lastIndex = -1;
	private E focused;

	public InteractiveListSelector() {

	}

	public void initialize(InteractiveList<E> list) {
		this.list = list;
	}

	/*
	 * (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	@Override
	public void mousePressed(MouseEvent me) {
		if (me.isConsumed() || me.getClickCount() != 1)
			return;
		E c = this.list.getElementAt(me.getPoint());
		select(c, SelectMode.getSelectionMode(me));
		me.consume();
	}

	/**
	 * @param c
	 * @param selectionMode
	 */
	private void select(E e, SelectMode sm) {
		int index = this.list.indexOf(e);
		switch (sm) {
			case SINGLE:
				clearSelected();
				if (e != null){
					e.viewStateSelected(true);
					this.selected.add(e);
				}
				break;
			case ADD:
				if (e != null){
					e.viewStateSelected(true);
					this.selected.add(e);
				}
				break;
			case RANGE:
				selectRange(index);
				break;
		}
		focus(e, index);
	}

	/**
	 * @param e
	 * @param index
	 */
	private void focus(E e, int index) {
		if (this.focused != null)
			this.focused.viewStateFocus(false);
		this.lastIndex = index;
		this.focused = e;
		if (this.focused != null)
			this.focused.viewStateFocus(true);
	}

	private int focus(int index) {
		index = Math.max(0, Math.min(index, this.list.getModel().elementCount()-1));
		E e = this.list.getModel().elementAt(index);
		focus(e, index);
		this.list.ensureVisible(e, index);
		return index;
	}

	private void selectRange(int index) {
		selectRange(this.lastIndex, index);
	}
	
	/**
	 * @param lastIndex2
	 * @param index
	 */
	private void selectRange(int lastIndex, int index) {
		if (lastIndex == -1 || index == -1)
			return;

		int begin = Math.min(lastIndex, index);
		int to = Math.max(lastIndex, index);

		for (int i = begin; i <= to; i++) {
			E e = this.list.elementAt(i);
			if (this.selected.contains(e))
				continue;
			e.viewStateSelected(true);
			this.selected.add(e);
		}
	}

	/**
	 * 
	 */
	private void clearSelected() {
		for (E e : this.selected)
			e.viewStateSelected(false);
		this.selected.clear();
	}

	public List<E> getSelected() {
		return new ArrayList<E>(this.selected);
	}

	public void unselect(E e) {
		e.viewStateSelected(false);
		e.viewStateFocus(false);
		this.selected.remove(e);
		if (this.focused == e)
			this.focused = null;
	}

	/*
	 * (non-Javadoc)
	 * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
	 */
	@Override
	public void focusGained(FocusEvent paramFocusEvent) {
		if (this.focused != null)
			this.focused.viewStateFocus(true);
	}

	/*
	 * (non-Javadoc)
	 * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
	 */
	@Override
	public void focusLost(FocusEvent paramFocusEvent) {
		if (this.focused != null)
			this.focused.viewStateFocus(false);
	}

	/*
	 * (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	@Override
	public void keyTyped(KeyEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			int start = this.lastIndex;
			int index = this.focus(this.lastIndex+1);
			if (e.isShiftDown())
				selectRange(start, index);
			e.consume();
		} else if (e.getKeyCode() == KeyEvent.VK_UP) {
			int start = this.lastIndex;
			int index = this.focus(this.lastIndex-1);
			if (e.isShiftDown())
				selectRange(start, index);
			e.consume();
		} else if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
			if (e.isShiftDown()){
				selectRange(this.lastIndex);
				e.consume();
			}else if ((OS.isOSX() && e.isMetaDown()) || (!OS.isOSX() && e.isControlDown())){	
				select(this.lastIndex);
				e.consume();
			} else {
				clearSelected();
				select(this.lastIndex);
				e.consume();
			}
		}
	}

	/**
	 * @param focusIndex2
	 */
	private void select(int index) {
		E e = this.list.elementAt(index);
		select(e, index);
	}

	/**
	 * @param e
	 * @param index
	 */
	private void select(E e, int index) {
		this.selected.add(e);
		e.viewStateSelected(true);
		this.lastIndex = index;
	}

	/*
	 * (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	@Override
	public void keyReleased(KeyEvent e) {
	}

}
