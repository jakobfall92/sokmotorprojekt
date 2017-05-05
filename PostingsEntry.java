/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.io.Serializable;
import java.util.ArrayList;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {
    
    public int docID;
    public double score;
    public ArrayList positionList = new ArrayList();

    /**
     *  PostingsEntries are compared by their score (only relevant 
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public PostingsEntry(int docID, int position) {
        this.docID=docID;
        positionList.add(position); //add position of first match in document
    }

    public PostingsEntry(int docID, ArrayList position) {
        this.docID=docID;
        this.positionList = position; //add position of first match in document
    }

    public PostingsEntry(int docID, double score) {
        this.docID=docID;
        this.score = score; //add calculated rank score
    }

    public int compareTo( PostingsEntry other ) {
	   return Double.compare( other.score, score );
    }

    public int getDocId() {
        return docID;
    }

    public ArrayList getPositionList() {
        return positionList;
    }

    public void addToPositionList(int position){ //adds position of token in documment to list
        positionList.add(position);
    }

    public int matchesInDoc() {
        return positionList.size();
    }

    public double getScore(){
        return score;
    }   
}

    
