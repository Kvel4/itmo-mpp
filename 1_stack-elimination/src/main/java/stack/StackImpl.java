package stack;

import kotlin.random.Random;
import kotlinx.atomicfu.AtomicArray;
import kotlinx.atomicfu.AtomicRef;


public class StackImpl implements Stack {
    private static final int ELIMINATION_SIZE = 15;
    private static final int SPIN_WAIT = 50;
    private static final int SEARCHING_RANGE = 5;

    private final AtomicRef<Node> head = new AtomicRef<>(null);
    private final AtomicArray<Integer> elimination = new AtomicArray<>(ELIMINATION_SIZE);


    @Override
    public void push(int x) {
        int rand = Random.Default.nextInt(ELIMINATION_SIZE);

        for (int i = Math.max(0, rand - SEARCHING_RANGE); i < Math.min(ELIMINATION_SIZE, rand + SEARCHING_RANGE); i++) {
            AtomicRef<Integer> el = elimination.get(i);
            Integer X = x;

            if (el.compareAndSet(null, X)) {
                for (int k = 0; k < SPIN_WAIT; k++) {
                    if (el.getValue() == null) return;
                }
                if (!el.compareAndSet(X, null)) return;
                break;
            }
        }

        while (true) {
            Node curHead = head.getValue();
            if (head.compareAndSet(curHead, new Node(x, curHead))) return;
        }
    }

    @Override
    public int pop() {
        int rand = Random.Default.nextInt(ELIMINATION_SIZE);

        for (int i = Math.max(0, rand - SEARCHING_RANGE); i < Math.min(ELIMINATION_SIZE, rand + SEARCHING_RANGE); i++) {
            AtomicRef<Integer> el = elimination.get(i);
            Integer value = el.getValue();

            if (value != null) {
                if (el.compareAndSet(value, null)) {
                    return value;
                }
            }
        }

        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) return Integer.MIN_VALUE;
            if (head.compareAndSet(curHead, curHead.next.getValue())) return curHead.x;
        }
    }


    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }
}
