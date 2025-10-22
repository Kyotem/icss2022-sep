package nl.han.ica.datastructures;

public class LinkedListNode<T> {
    T value;
    LinkedListNode<T> next;

    public LinkedListNode(T value) {
        this.value = value;
        this.next = null;
    }
}
