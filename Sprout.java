import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Sprout class implements a program that
 * makes a socket connection and scan a web-page retrieved through
 * HTTP request and response. Then, this collects all needed data from the page.
 *
 * @author Edwin Yang
 */
public class Sprout {
    private List<String> urls = new LinkedList<>();
    private Socket socket;
    private String remoteURL;
    private int port;
    private PrintWriter pw;
    private InputStreamReader isr;
    private BufferedReader br;
    private String relativeURL;
    private String status;
    private int nonHTMLCount;
    private Map<String, Integer> pagesSizes = new HashMap<>();
    private List<String> invalidURLs = new LinkedList<>();
    private Map<String, String> redirectedURLs = new HashMap<>();
    private Map<String, String> pagesTimes = new HashMap<>();

    /**
     * This method is used to process the crawling.
     * 1. Socket connection
     * 2. Sending an HTTP request
     * 3. Receiving an HTTP response
     * 4. Building a HTML Document String for scanning
     * 5. Scanning URLs
     * 6. Collecting recognised URLs
     * 7. Scanning status of the HTTP response
     * 8. Collecting invalid URLs and redirected URLs
     * 9. Scanning a page size
     * 10. Collecting its URL and size
     * 11. Scanning the last modified date
     * 12. Collecting its URL and the date
     * 11. Socket closing
     *
     * @param url
     */
    public void crawl(String url) {
        try {
        	//This part has to be modified.
            //Connecting the host with socket, passing only INetAddress and Port
            if(url.equals("comp3310.ddns.net:7880") || url.equals("/")) {
                if(url.equals("/")) {
                    connect("comp3310.ddns.net:7880");
                } else {
                    connect(url);
                }
                relativeURL = "";
                sendHTTPRequest(relativeURL);
            } else {
                connect(url.substring(0, url.indexOf("/")));
                relativeURL = url.substring(23);
                sendHTTPRequest(relativeURL);
            }
            recvHTTPResponse();

            StringBuilder strBuilder = new StringBuilder();
            String currentLine;
            String htmlDocument;

            //Building a String that contains an HTML document
            while((currentLine = br.readLine()) != null) {
                strBuilder.append(currentLine + "\n");
            }
            htmlDocument = strBuilder.toString();

            scanURLs(htmlDocument);
            scanStatus(htmlDocument);
            //Scan the size and the last modified time if the page status is valid
            if(status.equals("200")) {
                scanSize(htmlDocument);
                scanTime(htmlDocument);
            }

            //Socket closing
            pw.close();
            br.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * This method is used to return the collected URLs
     * @return A list of collected URLs
     */
    public List<String> getURLs() {
        return this.urls;
    }

    /**
     * This method is used to return the collected URL and
     * the its Content-Length
     * @return A Map of the page URL and its size
     */
    public Map<String, Integer> getPagesSizes() {
        return this.pagesSizes;
    }

    /**
     * This method is used to return the collected invalid URLs
     * @return A List of the URL
     */
    public List<String> getInvalidURLs() {
        return this.invalidURLs;
    }

    /**
     * This method is used to return the collected redirections
     * @return A map of the page URL and its redirection URL
     */
    public Map<String, String> getRedirectedURLs() {
        return this.redirectedURLs;
    }

    /**
     * This method is used to return the collected valid page URL and
     * its Last Modified Time
     * @return A map of the page URL and its Last Modified Time
     */
    public Map<String, String> getPagesTimes() {
        return this.pagesTimes;
    }

    /**
     * This method is used to return the counted non-HTML object
     * @return An integer of the counted non-HTML object
     */
    public int getNonHTMLCount() {
        return nonHTMLCount;
    }

    /**
     * This method is used to make a socket connection
     * @param host host address
     * @throws IOException Java IO Exceptions
     */
    public void connect(String host) throws IOException {
        String addressPort[] = host.split(":");
        remoteURL = addressPort[0];
        port = Integer.parseInt(addressPort[1]);
        socket = new Socket(remoteURL, port);
    }

    /**
     * This method is used to send an HTTP request
     * @param relativeURL A relative URL to send the request
     * @throws IOException Java IO Exceptions
     */
    public void sendHTTPRequest(String relativeURL) throws IOException {
        pw = new PrintWriter(socket.getOutputStream());
        pw.print("GET /" + relativeURL + " HTTP/1.0\r\n");
        //This part has to be modified.
        pw.print("Host: comp3310.ddns.net:7880\r\n");
        pw.print("\r\n");
        pw.flush();
    }

    /**
     * This method is used to receive an HTTP response
     * @throws IOException
     */
    public void recvHTTPResponse() throws IOException {
        isr = new InputStreamReader(socket.getInputStream());
        br = new BufferedReader(isr);
    }

    /**
     * This method is used to scan URLs in the page
     * @param str An HTML document
     */
    public void scanURLs(String str) {
        //Initialising patterns to find URLs
        Pattern p[] = new Pattern[2];
        p[0] = Pattern.compile("(href=\")([^=?]*)(\")|(href=\")(\\w*\\.\\w+)(\")|(href=\"\\w*[/])(\\w*\\.\\w+)(\")");
        p[1] = Pattern.compile("(src=\")([^=?]*)(\")|(src=\")(\\w*\\.\\w+)(\")|(src=\"\\w*[/])(\\w*\\.\\w+)(\")");
        Matcher matcher;
        for (Pattern urlPattern : p) {
            matcher = urlPattern.matcher(str);
            while(matcher.find()) {
                String tempURL;
                //Handling the non-relative URL
                if(matcher.group(2).startsWith("http")) {
                    tempURL = matcher.group(2).substring(30);
                } else {
                    //Handling the URL that has a sub-direct (i.e. k/301.html)
                    Pattern relativeURLPattern = Pattern.compile("(\\w*/)(\\w*.\\w*)");
                    Matcher relativeURLMatcher = relativeURLPattern.matcher(relativeURL);
                    tempURL = relativeURLMatcher.find() && !matcher.group(2).equals("/")?
                            relativeURLMatcher.group(1) + matcher.group(2) :
                            matcher.group(2);
                }
                //Adding the URL to the list
                urls.add(tempURL);
            }
        }
    }

    /**
     * This method is used to scan the status of the valid HTML page
     * @param str An HTML Document
     */
    public void scanStatus(String str) {
        //Initialising a pattern to find a status
        Pattern statusPattern = Pattern.compile("(HTTP/1\\.1\\s)(\\d+)(\\s\\w+)");
        Matcher matcher = statusPattern.matcher(str);
        status = "0";
        if(matcher.find()) {
            status = matcher.group(2);
            if(status.equals("404")) {
                //Adding 404 and the page URL
                invalidURLs.add(relativeURL);
            }
            if(status.matches("30\\d")) {
                //Counting 30x, URL, and where they redirect to
            	//This part has to be modified.
                Pattern locationPattern = Pattern.compile("(Location: http://comp3310\\.ddns\\.net:7880/)([a-z/]*\\w*[.]\\w*)");
                Matcher locationMatcher = locationPattern.matcher(str);

                //Calling find() first to use the matcher
                locationMatcher.find();
                String locationURL = locationMatcher.group(2);

                //Putting the URL and its redirected URL
                redirectedURLs.put(relativeURL, locationURL);
            }
        }
    }

    /**
     * This method is used to scan the size of the page
     * @param str An HTML document
     */
    public void scanSize(String str) {
        //Initialising a pattern to find the size
        Pattern lengthPattern = Pattern.compile("(Content-Length: )(\\d+)");
        Matcher matcher = lengthPattern.matcher(str);
        int fileSize;
        if(matcher.find() && (relativeURL.endsWith("html") || relativeURL.equals(""))) {
            fileSize = Integer.parseInt(matcher.group(2));
            //Collecting the URL(Key) and the size(Value) of each page
            if(relativeURL.equals("")) {
                pagesSizes.put("/", fileSize);
            } else {
                pagesSizes.put(relativeURL, fileSize);
            }
        } else if(!relativeURL.endsWith("html")) {
            //Counting non-HTML objects
            nonHTMLCount++;
        }
    }

    /**
     * This method is used to scan the Last Modified Time of the page
     * @param str An HTML document
     */
    public void scanTime(String str) {
        //Initialising a pattern to scan the Last Modified Time of the page
        Pattern datePattern = Pattern.compile("(Last-Modified: )(.*)");
        Matcher matcher = datePattern.matcher(str);
        if(matcher.find() && (relativeURL.endsWith("html") || relativeURL.equals(""))) {
            String lastModifiedDate = matcher.group(2);
            //Collecting the URL(Key) and the Last-modified date(Value) of each page
            //Handling the URL of the main page
            if(relativeURL.equals("")) {
                pagesTimes.put("/", lastModifiedDate);
            } else {
                pagesTimes.put(relativeURL, lastModifiedDate);
            }
        }
    }
}