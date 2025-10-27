package nl.han.ica.datastructures;

public class HANLinkedList<T> implements IHANLinkedList<T> {

    private LinkedListNode<T> head;
    private int size;
    // java:S1192 SonarLinter

    private static final String OUT_OF_BOUNDS_MESSAGE = "Index out of bounds";

    public HANLinkedList() {
        head = null;
        size = 0;
    }

    @Override
    public void addFirst(T value) {
        LinkedListNode<T> newNode = new LinkedListNode<>(value);
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
    public void insert(int index, T value) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(OUT_OF_BOUNDS_MESSAGE); // NOTE: Not going to handle the exceptions, but just doing this for code quality reasons.
        }

        if (index == 0) {
            addFirst(value);
            return;
        }

        LinkedListNode<T> prev = head;
        for (int i = 0; i < index - 1; i++) {
            prev = prev.next;
        }

        LinkedListNode<T> newNode = new LinkedListNode<>(value);
        newNode.next = prev.next;
        prev.next = newNode;

        size++;
    }

    @Override
    public void delete(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(OUT_OF_BOUNDS_MESSAGE); // NOTE: Not going to handle the exceptions, but just doing this for code quality reasons.
        }

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
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(OUT_OF_BOUNDS_MESSAGE); // NOTE: Not going to handle the exceptions, but just doing this for code quality reasons.
        }

        LinkedListNode<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }

        return current.value;
    }

    @Override
    public void removeFirst() {
        if (head == null) {
            throw new IllegalStateException("List is empty"); // NOTE: Again not handling this somewhere.
        }

        head = head.next;
        size--;
    }

    @Override
    public T getFirst() {

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


