/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package linearft;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.AfterEffectB;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicModelBE;
import org.apache.lucene.search.similarities.DFRSimilarity;
import org.apache.lucene.search.similarities.Distribution;
import org.apache.lucene.search.similarities.DistributionLL;
import org.apache.lucene.search.similarities.IBSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.LambdaDF;
import org.apache.lucene.search.similarities.NormalizationH3;
import org.apache.lucene.search.similarities.NormalizationZ;
import org.apache.lucene.search.similarities.Similarity;

/**
 * Stores the entire set of parameter vectors for all clusters.
 * 
 * @author Debasis
 */
public class ParameterVectors {
    int numClusters;
    float[][] parameters;
    float[] features;
    
    static final Similarity[] sims = {
        new BM25Similarity(),
        new LMJelinekMercerSimilarity(0.6f),
        new LMDirichletSimilarity(),
        new IBSimilarity(new DistributionLL(), new LambdaDF(), new NormalizationZ()),
        new DFRSimilarity(new BasicModelBE(), new AfterEffectB(), new NormalizationZ())
    };
    static final int NUM_MODELS = sims.length;
    
    public ParameterVectors(int numClusters) {
        this.numClusters = numClusters;
        parameters = new float[numClusters][NUM_MODELS];
    }
    
    // The column-sum (each parameter vector corresponding to a cluster)
    // must be 1.
    public void initRandom() {
        for (int i=0; i<numClusters; i++) {
            float min = 0;
            for (int j=0; j<NUM_MODELS-1; j++) {
                System.out.println("[0, " + (1-min) + ")");
                parameters[i][j] = (float)Math.random()*(1-min);
                min += parameters[i][j];
            }
            parameters[i][NUM_MODELS-1] = 1-min;
        }
    }
    
    void showMatrix() {
        for (int i=0; i<numClusters; i++) {
            float sum = 0;
            for (int j=0; j<NUM_MODELS; j++) {
                System.out.print(parameters[i][j] + "\t");
                sum += parameters[i][j];
            }
            System.out.print(sum);
            System.out.println("");
        }
    }
    
    TopDocs computeSimilarities(Query query, IndexSearcher searcher, int numTopDocs, int clusterId) throws Exception {
        Similarity sim = sims[clusterId];
        searcher.setSimilarity(sim);
        TopDocs topDocs = searcher.search(query, numTopDocs);
        return topDocs;
    }

    public static void main(String[] args) {
        ParameterVectors params = new ParameterVectors(2);
        params.initRandom();
        params.showMatrix();
    }
    
}
