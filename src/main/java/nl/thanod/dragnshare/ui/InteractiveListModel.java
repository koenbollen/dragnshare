/**
 * 
 */
package nl.thanod.dragnshare.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author nilsdijk
 *
 */
public class InteractiveListModel<E> {
	public interface Listener<E> {
		void addedToModel(int index, E e);
		void removedFromModel(int index, E e);
	}
	
	class ListenerContainer implements Listener<E> {
		private List<InteractiveListModel.Listener<E>> listeners = Collections.synchronizedList(new LinkedList<InteractiveListModel.Listener<E>>());
		
		@Override
		public void addedToModel(int index, E e){
			List<InteractiveListModel.Listener<E>> listeners = new LinkedList<InteractiveListModel.Listener<E>>(this.listeners);
			for (InteractiveListModel.Listener<E> listener:listeners)
				listener.addedToModel(index, e);
		}
		
		@Override
		public void removedFromModel(int index, E e){
			List<InteractiveListModel.Listener<E>> listeners = new LinkedList<InteractiveListModel.Listener<E>>(this.listeners);
			for (InteractiveListModel.Listener<E> listener:listeners)
				listener.removedFromModel(index, e);
		}
		
		public void addListener(InteractiveListModel.Listener<E> listener){
			this.listeners.add(listener);
		}
		
		public void removeListener(InteractiveListModel.Listener<E> listener){
			this.listeners.remove(listener);
		}
	}
	
	private final List<E> elements = new ArrayList<E>();
	private final ListenerContainer listeners = new ListenerContainer();
	
	public InteractiveListModel(){
		
	}
	
	public void addAll(Iterable<E> elements){
		addAll(this.elements.size(), elements);
	}
	
	public void addAll(E ... elements){
		addAll(this.elements.size(), elements);
	}
	
	public void addAll(int index, Iterable<E> elements){
		for (E e:elements)
			this.add(index++, e);
	}
	
	public void addAll(int index, E ... elements){
		for (E e:elements)
			this.add(index++, e);
	}

	public void add(int index, E e) {
		this.elements.add(index, e);
		this.listeners.addedToModel(index, e);
	}
	
	public void removeAll(Iterable<E> elements){
		for (E e:elements)
			this.remove(e);
	}
	
	public void removeAll(E ... elements){
		for (E e:elements)
			this.remove(e);
	}
	
	public void remove(E e) {
		int index = this.elements.indexOf(e);
		this.elements.remove(index);
		this.listeners.removedFromModel(index, e);
	}
	
	public void addListener(InteractiveListModel.Listener<E> listener){
		this.listeners.addListener(listener);
	}
	
	public void removeListener(InteractiveListModel.Listener<E> listener){
		this.listeners.removeListener(listener);
	}
	
	public int indexOf(E e){
		return this.elements.indexOf(e);
	}
	
	public E elementAt(int index){
		return this.elementAt(index);
	}
}
