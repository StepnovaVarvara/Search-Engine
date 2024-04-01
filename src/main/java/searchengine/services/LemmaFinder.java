package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import searchengine.config.ConnectionSettings;
import searchengine.config.LemmaFinderSettings;
import searchengine.dto.indexing.PageRsDto;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.variables.IndexProcessVariables;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LemmaFinder {
    private final LemmaFinderSettings lemmaFinderSettings;
    private final ConnectionSettings connectionSettings;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @SneakyThrows
    public HashMap<String, Integer> searchingLemmasAndTheirCount(String text) {
        Date startTime = new Date();

        HashMap<String, Integer> lemmasMap = new HashMap<>();
        String[] arrayWords = convertingTextToArray(text);

        for (String word : arrayWords) {
            //log.info("Процесс запущен? > {}", IndexProcessVariables.isRUNNING());
            if (!IndexProcessVariables.isRUNNING()) break;

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
            } else {
                lemmasMap.put(wordInNormalForm, 1);
            }
        }
        Date finishTime = new Date();
        log.info("Время работы мапы: {}", finishTime.getTime() - startTime.getTime());
        return lemmasMap;
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

    @SneakyThrows
    public String getTextByPage(String pagePath) {
        Connection.Response response = Jsoup.connect(pagePath)
                .ignoreHttpErrors(true)
                .userAgent(connectionSettings.getUserAgent())
                .referrer(connectionSettings.getReferrer())
                .execute();

        return Jsoup.parse(response.body()).text();
    }

    public String[] convertingTextToArray(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    public void parsePageAndSaveEntitiesToDB(String pageUrl, PageRsDto pageRsDto, int siteId) {
        PageEntity pageEntity = new PageEntity()
                .setSiteEntity(getSiteEntity(siteId))
                .setPagePath(getPagePath(pageUrl))
                .setResponseCode(pageRsDto.getStatusCode())
                .setContentPage(pageRsDto.getBody());
        pageRepository.save(pageEntity);

        String pageText = getTextByPage(pageUrl);

        HashMap<String, Integer> pageLemmasMap = searchingLemmasAndTheirCount(pageText);

        for (Map.Entry<String, Integer> pair : pageLemmasMap.entrySet()) {
            if (!IndexProcessVariables.isRUNNING()) break;

            if (!hasLemmaInDB(pair.getKey(), siteId)) {
                LemmaEntity lemmaEntity = new LemmaEntity()
                        .setSite(getSiteEntity(siteId))
                        .setLemmaName(pair.getKey())
                        .setFrequency(1);
                lemmaRepository.save(lemmaEntity);
            } else {
                LemmaEntity lemmaEntity = getLemmaEntity(pair.getKey(), siteId);
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
                lemmaRepository.save(lemmaEntity);
            }
            indexRepository.save(new IndexEntity()
                    .setPage(pageEntity)
                    .setLemma(getLemmaEntity(pair.getKey(), siteId))
                    .setCountOfLemmaForPage(pair.getValue()));
        }
        log.info("Информация сохранена в БД");
    }

    public SiteEntity getSiteEntity(int siteId) {
        return siteRepository.findById(siteId).get();
    }

    public boolean hasLemmaInDB(String lemmaName, int siteId) {
        return lemmaRepository.findAllByLemmaNameAndSiteId(lemmaName, siteId) != null;
    }

    public LemmaEntity getLemmaEntity(String lemmaName, int siteId) {
        return lemmaRepository.findAllByLemmaNameAndSiteId(lemmaName, siteId);
    }

    public String getPagePath(String pageUrl) {
        String[] array = pageUrl.split("\\/+");
        StringBuilder stringBuilder = new StringBuilder();

        if (array.length > 2) {
            for (int i = 0; i < array.length; i++) {
                if (i <= 1) {
                    continue;
                }
                stringBuilder.append("/" + array[i]);
            }
        } else {
            stringBuilder.append("/");
        }
        return stringBuilder.toString();
    }
}