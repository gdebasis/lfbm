/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package linearft;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
import wvec.WordVec;
import wvec.WordVecs;

/**
 * Stores the entire set of parameter vectors for all clusters.
 * 
 * @author Debasis
 */
public class ParameterVectors {
    int numClusters;
    float[][] parameters;
    float[] features;
    WordVecs wvecs;
    
    static final Similarity[] sims = {
        new BM25Similarity(),
        new LMJelinekMercerSimilarity(0.6f),
        new LMDirichletSimilarity(),
        new IBSimilarity(new DistributionLL(), new LambdaDF(), new NormalizationZ()),
        new DFRSimilarity(new BasicModelBE(), new AfterEffectB(), new NormalizationZ())
    };
    static final int NUM_MODELS = sims.length;
    
    public ParameterVectors(WordVecs wvecs) {
        this.numClusters = wvecs.getNumClusters();
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
        
    TopDocs computeSimilarities(Query query, List<WordVec> nncvecs, IndexSearcher searcher, int numTopDocs) throws Exception {
        TopDocs topDocs = null;
        List<TopDocs> topDocsArray = new ArrayList<>(nncvecs.size());
        
        for (WordVec ccvec : nncvecs) {
            int clusterId = ccvec.getClusterId();
            Similarity sim = sims[clusterId];
            searcher.setSimilarity(sim);
            topDocs = searcher.search(query, numTopDocs);
            topDocsArray.add(topDocs);
        }
        return topDocs;
    }

    public static void main(String[] args) {        
        try {
            if (args.length < 1) {
                args = new String[1];
                args[0] = "init.properties";
            }
            
            Properties prop = new Properties();
            prop.load(new FileReader(args[0]));
            WordVecs wvecs = new WordVecs(prop);
            
            ParameterVectors params = new ParameterVectors(wvecs);
            params.initRandom();
            params.showMatrix();
            
            wvecs.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
    
}
