/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wvec;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

/**
 * Builds up the list of centroid vectors in memory
 * 
 * @author Debasis
 */

class PartialSumVec implements Comparable<PartialSumVec> {
    WordVec sumvec;
    int nvecs;
    float distWithQuery;
    
    public PartialSumVec(WordVec wv) throws Exception {
        sumvec = new WordVec(wv.getDimension()); // zero vec
    }
    
    public void add(WordVec wv) {
        sumvec = WordVec.centroid(sumvec, wv);
        nvecs++;
    }

    @Override
    public int compareTo(PartialSumVec that) {
        return Float.compare(distWithQuery, that.distWithQuery);
    }
}

public class CentroidInfo {
    WordVecs wvecs;
    Properties prop;
    IndexReader clusterInfoReader;
    HashMap<Integer, PartialSumVec> partialSumVecMap;  // map partial sum vecs for each cluster

    public CentroidInfo(WordVecs wvecs) throws Exception {
        this.prop = wvecs.getProperties();
        this.wvecs = wvecs;
        
        int numVocabClusters = Integer.parseInt(prop.getProperty("retrieve.vocabcluster.numclusters", "0"));
        if (numVocabClusters > 0) {
            String clusterInfoIndexPath = prop.getProperty("wvecs.clusterids.basedir") + "/" + numVocabClusters;
            clusterInfoReader = DirectoryReader.open(FSDirectory.open(new File(clusterInfoIndexPath).toPath()));
        }
    }

    public void buildCentroids() throws Exception {
        System.out.println("Building cluster vecs in memory...");
        int numDocs = clusterInfoReader.numDocs();
        for (int i=0; i<numDocs; i++) {
            Document d = clusterInfoReader.document(i);
            String wordName = d.get(WordVecsIndexer.FIELD_WORD_NAME);
            int clusterId = Integer.parseInt(d.get(WordVecsIndexer.FIELD_WORD_VEC));
            
            PartialSumVec partialSumVec = partialSumVecMap.get(clusterId);
            WordVec wv = wvecs.getVec(wordName);
            if (partialSumVec == null) {
                partialSumVec = new PartialSumVec(wv);
            }
            partialSumVec.add(wv);
            partialSumVecMap.put(clusterId, partialSumVec);
        }        
        
        // Build the list of centroids
        for (PartialSumVec psv : partialSumVecMap.values()) {
            psv.sumvec.scalarMutiply(1/(float)numDocs);
        }
        
        clusterInfoReader.close();
    }
    
    // Get the nearest centroids
    List<PartialSumVec> getNearestClusterCentres(WordVec queryTermVec) {
        List<PartialSumVec> psvList = new ArrayList<>(partialSumVecMap.size());
        int k = Integer.parseInt(prop.getProperty("lfbm.knn.clusters", "5"));
        
        for (PartialSumVec cvec : partialSumVecMap.values()) {
            cvec.distWithQuery = queryTermVec.euclideanDist(cvec.sumvec);
            psvList.add(cvec);
        }
        Collections.sort(psvList);        
        return psvList.subList(0, k);
    }
}
