package com.vesanieminen.services;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EuriborService {
    public List<EuriborRate> getEuribor12MonthRates() {
        List<EuriborRate> rates = new ArrayList<>();
        try {
            // ECB SDMX URL for Euribor 12-month rates
            String url = "https://data-api.ecb.europa.eu/service/data/EXR/D.USD.EUR.SP00.A?startPeriod=2020-01-01";
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new URL(url).openStream());

            NodeList seriesList = doc.getElementsByTagName("generic:Series");
            for (int i = 0; i < seriesList.getLength(); i++) {
                Node series = seriesList.item(i);
                NodeList children = series.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node node = children.item(j);
                    if (node.getNodeName().equals("generic:Obs")) {
                        Element element = (Element) node;
                        String time = element.getElementsByTagName("generic:ObsDimension").item(0).getAttributes()
                                .getNamedItem("value").getNodeValue();
                        String value = element.getElementsByTagName("generic:ObsValue").item(0).getAttributes()
                                .getNamedItem("value").getNodeValue();

                        EuriborRate rate = new EuriborRate(LocalDate.parse(time), Double.parseDouble(value));
                        rates.add(rate);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rates;
    }
}