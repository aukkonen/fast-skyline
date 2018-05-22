public class DoubleMatrix
{
    private double[] data;
    private int      nrow;
    private int      ncol;

    public DoubleMatrix( int nrow, int ncol )
    {
        this.data = new double[ nrow*ncol ];
        this.nrow = nrow;
        this.ncol = ncol;
    }

    public double get( int i, int j )
    {
        return this.data[ j*nrow + i ];
    }

    public void set( int i, int j, double value )
    {
        this.data[ j*nrow + i ] = value;
    }
}
