/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retriever;

import linearft.WvecReranker;
import evaluator.Evaluator;
import indexer.TrecDocIndexer;
import java.util.*;
import java.io.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import trec.TRECQuery;
import wvec.WordVecs;

/**
 *
 * @author Debasis
 */
public class WVecLFBRetriever {
    Properties prop;
    IndexReader reader;
    WordVecs wvecs;
    IndexSearcher searcher;
    
    int numVocabClusters;
    static final int numWanted = 1000;
    static final String runName = "wvec-lbfm";
    
    public WVecLFBRetriever(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        
        File indexDir = new File(prop.getProperty("index"));
        System.out.println("Running queries against index: " + indexDir.getPath());
        reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));

        wvecs = new WordVecs(prop);
        searcher = new IndexSearcher(reader);
    }
        
    public void trainModel() throws Exception {
        TopScoreDocCollector collector;
        TopDocs topDocs = null;
        
        String resultsFile = prop.getProperty("res.file");        
        FileWriter fw = new FileWriter(resultsFile);
        
        // Construct queries
        Analyzer analyzer = TrecDocIndexer.constructAnalyzer();
        String queryFile = prop.getProperty("query.file");
        List<TRECQuery> queries = new TRECQueryBuilder(analyzer).constructQueries(queryFile);
        
        String cacheFilePrefix = prop.getProperty("topdocs.cache");
        
        // Retrieve for each query
        for (TRECQuery query : queries) {
            String cacheFile = cacheFilePrefix + "." + query.id;
            TopDocsCache topDocsCache = new TopDocsCache(cacheFile);
            // Get saved results
            topDocs = topDocsCache.load();
            
            if (topDocs == null) { // if no cache found, re-retrieve and save
                collector = TopScoreDocCollector.create(numWanted);
                searcher.search(query.getLuceneQueryObj(), collector);
                topDocs = collector.topDocs();
                topDocsCache.save(topDocs);
            } 
                        
            WvecReranker reranker = new WvecReranker(this, query, topDocs);
            reranker.rerank();
        }
        
        // Evaluate saved result list
        if (Boolean.parseBoolean(prop.getProperty("eval"))) {
            Evaluator evaluator = new Evaluator(prop);
            evaluator.load();
            evaluator.fillRelInfo();
            System.out.println(evaluator.computeAll());        
        }
    }
    
    public void saveRetrievedTuples(FileWriter fw, TRECQuery query, TopDocs topDocs) throws Exception {
        StringBuffer buff = new StringBuffer();
        ScoreDoc[] hits = topDocs.scoreDocs;
        int len = Math.min(numWanted, hits.length);
        for (int i = 0; i < len; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            buff.append(query.id.trim()).append("\tQ0\t").
                    append(d.get(TrecDocIndexer.FIELD_ID)).append("\t").
                    append((i+1)).append("\t").
                    append(hits[i].score).append("\t").
                    append(runName).append("\n");                
        }
        fw.write(buff.toString());        
    }

    public WordVecs getWordVecs() { return wvecs; }
    public IndexReader getIndexReader() { return reader; }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "init.properties";
        }
        try {
            WVecLFBRetriever searcher = new WVecLFBRetriever(args[0]);
            searcher.trainModel();            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }    
}
