/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retriever;

import java.util.*;
import org.apache.lucene.analysis.Analyzer;
import trec.TRECQuery;
import trec.TRECQueryParser;

/**
 *
 * @author Debasis
 */
public class TRECQueryBuilder {

    Analyzer analyzer;
            
    public TRECQueryBuilder(Analyzer analyzer) {
        this.analyzer= analyzer;
    }
    
    public List<TRECQuery> constructQueries(String queryFile) throws Exception {        
        TRECQueryParser parser = new TRECQueryParser(queryFile, analyzer);
        parser.parse();
        return parser.getQueries();
    }    
}
