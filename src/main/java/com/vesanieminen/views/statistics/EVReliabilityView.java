package com.vesanieminen.views.statistics;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DashStyle;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.PlotLine;
import com.vaadin.flow.component.charts.model.PlotOptionsBar;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.components.ChartExport;
import com.vesanieminen.i18n.T;
import com.vesanieminen.services.TraficomInspectionService;
import com.vesanieminen.services.TraficomInspectionService.InspectionRow;
import com.vesanieminen.views.MainLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

@Route(value = "reliability", layout = MainLayout.class)
public class EVReliabilityView extends Main implements HasDynamicTitle {

    private static final String ALL_YEARS = "all";

    private final ComboBox<String> cohortFilter = new ComboBox<>();
    private final IntegerField minCohortField = new IntegerField();
    private final Span emptyState = new Span();
    private final Chart chart = new Chart();
    private final VerticalLayout root = new VerticalLayout();

    @Override
    public String getPageTitle() {
        return T.tr("statistics.reliability.title");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setHeightFull();
        attachEvent.getUI().getPage().retrieveExtendedClientDetails(details -> {
            if (details.isTouchDevice() && details.isIOS()) {
                setHeight("var(--fullscreen-height)");
            }
        });

        removeAll();
        root.removeAll();
        root.setSizeFull();
        root.setPadding(false);
        root.setSpacing(false);

        root.add(buildFilters());
        emptyState.setText(T.tr("statistics.reliability.empty"));
        emptyState.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.TextColor.SECONDARY);
        emptyState.setVisible(false);
        root.add(emptyState);

        chart.setSizeFull();
        chart.getStyle().set("min-height", "400px");
        ChartExport.configure(chart, "ev-reliability");
        root.add(chart);
        root.expand(chart);

        add(root);
        updateChart();
    }

    private HorizontalLayout buildFilters() {
        cohortFilter.setLabel(T.tr("statistics.reliability.cohortYear"));
        List<String> items = new ArrayList<>();
        items.add(ALL_YEARS);
        for (Integer y : TraficomInspectionService.cohortYears()) {
            items.add(String.valueOf(y));
        }
        cohortFilter.setItems(items);
        cohortFilter.setValue(ALL_YEARS);
        cohortFilter.setItemLabelGenerator(v -> ALL_YEARS.equals(v) ? T.tr("statistics.reliability.allYears") : v);
        cohortFilter.setAllowCustomValue(false);
        cohortFilter.setClearButtonVisible(false);
        cohortFilter.addValueChangeListener(e -> updateChart());

        minCohortField.setLabel(T.tr("statistics.reliability.minCohort"));
        minCohortField.setMin(0);
        minCohortField.setStep(50);
        minCohortField.setStepButtonsVisible(true);
        minCohortField.setValue(100);
        minCohortField.addValueChangeListener(e -> updateChart());

        HorizontalLayout filters = new HorizontalLayout(cohortFilter, minCohortField);
        filters.setAlignItems(FlexComponent.Alignment.END);
        filters.setPadding(true);
        filters.setSpacing(true);
        filters.addClassNames(LumoUtility.FlexWrap.WRAP);
        return filters;
    }

    private void updateChart() {
        Integer cohortYear = parseCohort(cohortFilter.getValue());
        int minCohort = minCohortField.getValue() == null ? 1 : Math.max(1, minCohortField.getValue());

        List<InspectionRow> rows = TraficomInspectionService.leagueRows(cohortYear).stream()
                .filter(r -> r.inspections() >= minCohort)
                .toList();

        if (rows.isEmpty()) {
            chart.setVisible(false);
            emptyState.setVisible(true);
            return;
        }

        emptyState.setVisible(false);
        chart.setVisible(true);

        var configuration = chart.getConfiguration();
        configuration.setSeries(new java.util.ArrayList<>());
        configuration.setTitle("");
        OptionalDouble baseline = TraficomInspectionService.baselineFailPct(cohortYear);
        String subtitle = T.tr("statistics.reliability.subtitle");
        if (baseline.isPresent()) {
            subtitle += " — " + T.tr("statistics.reliability.baselineLabel") + ": " + formatPct(baseline.getAsDouble());
        }
        configuration.setSubTitle(subtitle);
        configuration.getChart().setType(ChartType.BAR);
        configuration.getChart().setStyledMode(true);
        configuration.getLegend().setEnabled(false);

        String[] categories = new String[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            InspectionRow r = rows.get(i);
            categories[i] = formatModel(r.make(), r.model());
        }

        XAxis xAxis = configuration.getxAxis();
        xAxis.setCategories(categories);
        xAxis.setType(AxisType.CATEGORY);
        xAxis.setTitle("");

        YAxis yAxis = configuration.getyAxis();
        yAxis.setTitle(T.tr("statistics.reliability.yAxis"));
        yAxis.setMin(0);
        double observedMax = rows.stream().mapToDouble(InspectionRow::failPct).max().orElse(50.0);
        yAxis.setMax(Math.max(50.0, observedMax + 5));
        yAxis.getLabels().setFormat("{value} %");
        yAxis.setPlotLines();

        if (baseline.isPresent()) {
            PlotLine plotLine = new PlotLine();
            plotLine.setValue(baseline.getAsDouble());
            plotLine.setDashStyle(DashStyle.LONGDASH);
            plotLine.setWidth(2);
            plotLine.setZIndex(5);
            yAxis.addPlotLine(plotLine);
        }

        DataSeries series = new DataSeries(T.tr("statistics.reliability.series"));
        for (InspectionRow r : rows) {
            DataSeriesItem item = new DataSeriesItem();
            item.setName(formatModel(r.make(), r.model()));
            item.setY(r.failPct());
            if (baseline.isPresent() && r.failPct() > baseline.getAsDouble()) {
                item.setColorIndex(2);
            } else {
                item.setColorIndex(0);
            }
            series.add(item);
        }
        configuration.addSeries(series);

        PlotOptionsBar plotOptions = new PlotOptionsBar();
        plotOptions.setAnimation(false);
        configuration.setPlotOptions(plotOptions);

        Tooltip tooltip = new Tooltip();
        tooltip.setHeaderFormat("<b>{point.key}</b><br/>");
        tooltip.setPointFormat(T.tr("statistics.reliability.tooltip.failPct") + ": <b>{point.y:.1f} %</b>");
        configuration.setTooltip(tooltip);

        chart.drawChart(true);
    }

    private static Integer parseCohort(String value) {
        if (value == null || ALL_YEARS.equals(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatModel(String make, String model) {
        return make + " " + model;
    }

    private static String formatPct(double v) {
        return String.format(Locale.ROOT, "%.1f %%", v);
    }
}
