/**
 * 
 */
package nl.thanod.dragnshare.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import nl.thanod.dragnshare.ui.InteractiveList.ListViewable;
import nl.thanod.util.OS;

/**
 * @author nilsdijk
 */
public class InteractiveListSelector<E extends ListViewable> extends MouseAdapter {

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
		if (c == null)
			return;
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
				e.viewStateSelected(true);
				this.selected.add(e);
				break;
			case ADD:
				e.viewStateSelected(true);
				this.selected.add(e);
				break;
			case RANGE:
				selectRange(this.lastIndex, index);
				break;
		}
		this.lastIndex = index;
		focus(e);
	}

	/**
	 * @param e
	 */
	private void focus(E e) {
		if (this.focused != null)
			this.focused.viewStateFocus(false);
		this.focused = e;
		this.focused.viewStateFocus(true);
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
	
	public void unselect(E e){
		e.viewStateSelected(false);
		e.viewStateFocus(false);
		this.selected.remove(e);
		if (this.focused == e)
			this.focused = null;
	}

}
