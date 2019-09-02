# JavaWebCrawling

**Disclaimer: This was made for the university project and the code has to be modified in order to be used in the real case.
There three parts you want to modify in `Sprout.java`.**

This WebCrawler application crawls the website and reports the findings, which are:
- The total number of distinct URLs found on the site
- The number of html pages and the number of non-html objects on the site
- The smallest and largest html pages, and their sizes
- The oldest and the most-recently modified page, and their date/timestamps
- A list of invalid URLs (not) found (404)
- A list of redirected URLs found (30x) and where they redirect to

## Usage example
After compile all three java files, you can run this command from your command line.

```
java WebCrawler your.address.com:port
```
