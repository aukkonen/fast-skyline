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

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Skyline
{
    private Map<String,Object> opts;  // used when setting options from R
    
    private HashMap<String,SkylineAlg> algorithms;
    private HashMap<String,Evaluator> evaluators;

    private Map<String,String> evaluatorStats;

    public Skyline() {
        this.opts = new HashMap<>();

        this.algorithms = new HashMap<>();
        this.evaluators = new HashMap<>();

        this.algorithms.put( "exact",     new ExactSkyline() );
        this.algorithms.put( "levelwise", new LevelwiseSkyline() );
        this.algorithms.put( "fast",      new FastSkyline() );
        this.algorithms.put( "parallel",  new ExactSkylineParallel() );
        this.algorithms.put( "scalar",    new ScalarizationSkyline() );
        
        this.evaluators.put( "sgset",    new SubgroupSetEvaluatorIntCover() );
        this.evaluators.put( "sgsetbit", new SubgroupSetEvaluatorBitCover() );
        this.evaluators.put( "infmax",   new InfmaxEvaluator() );
        this.evaluators.put( "setcover", new SetCoverEvaluator() );
    }

    public void setOption( String optname, String value )
    {
        this.opts.put( optname, value );
    }

    public void setOption( String optname, double[] value )
    {
        this.opts.put( optname, value );
    }

    public void setOption( String optname, int[] value )
    {
        this.opts.put( optname, value );
    }

    public void setOption( String optname, int value )
    {
        this.opts.put( optname, value );
    }
    
    public void setOptions( Map<String,Object> options )
    {
        this.opts = options;
    }

    public String getEvaluatorStat( String name )
    {
        return this.evaluatorStats.get( name );
    }

    public double[] run( int k )
        throws Exception
    {
        SkylineAlg alg = this.algorithms.get( (String)this.opts.get( "a" ) );
        alg.configure( this.opts );

        Evaluator eval = this.evaluators.get( (String)this.opts.get( "e" ) );
        eval.init( this.opts );

        double[] sl = Utils.skylineAsArray( alg.computeSkyline( k, eval ) );

        this.evaluatorStats = eval.getStats();
        System.err.printf( "peak memory usage: %d MB\n", alg.getPeakMemory()/(1024*1024) );

        return sl;
    }
    
    public static void main( String[] args )
        throws Exception
    {
        Map<String,Object> options = parseArgs( args );

        Skyline sl = new Skyline();
        sl.setOptions( options );
        
        int k = Integer.parseInt( (String)options.get( "k" ) );

        sl.run( k );
        System.err.println( sl.evaluatorStats );
    }

    private static Map<String,Object> parseArgs( String[] args )
    {
        Map<String,Object> options = new HashMap<>();

        for ( int i = 0; i < args.length; i++ ) {
            String token = args[i];
            if ( token.charAt(0) == '-' ) {
                // token is a flag, insert it into options with empty value
                // (this is done because some flags do not require a value,
                // they set some property by themselves)
                options.put( token.substring(1), "" );
            }
            else {
                // token is a value
                if ( i == 0 ) {
                    // 1st token must always be a flag
                    System.err.printf( "Error: malformed parameter flag %s\n", token );
                    System.exit(-1);
                }
                String prevToken = args[i-1];
                if ( prevToken.charAt(0) == '-' ) {
                    // prev token is a flag, replace its value with token
                    options.put( prevToken.substring(1), token );
                }
                else {
                    // a value must always be preceded by a flag
                    System.err.printf( "Error: malformed parameter flag %s\n", prevToken );
                    System.exit(-1);
                }
            }
        }
        return options;
    }
}
