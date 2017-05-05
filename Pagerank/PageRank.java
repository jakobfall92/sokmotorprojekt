/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2012
 */  

import java.util.*;
import java.io.*;

public class PageRank{

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();
    Hashtable<String,String> docTitle = new Hashtable<String,String>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a Hashtable, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a Hashtable whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    //Integer1: Local number of doc. Integer2: local number of the document that it is linked to.
    Hashtable<Integer,Hashtable<Integer,Boolean>> link = new Hashtable<Integer,Hashtable<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The number of documents with no outlinks.
     */
    int numberOfSinks = 0;

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.00001;

    /**
     *   Never do more than this number of iterations regardless
     *   of whether the transistion probabilities converge or not.
     */
    final static int MAX_NUMBER_OF_ITERATIONS = 1000;

    //Pagerank vector
    double[] pi = null;

    static boolean DEBUG = true;

    int nrPositionsShow = 30;

    
    /* --------------------------------------------- */


    public PageRank( String filename ) {
	   int noOfDocs = readDocs( filename );
	   computePagerank( noOfDocs );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and creates the docs table. When this method 
     *   finishes executing then the @code{out} vector of outlinks is 
     *   initialised for each doc, and the @code{p} matrix is filled with
     *   zeroes (that indicate direct links) and NO_LINK (if there is no
     *   direct link. <p>
     *
     *   @return the number of documents read.
     */
    
    /*void readTitles() {
        try {
            BufferedReader in = new BufferedReader( new FileReader( "filename" ));
            String line;
            while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
                int index = line.indexOf( ";" ); //The end of doc number
                int endIndex = line.length()-1;
                String nrDoc = line.substring( 0, index ); //Get doc number (not title of wikipedia page)
                String title = line.substring(index+1,endIndex);
                docTitle.put(nrDoc,title);
            }
        } catch ( FileNotFoundException e ) {
            System.err.println( "File not found!" );
        }
        catch ( IOException e ) {
            System.err.println( "Error reading file ");
        }
    }*/


    int readDocs( String filename ) {
	int fileIndex = 0;
	try {
	    System.err.print( "Reading file... " );
	    BufferedReader in = new BufferedReader( new FileReader( filename ));
	    String line;
	    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
    		int index = line.indexOf( ";" ); //The end of doc number
    		String title = line.substring( 0, index ); //Get doc number (not title of wikipedia page)
    		Integer fromdoc = docNumber.get( title ); //See if the doc has already been registered
    		//  Have we seen this document before?
    		if ( fromdoc == null ) { //	
    		    // This is a previously unseen doc, so add it to the table.
    		    fromdoc = fileIndex++;
    		    docNumber.put( title, fromdoc );
    		    docName[fromdoc] = title;
    		}
    		// Check all outlinks.
    		StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
    		while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
    		    String otherTitle = tok.nextToken(); //Get the document the page is linked to
    		    Integer otherDoc = docNumber.get( otherTitle ); //Get the local number for the document
    		    if ( otherDoc == null ) { //If the document hasn't been found yet
    		      // This is a previousy unseen doc, so add it to the table.
    		      otherDoc = fileIndex++;
    		      docNumber.put( otherTitle, otherDoc );
    		      docName[otherDoc] = otherTitle;
    		    }
    		    // Set the probability to 0 for now, to indicate that there is
    		    // a link from fromdoc to otherDoc.
    		    if ( link.get(fromdoc) == null ) { //If the document we are checking Hasn't got a link entry.
    		      link.put(fromdoc, new Hashtable<Integer,Boolean>()); //Add the structure that will hold the links
    		    }
    		    if ( link.get(fromdoc).get(otherDoc) == null ) { //There is no link between the documents yet.
    		      link.get(fromdoc).put( otherDoc, true ); //Set the link between the otherdoc(local)
    		      out[fromdoc]++;
    		    }
    		}
	    }
	    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
		System.err.print( "stopped reading since documents table is full. " );
	    }
	    else {
		System.err.print( "done. " );
	    }
	    // Compute the number of sinks.
	    for ( int i=0; i<fileIndex; i++ ) {
		if ( out[i] == 0 ) //find the docs that have 0 accumulated outgoing links.
		    numberOfSinks++;
	    }
	}
	catch ( FileNotFoundException e ) {
	    System.err.println( "File " + filename + " not found!" );
	}
	catch ( IOException e ) {
	    System.err.println( "Error reading file " + filename );
	}
	System.err.println( "Read " + fileIndex + " number of documents" );
	return fileIndex;
    }


    /* --------------------------------------------- */


    /*
     *   Computes the pagerank of each document.
     */
    void computePagerank( int numberOfDocs ) {
	   pi = powerIteration(numberOfDocs);
       showRank(numberOfDocs,pi);
    }


    /* --------------------------------------------- */

    public double[] powerIteration(int numberOfDocs) {
        //initiate the pagerank vector to number of docs found(that have links);
        double[] pi = new double[numberOfDocs];

        //Reset the iteration criterias
        boolean stopItteration = false;
        int nrIterations = 0;

        pi[0] = 1.0; //Assume that random surfer starts at document 0.
        for (int i=1;i<numberOfDocs;++i) {
            pi[i] = 0.0; //set all other probabilities of vector to 0.
        }

        double[] piNext = new double[numberOfDocs]; //t+1, the vector being calculated

        while((nrIterations < MAX_NUMBER_OF_ITERATIONS) && !stopItteration) {
            
            if (nrIterations%1 == 0) {
                System.err.println("Itterating, Iteration nr: "+(nrIterations+1)+" in progress!");    
            }
            
            for (int i=0;i<numberOfDocs;++i) { //update docs, on at a time
                piNext[i] = BORED/numberOfDocs; //Add probabilty that surfer will get bored and randomly choose this page
                for (int j=0;j<numberOfDocs;++j) {
                    Hashtable<Integer,Boolean> outlinks = link.get(j); //get outlinks for document j
                    if (outlinks == null) { //if the current document haven't got outlinks
                        piNext[i] += pi[j]*(1-BORED)/numberOfDocs; //If there are know links distribute the probability uniformly --> jumps to random node
                    } else {
                        if ((outlinks.get(i) != null) && outlinks.get(i)) { //check if  has a link to i.
                            piNext[i] += pi[j] * (1-BORED)/outlinks.size(); //Probability is pobability of being at j and chossing some of the outlinks 
                        }
                    }
                }
            }
            stopItteration = true;
            for (int i=0 ; i<numberOfDocs ; i++) {
              if ( Math.abs( piNext[i] - pi[i] ) > EPSILON ) { //True not enough covergence
                stopItteration = false;
                break;
              }
            }
            for (int i=0; i<numberOfDocs ; i++) {
                pi[i] = piNext[i]; //set the old vector to new vector
            }
            nrIterations++;
        }
        return pi;
    }

    public void showRank(int numberOfDocs, double[] pi){
       // Sort the pages by rank
        Integer[] iDs = new Integer[numberOfDocs]; //Array to hold docnumbers
        final Double[] vs = new Double[numberOfDocs]; //Array for page corresponding page rank
        for ( int i = 0 ; i < numberOfDocs ; ++i ) { 
            iDs[i] = i;
        }
        for ( int i = 0 ; i < numberOfDocs ; ++i ) {
            vs[i] = pi[i]; //this is ordered in the order that documents was inputed from begining.
        }
        Arrays.sort(iDs, new Comparator<Integer>() {
            @Override public int compare(final Integer o1, final Integer o2) {
                return -1 * Double.compare( vs[o1], vs[o2] );
            }
        });

        // Show the first ranked in the page rank.
        for ( int i = 0 ; i < nrPositionsShow; ++i ) {
            System.out.println(i+1 + ". " + docName[iDs[i]] + " " + pi[iDs[i]]); 
        }
    }




    public static void main( String[] args ) {
	if ( args.length != 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else {
	    new PageRank( args[0] );
	}
    }
}
