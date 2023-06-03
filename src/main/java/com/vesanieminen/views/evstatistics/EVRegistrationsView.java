package com.vesanieminen.views.evstatistics;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.Marker;
import com.vaadin.flow.component.charts.model.PlotOptionsLine;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vesanieminen.services.AUT_FI_Service;
import com.vesanieminen.views.MainLayout;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.ZoneOffset;

@PageTitle("New EV registrations per month")
@Route(value = "registrations", layout = MainLayout.class)
public class EVRegistrationsView extends Main {

    public EVRegistrationsView() {
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
            final var evStats = AUT_FI_Service.loadDataFromFile();
            if (evStats.isEmpty()) {
                return;
            }
            final var chart = new Chart();
            chart.setTimeline(true);
            chart.setHeightFull();
            final var configuration = chart.getConfiguration();
            final var evRegistrations = new DataSeries("BEV");
            for (AUT_FI_Service.EVStats stat : evStats.get()) {
                evRegistrations.add(new DataSeriesItem(stat.date().atStartOfDay().toInstant(ZoneOffset.UTC), stat.evAmount()));
            }
            configuration.getChart().setType(ChartType.COLUMN);
            configuration.getChart().setStyledMode(true);
            configuration.getLegend().setEnabled(true);
            configuration.getNavigator().setEnabled(false);
            configuration.getScrollbar().setEnabled(false);
            final var yAxis = configuration.getyAxis();
            yAxis.setMin(0);
            yAxis.setOpposite(false);
            configuration.addSeries(evRegistrations);

            final var plotOptionsLine = new PlotOptionsLine();
            plotOptionsLine.setAnimation(false);
            plotOptionsLine.setStickyTracking(true);
            plotOptionsLine.setMarker(new Marker(false));
            chart.getConfiguration().setPlotOptions(plotOptionsLine);
            final var tooltip = new Tooltip();
            tooltip.setShared(true);
            configuration.setTooltip(tooltip);

            add(chart);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
