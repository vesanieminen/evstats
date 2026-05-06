package com.vesanieminen.views.statistics;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.events.PointClickEvent;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DashStyle;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.Marker;
import com.vaadin.flow.component.charts.model.PlotOptionsColumn;
import com.vaadin.flow.component.charts.model.PlotOptionsLine;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.components.ChartExport;
import com.vesanieminen.i18n.T;
import com.vesanieminen.services.TraficomInspectionService;
import com.vesanieminen.services.TraficomInspectionService.InspectionRow;
import com.vesanieminen.views.MainLayout;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Route(value = "tesla-reliability", layout = MainLayout.class)
public class TeslaReliabilityView extends Main implements HasDynamicTitle {

    private static final List<String> TESLA_MODELS = List.of("MODEL S", "MODEL X", "MODEL 3", "MODEL Y");
    private static final int MIN_YEAR = 2014;
    private static final int MAX_YEAR = 2021;

    private final Chart chart = new Chart();
    private final Div panel = new Div();
    private final VerticalLayout root = new VerticalLayout();

    private List<InspectionRow> teslaRows;

    @Override
    public String getPageTitle() {
        return T.tr("statistics.teslaReliability.title");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setHeightFull();
        attachEvent.getUI().getPage().retrieveExtendedClientDetails(details -> {
            if (details.isTouchDevice() && details.isIOS()) {
                setHeight("var(--fullscreen-height)");
            }
        });

        teslaRows = TraficomInspectionService.teslaRows();

        removeAll();
        root.removeAll();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(false);

        chart.setHeight("440px");
        chart.getStyle().set("width", "100%");
        ChartExport.configure(chart, "tesla-reliability");
        root.add(chart);

        panel.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Margin.Top.MEDIUM
        );
        root.add(panel);

        add(root);

        renderChart();
        // Default selection per spec: Tesla Model 3 / 2021.
        selectPoint(findRow("MODEL 3", 2021).orElse(null));
    }

    private void renderChart() {
        var configuration = chart.getConfiguration();
        configuration.setSeries(new ArrayList<>());
        configuration.setTitle(T.tr("statistics.teslaReliability.title"));
        configuration.setSubTitle(T.tr("statistics.teslaReliability.subtitle"));
        configuration.getChart().setType(ChartType.COLUMN);
        configuration.getChart().setStyledMode(true);
        configuration.getLegend().setEnabled(true);

        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.CATEGORY);
        String[] years = new String[MAX_YEAR - MIN_YEAR + 1];
        for (int i = 0; i < years.length; i++) {
            years[i] = String.valueOf(MIN_YEAR + i);
        }
        xAxis.setCategories(years);
        xAxis.setTitle("");

        YAxis yAxis = configuration.getyAxis();
        yAxis.setMin(0);
        yAxis.setMax(70);
        yAxis.setTickInterval(10);
        yAxis.setTitle("");
        yAxis.getLabels().setFormat("{value} %");

        // One column series per Tesla model.
        for (String model : TESLA_MODELS) {
            DataSeries series = new DataSeries("Tesla " + model);
            for (int year = MIN_YEAR; year <= MAX_YEAR; year++) {
                Optional<InspectionRow> row = findRow(model, year);
                DataSeriesItem item = new DataSeriesItem();
                item.setX((double) (year - MIN_YEAR));
                if (row.isPresent()) {
                    item.setY(row.get().failPct());
                } else {
                    item.setY(null);
                }
                series.add(item);
            }
            configuration.addSeries(series);
        }

        // Dashed all-cars cohort baseline as a separate line series.
        DataSeries baselineSeries = new DataSeries(T.tr("statistics.teslaReliability.baseline"));
        baselineSeries.setPlotOptions(linePlotOptions());
        for (int year = MIN_YEAR; year <= MAX_YEAR; year++) {
            DataSeriesItem item = new DataSeriesItem();
            item.setX((double) (year - MIN_YEAR));
            item.setY(TraficomInspectionService.baselineFailPct(year).orElse(0.0));
            baselineSeries.add(item);
        }
        configuration.addSeries(baselineSeries);

        PlotOptionsColumn columnOptions = new PlotOptionsColumn();
        columnOptions.setAnimation(false);
        configuration.setPlotOptions(columnOptions);

        Tooltip tooltip = new Tooltip();
        tooltip.setShared(false);
        tooltip.setHeaderFormat("<b>{series.name}</b><br/>");
        tooltip.setPointFormat(T.tr("statistics.teslaReliability.tooltip.year")
                + ": {point.category}<br/>"
                + T.tr("statistics.teslaReliability.tooltip.failPct") + ": <b>{point.y:.1f} %</b>");
        configuration.setTooltip(tooltip);

        chart.drawChart(true);

        // Register click listener once (idempotent across re-renders since we
        // call this from onAttach only).
        chart.addPointClickListener(this::onPointClicked);
    }

    private PlotOptionsLine linePlotOptions() {
        PlotOptionsLine line = new PlotOptionsLine();
        line.setDashStyle(DashStyle.LONGDASH);
        line.setMarker(new Marker(false));
        line.setAnimation(false);
        return line;
    }

    private void onPointClicked(PointClickEvent event) {
        String seriesName = event.getSeries().getName();
        if (seriesName == null || !seriesName.startsWith("Tesla ")) {
            return; // baseline series — ignore clicks
        }
        String model = seriesName.substring("Tesla ".length());
        int year = MIN_YEAR + event.getItemIndex();
        findRow(model, year).ifPresent(this::selectPoint);
    }

    private Optional<InspectionRow> findRow(String model, int year) {
        return teslaRows.stream()
                .filter(r -> model.equals(r.model()) && r.cohortYear() != null && r.cohortYear() == year)
                .findFirst();
    }

    private void selectPoint(InspectionRow row) {
        panel.removeAll();
        if (row == null) {
            panel.add(new Span(T.tr("statistics.teslaReliability.empty")));
            return;
        }

        H3 header = new H3(T.tr(
                "statistics.teslaReliability.selected.label",
                "Tesla " + row.model(),
                String.valueOf(row.cohortYear())
        ));
        header.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.LARGE);
        panel.add(header);

        NumberFormat fmt = NumberFormat.getIntegerInstance(Locale.ENGLISH);
        Map<String, String> stats = new LinkedHashMap<>();
        stats.put(T.tr("statistics.teslaReliability.selected.failPct"),
                String.format(Locale.ROOT, "%.1f %%", row.failPct()));
        stats.put(T.tr("statistics.teslaReliability.selected.inspections"),
                fmt.format(row.inspections()));
        stats.put(T.tr("statistics.teslaReliability.selected.avgKm"),
                fmt.format(row.avgKm()) + " km");

        HorizontalLayout statsRow = new HorizontalLayout();
        statsRow.setSpacing(true);
        statsRow.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.FlexWrap.WRAP);
        for (Map.Entry<String, String> e : stats.entrySet()) {
            Div stat = new Div();
            Span label = new Span(e.getKey());
            label.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            Span value = new Span(e.getValue());
            value.addClassNames(LumoUtility.FontWeight.MEDIUM);
            stat.add(label, new Div(value));
            stat.addClassNames(LumoUtility.Padding.Right.LARGE);
            statsRow.add(stat);
        }
        panel.add(statsRow);

        if (!row.topDefects().isEmpty()) {
            H3 defectsHeader = new H3(T.tr("statistics.teslaReliability.selected.defects"));
            defectsHeader.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.Margin.Bottom.SMALL,
                    LumoUtility.FontSize.MEDIUM);
            panel.add(defectsHeader);
            for (int i = 0; i < row.topDefects().size(); i++) {
                String fi = row.topDefects().get(i);
                String key = TraficomInspectionService.defectKey(fi);
                String en = key != null ? T.tr(key) : fi;
                Div line = new Div();
                Span rank = new Span((i + 1) + ".");
                rank.addClassNames(LumoUtility.FontWeight.MEDIUM, LumoUtility.Padding.Right.SMALL);
                Span englishText = new Span(en);
                Span fiText = new Span(" — " + fi);
                fiText.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
                line.add(rank, englishText, fiText);
                panel.add(line);
            }
        }
    }
}
