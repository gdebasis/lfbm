/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package linearft;

import indexer.TrecDocIndexer;
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
    
    public WvecReranker(WVecLFBRetriever retriever, TRECQuery query, TopDocs topDocs) throws Exception {
        this.retriever = retriever;
        this.wvecs = retriever.getWordVecs();
        
        qvec = new DocVec(wvecs, query.luceneQuery);
        this.topDocs = topDocs;
        
        int numClusters = Integer.parseInt(
                wvecs.getProperties().getProperty("wvecs.numclusters"));
        
        params = new ParameterVectors(numClusters);
        params.initRandom();
        
        inMemReader = DirectoryReader.open(buildInMemIndex());
        inMemSearcher = new IndexSearcher(inMemReader);
    }
    
    // Useful to compute similarities with different models for reranking
    Directory buildInMemIndex() throws Exception {
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

    public void rerank() {
        params.computeSimilarities(null, inMemSearcher, numTopDocs, numTopDocs);
        
    }
}
