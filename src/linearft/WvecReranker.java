/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package linearft;

import indexer.TrecDocIndexer;
import java.util.List;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import retriever.WVecLFBRetriever;
import trec.TRECQuery;
import wvec.DocVec;
import wvec.WordVec;
import wvec.WordVecs;

/**
 * Rerank documents by combining different similarities...
 * which depend on the spatial distribution of the words.
 * 
 * @author Debasis
 */
public class WvecReranker {
    DocVec qvec;
    TopDocs topDocs;
    WVecLFBRetriever retriever;
    WordVecs wvecs;
    ParameterVectors params;
    IndexReader inMemReader;
    IndexSearcher inMemSearcher;
    int numTopDocs;
    TRECQuery query;
    double[][] covMatrix;
    
    public WvecReranker(WVecLFBRetriever retriever, TRECQuery query, TopDocs topDocs) throws Exception {
        this.retriever = retriever;
        this.wvecs = retriever.getWordVecs();
        
        qvec = new DocVec(wvecs, query.luceneQuery);
        this.topDocs = topDocs;
        
        params = new ParameterVectors(this.wvecs);
        params.initRandom();
        
        inMemReader = DirectoryReader.open(buildInMemIndex());
        inMemSearcher = new IndexSearcher(inMemReader);        
    }
    
    // Useful to compute similarities with different models for reranking
    final Directory buildInMemIndex() throws Exception {
        Directory ramdir = new RAMDirectory();
        Analyzer analyzer = TrecDocIndexer.constructAnalyzer();
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(ramdir, iwcfg);

        numTopDocs = topDocs.scoreDocs.length;
        IndexReader reader = retriever.getIndexReader();        
        for (ScoreDoc hit : topDocs.scoreDocs) {
            writer.addDocument(reader.document(hit.doc));
        }
        
        writer.commit();
        writer.close();
        return writer.getDirectory();        
    }

    double[][] getCovMatrix(WordVec cvec, float sigma) {
        if (this.covMatrix!=null)
            return this.covMatrix;
        
        int n = cvec.getDimension();
        double[][] covMatrix = new double[n][n];
        for (int i=0; i<n; i++)
            covMatrix[i][i] = sigma;
        this.covMatrix = covMatrix;
        return covMatrix;
    }

    
    public void rerank() throws Exception {
        float sigma = Float.parseFloat(retriever.getProperties().getProperty("lfbm.sigma", "1"));
        
        for (WordVec wv : qvec.getWordVecMap().values()) { // Iterate over each query term
            
            int clusterId = wv.getClusterId();
            // It may be the case that we don't have a trained optimal
            // parameter for this query term. To allow for a soft match
            // over weighted word class types, get the k nearest cluster
            // centres of this word vector and the parameter value at this
            // word vector is then a linear combination of the k parameter
            // vectors weighted by their distances from this point.
            
            List<WordVec> nncvecs = wvecs.getCentroidInfo().getNearestClusterCentres(wv);
            
            WordVec cvec = wvecs.getCentroidVec(clusterId);
            float density = (float)(new MultivariateNormalDistribution(cvec.getPoint(), getCovMatrix(cvec, sigma)).density(wv.getPoint()));
            
            params.computeSimilarities(query.getLuceneQueryObj(), nncvecs, inMemSearcher, numTopDocs);
        }
    }
}
