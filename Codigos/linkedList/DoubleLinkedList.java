public class DoubleLinkedList {
   public Node head;
   public Node tail;

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
        if (head == null) {
            head = newNode;
            tail = head;
        }
        else {
            newNode.nextNode = head; // newNode -> head
            head.prevNode = newNode; // newNode <- head
            head = newNode; // newNode = head
        }
    }

    public int topFront() {
        if (head != null) {
            return head.key;
        }
        else{
            throw new IllegalStateException("No se puede retornar nada de una lista vacia");
        }
    }

    public void popFront() {
        if (head != null) {
            head = head.nextNode;
            if (head == null) {
                tail = null;
                return;
            }
            head.prevNode = null;
        }
   }

    public void pushBack(int key) {
        Node newNode = new Node(key);
        if (head == null){
            head = newNode;
            tail = head;
        }
        else {
            tail.nextNode = newNode;
            newNode.prevNode = tail;
            tail = tail.nextNode;
        }
    }    

    public int topBack() {
        
        if (head != null) {
            return tail.key;
        }
        else {
            throw new IllegalStateException("No se puede retornar nada de una lista vacia");
        }
    }

    public void popBack(){

        if (head != null && head.nextNode != null) {
            tail = tail.prevNode;
            tail.nextNode = null; 
        }
        else {
            head = null;
            tail = null;
        }
    }

    public boolean find(int key) {

        Node probe = head;
            
        while (probe != null){
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

        if (head == null) {
            return;
        }

        // The element is at the beginning
        if (head.key == key) {
            if (head.nextNode != null) {
                head.nextNode.prevNode = null;
                head = head.nextNode;
            }
            else {
                head = null;
                tail = null;
            }
            return;
        }

        Node current = head.nextNode;

        while (current != null) {

            if (current.key == key) {
                if (current.nextNode != null) {
                    current.prevNode.nextNode = current.nextNode;
                    current.nextNode.prevNode = current.prevNode;
                }
                else {
                    current.prevNode.nextNode = null;
                    tail = current.prevNode;
                }
                return;
            }

            current = current.nextNode;
        }
    }

    // =========================
    // AddAfter
    // O(1) if node is known
    // =========================
    private void addAfter(Node node, int key) {

        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        Node newNode = new Node(key);

        newNode.nextNode = node.nextNode;
        node.nextNode = newNode;
    }

   // =========================
   // AddBefore
   // O(n)
   // =========================
    private void addBefore(Node node, int key) {

        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        // Insert before head
        if (node == head) {
            pushFront(key);
            return;
        }

        Node current = head;

        // Find predecessor
        while (current != null && current.nextNode != node) {
            current = current.nextNode;
        }

        if (current == null) {
            throw new IllegalArgumentException(
                "Node does not belong to the list"
            );
        }

        Node newNode = new Node(key);

        newNode.nextNode = node;
        current.nextNode = newNode;
    }

   // =========================
   // Print
   // O(n)
   // =========================
    public void printList() {

        Node current = head;

        while (current != null) {
            System.out.print(current.key + " -> ");
            current = current.nextNode;
        }

        System.out.println("null");
    }
}