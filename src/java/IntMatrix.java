// stores an integer matrix in column major format
public class IntMatrix
{
    private int[] data;
    private int   nrow;
    private int   ncol;

    public IntMatrix( int nrow, int ncol )
    {
        this.data = new int[ nrow*ncol ];
        this.nrow = nrow;
        this.ncol = ncol;
    }

    public IntMatrix( int[] data, int nrow, int ncol )
    {
        if ( data.length != nrow*ncol ) {
            throw new IllegalArgumentException( "Data size does not match matrix dimensions!" );
        }
        this.data = data;
        this.nrow = nrow;
        this.ncol = ncol;
    }

    public int nrow()
    {
        return this.nrow;
    }

    public int ncol()
    {
        return this.ncol;
    }

    public int get( int i, int j )
    {
        return this.data[ j*nrow + i ];
    }

    public void set( int i, int j, int value )
    {
        this.data[ j*nrow + i ] = value;
    }
}
