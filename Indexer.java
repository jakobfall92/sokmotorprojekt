/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Third version:  Johan Boye, 2016
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.*;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.pdmodel.PDDocument;


/**
 *   Processes a directory structure and indexes all PDF and text files.
 */
public class Indexer {

    /** The index to be built up by this indexer. */
    public Index index = new HashedIndex();
    
    /** The next docID to be generated. */
    private int lastDocID = 0;

     /** The patterns matching non-standard words (e-mail addresses, etc.) */
    String patterns_file;
    boolean verbose = false;


    /* ----------------------------------------------- */


    /** Constructor */
    public Indexer( String patterns_file ) {
		this.patterns_file = patterns_file;
    }


    /** Generates a new document identifier as an integer. */
    private int generateDocID() {
		return lastDocID++;
    }



    /**
     *  Tokenizes and indexes the file @code{f}. If @code{f} is a directory,
     *  all its files and subdirectories are recursively processed.
     */
    public void processFiles( File f ) {
	// do not try to index fs that cannot be read
	if ( f.canRead() ) {
	    if ( f.isDirectory() ) { //First time will run here second time is file
			String[] fs = f.list();
			// an IO error could occur
		if ( fs != null ) { //we have our list of files
		    for ( int i=0; i<fs.length; i++ ) {
				processFiles( new File( f, fs[i] )); //recursice calls if we have actuall files to read
		    }
		}
	    } else {
			//System.err.println( "Indexing " + f.getPath() );
			// First register the document and get a docID
			int docID = generateDocID();
			//System.out.println("ID GENERATED FOR FILE: "+f.getName()+" ID: "+docID);
			if(docID > 15000) {
				if (docID%50 == 0) {
					System.err.println( "Indexed " + docID + " files" );
				}
			} else if (docID > 17600) {
				if (docID%5 == 0) {
					System.err.println( "Indexed " + docID + " files" );
				}
			} else if (docID > 17000) {
				if (docID%10 == 0) {
					System.err.println( "Indexed " + docID + " files" );
				}
			} else {
				if ( docID%200 == 0 ){
					System.err.println( "Indexed " + docID + " files" );	
				} 
			}
			index.docIDs.put( "" + docID, f.getName() ); //enters docid and name into index
			try {
			    //  Read the first few bytes of the file to see if it is 
			    // likely to be a PDF 
			    Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
			    char[] buf = new char[4];
			    reader.read( buf, 0, 4 );
			    reader.close();
			    if ( buf[0] == '%' && buf[1]=='P' && buf[2]=='D' && buf[3]=='F' ) {
					// We assume this is a PDF file
					try {
					    String contents = extractPDFContents( f );
					    reader = new StringReader( contents );
					}
					catch ( IOException e ) {
					    // Perhaps it wasn't a PDF file after all
					    reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
					}
			    }
			    else {
					// We hope this is ordinary text
					reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
			    }
			    if (docID == 17500) {
			    	verbose = false;
			    }
			    Tokenizer tok = new Tokenizer( reader, true, false, true, patterns_file,verbose);
			    int offset = 0;
			    while ( tok.hasMoreTokens() ) {
					String token = tok.nextToken();
					insertIntoIndex( docID, token, offset++ );
			    }
			    index.docLengths.put( "" + docID, offset );
			    reader.close();
			}
			catch ( IOException e ) {
			    System.err.println( "Warning: IOException during indexing." );
			}
	    }
	}
    }

    
    /* ----------------------------------------------- */


    /**
     *  Extracts the textual contents from a PDF file as one long string.
     */
    public String extractPDFContents( File f ) throws IOException {
		FileInputStream fi = new FileInputStream( f );
		PDFParser parser = new PDFParser( fi );   
		parser.parse();   
		fi.close();
		COSDocument cd = parser.getDocument();   
		PDFTextStripper stripper = new PDFTextStripper();   
		String result = stripper.getText( new PDDocument( cd ));  
		cd.close();
		return result;
    }


    /* ----------------------------------------------- */


    /**
     *  Indexes one token.
     */
    public void insertIntoIndex( int docID, String token, int offset ) {
		index.insert( token, docID, offset );
    }
}
	
