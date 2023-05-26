package com.vesanieminen.views.evstatistics;


import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vesanieminen.views.MainLayout;

@PageTitle("EV Statistics")
@Route(value = "", layout = MainLayout.class)
public class EVStatisticsView extends Main {

    public EVStatisticsView() {
        final var chart = new Chart();
        final var listSeries = new ListSeries(1, 2, 1.5, 3, 2, 25, 5, 0.75);
        chart.getConfiguration().addSeries(listSeries);
        add(chart);
    }

}
