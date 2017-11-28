/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wvec;

/**
 *
 * @author Debasis
 */

public class CentroidVec implements Comparable<CentroidVec> {
    WordVec sumvec;
    int nvecs;
    float distWithQuery;
    
    public CentroidVec(WordVec wv, int clusterId) throws Exception {
        sumvec = new WordVec(wv.getDimension()); // zero vec
        sumvec.clusterId = clusterId;
    }
    
    public void add(WordVec wv) {
        sumvec = WordVec.centroid(sumvec, wv);
        nvecs++;
    }

    @Override
    public int compareTo(CentroidVec that) {
        return Float.compare(distWithQuery, that.distWithQuery);
    }
}
