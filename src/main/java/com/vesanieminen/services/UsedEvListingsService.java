package com.vesanieminen.services;

import com.vesanieminen.model.UsedEvSnapshot;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UsedEvListingsService {

    private static final Logger log = LoggerFactory.getLogger(UsedEvListingsService.class);

    private static final String RESOURCE_KEY =
            "aHR0cHM6Ly93d3cubmV0dGlhdXRvLmNvbS9oYWt1dHVsb2tzZXQ/aGFrdT1QMTYzNDUyMTAzNw==";

    // The upstream listing page renders the total count into a span carrying
    // a "totalRecords" CSS class — match its inner text.
    private static final Pattern TOTAL_RECORDS_PATTERN = Pattern.compile(
            "totalRecords[^>]*>([^<]+)<");

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final Duration BASE_INTERVAL = Duration.ofHours(1);
    private static final Duration MAX_JITTER = Duration.ofMinutes(30);

    private final UsedEvSnapshotRepository repository;
    private final TaskScheduler taskScheduler;
    private final String sourceUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public UsedEvListingsService(UsedEvSnapshotRepository repository, TaskScheduler taskScheduler) {
        this.repository = repository;
        this.taskScheduler = taskScheduler;
        this.sourceUrl = new String(Base64.getDecoder().decode(RESOURCE_KEY), StandardCharsets.UTF_8);
    }

    @PostConstruct
    void start() {
        scheduleNextRun(jitter());
    }

    private void scheduleNextRun(Duration delay) {
        Instant nextRun = Instant.now().plus(delay);
        log.debug("Next listings fetch scheduled for {}.", nextRun);
        taskScheduler.schedule(this::runAndReschedule, nextRun);
    }

    private void runAndReschedule() {
        try {
            fetchAndPersist();
        } catch (Exception e) {
            log.warn("Scheduled listings fetch failed.", e);
        }
        scheduleNextRun(BASE_INTERVAL.plus(jitter()));
    }

    private static Duration jitter() {
        return Duration.ofMillis(ThreadLocalRandom.current().nextLong(MAX_JITTER.toMillis() + 1));
    }

    public synchronized UsedEvSnapshot fetchAndPersist() throws IOException, InterruptedException {
        int count = fetchCount();
        UsedEvSnapshot snapshot = new UsedEvSnapshot(Instant.now(), count);
        UsedEvSnapshot saved = repository.save(snapshot);
        log.info("Persisted used-EV listings snapshot: {} listings.", count);
        return saved;
    }

    int fetchCount() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "fi,en;q=0.8")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Unexpected HTTP status " + response.statusCode() + " from listings source.");
        }
        return parseCount(response.body());
    }

    static int parseCount(String html) throws IOException {
        Matcher matcher = TOTAL_RECORDS_PATTERN.matcher(html);
        if (!matcher.find()) {
            throw new IOException("Could not locate totalRecords element in listings response.");
        }
        String digits = matcher.group(1).replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw new IOException("totalRecords element contained no digits: " + matcher.group(1));
        }
        return Integer.parseInt(digits);
    }

    public Optional<UsedEvSnapshot> latestSnapshot() {
        return repository.findFirstByOrderByFetchedAtDesc();
    }

    public List<UsedEvSnapshot> history() {
        return repository.findAllByOrderByFetchedAtAsc();
    }
}
