package com.vesanieminen.views.evstatistics;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vesanieminen.services.AUT_FI_Service;
import com.vesanieminen.views.MainLayout;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

@PageTitle("EV Statistics")
@Route(value = "", layout = MainLayout.class)
public class EVStatisticsView extends Main {

    public EVStatisticsView() {
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        try {
            final var evStats = AUT_FI_Service.loadDataFromFile();
            if (evStats.isEmpty()) {
                return;
            }
            final var chart = new Chart();
            chart.setHeightFull();
            setHeightFull();
            final var configuration = chart.getConfiguration();
            final var evRegistrations = new DataSeries("BEV");
            final var otherRegistrations = new DataSeries("Other (incl. PHEV etc.)");
            final var categories = new ArrayList<String>();
            for (AUT_FI_Service.EVStats stat : evStats.get()) {
                categories.add(stat.name());
                evRegistrations.add(new DataSeriesItem(stat.name(), (double) stat.evAmount() / stat.totalAmount() * 100.0));
                otherRegistrations.add(new DataSeriesItem(stat.name(), (double) stat.otherAmount() / stat.totalAmount() * 100.0));
            }
            configuration.getxAxis().setCategories(categories.toArray(String[]::new));
            configuration.getyAxis().setMax(100);
            configuration.getyAxis().setMin(0);
            configuration.addSeries(evRegistrations);
            configuration.addSeries(otherRegistrations);
            configuration.setTitle("Car registration percentage in Finland");
            add(chart);

        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
