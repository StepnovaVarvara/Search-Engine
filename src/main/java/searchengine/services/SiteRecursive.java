package searchengine.services;

import searchengine.config.Site;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class SiteRecursive extends RecursiveTask<List<PageEntity>> {

    private LinkedList<Site> siteList;
    SiteRecursive(Site site) {
        this.siteList = new LinkedList<>();
        this.siteList.add(site);
    }

    SiteRecursive(LinkedList<Site> siteList) {
        this.siteList = siteList;
    }

    @Override
    protected List<PageEntity> compute() {

        if (siteList.size() == 1) {
            //connect site
            // elements <a>
            // link <href>
            // filtr link
            // link.fork()
            // pageEntity
            return null; // pageEntity
        }

        LinkedList<Site> copySiteList = (LinkedList) siteList.clone();

        SiteRecursive siteRecursive1 = new SiteRecursive(copySiteList.getLast());
        copySiteList.removeLast();
        SiteRecursive siteRecursive2 = new SiteRecursive(copySiteList);
        siteRecursive1.fork();
        siteRecursive2.fork();

        List<PageEntity> join = siteRecursive1.join();


        return null;
    }
}
