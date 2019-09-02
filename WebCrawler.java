/**
 * The WebCrawler program implements an application that
 * crawls the website and report the findings, which are
 *
 * 1. The total number of distinct URLs found on the site
 * 2. The number of html pages and the number of non-html objects
 *    on the site
 * 3. The smallest and largest html pages, and their sizes
 * 4. The oldest and the most-recently modified page,
 *    and their date/timestamps
 * 5. A list of invalid URLs (not) found (404)
 * 6. A list of redirected URLs found (30x) and where they redirect to
 *
 * @author Edwin Yang
 */
public class WebCrawler {
    /**
     * This is the main method which makes use of Seed class.
     * @param args This argument will be the network address (i.e. your.address.com:[port])
     * @throws InterruptedException TimeUnit exceptions
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Client: Connecting to " + args[0]);
        Seed seed = new Seed();
        seed.explore(args[0]);

    }
}
