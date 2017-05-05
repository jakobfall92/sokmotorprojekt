/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */  


package ir;
import java.util.*;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();
    private HashMap<String,HashMap> docIDsInPostings = new HashMap<String,HashMap>();

    /**
     *  Inserts this token in the index.
     */
    public void insert( String token, int docID, int offset ) { //offset --> Where in document'
        //System.out.println("Inserting....");
        if (index.containsKey(token)) { //token already in dictionary
            PostingsList postList = index.get(token); //get that PostingsList
            HashMap<Integer,Integer> docIDsOfPosting = docIDsInPostings.get(token); //Get docIDs in this posting
            if(docIDsOfPosting.containsKey(docID)) { //if token has already been found earlier in same document
                int docIdIndex = docIDsOfPosting.get(docID); //get index of doc with id: docID
                PostingsEntry docsEntry = postList.get(docIdIndex); //Get entry for doc
                docsEntry.addToPositionList(offset); //add token positon info to entry
            } else {
                PostingsEntry docsEntry = new PostingsEntry(docID, offset);
                int docIndex = postList.size(); //index of new entry
                docIDsOfPosting.put(docID,docIndex); //add the docID and corresponding index to map
                postList.addEntry(docsEntry);
            }
        } else { //if token is not already in dictonary
            PostingsList postList = new PostingsList(); //creates new postinglist for the new token
            PostingsEntry docsEntry = new PostingsEntry(docID, offset); //creates new entry to add to list
            HashMap<Integer,Integer> docIdIndexList = new HashMap<Integer,Integer>(); 
            docIdIndexList.put(docID, 0);
            docIDsInPostings.put(token,docIdIndexList);
            postList.addEntry(docsEntry); //Add entry to newly created postinglist
            index.put(token,postList); //adds token and corresponding PostingsList to dictonary.
        }
    }


    /**
     *  Returns all the words in the index.
     */
    public Iterator<String> getDictionary() {
	   return index.keySet().iterator();
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
	   return index.get(token);
    }

    public String postingsString( String token ) {
        PostingsList postlist = index.get(token);
        int index = postlist.size();
        String msg = "TOKEN: " + token +"| FOUND IN DOCS(NR): "+ index+" |";
        for (int i = 0; i<index; i++) {
            PostingsEntry postE = postlist.get(i);
            int docId = postE.getDocId();
            int hits = postE.matchesInDoc();
            msg = msg + " " + hits + " MATCH(ES) IN DOCNR: "+docId+" |";
        }
        return msg;
    }

    /**
     *  Searches the index for postings matching the query.
     */
    public PostingsList search( Query query, int queryType, int rankingType, int structureType ) {
        int nrWords = query.size();
        System.out.println("Querytype: "+queryType);
        if ((nrWords == 1) && queryType != 2) {
            String queryToken = query.terms.get(0);
            System.out.println("QUERY RECIEVED: " + queryToken); 
            return index.get(queryToken);    
        } else if ((nrWords) > 1 && (queryType == 0)) {
            return intersectionSearch(query);
        } else if ((nrWords > 1) && (queryType == 1)) {
            return phraseSearch(query);
        } else if ((queryType == 2) && rankingType == 0) { //Rankedsearch using tf-idf
            return rankedSearchTfIdf(query);
        } else {
            return null;
        }   
    }

    public PostingsList phraseSearch(Query query) {
        System.out.println("***||PHRASE SEARCH||***");
        System.out.print("QUERY: ");
        for (int i=0;i<query.size();i++) {
            System.out.print(" "+query.terms.get(i));
        }
        System.out.print("\n");

        PostingsList matchingDocs = intersectionSearch(query);
        if(matchingDocs == null) {
            System.out.println("NO MATCHES FOUND AT INTERSECTION!");
            System.out.println("***||PHRASE SEARCH||***");
            return null;
        }
        int nrMatchingDoc = matchingDocs.size();
        int nrWords = query.size();
        System.out.println("DOCS TO BE EXAMINED: "+nrMatchingDoc);
        System.out.println("QUERY LENGTH: "+nrMatchingDoc);
        System.out.println("*--------LOG--------*");
        ListIterator<PostingsEntry> docsIterator = matchingDocs.getIterator();
        PostingsList returnPostingsList = new PostingsList();
        for (int j=0;j<nrMatchingDoc;j++) {
            System.out.println("-: Processing doc nr: "+j);
            System.out.println("_______________________");
            int currentDocId = docsIterator.next().getDocId();
            ArrayList<Integer> currentPositions = new ArrayList<Integer>(); //will hold valid positions for this itteration
            currentPositions = getPositions(query.terms.get(0),currentDocId); //Positions of first word in search phrase in current docId
            for (int i=0;i<nrWords-1;i++){
                System.out.println("-: Processing token "+i+" & "+(i+1));
                System.out.println("----------------------------");
                ArrayList<Integer> tempPositions = new ArrayList<Integer>(); //temp store for found valid positions
                ArrayList<Integer> posList1 = currentPositions; //first time: position list for first term, otherwise: found valid positions
                if (posList1.size() == 0) {
                    break; //there where no valid positions for previous terms, no use to go on.
                }
                ArrayList<Integer> posList2 = getPositions(query.terms.get(i+1),currentDocId); //positionlist of second term
                ListIterator<Integer> i1 = posList1.listIterator(); //itterator positionlist first term
                ListIterator<Integer> i2 = posList2.listIterator(); //itterator positionlist second term
                int pos1 = i1.next();
                int pos2 = i2.next();
                System.out.println("-: LENGTH OF posList1: "+posList1.size());
                System.out.println("-: LENGTH OF posList2: "+posList2.size());
                System.out.println("-: WHILE LOOP");
                while(i1.hasNext() || i2.hasNext() || pos2 == (pos1+1)) {
                    System.out.println("-: pos1= "+pos1);
                    System.out.println("-: pos2= "+pos2);
                    if(pos2 == (pos1+1)) { //if we find matching document ID
                            tempPositions.add(pos2); //add position 2 since this will be first term in next itteration
                            if ((!i1.hasNext()) && (!i2.hasNext())) {
                                break;
                            } if (i1.hasNext()) {
                                pos1 = i1.next();
                            } if (i2.hasNext()) {
                                pos2 = i2.next();
                            }
                            System.out.println("-: MEASURE: Match found");
                    } else if ((pos1 < pos2) && i1.hasNext()) { //Find higer docId
                        pos1 = i1.next();
                        System.out.println("-: MEASURE: move pos1");
                    } else if ((pos2 < pos1) && i2.hasNext()) {
                        pos2 = i2.next();
                        System.out.println("-: MEASURE: move pos2");
                    } else {
                        System.out.println("-: MEASURE: No match found BREAK");
                        break;
                    }
                }
                currentPositions = tempPositions;
            }
            System.out.println("----------------------------");
            if (currentPositions.size() > 0) {
                PostingsEntry entry = new PostingsEntry(currentDocId,currentPositions);
                returnPostingsList.addEntry(entry);
            }
        }
        System.out.println("_______________________");
        System.out.println("*--------LOG--------*");
        System.out.println("***||PHRASE SEARCH||***");
        if (returnPostingsList.size() > 0) {
            return returnPostingsList;
        } else {
            return null;
        }
    }

    public PostingsList rankedSearchTfIdf(Query query) {
        System.out.println("*------------------------*");
        System.out.println("Running rankedSearchTfIdf");
        PostingsList resultList = new PostingsList();
        int nrWords = query.size();
        int nrDocs = docIDs.size();
        double queryVector[] = new double[nrWords];
        Arrays.fill(queryVector,0);
        HashMap<Integer,double[]> vectors = new HashMap<Integer,double[]>(); 
        for (int i=0;i<nrWords;i++) { 
            PostingsList posting = index.get(query.terms.get(i));
            int nrDocsWithTerm = posting.size();
            System.out.println("------------------------------");
            System.out.println("tfIdf for "+query.terms.get(i));
            System.out.println("Docs in total: "+nrDocs);
            System.out.println("Docs with term match: "+nrDocsWithTerm);
            double iDf = Math.log(nrDocs/nrDocsWithTerm);
            System.out.println("iDf: "+iDf);
            queryVector[i] = iDf/nrWords; //Set tfidf for term for queryvector
            ListIterator<PostingsEntry> postingsList = posting.getIterator();
            while(postingsList.hasNext()) {
                PostingsEntry entry = postingsList.next();
                int docID = entry.getDocId();
                double matchesInDoc = entry.matchesInDoc();
                System.out.println("Matches in doc: "+matchesInDoc);
                double nrTokensDoc = docLengths.get(""+docID);
                System.out.println("Tokens in doc: "+nrTokensDoc);
                double tf = matchesInDoc/Math.sqrt(nrTokensDoc); //termfrequency normalized with length;
                System.out.println("tf: "+tf);
                double tfIdf = tf*iDf;
                System.out.println("tfIDF: "+tfIdf);
                if(vectors.containsKey(docID)) { //The doc has previously been encountered
                    vectors.get(docID)[i] = tfIdf; //Add value to current dimension
                } else {
                    double vectorArray[] = new double[nrWords]; //Make a vector representation
                    Arrays.fill(vectorArray,0); //fill with zeros
                    vectorArray[i] = tfIdf; //Save value
                    vectors.put(docID,vectorArray); //add to list of vectors
                }
            }
        }
        for(Map.Entry<Integer,double[]> me : vectors.entrySet()) {
            double vector[] = new double[nrWords];
            vector = me.getValue();
            double cosineSim = cosineSim(queryVector,vector);
            System.out.print("QUERYVECTOR: [");
            for (int i=0;i<vector.length;i++) {
                System.out.print(queryVector[i]+"  ");
            }
            System.out.print("]\n");
            System.out.print("DOCUMENTVECTOR: [");
            for (int i=0;i<vector.length;i++) {
                System.out.print(vector[i]+"  ");
            }
            System.out.print("]\n");
            System.out.println("COSINE SIMILARITY: "+cosineSim);

            PostingsEntry entry = new PostingsEntry(me.getKey(),cosineSim);
            resultList.addEntry(entry);
        } 
        resultList.sortScore();
        System.out.println("*------------------------*");
        if (resultList.size() == 0 ) {
            return null;
        } else {
            return resultList;    
        }
        
    }

    public PostingsList intersectionSearch(Query query) {
        int nrWords = query.size();
        System.out.print("QUERY RECIEVED (INTERSECTION): ");
        for (int i=0;i<nrWords;i++) {
            System.out.print(" "+query.terms.get(i));
        }
        System.out.print("\n");
        PostingsList returnList = index.get(query.terms.get(0));
        for (int i = 0;i<nrWords-1;i++) {
            PostingsList tempResult = new PostingsList();
            PostingsList p1 = returnList;
            PostingsList p2 = index.get(query.terms.get(i+1));
            if ((p1.size()==0) || (p2.size() == 0)) {
                return null;
            }
            ListIterator<PostingsEntry> i1 = p1.getIterator();
            ListIterator<PostingsEntry> i2 = p2.getIterator();
            PostingsEntry e1 = i1.next();
            PostingsEntry e2 = i2.next();
            while(i1.hasNext() || i2.hasNext() || (e1.getDocId() == e2.getDocId())) {
                if(e1.getDocId() == e2.getDocId()) { //if we find matching document ID
                    tempResult.addEntry(e1);
                    if ((!i1.hasNext()) && (!i2.hasNext())) {
                        break;
                    } if (i1.hasNext()) {
                        e1 = i1.next();
                    } if (i2.hasNext()) {
                        e2 = i2.next();
                    }
                } else if ((e1.getDocId() < e2.getDocId()) && i1.hasNext()) { //Find higer docId
                    e1 = i1.next();
                } else if ((e2.getDocId() < e1.getDocId()) && i2.hasNext()) {
                    e2 = i2.next();
                } else {
                    break;
                }
            }
            returnList = tempResult; 
        }
        return returnList;
    }

    public ArrayList getPositions(String token, int docID) {
        HashMap<Integer,Integer> docIDsOfPosting = docIDsInPostings.get(token); //get listing docID, index map
        int indexOfDocId = docIDsOfPosting.get(docID); //Get index of docID
        PostingsList list = index.get(token); //get posting for the token
        PostingsEntry entry = list.get(indexOfDocId); //Get the entry for token docId combination
        ArrayList tokenPositions = entry.getPositionList(); //Get positions of token in docId
        return tokenPositions;
    }

    public double cosineSim(double[] queryVector,double[] documentVector) {
        int length = queryVector.length;
        double dotProduct = 0.0;
        double eDistQuery = 0.0;
        double eDistDoc = 0.0; 
        for (int i=0;i<length;i++) {
            dotProduct += queryVector[i]*documentVector[i];
            eDistQuery += Math.pow(queryVector[i],2); 
            eDistDoc += Math.pow(documentVector[i],2);
        }
        eDistQuery = Math.sqrt(eDistQuery); //eculidian distance of query vector
        eDistDoc = Math.sqrt(eDistDoc); //eculidan distance of document vector 
        double distanceProduct = eDistQuery*eDistDoc;
        double cosineSim = dotProduct/distanceProduct;
        return cosineSim;
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
