public class CircularSingleLinkedList {
    Node head;
    Node tail;

    public class Node
    {
        int key;
        Node nextNode;

        public Node(int key) {
            this.key = key;
            this.nextNode = null;
        }
    }

    public boolean isEmpty(){
        return (head == null);
        
    }

    public void pushFront(int key) {
        Node newNode = new Node(key);
        if (head == null) {
            head = tail = newNode;
            newNode.nextNode = tail;
            tail.nextNode = head;
            return; // si no colocaba esto se ejutaba el siguiente bloque
        }

        tail.nextNode = newNode; // invariante tail.nextNode = head;
        newNode.nextNode = head;
        head = newNode;
    }

    public int topFront(){
        if (head != null) {
            return head.key;
        }
        else{
            throw new IllegalStateException("error");
        }
    }

    public void popFront(){
        if (tail != null){
            if (tail == head) {
                tail = head = null;
                return;
            }

            tail.nextNode = head.nextNode;
            head = null; // esto sirve para limpiar la memoria del nodo a eliminar??
            head = tail.nextNode;
        }
    }

    public void pushBack(int key) {
        Node newNode = new Node(key);

        if (head == tail){
            head = tail = newNode;
            tail.nextNode = head;
        }
        else {
            tail.nextNode = newNode;
            newNode.nextNode = head;
            tail = tail.nextNode;
        }
    }

    public int topBack() {
        if (isEmpty()) {
            throw new IllegalStateException("error");
        }

        return tail.key;
    }

    public void popback(){
        if (head != tail) {
            Node probe = head;
            
            while (probe.nextNode != tail){ // llega al nodo antes de tail
                probe = probe.nextNode;
            }
            
            tail = null; // elimino el nodo
            probe.nextNode = head;
            tail = probe;
        }
        else {
            head = null;
            tail = null;
        }
    }

    public boolean find(int key) {
        if (isEmpty()) return false;

        Node probe = head;
        
        do {
            if (probe.key == key) {
                return true;
            }
            probe = probe.nextNode;
        } while (probe != head); // aca toca usar un do while porque si el siguiente nodo es el head no servia

        return false;
    }
    
    // =========================
    // Erase
    // O(n)
    // Removes first occurrence
    // =========================
    public void erase(int key) {

        if (head == null) {
            return;
        }

        // The element is at the beginning
        if (head.key == key) {
            if (head == tail) {
                head = tail = null;
            } else {
                tail.nextNode = head.nextNode;
                head = head.nextNode;
            }
            return;
        }

        Node current = head;

        while (current.nextNode != head) { // llega hasta antes de tail porque al volverse tail, sale del bucle
            if (current.nextNode.key == key) {
                if (current.nextNode == tail) {
                    tail = null;
                    current.nextNode = head;
                    tail = current;
                }
                else {
                    current.nextNode = current.nextNode.nextNode;
                }

                return;
            }

            current = current.nextNode;
        }
    }
    
    // =========================
    // Print
    // O(n)
    // =========================
    public void printList() {

        System.out.println("Head -> ");

        Node current = head;

        do {
            System.out.print(current.key + " -> ");
            current = current.nextNode;
        } while (current != head); 

        System.out.println("Tail  ->  Head");
    }

    // // =========================
    // // AddAfter
    // // O(1) if node is known
    // // =========================
    // private void addAfter(Node node, int key) {

    //     if (node == null) {
    //         throw new IllegalArgumentException("Node cannot be null");
    //     }

    //     Node newNode = new Node(key);

    //     newNode.nextNode = node.nextNode;
    //     node.nextNode = newNode;
    // }

    // // =========================
    // // AddBefore
    // // O(n)
    // // =========================
    // private void addBefore(Node node, int key) {

    //     if (node == null) {
    //         throw new IllegalArgumentException("Node cannot be null");
    //     }

    //     // Insert before head
    //     if (node == head) {
    //         pushFront(key);
    //         return;
    //     }

    //     Node current = head;

    //     // Find predecessor
    //     while (current != null && current.nextNode != node) {
    //         current = current.nextNode;
    //     }

    //     if (current == null) {
    //         throw new IllegalArgumentException(
    //             "Node does not belong to the list"
    //         );
    //     }

    //     Node newNode = new Node(key);

    //     newNode.nextNode = node;
    //     current.nextNode = newNode;
    // }

}
