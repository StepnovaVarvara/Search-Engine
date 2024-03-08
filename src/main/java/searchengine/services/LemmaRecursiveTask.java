package searchengine.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.config.LemmaFinderSettings;

import java.util.*;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class LemmaRecursiveTask extends RecursiveTask<HashMap<String, Integer>> {
    private String word;
    private String text;
    private LemmaFinderSettings lemmaFinderSettings;

    public LemmaRecursiveTask(String word) {
        this.word = word;
    }

    public LemmaRecursiveTask(String text, LemmaFinderSettings lemmaFinderSettings) {
        this.text = text;
        this.lemmaFinderSettings = lemmaFinderSettings;
    }

    @SneakyThrows
    @Override
    protected HashMap<String, Integer> compute() {
//        List<LemmaRecursiveTask> lemmaRecursiveTaskList = new ArrayList<>();
//
//        String[] arrayWords = convertingTextToArray(text);
//
//        for (String word : arrayWords) {
//            LemmaRecursiveTask lemmaRecursiveTask = new LemmaRecursiveTask(word);
//            lemmaRecursiveTask.fork();
//
//            if (word.isBlank()) {
//                continue;
//            }
//            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
//            List<String> wordBaseForms = luceneMorph.getMorphInfo(word);
//
//            if (checkComplianceWordToParticlesNames(wordBaseForms)) {
//                continue;
//            }
//
//            List<String> wordNormalFormList = luceneMorph.getNormalForms(word);
//
//            String wordInNormalForm = wordNormalFormList.get(0);
//
//            if (LemmaFinder.LEMMAS_MAP.containsKey(wordInNormalForm)) {
//                LemmaFinder.LEMMAS_MAP.put(wordInNormalForm, LemmaFinder.LEMMAS_MAP.get(wordInNormalForm) + 1);
//                lemmaRecursiveTaskList.add(lemmaRecursiveTask);
//
//                log.info("Слово добавлено в мапу: {}", word);
//            } else {
//                LemmaFinder.LEMMAS_MAP.put(wordInNormalForm, 1);
//                lemmaRecursiveTaskList.add(lemmaRecursiveTask);
//
//                log.info("Слово добавлено в мапу со значением 1: {}", word);
//            }
//        }
//        for (LemmaRecursiveTask lemmaRecursiveTask: lemmaRecursiveTaskList) {
//            lemmaRecursiveTask.join();
//            log.info("Джоиним задачу: {}", word);
//        }
        return null;
    }
    public String[] convertingTextToArray(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }
    public boolean checkComplianceWordToParticlesNames(List<String> stringList) {
        for (String string : stringList) {
            for (String particleName : lemmaFinderSettings.getParticlesNamesList()) {
                if (string.toUpperCase().contains(particleName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
