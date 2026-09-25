public class SingleLinkedList {
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
      newNode.nextNode = head;
      head = newNode;
      
      if (tail == null) {
         tail = head;
      }
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
      if (head != null){
         head = head.nextNode;

         if (head == null) {
            tail = null;
         }
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
         tail = tail.nextNode;
      }
   }

   public int topBack() {
      return (head != null) ? head.key : tail.key;
   }

   public void popback(){
      if (head != null && head.nextNode != null) {
         Node probe = head;
         
         while (probe.nextNode.nextNode != null){
            probe = probe.nextNode;
         }
         
         probe.nextNode = null;
         tail = probe;
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
         head = head.nextNode;
         return;
      }

      Node current = head;

      while (current.nextNode != null) {

         if (current.nextNode.key == key) {
            current.nextNode = current.nextNode.nextNode;
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