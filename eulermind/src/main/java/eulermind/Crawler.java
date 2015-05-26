package eulermind;

import com.tinkerpop.blueprints.Vertex;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import eulermind.importer.TikaPlainTextImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.Header;

/*
The MIT License (MIT)
Copyright (c) 2012-2014 wangxuguang ninesunqian@163.com

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

public class Crawler extends WebCrawler {

    static MindDB sm_mindDb;
    CrawlConfig m_config = new CrawlConfig();
    int m_numberOfCrawlers = 1;

    TikaPlainTextImporter m_importer;

    HashMap<Integer, Object> m_docId2DbIdMap = new HashMap<Integer, Object>();
    static final int m_rootDocId = 0;

    int page_count = 0;

    public Crawler() {
        initConfig(10000, 100);
        m_docId2DbIdMap.put(m_rootDocId, sm_mindDb.getRootId());

        //TODO: 放到mindModel内部，用一个函数实现import text的功能
        m_importer = new TikaPlainTextImporter(sm_mindDb);
    }

    private static Logger m_logger = LoggerFactory.getLogger(WebCrawler.class);

    private final static Pattern BINARY_FILES_EXTENSIONS =
            Pattern.compile(".*\\.(bmp|gif|jpe?g|png|tiff?|pdf|ico|xaml|pict|rif|pptx?|ps" +
                    "|mid|mp2|mp3|mp4|wav|wma|au|aiff|flac|ogg|3gp|aac|amr|au|vox" +
                    "|avi|mov|mpe?g|ra?m|m4v|smil|wm?v|swf|aaf|asf|flv|mkv" +
                    "|zip|rar|gz|7z|aac|ace|alz|apk|arc|arj|dmg|jar|lzip|lha)" +
                    "(\\?.*)?$"); // For url Query parts ( URL?q=... )


    /**
     * You should implement this function to specify whether the given url
     * should be crawled or not (based on your crawling logic).
     */
    @Override
    public boolean shouldVisit(Page page, WebURL url) {
        String href = url.getURL().toLowerCase();
        return !BINARY_FILES_EXTENSIONS.matcher(href).matches();
    }

    private Object addChild(Object parentDbId, String text)
    {
        Vertex dbParent = sm_mindDb.getVertex(parentDbId);
        MindDB.EdgeVertex edgeVertex = sm_mindDb.addChild(dbParent);
        edgeVertex.m_target.setProperty(MindModel.TEXT_PROP_NAME, text);
        return edgeVertex.m_target.getId();
    }

    /**
     * This function is called when a page is fetched and ready to be processed
     * by your program.
     */
    @Override
    synchronized public void visit(Page page) {
        super.visit(page);

        int docid = page.getWebURL().getDocid();
        int parentDocid = page.getWebURL().getParentDocid();
        String url = page.getWebURL().getURL();
        String parentUrl = page.getWebURL().getParentUrl();
        page.getWebURL().getParentDocid();

        m_logger.info("URL: {}, parentUrl: {}", url, parentUrl);
        m_logger.info("docId:{},  parentDocid: {}", docid, parentDocid);



        if (page.getParseData() instanceof HtmlParseData) {

            Object parentDbId = m_docId2DbIdMap.get(parentDocid);

            Object currentDbId = addChild(parentDbId, url);
            m_docId2DbIdMap.put(docid, currentDbId);

            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String text = htmlParseData.getText();

            page_count++;

            m_importer.importString(currentDbId, 0, text);

            m_logger.info("add page: {},  vertex: {}, page_count: {}, vertex_count {}",
                    url, currentDbId.toString(), page_count, sm_mindDb.getVertexCount());

            String html = htmlParseData.getHtml();

            Set<WebURL> links = htmlParseData.getOutgoingUrls();

            m_logger.debug("Text length: {}", text.length());
            m_logger.debug("Html length: {}", html.length());
            m_logger.debug("Number of outgoing links: {}", links.size());
        }

        Header[] responseHeaders = page.getFetchResponseHeaders();
        if (responseHeaders != null) {
            m_logger.debug("Response headers:");
            for (Header header : responseHeaders) {
                m_logger.debug("\t{}: {}", header.getName(), header.getValue());
            }
        }

        m_logger.debug("=============");
    }

    void start() {
        /*
         * Instantiate the controller for this crawl.
         */
        PageFetcher pageFetcher = new PageFetcher(m_config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = null;

        try {
            controller = new CrawlController(m_config, pageFetcher, robotstxtServer);

            //下面的是改过的crawler4j调用的函数，去掉了robotstxtServer
            //controller = new CrawlController(m_config, pageFetcher);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        /*
         * For each crawl, you need to add some seed urls. These are the first
         * URLs that are fetched and then the crawler starts following links
         * which are found in these pages
         */
        controller.addSeed("http://baike.baidu.com");
        controller.addSeed("http://zhidao.baidu.com");
        controller.addSeed("http://www.163.com");
        controller.addSeed("http://www.sohu.com");
        controller.addSeed("http://www.sina.com");
        controller.addSeed("http://www.sina.com.cn");
        controller.addSeed("http://www.taobao.com");
        controller.addSeed("http://zh.wikipedia.org/wiki/Wikipedia:%E9%A6%96%E9%A1%B5");

        /*
         * Start the crawl. This is a blocking operation, meaning that your code
         * will reach the line after this only when crawling is finished.
         */
        controller.start(Crawler.class, m_numberOfCrawlers);

    }

    void initConfig(int maxPage, int maxDepth)  {

    /*
     * crawlStorageFolder is a folder where intermediate crawl data is
     * stored.
     */
        m_config.setCrawlStorageFolder("/tmp/crawler/");

    /*
     * Be polite: Make sure that we don't send more than 1 request per
     * second (1000 milliseconds between requests).
     */
        m_config.setPolitenessDelay(1500);

    /*
     * You can set the maximum crawl depth here. The default value is -1 for
     * unlimited depth
     */
        m_config.setMaxDepthOfCrawling(maxDepth);

    /*
     * You can set the maximum number of pages to crawl. The default value
     * is -1 for unlimited number of pages
     */
        m_config.setMaxPagesToFetch(maxPage);

        /**
         * Do you want crawler4j to crawl also binary data ?
         * example: the contents of pdf, or the metadata of images etc
         */
        m_config.setIncludeBinaryContentInCrawling(false);

    /*
     * This config parameter can be used to set your crawl to be resumable
     * (meaning that you can resume the crawl from a previously
     * interrupted/crashed crawl). Note: if you enable resuming feature and
     * want to start a fresh crawl, you need to delete the contents of
     * rootFolder manually.
     */
        m_config.setResumableCrawling(false);

        m_config.setUserAgentString("Baiduspider");
    }
}
