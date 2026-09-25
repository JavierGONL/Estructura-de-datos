public class CircularDoubleLinkedList {
    Node head;
    Node tail;

    public class Node
    {
        int key;
        Node nextNode;
        Node prevNode;

        public Node(int key) {
            this.key = key;
            this.nextNode = null;
            this.prevNode = null;
        }
    }

    public boolean isEmpty(){
        return (head == null);
    }

    public void pushFront(int key) {
        Node newNode = new Node(key);

        if (isEmpty()) { // basicamente crear la lista
            head = tail = newNode;
            newNode.nextNode = tail;
            newNode.prevNode = head;
            return; // si no colocaba esto se ejutaba el siguiente bloque
        }

        tail.nextNode = newNode;
        newNode.nextNode = head;
        head.prevNode = newNode;
        newNode.prevNode = tail;
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
        if (tail != null) { // si tail no es nulo, apunta a head siempre
            if (tail == head) {
                tail = head = null;
                return;
            }

            tail.nextNode = head.nextNode; // salta al nodo inmediatamente siguienta a head 
            head = null; // esto sirve para limpiar la memoria del nodo a eliminar??
            head = tail.nextNode;
            head.prevNode = tail;
        }
    }

    public void pushBack(int key) { // agregar al ultimo
        Node newNode = new Node(key);

        if (isEmpty()){
            head = tail = newNode;
            newNode.nextNode = tail;
            newNode.prevNode = head;
            return; // si no colocaba esto se ejutaba el siguiente bloque
        }
        else {
            tail.nextNode = newNode;
            head.prevNode = newNode;
            newNode.nextNode = head;
            newNode.prevNode = tail;
            tail = tail.nextNode;
        }
    }

    public int topBack() {
        if (isEmpty()) { // si no esta vacio tengo un ultimo valor
            throw new IllegalStateException("error");
        }

        return tail.key;
    }

    public void popback(){
        if (head != tail) { // esta condicion me asegura minimo 2 nodos
            head.prevNode = tail.prevNode;
            tail.prevNode.nextNode = head;
            tail = null;
            tail = head.prevNode;
        }
        else {
            head = null;
            tail = null;
        }
    }

    public boolean find(int key) {
        if (isEmpty()) return false;

        Node probe = head;
            
        while (probe.nextNode != head){
            if (probe.key == key) {
                return true;
            }
            probe = probe.nextNode;
        }
        return false;
    }
    
    // =========================
    // Erase
    // O(n)
    // Removes first occurrence
    // =========================
    public void erase(int key) {

        if (isEmpty()) {
            return;
        }

        // The element is at the beginning
        if (head.key == key) {
            if (head == tail) {
                head = tail = null;
            } else { // este else entra solo si son 2 nodos o mas
                tail.nextNode = head.nextNode;
                head.nextNode.prevNode = tail; 
                head = null;
                head = tail.nextNode;
            }
            return;
        }

        // aca son nodos intermedio o el ultimo
        Node current = head;

        while (current.nextNode != head) { // llega hasta antes de tail porque al volverse tail, sale del bucle
            if (current.nextNode.key == key) {
                if (current.nextNode == tail) {
                    tail = null;
                    head.prevNode = current;
                    current.nextNode = head;
                    tail = current;
                }
                else {
                    current.nextNode.nextNode.prevNode = current;
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

        Node current = head;

        while (current != head) {
            System.out.print(current.key + " -> ");
            current = current.nextNode;
        }

        System.out.println("null");
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
