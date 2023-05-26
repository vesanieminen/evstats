package com.vesanieminen.services;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.vesanieminen.Utils.getInt;
import static com.vesanieminen.Utils.getMonth;

public class AUT_FI_Service {

    private final static String URL = "https://www.aut.fi/tilastot/ensirekisteroinnit/ensirekisteroinnit_kayttovoimittain/henkiloautojen_kayttovoimatilastot?download_8097=1";
    private final static String CSV_FILENAME = "aut.fi.csv";
    private final static String DEFAULT_CSV_FILENAME = "data/ensirekisteroinnit_kayttovoimat_jakauma-2.csv";

    // Doesn't work atm. due to the url serving a page instead of the actual file.
    public static HttpResponse<String> loadData() {
        try {
            final var readableByteChannel = Channels.newChannel(new URL(URL).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(CSV_FILENAME);
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final HttpRequest request;
        final HttpResponse<String> response;
        try {
            request = HttpRequest.newBuilder().uri(new URI(URL)).GET().build();
            response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
            int i = 0;
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    public static Optional<List<EVStats>> loadDataFromFile() throws IOException, URISyntaxException {
        try (InputStream inputStream = AUT_FI_Service.class.getClassLoader().getResourceAsStream(DEFAULT_CSV_FILENAME);
             InputStreamReader inputStreamReader = new InputStreamReader(Objects.requireNonNull(inputStream));
             BufferedReader reader = new BufferedReader(inputStreamReader)) {

            CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
            CSVReader csvReader = new CSVReaderBuilder(reader)
                    .withSkipLines(0)
                    .withCSVParser(parser)
                    .build();

            String[] header = csvReader.readNext();

            String[] line;
            final List<EVStats> evStats = new ArrayList<>();
            String year = "";
            while ((line = csvReader.readNext()) != null) {
                String month = line[0];
                if (line[0].contains("/")) {
                    final var split = line[0].split("/");
                    year = split[0];
                    month = split[1];
                }
                var date = LocalDate.of(Integer.parseInt(year), getMonth(month), 1);
                date = date.withDayOfMonth(date.lengthOfMonth());
                final var evAmount = getInt(line[3]);
                final var totalAmount = getInt(line[11]);
                final var otherAmount = totalAmount - evAmount;
                evStats.add(new EVStats(line[0], date, evAmount, otherAmount, totalAmount));
            }
            csvReader.close();
            return Optional.of(evStats);
        } catch (IOException e) {
            // Handle or log the exception accordingly
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public record EVStats(String name, LocalDate date, int evAmount, int otherAmount, int totalAmount) {
    }

}
