package com.vesanieminen.views.statistics;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.Marker;
import com.vaadin.flow.component.charts.model.PlotOptionsLine;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vesanieminen.components.ChartExport;
import com.vesanieminen.i18n.T;
import com.vesanieminen.model.UsedEvSnapshot;
import com.vesanieminen.services.UsedEvListingsService;
import com.vesanieminen.views.MainLayout;

import java.util.List;

@Route(value = "used-evs", layout = MainLayout.class)
public class UsedEvsView extends Main implements HasDynamicTitle {

    private final UsedEvListingsService service;

    public UsedEvsView(UsedEvListingsService service) {
        this.service = service;
    }

    @Override
    public String getPageTitle() {
        return T.tr("statistics.usedEvs.title");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setHeightFull();
        removeAll();
        attachEvent.getUI().getPage().retrieveExtendedClientDetails(details -> {
            if (details.isTouchDevice() && details.isIOS()) {
                setHeight("var(--fullscreen-height)");
            }
        });

        final List<UsedEvSnapshot> history = service.history();
        if (history.isEmpty()) {
            add(new Span(T.tr("statistics.usedEvs.noData")));
            return;
        }

        final var chart = new Chart();
        chart.setHeightFull();
        final var configuration = chart.getConfiguration();
        final var series = new DataSeries(T.tr("statistics.usedEvs.series"));
        for (UsedEvSnapshot snapshot : history) {
            series.add(new DataSeriesItem(snapshot.getFetchedAt(), snapshot.getCount()));
        }
        configuration.getChart().setType(ChartType.LINE);
        configuration.getChart().setStyledMode(true);
        configuration.getLegend().setEnabled(false);
        configuration.getxAxis().setType(AxisType.DATETIME);
        final var yAxis = configuration.getyAxis();
        yAxis.setMin(0);
        yAxis.setOpposite(false);
        yAxis.setTitle("");
        configuration.addSeries(series);

        final var plotOptionsLine = new PlotOptionsLine();
        plotOptionsLine.setAnimation(false);
        plotOptionsLine.setStickyTracking(true);
        plotOptionsLine.setMarker(new Marker(true));
        configuration.setPlotOptions(plotOptionsLine);
        final var tooltip = new Tooltip();
        tooltip.setShared(true);
        configuration.setTooltip(tooltip);

        ChartExport.configure(chart, "used-evs-on-sale");
        add(chart);
    }
}
