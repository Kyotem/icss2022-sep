package nl.han.ica.datastructures;

import java.util.ArrayList;

public class HANStack implements IHANStack {

    private ArrayList<Object> stack = new ArrayList<>();

    @Override
    public void push(Object value) {
        stack.add(value);
    }

    @Override
    public Object pop() {
        if (stack.isEmpty())
            return null;

        return stack.remove(stack.size() - 1);
    }

    @Override
    public Object peek() {
        if (stack.isEmpty())
            return null;

        return stack.get(stack.size() - 1);
    }
}
