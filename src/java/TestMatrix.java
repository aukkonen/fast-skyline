public class TestMatrix
{
    public static void main( String[] args ) {
        Matrix<Integer> A = new Matrix<>( 5, 5 );
        A.set( 0, 0, 1 );
        System.err.println( A.get( 0, 0 ) );

        Matrix<Double> B = new Matrix<>( 5, 5 );
        B.set( 0, 0, 1.45 );
        System.err.println( B.get( 0, 0 ) );
    }
}
