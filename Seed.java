import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * The Seed Class implements an interface that
 * controls a web-crawling navigation,
 * collects all data which is parsed from each web-page and
 * reports the result.
 *
 * @author Edwin Yang
 */
public class Seed {
    private Set<String> pagesVisited = new HashSet<>();
    private List<String> pagesToVisit = new LinkedList<>();
    private int nonHTMLCount = 0;
    private Map<String, Integer> pagesSizes = new HashMap<>();
    private List<String> invalidURLs = new LinkedList<>();
    private Map<String, String> redirectedURLs = new HashMap<>();
    private Map<String, String> pagesTimes = new HashMap<>();
    private int maxSize;
    private int minSize;
    private String minSizePage;
    private String maxSizePage;

    private String oldestModifiedTime;
    private String latestModifiedTime;
    private String oldestModifiedPage;
    private String latestModifiedPage;

    /**
     * This method is used to explore a web-site. During exploration,
     * this creates its own sprouts that make an HTTP request and parse the response.
     * Then, this collects all data from the sprout each time it finishes parsing.
     * @param url A Starting point of crawling
     * @throws InterruptedException TimeUnit exceptions
     */
    public void explore(String url) throws InterruptedException {
        String currentURL = url;
        //Add "/" to pagesToVisit at the start given this will visit it's main page
        pagesToVisit.add("/");
        System.out.println("Client: Crawling the site...");
        System.out.println("Client: Please wait for a while");

        while(!pagesToVisit.isEmpty()) {
            //Initiating the sprout to crawl the site
            Sprout sprout = new Sprout();

            //Prevent the server overloading
            TimeUnit.MILLISECONDS.sleep(2001);

            //A sprout starts crawling with currentURL
            sprout.crawl(currentURL);

            //Collecting all recognised URLs in the page
            this.pagesToVisit.addAll(sprout.getURLs());

            //Collecting all URL of valid html pages with their sizes
            this.pagesSizes.putAll(sprout.getPagesSizes());

            //Collecting all URL of valid html pages with their last-modified date
            this.pagesTimes.putAll(sprout.getPagesTimes());

            //Collecting all invalid URLs
            this.invalidURLs.addAll(sprout.getInvalidURLs());

            //Collecting all redirected URLs
            this.redirectedURLs.putAll(sprout.getRedirectedURLs()); //6

            //Counting non-html objects
            this.nonHTMLCount += sprout.getNonHTMLCount();

            //Changing currentURL to URL of the next page with nextURL method
            currentURL = url + "/" + nextURL();

            //Quitting the while-loop when there are no pages to visit anymore
            if(pagesToVisit.isEmpty()) {break;}

        }

        //Initiating the final report
        report();
    }

    /**
     * This method is used to change currentURL that
     * is used to send an HTTP request
     * @return next URL to retrieve
     */
    private String nextURL() {
        String nextURL;
        do {
            //adding the URL of the visited page to pagesVisited
            pagesVisited.add(pagesToVisit.remove(0));
            if(!pagesToVisit.isEmpty()) {
                nextURL = pagesToVisit.get(0);
            } else {
                //handling NullException
                nextURL = "";
            }
          //checking if nextURL is already visited
        } while(pagesVisited.contains(nextURL));
        return nextURL;
    }

    /**
     * This method is used to form the final report
     * @return nothing
     */
    private void report() {
        maxSizePage(pagesSizes);
        minSizePage(pagesSizes);

        processLastModifiedTime(pagesTimes);

        System.out.println("\n\n[================ Web Crawling Report ================]\n\n" +
                "#1 The total number of distinct URLs found on the site\n" +
                "(including errors and redirects)\n" +
                +(pagesVisited.size())+"\n\n" +
                "#2 The number of html pages and the number of non-html objects\n" +
                "on the site (e.g. images)\n" +
                "The number of html pages: " + pagesSizes.size() + "\n" +
                "The number of non-html objects: " + nonHTMLCount + "\n\n" +
                "#3 The smallest and largest html pages, and their sizes\n" +
                "URL of the smallest html page: " + minSizePage + ", " + minSize + "Bytes\n" +
                "URL of the largest html page: " + maxSizePage + ", " + maxSize + "Bytes\n\n" +
                "#4 The oldest and the most-recently modified page,\n" +
                "and their date/timestamps\n" +
                "URL of the oldest modified page: " + oldestModifiedPage + ", " + oldestModifiedTime + "\n" +
                "URL of the latest modified page: " + latestModifiedPage + ", " + latestModifiedTime + "\n");

        System.out.println("#5 A list of invalid URLs (not) found (404)");
        for(String str : invalidURLs) {
            System.out.println(str);
        }
        System.out.print("\n");
        System.out.println("#6 A list of redirected URLs found (30x) and where they redirect to");
        for(Entry<String, String> entry : redirectedURLs.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

    /**
     * This method is used to find the page that
     * has the maximum Content-Length
     * @param map A map that contains the page URL and the size
     */
    public void maxSizePage(Map<String, Integer> map) {
        maxSize = Collections.max(map.values());
        for(Entry<String, Integer> entry : map.entrySet()) {
            if(entry.getValue().equals(maxSize)) {
                maxSizePage = entry.getKey();
            }
        }
    }

    /**
     * This method is used to find the page that
     * has the minimum Content-Length
     * @param map A map that contains the page URL and the size
     */
    public void minSizePage(Map<String, Integer> map) {
        minSize = Collections.min(map.values());
        for(Entry<String, Integer> entry : map.entrySet()) {
            if(entry.getValue().equals(minSize)) {
                minSizePage = entry.getKey();
            }
        }
    }

    /**
     * This method is used to find the oldest modified page and
     * the latest modified page
     * @param map A map that contains the page URL and the Last-Modified date
     */
    public void processLastModifiedTime(Map<String, String> map) {
        List<ZonedDateTime> zdtList = new LinkedList<>();
        List<Long> zdtEpochList = new LinkedList<>();

        //Dates to List<ZonedDateTime>
        for(Entry<String, String> entry : map.entrySet()) {
            ZonedDateTime zdt = ZonedDateTime.parse(entry.getValue(), DateTimeFormatter.RFC_1123_DATE_TIME);
            zdtList.add(zdt);
        }

        //List<ZonedDateTime> to List<Long>
        for(ZonedDateTime zdt : zdtList) {
            zdtEpochList.add(zdt.toEpochSecond());
        }

        //Minimum value(The oldest modified time)
        long minEpochSecond = Collections.min(zdtEpochList);

        //Maximum value(The latest modified time)
        long maxEpochSecond = Collections.max(zdtEpochList);

        //Parsing Minimum and maximum values back to ZonedDateTime form
        ZonedDateTime oldestZDT = Instant.ofEpochSecond(minEpochSecond).atZone(ZoneId.of("GMT"));
        ZonedDateTime latestZDT = Instant.ofEpochSecond(maxEpochSecond).atZone(ZoneId.of("GMT"));

        //Extracting the oldest modified page URL and timestamp
        for(Entry<String, String> entry : map.entrySet()) {
            if(entry.getValue().equals(DateTimeFormatter.RFC_1123_DATE_TIME.format(oldestZDT))) {
                oldestModifiedPage = entry.getKey();
                oldestModifiedTime = entry.getValue();
            }
        }

        //Extracting the latest modified page URL and timestamp
        for(Entry<String, String> entry : map.entrySet()) {
            if(entry.getValue().equals(DateTimeFormatter.RFC_1123_DATE_TIME.format(latestZDT))) {
                latestModifiedPage = entry.getKey();
                latestModifiedTime = entry.getValue();
            }
        }
    }
}
