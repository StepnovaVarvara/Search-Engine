package searchengine.example;

import lombok.Data;

@Data
public class Lemma implements Comparable<Lemma>{
    private String word;
    private int count;

    @Override
    public int compareTo(Lemma o) {
        return this.getCount() - o.getCount();
    }
}
