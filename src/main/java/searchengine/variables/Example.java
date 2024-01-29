package searchengine.variables;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import searchengine.config.ConnectionSettings;
import searchengine.dto.indexPage.LemmaFinder;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class Example {
    private static String url = "https://www.lenta.ru/";

    private final LuceneMorphology luceneMorphology;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};

    @SneakyThrows
    public static void main(String[] args) {
        String pagePath = "https://lenta.ru/rubrics/russia/";

        String[] array = pagePath.split("\\/+");
        StringBuilder stringBuilder = new StringBuilder();

        if (array.length > 2) {
            for (int i = 0; i < array.length; i++) {
                if (i >= 2) {
                    break;
                }
                if (i == 0) {
                    stringBuilder.append(array[i] + "//");
                } else {
                    stringBuilder.append(array[i]);
                }
            }
        }
        System.out.println(stringBuilder.toString());

//        Connection.Response response = getConnectionToPage(url);
//
//        String pageText = removeHtmlTags(url);
//
//        HashMap<String, Integer> pageLemmasMap = searchingLemmasTheirCount(pageText);
//
//        for (Map.Entry<String, Integer> pair : pageLemmasMap.entrySet()) {
//            System.out.println(pair.getKey() + " - " + pair.getValue());
//        }

   }
    @SneakyThrows
    public static HashMap<String, Integer> searchingLemmasTheirCount(String text) {
        HashMap<String, Integer> lemmasMap = new HashMap<>();

        String[] arrayWords = convertingTextToArray(text);

        for (String word : arrayWords) {
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            List<String> wordBaseForms = luceneMorph.getMorphInfo(word);

            if (checkComplianceWordToParticlesNames(wordBaseForms)) {
                continue;
            }

            List<String> wordNormalFormList = luceneMorph.getNormalForms(word);

            String wordInNormalForm = wordNormalFormList.get(0); // TODO почему берем только первое слово???

            if (lemmasMap.containsKey(wordInNormalForm)) {
                lemmasMap.put(wordInNormalForm, lemmasMap.get(wordInNormalForm) + 1);
            } else {
                lemmasMap.put(wordInNormalForm, 1);
            }
        }
        return lemmasMap;
    }
    public static String[] convertingTextToArray(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }
    @SneakyThrows
    public static Connection.Response getConnectionToPage(String pagePath) {
        return Jsoup.connect(pagePath)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .execute();
    }
    public String getSiteDomain(String pagePath) {
        String[] array = pagePath.split("\\/+");
        StringBuilder stringBuilder = new StringBuilder();

        if (array.length > 2) {
            for (int i = 0; i < array.length; i++) {
                if (i >= 2) {
                    break;
                }
                if (i == 0) {
                    stringBuilder.append(array[i] + "//");
                }
                stringBuilder.append(array[i] + "/");
            }
        }
        return stringBuilder.toString();
    }

    //TODO метод вывода слов с указанием кол-ва каждого слова
    @SneakyThrows
    public HashMap<String, Integer> searchingLemmas(String text) {
        HashMap<String, Integer> lemmasMap = new HashMap<>();

        String[] arrayWords = convertingTextToArrayWords(text);

        for (String word : arrayWords) {
            if (word.isBlank()) { //проверяем, есть ли в строке пробел, пустая строка или null.
                continue;
            }

            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            List<String> wordBaseFormsList = luceneMorph.getMorphInfo(word);

            if (checkComplianceWordToParticlesNames(wordBaseFormsList)) {
                continue;
            }

            LuceneMorphology russianLuceneMorphology = new RussianLuceneMorphology();
            List<String> wordNormalFormList = russianLuceneMorphology.getNormalForms(word);

            String wordInNormalForm = wordNormalFormList.get(0); // TODO почему берем только первое слово???

            if (lemmasMap.containsKey(wordInNormalForm)) {
                lemmasMap.put(wordInNormalForm, lemmasMap.get(wordInNormalForm) + 1);
            } else {
                lemmasMap.put(wordInNormalForm, 1);
            }
        }
        return lemmasMap;
    }

    // TODO метод, который будет очищать код веб-страниц от HTML-тегов
    @SneakyThrows
    public static String removeHtmlTags(String HtmlPage) {
        return Jsoup.connect(HtmlPage).get().text();
    }

    //TODO метод добавления каждого слова из строки в арэйлист
    public static String[] convertingTextToArrayWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    //TODO метод провеки слова на соответствие служебной части речи
    public static boolean checkComplianceWordToParticlesNames(List<String> stringList) {
        for (String string : stringList) {
            for (String property : particlesNames) {
                if (string.toUpperCase().contains(property)) {
                    return true;
                }
            }
        }
        return false;
    }
}
