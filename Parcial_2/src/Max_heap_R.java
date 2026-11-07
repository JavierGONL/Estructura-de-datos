public class Max_heap_R{
    private Registro[] monton = new Registro [1];
    private int size = 0;
    static int padre(int i) {return (i-1)/2;}
    static int left(int i) {return ((2*i)+1);}
    static int right(int i) {return ((2*i)+2);}
    static void swap(Registro[] monton, int i, int j){
        Registro t= monton[i];
        monton[i]=monton[j];
        monton[j]=t;
    }
    public int size() { return size; }
    public Registro max() {
        if (size == 0)
            throw new java.util.NoSuchElementException("no hay registros disponibles");
        return monton[0];
    }
    public int busqueda(int prioridad){
        if (size == 0)
            throw new java.util.NoSuchElementException("no hay registros disponibles");
        for (int i=0; i<size; i++){
            if (monton[i].prioridad== prioridad){
                return i;
            }
        }
        return -1;
    }
    public void insert(Registro v){
        if (size == monton.length){
            monton = java.util.Arrays.copyOf(monton,2*size);
        }
        monton[size] = v;
        size++;
        siftup(monton, size-1);
    }
    static void siftup(Registro [] monton, int i){
        while (i>0){
            if (monton[i].prioridad == monton[padre(i)].prioridad)
                if (monton[i].id < monton[padre(i)].id){
                    swap(monton, i, padre(i));
                }
            else if ((monton[i].prioridad> monton[padre(i)].prioridad)){
                swap(monton, i, padre(i));
            } 
            else {break;}
            i=padre(i);
        }
    }
    public Registro extractMax() {
        if (size == 0)
            throw new java.util.NoSuchElementException("no hay registros disponibles");
        Registro top = monton[0];
        size--;
        monton[0] = monton[size];
        siftDown(monton, 0, size);
        return top;
    }
    static void siftDown(Registro[] monton, int i, int m) {
        while (left(i) < m) {
            int c = left(i);
            if (c + 1 < m && monton[c + 1].prioridad > monton[c].prioridad) {
                c++;
            }
            if (monton[i].prioridad == monton[c].prioridad){
                if (monton[i].id < monton[c].id){
                    i=c;
                    continue;
                }
            }
            if (monton[i].prioridad > monton[c].prioridad) {
                break;
            }
            swap(monton, i, c);
            i = c;
        }
    }
    public static Registro[] buildheap(Registro[] monton){
        int n= monton.length;
        int x= (int)Math.floor(n/2);
        for (int i = x -1; i>0; i--){
            siftDown(monton, i, n);
        }
        return monton;
    }
    public void reconstruccion(int cancelado){
        for (int i= cancelado; i< size-1; i++){
            monton[i]= monton[i-1];

        }
    }
    public static Registro[] heapSort ( Registro [] a) {
        Registro[] O = a;
        int n = O.length ;
        O = buildheap(O);
        for ( int end = n - 1; end > 0; end--) {
            swap(O , 0 , end ); // max to end
            siftDown(O , 0, end ); // a [0.. end -1]
        }
        return O;
    } 
}