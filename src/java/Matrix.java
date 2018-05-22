public class Matrix<T>
{
    private Object[] data;
    private int nrow;
    private int ncol;
    
    public Matrix( int nrow, int ncol )
    {
        this.data = new Object[ nrow * ncol ];
        this.nrow = nrow;
        this.ncol = ncol;
    }

    public T get( int i, int j )
    {
        return (T)this.data[ j*nrow + i ];
    }

    public void set( int i, int j, T value )
    {
        this.data[ j*nrow + i ] = value;
    }
}
