package pl.net.lkd.qadsimilar;

import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealVectorFormat;
import org.apache.commons.math3.linear.SparseRealVector;

import java.util.Map;

public class DocVector {
    private int id;
    private String name;
    private Map<String, Integer> terms;
    private SparseRealVector vector;

    public DocVector(Map<String, Integer> terms) {
        this.terms = terms;
        this.vector = new OpenMapRealVector(terms.size());
    }

    public DocVector(int id, String name, Map<String, Integer> terms) {
        this.id = id;
        this.name = name;
        this.terms = terms;
        this.vector = new OpenMapRealVector(terms.size());
    }

    public void setEntry(String term, int freq) {
        if (terms.containsKey(term)) {
            int pos = terms.get(term);
            vector.setEntry(pos, (double) freq);
        }
    }

    public void normalize() {
        double sum = vector.getL1Norm();
        vector = (SparseRealVector) vector.mapDivide(sum);
    }

    public String toString() {
        RealVectorFormat formatter = new RealVectorFormat();
        return formatter.format(vector);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Integer> getTerms() {
        return terms;
    }

    public void setTerms(Map<String, Integer> terms) {
        this.terms = terms;
    }

    public SparseRealVector getVector() {
        return vector;
    }

    public void setVector(SparseRealVector vector) {
        this.vector = vector;
    }
}
