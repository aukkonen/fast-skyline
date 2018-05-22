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

import java.util.Map;
import java.util.HashMap;

public abstract class Evaluator
{
    protected boolean maxSum = false;

    protected int[] origItem = null;

    protected int sumCalls  = 0;
    protected int submCalls = 0;
    
    public void init( Map<String,Object> options ) throws Exception
    {
        this.maxSum  = options.containsKey( "max" );
    }

    public boolean maxSum()
    {
        return this.maxSum;
    }

    public int[] actualItems( int[] indexes )
    {
        int[] a = new int[ indexes.length ];
        for ( int i = 0; i < a.length; i++ ) {
            a[i] = this.origItem[ indexes[i] ];
        }
        return a;
    }

    public abstract double sumFnc( int item );
    
    public abstract double sumFnc( int[] itemset );

    public abstract double submodularFnc( int item );

    public abstract double submodularFnc( int[] itemset );

    public abstract int numItems();

    public void printStats()
    {
        System.err.printf( "sumCalls = %d, submCalls = %d\n", this.sumCalls, this.submCalls );
    }
    
    public Map<String,String> getStats()
    {
        Map<String,String> stats = new HashMap<>();
        stats.put( "sumCalls", String.valueOf( this.sumCalls ) );
        stats.put( "submCalls", String.valueOf( this.submCalls ) );
        return stats;
    }
}
