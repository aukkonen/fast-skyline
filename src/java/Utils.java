// The MIT License (MIT)

// Copyright (c) 2015 Antti Ukkonen

// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:

// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Random;
import java.io.File;

public class Utils
{
    public static int[] range( int k )
    {
        int[] x = new int[ k ];
        for ( int i = 0; i < k; i++ ) {
            x[i] = i;
        }
        return x;
    }

    // Range from startpos (inclusive) to endpos (exclusive).
    public static int[] range( int startpos, int endpos )
    {
        int[] x = new int[ endpos-startpos ];
        for ( int i = 0; i < endpos-startpos; i++ ) {
            x[i] = startpos+i;
        }
        return x;
    }

    public static int[] arraySuffix( int[] array, int beginPos )
    {
        int[] suffix = new int[ array.length - beginPos ];
        System.arraycopy( array, beginPos, suffix, 0, suffix.length );
        return suffix;
    }

    // Checks if set A is a subset of set B.
    public static boolean isSubset( int[] setA, int[] setB )
    {
        for ( int value : setA ) {
            if ( arrayIndexOf( value, setB ) == -1 ) {
                return false;
            }
        }
        return true;
    }

    public static int arrayIndexOf( int value, int[] array )
    {
        for ( int i = 0; i < array.length; i++ ) {
            if ( array[i] == value ) {
                return i;
            }
        }
        return -1;
    }

    public static double arrayMax( double[] array )
    {
        double m = array[0];
        for( int i = 1; i < array.length; i++ ) {
            m = Math.max( m, array[i] );
        }
        return m;
    }

    // Returns an array x of indices into values st. values[x[i]]
    // is the i:th smallest (or largest if decreasing=true) value in values.
    public static int[] sortedIdx( double[] values, boolean decreasing )
    {
        Integer[] tmp = new Integer[ values.length ];
        for ( int i = 0; i < tmp.length; i++ ) {
            tmp[i] = i;
        }
        Arrays.sort( tmp, new Comparator<Integer>() {
                public int compare( Integer a, Integer b )
                {
                    if ( decreasing ) {
                        return (int)Math.signum( values[b] - values[a] );
                    }
                    else {
                        return (int)Math.signum( values[a] - values[b] );
                    }
                }
            } );
        int[] idx = new int[ tmp.length ];
        for ( int i = 0; i < idx.length; i++ ) {
            idx[i] = tmp[i];
        }
        return idx;
    }

    public static IntMatrix rearrangeColumns( IntMatrix orig, int[] permutation )
    {
        IntMatrix output = new IntMatrix( orig.nrow(), orig.ncol() );
        for ( int row = 0; row < orig.nrow(); row++ ) {
            for ( int item = 0; item < orig.ncol(); item++ ) {
                output.set( row, item, orig.get( row, permutation[item] ) );
            }
        }
        return output;
    }

    public static BigInteger nchoosek( int n, int k )
    {
        return factorial(n).divide( factorial(k).multiply( factorial(n-k) ) );
    }

    public static BigInteger factorial( int n )
    {
        if ( n == 0 ) {
            return BigInteger.ONE;
        }
        BigInteger v = BigInteger.valueOf( (long)n );
        for ( int i = n-1; i >= 1; i-- ) {
            v = v.multiply( BigInteger.valueOf( (long)i ) );
        }
        return v;
    }

    public static Iterable<String> fileLineIterator( final String filename,
                                                     boolean skipHeader )
        throws Exception
    {
        final Scanner scanner = new Scanner( new File(filename) );
        scanner.useDelimiter( "\n" );

        if ( skipHeader && scanner.hasNext() ) {
            scanner.next();
        }
        
        return new Iterable<String>()
        {
            public Iterator<String> iterator()
            {
                return new Iterator<String>()
                {
                    public boolean hasNext()
                    {
                        boolean b = scanner.hasNext();
                        if ( b == false ) {
                            // We close up shop!
                            scanner.close();
                        }
                        return b;
                    }

                    public String next()
                    {
                        return scanner.next();
                    }
                };
            }
        };
    }

    // This returns samples WITH REPLACEMENT!!
    public static int[] sample( int n, int k )
    {
        Random rnd = new Random();
        int[] s = new int[ k ];
        for ( int i = 0; i < k; i++ ) {
            s[i] = rnd.nextInt( n );
        }
        return s;
    }

    public static int arrayHashCode( int[] arr )
    {
        int value = 0x345678;
        for ( int i : arr ) {
            value = value*1000003 ^ i;
        }
        return value ^ arr.length;
    }

    // Used by the R interface.
    // Returns the data array of a matrix where the 1st column is sumFnc,
    // the 2nd column is submodularFnc, and the remaining columns
    // are indexes of the subgroups.
    // The matrix will have as many rows as there are points in the skyline.
    public static double[] skylineAsArray( List<SkylinePoint> skyline )
    {
        int numPoints = skyline.size();
        int k         = skyline.get(0).size();
        double[] s    = new double[ numPoints * (k+2) ];

        int i = 0;
        for ( SkylinePoint p : skyline ) {
            s[i]           = p.sumValue();
            s[i+numPoints] = p.submodularValue();
            for ( int j = 0; j < p.size(); j++ ) {
                s[i + (j+2)*numPoints ] = p.itemAt( j );
            }
            i++;
        }
        return s;
    }

    public static Comparator<SkylinePoint> getSkylinePointComparator( boolean maxSum )
    {
        if ( maxSum ) {
            return new Comparator<SkylinePoint>() {
                public int compare( SkylinePoint a, SkylinePoint b ) {
                    return (int)Math.signum( b.sumValue() - a.sumValue() );
                }
            };
        }
        else {
            return new Comparator<SkylinePoint>() {
                public int compare( SkylinePoint a, SkylinePoint b ) {
                    return (int)Math.signum( a.sumValue() - b.sumValue() );
                }
            };
        }
    }

    public static void toActualItems( List<SkylinePoint> skyline,
                                      Evaluator eval )
    {
        for ( SkylinePoint p : skyline ) {
            p.toActualItems( eval );
        }
    }

    public static int lexicographicArrayComparison( int[] a, int[] b )
    {
        for ( int i = 0; i < a.length; i++ ) {
            if ( a[i] < b[i] ) {
                return -1;
            }
            else if ( a[i] > b[i] ) {
                return 1;
            }
        }
        return 0;
    }

    public static int[] intListToArray( List<Integer> l )
    {
        int[] x = new int[ l.size() ];
        for ( int i = 0; i < x.length; i++ ) {
            x[i] = l.get(i);
        }
        return x;
    }
}
