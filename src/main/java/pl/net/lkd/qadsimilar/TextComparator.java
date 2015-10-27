package pl.net.lkd.qadsimilar;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TextComparator {

    public static void main(String[] argv) throws ClassNotFoundException,
          IOException {
        if (argv.length != 1) {
            usage();
        } else {
            compareTexts(argv[0]);
        }
    }

    private static void compareTexts(String textDirPath) throws IOException {

        IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_36,
              new EnglishAnalyzer(Version.LUCENE_36));
        iwc.setOpenMode(OpenMode.CREATE);
        RAMDirectory idx = new RAMDirectory();

        IndexWriter writer = new IndexWriter(idx, iwc);

        for (File f : new File(textDirPath).listFiles()) {
            System.out.println(String.format("Indexing file %s", f.getName()));
            Document doc = new Document();
            doc.add(new Field("file_name", f.getName(), Field.Store.YES,
                  Field.Index.NOT_ANALYZED));
            doc.add(new Field("content", FileUtils.readFileToString(f),
                  Field.Store.NO, Field.Index.ANALYZED,
                  Field.TermVector.WITH_POSITIONS_OFFSETS));

            writer.addDocument(doc);
        }
        writer.close();
        System.out.println("Done!");

        // search the index
        System.out.println("Searching for similar docs.");
        IndexReader ir = IndexReader.open(idx);
        IndexSearcher is = new IndexSearcher(ir);
        MoreLikeThis mlt = new MoreLikeThis(ir);
        mlt.setFieldNames(null);
        Query allDocsQ = new MatchAllDocsQuery();

        // iterate over all langs & docs and get an MLT query
        System.out.println("----- Getting MLT scores -----");
        TopDocs allDocs = is.search(allDocsQ, ir.maxDoc());
        for (ScoreDoc mainSDoc : allDocs.scoreDocs) {
            Document mainDoc = is.doc(mainSDoc.doc);
            mlt.setFieldNames(new String[]{"content"});
            Query query = mlt.like(mainSDoc.doc);
            TopDocs hits = is.search(query, 10);
            System.out.println("----------");
            for (ScoreDoc similarSDoc : hits.scoreDocs) {
                Document similarDoc = is.doc(similarSDoc.doc);
                System.out.println(String.format("Docs %s - %s, score: %s",
                      mainDoc.get("file_name"), similarDoc.get("file_name"),
                      similarSDoc.score));
            }
        }

        // calculate cosine similarities
        System.out.println("----- Getting cosine similarities -----");
        Map<String, Integer> terms = new HashMap<String, Integer>();
        TermEnum termEnum = ir.terms(new Term("content"));
        int pos = 0;
        while (termEnum.next()) {
            Term term = termEnum.term();
            if (!"content".equals(term.field())) { break; }
            terms.put(term.text(), pos++);
        }

        DocVector[] docVectors = new DocVector[ir.numDocs() - ir.numDeletedDocs()];
        int v = 0;
        for (int i = 0; i < ir.maxDoc(); i++) {
            if (ir.isDeleted(i)) { continue; }

            Document doc = ir.document(i);
            TermFreqVector[] tfvs = ir.getTermFreqVectors(i);
            docVectors[v] = new DocVector(i, doc.get("file_name"), terms);
            for (TermFreqVector tfv : tfvs) {
                String[] termTexts = tfv.getTerms();
                int[] termFreqs = tfv.getTermFrequencies();
                for (int j = 0; j < termTexts.length; j++) {
                    docVectors[v].setEntry(termTexts[j], termFreqs[j]);
                }
            }
            docVectors[v].normalize();
            v++;
        }
        // now get similarity between docs
        for (DocVector dv1 : docVectors) {
            for (DocVector dv2 : docVectors) {
                double cosim = getCosineSimilarity(dv1, dv2);
                System.out.println(String.format("Docs %s - %s, cosim: %s", dv1.getName(), dv2.getName(), cosim));
            }
            System.out.println("----------");
        }
    }

    private static void usage() {
        System.out.println("Usage: java -jar QADSimilar.jar text_dir");
    }

    private static double getCosineSimilarity(DocVector d1, DocVector d2) {
        return (d1.getVector().dotProduct(d2.getVector()))
              / (d1.getVector().getNorm() * d2.getVector().getNorm());
    }

}
