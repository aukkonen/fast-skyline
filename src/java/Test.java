import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Comparator;

public class Test
{
    public static void main( String[] args ) throws Exception {
        Comparator<Double> cmp1 = new Comparator<Double>() {
                public int compare( Double a, Double b ) {
                    return (int)Math.signum( b - a );
                }
            };
        Comparator<Double> cmp2 = new Comparator<Double>() {
                public int compare( Double a, Double b ) {
                    return (int)Math.signum( a - b );
                }
            };
        ConcurrentSkipListMap<Double,Double> l1 = new ConcurrentSkipListMap<>( cmp1 );
        ConcurrentSkipListMap<Double,Double> l2 = new ConcurrentSkipListMap<>( cmp2 );

        l1.put( Double.NEGATIVE_INFINITY, 0.0 );
        l1.put( Double.POSITIVE_INFINITY, 1.0 );

        l2.put( Double.NEGATIVE_INFINITY, 0.0 );
        l2.put( Double.POSITIVE_INFINITY, 1.0 );

        System.out.printf( "l1.floorKey(0.5) = %f\n", l1.floorKey(0.5) );
        System.out.printf( "l2.floorKey(0.5) = %f\n", l2.floorKey(0.5) );
    }
}
