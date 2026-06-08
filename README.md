# virtual-thread-webcrawler

A small, dependency-light **concurrent web crawler** in Java 21 that uses
**virtual threads** to fetch many pages in parallel. It does a breadth-first
crawl from a list of seed URLs, follows links to a configurable depth, parses
each page's title and outbound links with [jsoup](https://jsoup.org/), and
writes a CSV report.

HTTP is handled by the JDK's built-in `java.net.http.HttpClient` — there is **no
third-party HTTP dependency**. Virtual threads make a blocking-style
`client.send(...)` per page cheap, so thousands of in-flight requests cost almost
nothing while remaining easy to read.

## Features

- **Virtual-thread concurrency** — one virtual thread per URL (`Executors.newVirtualThreadPerTaskExecutor()`).
- **Real BFS crawl** — follows discovered `<a href>` links to `--depth N`, with a
  thread-safe visited set so every URL is fetched at most once.
- **Same-domain option** — stay on the seed's host (`www.` ignored) or roam.
- **Bounded & polite** — a `Semaphore` caps in-flight requests; `--max-pages`
  hard-limits total fetches; every request has a connect + response timeout.
- **Shared HTTP client** — one pooled `HttpClient`, not one per request.
- **CSV report** — `output/results.csv` with url, depth, status, timing, link count, title.
- **Tested without the network** — JUnit 5 tests run the crawler against an
  in-JVM `HttpServer`.

## Requirements

- JDK 21+ (virtual threads). Build targets `--release 21`.
- Maven 3.9+.

## Build & test

```bash
mvn clean package      # compiles, runs tests, builds a runnable fat jar
mvn test               # tests only
```

## Run

Via the fat jar:

```bash
java -jar target/virtual-thread-webcrawler-1.0-SNAPSHOT.jar --depth 1
```

Via Maven:

```bash
mvn exec:java -Dexec.args="--depth 1 --concurrency 50"
```

### Options

| Flag | Default | Meaning |
|---|---|---|
| `--seeds <path>` | bundled `urls.txt` | newline-delimited seed URLs (`#` comments allowed) |
| `--depth <n>` | `1` | link-hops to follow (`0` = fetch seeds only) |
| `--concurrency <n>` | `50` | max simultaneous in-flight requests |
| `--max-pages <n>` | `500` | hard cap on total pages fetched |
| `--same-domain <bool>` | `true` | only follow links on the parent page's host |
| `--out <dir>` | `output` | directory for `results.csv` |

### Example

```text
$ java -jar target/...jar --seeds <(echo https://maven.apache.org) --depth 1 --max-pages 8 --concurrency 4
Crawling 1 seed(s) | depth=1 sameDomain=true concurrency=4 maxPages=8
[d0] https://maven.apache.org -> 200 OK | Welcome to Apache Maven | 195 ms | 60 links
[d1] https://maven.apache.org/pom/index.html -> 200 OK | Parent POMs – Maven | 142 ms | 65 links
...
Done: 8 pages (8 ok, 0 failed) in 829 ms wall-clock | avg fetch 168 ms | report: output/results.csv
```

The "wall-clock" total being far smaller than the sum of individual fetch times
is the whole point — the pages are crawled concurrently on virtual threads.

## Project layout

```
src/main/java/com/rishav/projects/
  App.java                       CLI: arg parsing, run, CSV report, summary
  service/WebCrawlerService.java fetch() + BFS crawl() on virtual threads
  model/CrawlResult.java         immutable record of one fetched page
  util/CrawlerUtil.java          seed loading + URL normalization / same-host
src/main/resources/urls.txt      default seed list
src/test/java/...                JUnit 5 tests (in-JVM HttpServer, no network)
```

## Notes & limitations

- Title/link extraction is HTML-only; it does not execute JavaScript, so
  JS-rendered sites may report "No title found".
- `robots.txt` is not consulted — keep `--max-pages` / `--depth` modest and be a
  polite citizen when pointing it at sites you don't own.
- `403`/timeouts are recorded as failed rows rather than aborting the crawl.
