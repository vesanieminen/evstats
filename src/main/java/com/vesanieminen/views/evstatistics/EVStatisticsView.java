package com.vesanieminen.views.evstatistics;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.Labels;
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

import static com.vaadin.flow.component.charts.model.style.SolidColor.BLUE;
import static com.vaadin.flow.component.charts.model.style.SolidColor.RED;

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
            chart.setTimeline(true);
            chart.setHeightFull();
            setHeightFull();
            final var configuration = chart.getConfiguration();
            final var evRegistrations = new DataSeries("BEV");
            final var otherRegistrations = new DataSeries("Other (incl. PHEV etc.)");
            for (AUT_FI_Service.EVStats stat : evStats.get()) {
                evRegistrations.add(new DataSeriesItem(stat.date().atStartOfDay().toInstant(ZoneOffset.UTC), (double) stat.evAmount() / stat.totalAmount() * 100.0));
                otherRegistrations.add(new DataSeriesItem(stat.date().atStartOfDay().toInstant(ZoneOffset.UTC), (double) stat.otherAmount() / stat.totalAmount() * 100.0));
            }
            configuration.getLegend().setEnabled(true);
            final var yAxis = configuration.getyAxis();
            yAxis.setMax(100);
            yAxis.setMin(0);
            yAxis.setOpposite(false);
            var labels = new Labels();
            labels.setFormatter("return this.value +'%'");
            yAxis.setLabels(labels);
            //yAxis.setTitle(getTranslation(""));
            yAxis.setLabels(labels);
            configuration.addSeries(evRegistrations);
            configuration.addSeries(otherRegistrations);
            configuration.setTitle("Car registration percentages in Finland");

            final var evPlotOptions = new PlotOptionsLine();
            evPlotOptions.setColor(BLUE);
            evRegistrations.setPlotOptions(evPlotOptions);
            final var otherPlotOptions = new PlotOptionsLine();
            otherPlotOptions.setColor(RED);
            otherRegistrations.setPlotOptions(otherPlotOptions);

            final var plotOptionsLine = new PlotOptionsLine();
            plotOptionsLine.setAnimation(false);
            plotOptionsLine.setStickyTracking(true);
            plotOptionsLine.setMarker(new Marker(false));
            chart.getConfiguration().setPlotOptions(plotOptionsLine);
            final var tooltip = new Tooltip();
            tooltip.setValueDecimals(2);
            tooltip.setValueSuffix("%");
            configuration.setTooltip(tooltip);


            add(chart);

        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
