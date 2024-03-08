package searchengine.example;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.ConnectionSettings;
import searchengine.dto.search.Data;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repository.LemmaRepository;
import searchengine.services.LemmaFinder;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class Example {
    private static String url = "https://www.lenta.ru/";

    private final LuceneMorphology luceneMorphology;
    private final ConnectionSettings connectionSettings;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};

    private static String text = "Повторное появление появление леопарда леопарда в Осетии позволяет предположить, " +
            "что леопард постоянно обитает в некоторых районах Северного Кавказа.";


    @SneakyThrows
    public static void main(String[] args) {
        String word = " добро ";

        word = word.trim();
        if (!word.contains(" ")) {
            String[] queryToArray = new String[] {word};
            System.out.println(queryToArray.length);
        }
        //String[] queryToArray = word.split(" ");
        //System.out.println(queryToArray.length);


//        String word = "добро";
//        String query = "добро lj,hjdjktw fghjkl;l.kjhgfdxzcvjkl; fdgbhnjm";
//
//        String regex = ".*\\b" + word + "\\b.*";
//        Pattern pattern = Pattern.compile(regex);
//
//        Matcher matcher = pattern.matcher(query);
//        System.out.println(matcher.matches());


//        if (query.contains(word)) {
//            System.out.println(query);
//        }


//        String query = "президент";
//        query = query + " ";
//        String[] queryToArray = query.split(" ");
//        System.out.println(queryToArray.length);


//        String url = "https://lenta.ru/catalog/1303.html";
//        String query = "президент засыпает";
//
//        Connection.Response response = Jsoup.connect(url)
//                .ignoreHttpErrors(true)
//                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
//                .referrer("http://www.google.com")
//                .execute();
//
//        System.out.println(response.statusCode());

//        String document = response.parse().text().toLowerCase();
//        System.out.println("Ответ респонса: " + document);
//
//        String[] documentToArray = document.split("\\.");
//        System.out.println("Документ в массив: ");
//        for (String sentence : documentToArray) {
//            System.out.println(sentence);
//        }
//
//        Set<String> sentenceSet = new HashSet<>(List.of(documentToArray));
//        System.out.println("Сет перложений страницы: " + sentenceSet);
//        for (String sentence : sentenceSet) {
//            System.out.println(sentence);
//        }
//
//        String[] queryToArray = query.split(" ");
//        System.out.println("Запрос в массив: ");
//        for (String word : queryToArray) {
//            System.out.println(word);
//        }


//        String url = "https://lenta.ru/news/2024/02/28/rossiyanka-zaperla-pyateryh-golodnyh-detey-doma-i-ushla-na-chetyre-dnya/";
//        List<String> lemmaList = new ArrayList<>();
//        lemmaList.add("россиянка");
//        lemmaList.add("вернулась");
//        lemmaList.add("домой");
//        lemmaList.add("после");
//        lemmaList.add("требования");
//        lemmaList.add("требования");
//        lemmaList.add("требования");
//        lemmaList.add("требования");
//        lemmaList.add("требования");
//        lemmaList.add("требования");
//
//        int offset = 4;
//        int limit = 9;
//
//        for (int i = offset; i < limit; i++) {
//            System.out.println(lemmaList.get(i));
//        }

//        Connection.Response response = Jsoup.connect(url)
//                .ignoreHttpErrors(true)
//                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
//                .referrer("http://www.google.com")
//                .execute();
//
//        String document = response.parse().text().toLowerCase();
//
//        String[] textToArray = document.split("\\.");
//
//        Set<String> sentenceSet = new HashSet<>(List.of(textToArray));
//
//        for (String lemma : lemmaList) {
//            Iterator<String> iterator = sentenceSet.iterator();
//            while (iterator.hasNext()) {
//                String nextSentence = iterator.next();
//                if (!nextSentence.contains(lemma)) {
//                    iterator.remove();
//                }
//            }
//        }
//        String sentence = sentenceSet.iterator().next().trim();
//
//        String[] sentenceToArray = sentence.split(" ");
//
//        ArrayList<String> wordList = new ArrayList<>(List.of(sentenceToArray));
//
//        for (String lemma : lemmaList) {
//            ListIterator<String> iteratorList = wordList.listIterator();
//            while (iteratorList.hasNext()) {
//                String word = iteratorList.next();
//                if (lemma.equals(word)) {
//                    iteratorList.set("<b>" + word + "</b>");
//                }
//            }
//        }
//        StringBuilder stringBuilder = new StringBuilder();
//        for (String word : wordList) {
//            stringBuilder.append(word + " ");
//        }
//        System.out.println(stringBuilder);
//
//        for (int i = 0; i < wordList.size(); i++) {
//            for (String lemma : lemmaList) {
//                if (wordList.get(i).equals(lemma)) {
//                    word = " <b>" + word + "<b>";
//                }
//            }
//        }
        //wordList.forEach(System.out::println);


//        System.out.println(currentSentence);
//
//        String[] sentenceToArray = currentSentence.trim().split(" ");


//        for (String word : sentenceToArray) {
//            for (String lemma : lemmaList) {
//                if (word.equals(lemma)) {
//                    word = " <b>" + word + "<b>";
//                }
//            }
//        }
//        for (String word : sentenceToArray) {
//            System.out.println(word);
//        }


//        String site = "https://lenta.ru/news/2024/02/28/rossiyanka-zaperla-pyateryh-golodnyh-detey-doma-i-ushla-na-chetyre-dnya/";
//
//        Connection.Response response = Jsoup.connect(site)
//                .ignoreHttpErrors(true)
//                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
//                .referrer("http://www.google.com")
//                .execute();
//
//        Document document = response.parse();
//
//        String title = null;
//        Elements elements = document.select("title");
//        for (Element element : elements) {
//            title = element.text();
//        }
//        System.out.println(title);
//        System.out.println(elements.size());


//        for (int i = 0; i < lemmaList.size(); i++) {
//            int value;
//            if (i == 0) {
//                value = document.indexOf(lemmaList.get(i), i);
//            } else {
//                value = document.indexOf(lemmaList.get(i), positionWordList.get(i - 1));
//            }
//            positionWordList.add(value);
//        }
//        String text = document.substring(positionWordList.get(0));
//        System.out.println(text);


//        Data data = new Data()
//                .setSiteUrl("cvbnm")
//                .setSiteName("fghjk")
//                .setUri("cvbnm")
//                .setTitle("vbnm,")
//                .setSnippet("fghj")
//                .setRelevance(9);
//
//        Data data1 = new Data()
//                .setSiteUrl("cvbnm")
//                .setSiteName("fghjk")
//                .setUri("cvbnm")
//                .setTitle("vbnm,")
//                .setSnippet("fghj")
//                .setRelevance(8);
//
//        Data data2 = new Data()
//                .setSiteUrl("cvbnm")
//                .setSiteName("fghjk")
//                .setUri("cvbnm")
//                .setTitle("vbnm,")
//                .setSnippet("fghj")
//                .setRelevance(7);
//
//        List<Data> dataList = new ArrayList<>();
//        dataList.add(data1);
//        dataList.add(data2);
//        dataList.add(data);
//
//        Comparator<Data> compareByRelevance = Comparator.comparing(Data::getRelevance);
//        List<Data> sortedDataList = dataList.stream().sorted(compareByRelevance.reversed()).toList();
//
//        for (Data d : sortedDataList) {
//            System.out.println(d);
//        }


//        String site = "https://www.lenta.ru/";
//
//        Connection.Response response = Jsoup.connect(site)
//                .ignoreHttpErrors(true)
//                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
//                .referrer("http://www.google.com")
//                .execute();
//
//        Document document = response.parse();
//
//        String title = null;
//        Elements elements = document.select("title");
//        for (Element element : elements) {
//            title = element.text();
//        }
//        System.out.println(title);


//        HashMap<String, Integer> map = new HashMap<>();
//        map.put("Аня", 3);
//        map.put("Варя", 1);
//        map.put("Петя", 2);
//
//        map.entrySet().stream().sorted(Map.Entry.comparingByValue());
//
//        LinkedHashMap<String, Integer> sortMap = new LinkedHashMap<>(map);
//
//        LinkedHashMap<String, Integer> sortedMap = new LinkedHashMap<>();
//
//        Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
//        while (iterator.hasNext()) {
//            Optional<Map.Entry<String, Integer>> maxEntry = map.entrySet()
//                    .stream().max(Map.Entry.comparingByValue());
//            if (!maxEntry.isPresent()) {
//                break;
//            } else {
//                sortedMap.put(maxEntry.get().getKey(), maxEntry.get().getValue());
//                map.remove(maxEntry.get().getKey());
//            }
//        }
//        sortedMap.entrySet().forEach(System.out::println);


//        Optional<Map.Entry<String, Integer>> maxEntry = map.entrySet()
//                    .stream().max(Map.Entry.comparingByValue());
//            Integer maxValue = maxEntry.get().getValue();
//        System.out.println(maxValue);


//        List<String> tempPageEntityList = new ArrayList<>();
//
//        List<String> list1 = new ArrayList<>();
//        list1.add("/sale/main");
//        list1.add("/");
//        list1.add("/company");
//        list1.add("/otzyvy");
//        list1.add("/contacts");
//
//        List<String> list2 = new ArrayList<>();
//        list2.add("/company");
//        list2.add("/sale/main");
//        list2.add("/sfg");
//        list2.add("/otzyv");
//        list2.add("/contacts");
//
//
//        List<String> list3 = new ArrayList<>();
//        list3.add("/sale/main");
//        list3.add("/sfg");
//        list3.add("/company");
//
//
//        List<String> list4 = new ArrayList<>();
//        list4.add("/contacts");
//        list4.add("/sale/main");
//
//        ArrayList<List<String>> lists = new ArrayList<>();
//        lists.add(list1);
//        lists.add(list2);
//        lists.add(list3);
//        lists.add(list4);
//
//        Collection<String> pageList = new ArrayList<>();
//
//        for (int i = 0; i < lists.size(); i++) {
//            if (i == 0) {
//                pageList = lists.get(i);
//            } else {
//                Collection<String> currentPageList = lists.get(i);
//                pageList = CollectionUtils.retainAll(pageList,currentPageList);
//            }
//        }
//
//        //Collection<String> commonList = CollectionUtils.retainAll(list1,list2);
//        for (String page : pageList) {
//            System.out.println(page);
//        }

//        List<String> firstList = Arrays.asList("A", "B", "C", "D", "E", "F");
//        List<String> secondList = Arrays.asList("B", "D", "F", "G", "H", "K");
//
//        Collection<String> result = CollectionUtils.intersection(firstList, secondList);
//        System.out.println(ArrayUtils.toString(result));

//        Set<String> pages = new HashSet<>();
//        Set<String> pageSet = new HashSet<>();
//        Set<String> finish = new HashSet<>();
//
//        for (int i = 0; i < lists.size() - 1; i++) {
//            if (i == 0) {
//                pageSet.addAll(lists.get(i));
//            } else {
//                List<String> pageLemmaList = lists.get(i);
//                Iterator<String> pageIterator = pageSet.iterator();
//                while (pageIterator.hasNext()) {
//                    String pageEntity = pageIterator.next();
//                    for (String page : pageLemmaList) {
//
//                    }
//
//                }
//            }
//        }


//        Set<String> pageSet = new HashSet<>();
//        Iterator<String> pageIterator = list1.iterator();
//        while (pageIterator.hasNext()) {
//            String pageEntity = pageIterator.next();
//            for (int i = 0; i < lists.size(); i++) {
//                List<String> tempList = lists.get(i);
//                for (String page : tempList) {
//                    if (pageEntity.equals(page)) {
//                        pageSet.add(page);
//                        //pageIterator.remove();
//                        //list1.size();
//                    }
//                }
//            }
//        }
//        for (String page : list1) {
//            System.out.println(page);
//        }


//        for (String page : list1) {
//            for (String currentPage : list2) {
//                if (page.equals(currentPage)) {
//                    tempPageEntityList.add(page);
//                }
//            }
//        }
//        for (String page : tempPageEntityList) {
//            System.out.println(page);
//        }

//        ArrayList<Lemma> lemmaList = new ArrayList<>();
//        Lemma lemma1 = new Lemma();
//        lemma1.setWord("рука");
//        lemma1.setCount(1);
//
//        Lemma lemma2 = new Lemma();
//        lemma2.setWord("нога");
//        lemma2.setCount(5);
//
//        Lemma lemma3 = new Lemma();
//        lemma3.setWord("голова");
//        lemma3.setCount(9);
//
//        lemmaList.add(lemma1);
//        lemmaList.add(lemma2);
//        lemmaList.add(lemma3);
//
//        Collections.sort(lemmaList);
//        for (Lemma lemma : lemmaList) {
//            System.out.println(lemma);
//        }

//        HashMap<String, Integer> lemmasMap = new HashMap<>();
//        String[] arrayWords = convertingTextToArray(text);
//        for (String word : arrayWords) {
//            if (word.isBlank()) {
//                continue;
//            }
//            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
//            List<String> wordBaseForms = luceneMorph.getMorphInfo(word);
//            if (checkComplianceWordToParticlesNames(wordBaseForms)) {
//                continue;
//            }
//            List<String> wordNormalFormList = luceneMorph.getNormalForms(word);
//            String wordInNormalForm = wordNormalFormList.get(0);
//            if (lemmasMap.containsKey(wordInNormalForm)) {
//                lemmasMap.put(wordInNormalForm, lemmasMap.get(wordInNormalForm) + 1);
//            } else {
//                lemmasMap.put(wordInNormalForm, 1);
//            }
//        }

//        Stream<Map.Entry<String, Integer>> sortedLemmasMap = lemmasMap.entrySet()
//                .stream().sorted(Map.Entry.comparingByValue());
//
//        sortedLemmasMap.forEach(System.out::println);

//
//        String pagePath = "https://lenta.ru/rubrics/economics";
//        String page = "https://lenta.ru/news/2024/03/06/polzovatelyam-whatsapp-stanet-dostupna-novaya-funktsiya/";

//        Connection.Response response = Jsoup.connect(pagePath)
//                .ignoreHttpErrors(true)
//                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
//                .referrer("http://www.google.com")
//                .execute();
//
//        String noHTMLString = response.body().replaceAll("\\<.*?\\>", " ");
//
//        String plain = Jsoup.parse(response.body()).text();
//        plain.replaceAll("([^а-я\\s])", " ")
//                .trim()
//                .split("\\s+");
//        String text = getTextByPage(page);
//
//        System.out.println(text);

    }


    @SneakyThrows
    public static HashMap<String, Integer> searchingLemmasTheirCount(String text) {
        HashMap<String, Integer> lemmasMap = new HashMap<>();

        String[] arrayWords = convertingTextToArray(text);

        for (String word : arrayWords) {

            if (word.isBlank()) {
                continue;
            }

            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            List<String> wordBaseForms = luceneMorph.getMorphInfo(word);

            if (checkComplianceWordToParticlesNames(wordBaseForms)) {
                continue;
            }

            List<String> wordNormalFormList = luceneMorph.getNormalForms(word);

            String wordInNormalForm = wordNormalFormList.get(0);

            if (lemmasMap.containsKey(wordInNormalForm)) {
                lemmasMap.put(wordInNormalForm, lemmasMap.get(wordInNormalForm) + 1);
                System.out.println("Заполняем мапу: ");
            } else {
                lemmasMap.put(wordInNormalForm, 1);
                System.out.println("Заполняем мапу: ");
            }
        }
        return lemmasMap;
    }

    @SneakyThrows
    public static void parsePageAndSaveEntitiesToDB(String pageUrl) {

        String pageText = getTextByPage(pageUrl);
        System.out.println("Получили текст: ");

        HashMap<String, Integer> pageLemmasMap = searchingLemmasTheirCount(pageText);
        System.out.println("Получили мапу: ");

        for (Map.Entry<String, Integer> pair : pageLemmasMap.entrySet()) {
            System.out.println("Преступили к перебору: ");

            System.out.println("Вытащили lemma: " + pair.getKey());

            System.out.println("Вытащили index: " + pair.getValue());
        }
    }

    @SneakyThrows
    public static String getTextByPage(String pagePath) {
        Connection.Response response = Jsoup.connect(pagePath)
                .ignoreHttpErrors(true)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .execute();

        return Jsoup.parse(response.body()).text();
    }

    public static String[] convertingTextToArray(String text) {
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
