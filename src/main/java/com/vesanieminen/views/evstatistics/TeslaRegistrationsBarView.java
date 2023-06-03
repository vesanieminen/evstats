package com.vesanieminen.views.evstatistics;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DataLabels;
import com.vaadin.flow.component.charts.model.Labels;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.PlotOptionsBar;
import com.vaadin.flow.component.charts.model.Stacking;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vesanieminen.services.AUT_FI_Service;
import com.vesanieminen.views.MainLayout;

import java.io.IOException;
import java.net.URISyntaxException;

@PageTitle("Tesla Sales in Finland per Year and Month")
@Route(value = "tesla-registrations-bar", layout = MainLayout.class)
public class TeslaRegistrationsBarView extends Main {

    public TeslaRegistrationsBarView() {
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setHeightFull();
        attachEvent.getUI().getPage().retrieveExtendedClientDetails(details -> {
            if (details.isTouchDevice() && details.isIOS()) {
                setHeight("var(--fullscreen-height)");
            }
        });
        try {
            final var evStats = AUT_FI_Service.loadTeslaDataFromFile();
            if (evStats.isEmpty()) {
                return;
            }
            final var chart = new Chart();
            chart.setHeightFull();
            final var configuration = chart.getConfiguration();
            final var teslaStats = evStats.get();
            for (int month = 4; month >= 0; --month) {
                final var series = new ListSeries();
                switch (month) {
                    case 0 -> series.setName("January");
                    case 1 -> series.setName("February");
                    case 2 -> series.setName("March");
                    case 3 -> series.setName("April");
                    case 4 -> series.setName("May");
                }
                for (int year = 0; year < 5; ++year) {
                    final var stat = teslaStats.get(month + year * 12);
                    series.addData(stat.amount());
                }
                configuration.addSeries(series);
            }
            configuration.getChart().setType(ChartType.BAR);
            configuration.getChart().setStyledMode(true);
            configuration.getLegend().setEnabled(true);
            configuration.getLegend().setReversed(true);
            configuration.getNavigator().setEnabled(false);
            configuration.getScrollbar().setEnabled(false);
            final var yAxis = configuration.getyAxis();
            yAxis.setMin(0);
            final var labels = new Labels();
            yAxis.setLabels(labels);
            yAxis.setOpposite(false);
            configuration.getxAxis().setCategories("2019", "2020", "2021", "2022", "2023");

            final var plotOptionsBar = new PlotOptionsBar();
            final var dataLabels = new DataLabels();
            dataLabels.setEnabled(true);
            plotOptionsBar.setDataLabels(dataLabels);
            plotOptionsBar.setStacking(Stacking.NORMAL);
            plotOptionsBar.setAnimation(false);
            plotOptionsBar.setStickyTracking(true);
            //plotOptionsLine.setMarker(new Marker(false));
            chart.getConfiguration().setPlotOptions(plotOptionsBar);
            final var tooltip = new Tooltip();
            //tooltip.setShared(true);
            configuration.setTooltip(tooltip);

            add(chart);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
