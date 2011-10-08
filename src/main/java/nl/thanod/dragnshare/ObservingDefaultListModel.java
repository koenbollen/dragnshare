/**
 * 
 */
package nl.thanod.dragnshare;

import java.util.Observable;
import java.util.Observer;

import javax.swing.DefaultListModel;

/**
 * @author nilsdijk
 */
public class ObservingDefaultListModel extends DefaultListModel implements Observer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -34667882487930985L;

	@Override
	public void setElementAt(Object paramObject, int paramInt) {
		super.setElementAt(paramObject, paramInt);
	}

	@Override
	public void removeElementAt(int index) {
		unlisten(elementAt(index));
		super.removeElementAt(index);
	}

	@Override
	public void insertElementAt(Object paramObject, int paramInt) {
		listen(paramObject);
		super.insertElementAt(paramObject, paramInt);
	}

	@Override
	public void addElement(Object paramObject) {
		listen(paramObject);
		super.addElement(paramObject);
	}

	@Override
	public boolean removeElement(Object paramObject) {
		unlisten(paramObject);
		return super.removeElement(paramObject);
	}

	@Override
	public void removeAllElements() {
		unlisten(super.elements());
		super.removeAllElements();
	}

	@Override
	public Object set(int paramInt, Object paramObject) {
		listen(paramObject);
		Object o = super.set(paramInt, paramObject);
		unlisten(o);
		return o;
	}

	@Override
	public void add(int paramInt, Object paramObject) {
		listen(paramObject);
		super.add(paramInt, paramObject);
	}

	@Override
	public Object remove(int paramInt) {
		Object o = super.remove(paramInt);
		unlisten(o);
		return o;
	}

	@Override
	public void removeRange(int paramInt1, int paramInt2) {
		for (int i = paramInt2; i >= paramInt1; --i) {
			unlisten(elementAt(i));
		}
		super.removeRange(paramInt1, paramInt2);
	}

	protected void listen(Object o) {
		if (o instanceof Observable)
			((Observable) o).addObserver(this);
	}

	protected void listen(Object... os) {
		for (Object o : os)
			listen(o);
	}

	protected void listen(Iterable<?> os) {
		for (Object o : os)
			listen(o);
	}

	protected void unlisten(Object o) {
		if (o instanceof Observable)
			((Observable) o).deleteObserver(this);
	}

	protected void unlisten(Object... os) {
		for (Object o : os)
			unlisten(o);
	}

	protected void unlisten(Iterable<?> os) {
		for (Object o : os)
			unlisten(o);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable paramObservable, Object paramObject) {
		int index = super.indexOf(paramObservable);
		super.fireContentsChanged(paramObservable, index, index);
	}

}
