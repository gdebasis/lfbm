/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retriever;

import java.io.*;
import java.util.*;
import org.apache.lucene.search.TopDocs;

/**
 *
 * @author Debasis
 */
public class TopDocsCache implements Serializable {
    TopDocs topDocs;
    String cacheFile;

    public TopDocsCache(String cacheFile) {
        this.cacheFile = cacheFile;
    }
    
    public void save(TopDocs topDocs) throws Exception {
        this.topDocs = topDocs;
        FileOutputStream fos = new FileOutputStream(cacheFile);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(topDocs);
        oos.close();
        fos.close();
    }
    
    public TopDocs load() throws Exception {
        FileInputStream fis = new FileInputStream(cacheFile);
        ObjectInputStream in = new ObjectInputStream(fis);
        topDocs = (TopDocs)in.readObject();
        in.close();
        fis.close();
        
        return topDocs;
    }    
}
