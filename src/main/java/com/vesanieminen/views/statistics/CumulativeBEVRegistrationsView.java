package com.vesanieminen.views.statistics;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.Marker;
import com.vaadin.flow.component.charts.model.PlotOptionsLine;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vesanieminen.components.ChartExport;
import com.vesanieminen.i18n.T;
import com.vesanieminen.services.AUT_FI_Service;
import com.vesanieminen.views.MainLayout;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Route(value = "bev-registrations-by-year", layout = MainLayout.class)
public class CumulativeBEVRegistrationsView extends Main implements HasDynamicTitle {

    private static volatile Map<Integer, Number[]> cumulativeCache;

    public CumulativeBEVRegistrationsView() {
    }

    @Override
    public String getPageTitle() {
        return T.tr("statistics.bevPerYear.title");
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
            final Map<Integer, Number[]> cumulativeByYear = getCumulativeByYear();
            if (cumulativeByYear.isEmpty()) {
                return;
            }

            final var chart = new Chart();
            chart.setHeightFull();
            final var configuration = chart.getConfiguration();

            for (Map.Entry<Integer, Number[]> entry : cumulativeByYear.entrySet()) {
                final var series = new ListSeries(String.valueOf(entry.getKey()));
                series.setData(entry.getValue());
                configuration.addSeries(series);
            }

            configuration.getChart().setType(ChartType.LINE);
            configuration.getChart().setStyledMode(true);
            configuration.getLegend().setEnabled(true);
            configuration.getxAxis().setCategories(
                    T.tr("month.short.jan"), T.tr("month.short.feb"), T.tr("month.short.mar"),
                    T.tr("month.short.apr"), T.tr("month.short.may"), T.tr("month.short.jun"),
                    T.tr("month.short.jul"), T.tr("month.short.aug"), T.tr("month.short.sep"),
                    T.tr("month.short.oct"), T.tr("month.short.nov"), T.tr("month.short.dec")
            );
            final var yAxis = configuration.getyAxis();
            yAxis.setMin(0);
            yAxis.setOpposite(false);
            yAxis.setTitle("");

            final var plotOptionsLine = new PlotOptionsLine();
            plotOptionsLine.setAnimation(false);
            plotOptionsLine.setStickyTracking(true);
            plotOptionsLine.setMarker(new Marker(true));
            configuration.setPlotOptions(plotOptionsLine);

            final var tooltip = new Tooltip();
            tooltip.setShared(false);
            tooltip.setHeaderFormat("<span style=\"font-size: 0.85em\">{point.x}</span><br/>");
            tooltip.setPointFormat("<span class=\"highcharts-color-{series.colorIndex}\">●</span> "
                    + "<b>{series.name}</b>: {point.y:,.0f}<br/>");
            configuration.setTooltip(tooltip);

            ChartExport.configure(chart, "bev-registrations-by-year");

            add(chart);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<Integer, Number[]> getCumulativeByYear() throws IOException, URISyntaxException {
        Map<Integer, Number[]> cached = cumulativeCache;
        if (cached != null) {
            return cached;
        }
        synchronized (CumulativeBEVRegistrationsView.class) {
            cached = cumulativeCache;
            if (cached != null) {
                return cached;
            }
            final var evStats = AUT_FI_Service.loadDataFromFile();
            cached = evStats.map(CumulativeBEVRegistrationsView::computeCumulativeByYear)
                    .orElseGet(TreeMap::new);
            cumulativeCache = cached;
            return cached;
        }
    }

    static Map<Integer, Number[]> computeCumulativeByYear(List<AUT_FI_Service.EVStats> stats) {
        final Map<Integer, Integer[]> monthlyByYear = new TreeMap<>();
        for (AUT_FI_Service.EVStats stat : stats) {
            final int year = stat.date().getYear();
            final int monthIndex = stat.date().getMonthValue() - 1;
            monthlyByYear.computeIfAbsent(year, y -> new Integer[12])[monthIndex] = stat.evAmount();
        }
        final Map<Integer, Number[]> cumulativeByYear = new TreeMap<>();
        for (Map.Entry<Integer, Integer[]> e : monthlyByYear.entrySet()) {
            final Integer[] monthly = e.getValue();
            int lastFilled = -1;
            for (int m = 0; m < 12; m++) {
                if (monthly[m] != null) {
                    lastFilled = m;
                }
            }
            if (lastFilled < 0) {
                continue;
            }
            final Number[] cumulative = new Number[lastFilled + 1];
            int running = 0;
            for (int m = 0; m <= lastFilled; m++) {
                if (monthly[m] != null) {
                    running += monthly[m];
                }
                cumulative[m] = running;
            }
            cumulativeByYear.put(e.getKey(), cumulative);
        }
        return cumulativeByYear;
    }
}
