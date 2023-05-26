package com.vesanieminen.services;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.vesanieminen.Utils.getInt;

public class AUT_FI_Service {

    private final static String URL = "https://www.aut.fi/tilastot/ensirekisteroinnit/ensirekisteroinnit_kayttovoimittain/henkiloautojen_kayttovoimatilastot?download_8097=1";
    private final static String CSV_FILENAME = "aut.fi.csv";
    private final static String DEFAULT_CSV_FILENAME = "data/ensirekisteroinnit_kayttovoimat_jakauma-2.csv";

    public static HttpResponse<String> loadData() {
        try {
            final var readableByteChannel = Channels.newChannel(new URL(URL).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(CSV_FILENAME);
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
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
        URL url = AUT_FI_Service.class.getClassLoader().getResource(DEFAULT_CSV_FILENAME);
        if (url == null) {
            return Optional.empty();
        }
        final var file = new File(url.toURI());
        final var fileReader = new FileReader(file);

        CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
        CSVReader csvReader = new CSVReaderBuilder(fileReader)
                .withSkipLines(0)
                .withCSVParser(parser)
                .build();

        String[] header = csvReader.readNext();

        String[] line;
        final var evStats = new ArrayList<EVStats>();
        while ((line = csvReader.readNext()) != null) {
            final var evAmount = getInt(line[3]);
            final var totalAmount = getInt(line[11]);
            final var otherAmount = totalAmount - evAmount;
            evStats.add(new EVStats(line[0], evAmount, otherAmount, totalAmount));
        }
        csvReader.close();
        return Optional.of(evStats);
    }

    public record EVStats(String name, int evAmount, int otherAmount, int totalAmount) {
    }

}
