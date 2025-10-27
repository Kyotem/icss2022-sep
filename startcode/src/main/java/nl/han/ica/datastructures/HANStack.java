package nl.han.ica.datastructures;

import java.util.ArrayList;

public class HANStack<T> implements IHANStack<T> {

    private ArrayList<T> stack = new ArrayList<>();

    @Override
    public void push(T value) {
        stack.add(value);
    }

    @Override
    public T pop() {
        if (stack.isEmpty())
            return null;

        return stack.remove(stack.size() - 1);
    }

    @Override
    public T peek() {
        if (stack.isEmpty())
            return null;

        return stack.get(stack.size() - 1);
    }
}
