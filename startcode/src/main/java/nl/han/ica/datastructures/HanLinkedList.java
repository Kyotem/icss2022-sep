package nl.han.ica.datastructures;

public class HanLinkedList<T> implements IHANLinkedList {

    private LinkedListNode<T> head;
    private int size;

    public HanLinkedList() {
        head = null;
        size = 0;
    }

    @Override
    public void addFirst(Object value) {
        // TODO: Casting, though maybe change the interface to allow for T specifically?
        T val = (T) value;
        LinkedListNode<T> newNode = new LinkedListNode<>(val);
        newNode.next = head;
        head = newNode;
        size++;
    }

    @Override
    public void clear() {
        // Assuming the garbage collector will clean it up later.
        head = null;
        size = 0;
    }

    @Override
    public void insert(int index, Object value) { // Maybe add a check to make sure index is within range?

        // TODO: Casting, though maybe change the interface to allow for T specifically?
        T val = (T) value;

        if (index == 0) {
            addFirst(val);
            return;
        }

        LinkedListNode<T> prev = head;
        for (int i = 0; i < index - 1; i++) {
            prev = prev.next;
        }

        LinkedListNode<T> newNode = new LinkedListNode<>(val);
        newNode.next = prev.next;
        prev.next = newNode;

        size++;
    }

    @Override
    public void delete(int index) { // Maybe add a check to make sure index is within range?

        if (index == 0) {
            removeFirst();
            return;
        }

        LinkedListNode<T> prev = head;
        for (int i = 0; i < index - 1; i++) {
            prev = prev.next;
        }

        prev.next = prev.next.next;
        size--;
    }


    @Override
    public Object get(int index) {  // Maybe add a check to make sure index is within range?

        LinkedListNode<T> current = head;

        for (int i = 0; i < index; i++) {
            current = current.next;
        }

        return current.value;
    }

    @Override
    public void removeFirst() { // FIXME: How to handle if list is empty? Can't access head if it doesn't exist!

        head = head.next;
        size--;
    }

    @Override
    public Object getFirst() {

        if (head == null) {
            return null;
        }

        return head.value;
    }

    @Override
    public int getSize() {

        return size;
    }
}


