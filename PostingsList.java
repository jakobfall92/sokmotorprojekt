/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.io.Serializable;
import java.util.*;

/**
 *   A list of postings for a given word.
 */
public class PostingsList implements Serializable {
    
    /** The postings list as a linked list. */
    private LinkedList<PostingsEntry> list = new LinkedList<PostingsEntry>();


    /**  Number of postings in this list  */
    public int size() {
	   return list.size();
    }

    /**  Returns the ith posting */
    public PostingsEntry get( int i ) {
	   return list.get(i);
    }

    public int docIdIndex (int matchDocID) {
        int lowerIndex = 0;
        if (list.size() > 0) {
            int upperIndex = list.size()-1;
        }
        int upperIndex = 0;
        while (lowerIndex <= upperIndex) {
            int mid = lowerIndex + (lowerIndex - upperIndex)/2;
            int currentDocId = list.get(mid).getDocId();
            if (matchDocID == currentDocId) {
                return mid;
            } else if (currentDocId < matchDocID) {
                lowerIndex = mid+1;
            } else if (currentDocId > matchDocID) {
                upperIndex = mid-1;
            }    
        }
        return -1;   
    }

    public void addEntry(PostingsEntry entry) {
        list.add(entry);
    }

    public ListIterator<PostingsEntry> getIterator() {
        return list.listIterator(0);
    }

    public void sortScore() {
        Collections.sort(list, new Comparator<PostingsEntry>(){
            @Override
            public int compare(PostingsEntry o1, PostingsEntry o2){
                if(o1.getScore() > o2.getScore()){
                    return -1; 
                }
                if(o1.getScore() < o2.getScore()){
                   return 1; 
                }
                return 0;
            }
        });
    }
}
	

			   
