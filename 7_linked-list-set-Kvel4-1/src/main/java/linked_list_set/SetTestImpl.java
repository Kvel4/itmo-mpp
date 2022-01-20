package linked_list_set;

import kotlinx.atomicfu.AtomicInt;
import kotlinx.atomicfu.AtomicRef;

import java.util.concurrent.atomic.AtomicMarkableReference;


@SuppressWarnings("ALL")
public class SetTestImpl implements Set {
    private static class Node {
        AtomicMarkableReference<Node> next;
        int x;

        Node(int x, Node next) {
            this.next = new AtomicMarkableReference<>(next, false);
            this.x = x;
        }
    }

    private static class Window {
        Node cur, next;
    }

    private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        Window w = new Window();
        boolean[] mark = new boolean[1];

        retry:
        while (true) {
            w.cur = head;
            w.next = w.cur.next.getReference();

//            while (w.next.x < x) {
//                Node node = w.next.next.get(mark);
//                if (mark[0]) {
//                    if (!w.cur.next.compareAndSet(w.next, node, false,false)) {
//                        continue retry;
//                    }
//                    w.next = node;
//                } else {
//                    w.cur = w.next;
//                    w.next = w.cur.next.getReference();
//                }
//            }
            while (true) {
                Node node = w.next.next.get(mark);
                while (mark[0]) {
                    if (!w.cur.next.compareAndSet(w.next, node,false, false)) {
                        continue retry;
                    }
                    w.next = node;
                    node = w.next.next.get(mark);
                }
                if (w.next.x < x) {
                    w.cur = w.next;
                    w.next = w.cur.next.getReference();
                } else {
                    break;
                }
            }
//            Node node = w.next.next.get(mark);
//            if (mark[0]) {
//                w.cur.next.compareAndSet(w.next, node, false, false);
//                continue retry;
//            }

            return w;
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x == x) return false;


            if (w.cur.next.compareAndSet(w.next, new Node(x, w.next), false, false)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x != x)  {
                return false;
            }

//            if (w.cur instanceof Removed) {
//                System.out.println("asd");
//            }

//            if (w.cur.next.compareAndSet(w.next, new Removed(w.next))) {
//                findWindow(x);
//                w.cur.next = w.next.next;
//                w.cur.next.compareAndSet(w.next, w.next.next.getValue());
//                return true;
            Node node = w.next.next.getReference();
            if (w.next.next.compareAndSet(node, node, false, true)) {
//                w.cur.next.compareAndSet(w.next, node, false, false);
                return true;
            }

        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        return w.next.x == x;
    }
}