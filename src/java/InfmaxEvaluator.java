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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InfmaxEvaluator extends Evaluator
{
    private SampleDB      db;
    private double[]      cost;
    private EdgelistGraph g;

    public InfmaxEvaluator() { }

    public void init( Map<String,Object> options )
        throws Exception
    {
        super.init( options ); // this sets maxSum
        String graphFileName = (String)options.get( "g" );
        double edgeProb      = Double.parseDouble( (String)options.get( "p" ) );
        int    numSamples    = Integer.parseInt( (String)options.get( "s" ) );
        init( graphFileName, edgeProb, numSamples );
    }

    public void init( int[] i, int[] j, double[] prob, int numSamples )
    {
        this.g  = new EdgelistGraph( i, j, prob );
        this.origItem = new int[ this.g.numVertices() ];
        for ( int l = 0; l < this.origItem.length; l++ ) {
            this.origItem[ l ] = l;
        }
        this.cost = new double[ this.g.numVertices() ]; // zero cost vertices
        this.db = new SampleDB( this.g, numSamples );
    }

    private void init( String graphFileName,
                       double edgeProb,
                       int numSamples )
        throws Exception
    {
        EdgelistGraph g = new EdgelistGraph( graphFileName, edgeProb );
        double[] c = initRandomCost( g.numVertices(), 1.0, 5.0 );
        this.origItem = Utils.sortedIdx( c, this.maxSum );
        this.cost = new double[ c.length ];
        for ( int i = 0; i < c.length; i++ ) {
            this.cost[ i ] = c[ this.origItem[i] ];
        }
        this.db = new SampleDB( g, numSamples );
    }
    
    public double sumFnc( int item )
    {
        this.sumCalls++;
        return this.cost[item];
    }

    public double sumFnc( int[] items )
    {
        this.sumCalls++;
        double s = 0.0;
        for ( int item : items ) {
            s += this.cost[item];
        }
        return s;
    }

    public double submodularFnc( int item )
    {
        this.submCalls++;
        return this.db.influence( item );
    }

    public double submodularFnc( int[] items )
    {
        this.submCalls++;
        return this.db.influence( items );
    }

    public int numItems()
    {
        return this.cost.length;
    }

    public int vertexIdToName( int id ) {
        return this.g.getVertexName( id );
    }

    private double[] initRandomCost( int numVertices, double minCost, double maxCost )
    {
        double[] costs = new double[ numVertices ];
        for ( int i = 0; i < costs.length; i++ ) {
            costs[i] = minCost + (maxCost - minCost)*Math.random();
        }
        return costs;
    }

    private class SampleDB
    {
        private List<Sample> samples;
        
        public SampleDB( EdgelistGraph g, int numSamples )
        {
            this.samples = new LinkedList<Sample>();
            for ( int i = 0; i < numSamples; i++ ) {
                this.samples.add( new Sample( g ) );
                System.err.print(".");
            }
            System.err.println();
        }

        public double influence( int item )
        {
            int influence = 0;
            for ( Sample s : this.samples ) {
                influence += ( s.influence(item) - 1 );
            }
            return (double)influence/(double)this.samples.size();
        }

        public double influence( int[] items )
        {
            int influence = 0;
            for ( Sample s : this.samples ) {
                influence += ( s.influence(items) - items.length );
            }
            return (double)influence/(double)this.samples.size();
        }
    }
    
    private class Sample
    {
        private int[][] reach;

        public Sample( EdgelistGraph g )
        {
            HashSet<Integer>[] R = new HashSet[ g.numVertices() ];
            HashSet<Integer>[] U = new HashSet[ g.numVertices() ];
            for ( int vertex = 0; vertex < g.numVertices(); vertex++ ) {
                R[vertex] = new HashSet<Integer>();
                R[vertex].add( vertex );
                U[vertex] = new HashSet<Integer>();
                U[vertex].add( vertex );
            }
            for ( Edge edge : g.edges ) {
                if ( Math.random() <= edge.weight ) {
                    HashSet<Integer> Uu = U[ edge.u ];
                    HashSet<Integer> Rv = R[ edge.v ];
                    for ( Integer uprime : Uu ) {
                        R[uprime].addAll( Rv );
                    }
                    for ( Integer vprime : Rv ) {
                        U[vprime].addAll( Uu );
                    }
                }
            }
            int[][] r = new int[g.numVertices()][0];
            for ( int vertex = 0; vertex < g.numVertices(); vertex++ ) {
                int[] rvertex = new int[ R[vertex].size() ];
                int i = 0;
                for ( int j : R[vertex] ) {
                    rvertex[i] = j;
                    i++;
                }
                r[vertex] = rvertex;
            }
            // rearrange r so that it matches with sorted costs
            // and put result in this.reach
            this.reach = new int[g.numVertices()][0];
            for ( int i = 0; i < this.reach.length; i++ ) {
                this.reach[i] = r[ origItem[i] ]; //origItem defined in Evaluator.java
            }
        }

        public int influence( int[] items ) {
            BitSet cover = new BitSet( this.reach.length );
            for ( int v : items ) {
                for ( int u : this.reach[v] ) {
                    cover.set( u );
                }
            }
            return cover.cardinality();
        }

        public int influence( int item ) {
            return this.reach[ item ].length;
        }
    }

    private class EdgelistGraph
    {
        private List<Edge> edges;
        private int nextNode = 0;
        private Map<Integer,Integer> idmap;
        private Map<Integer,Integer> namemap;

        // Loads an edgelist graph from file graphFn and associates
        // every edge with probability edgeProb.
        public EdgelistGraph( String graphFn, double edgeProb )
            throws Exception
        {
            this.idmap = new HashMap<>();
            this.namemap = new HashMap<>();
            this.edges = new ArrayList<>();
            for ( String row : Utils.fileLineIterator( graphFn, false ) ) {
                if ( row.charAt(0) == '#' ) {
                    continue;
                }
                String[] edges = row.trim().split( "\\s" );
                this.edges.add( new Edge( getVertexId( Integer.parseInt( edges[0] ) ),
                                          getVertexId( Integer.parseInt( edges[1] ) ),
                                          edgeProb ) );
                                                         
            }
            System.err.printf( "InfmaxEvaluator: Got %d vertices.\n", this.nextNode );
        }

        // Constructs and edgelist graph from row, column, weight triplets.
        // The weights should be interpreted as influence probabilities.
        public EdgelistGraph( int[] i, int[] j, double[] w ) {
            this.idmap   = new HashMap<>();
            this.namemap = new HashMap<>();
            this.edges   = new ArrayList<>();
            for ( int r = 0; r < i.length; r++ ) {
                this.edges.add( new Edge( getVertexId(i[r]),
                                          getVertexId(j[r]),
                                          w[r] ) );
            }
            System.err.printf( "InfmaxEvaluator: Got %d vertices.\n", this.nextNode );
        }

        public int numVertices()
        {
            return this.nextNode;
        }

        private int getVertexId( int vname )
        {
            if ( !this.idmap.containsKey( vname ) ) {
                this.idmap.put( vname, this.nextNode );
                this.namemap.put( this.nextNode, vname );
                this.nextNode++;
            }
            return this.idmap.get( vname );
        }

        // inverts the name->id mapping
        private int getVertexName( int vid )
        {
            return this.namemap.get( vid );
        }
    }

    private class Edge {
        private int    u;
        private int    v;
        private double weight;

        public Edge( int u, int v, double w ) {
            this.u = u;
            this.v = v;
            this.weight = w;
        }
    }
}
