package com.vesanieminen.views.statistics;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DataLabels;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.PlotOptionsBar;
import com.vaadin.flow.component.charts.model.Stacking;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vesanieminen.components.ChartExport;
import com.vesanieminen.i18n.T;
import com.vesanieminen.services.TraficomInspectionService;
import com.vesanieminen.services.TraficomInspectionService.DefectTheme;
import com.vesanieminen.services.TraficomInspectionService.MakeModel;
import com.vesanieminen.services.TraficomInspectionService.ThemeBreakdown;
import com.vesanieminen.views.MainLayout;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Route(value = "defect-breakdown", layout = MainLayout.class)
public class EVDefectBreakdownView extends Main implements HasDynamicTitle {

    private final MultiSelectComboBox<MakeModel> picker = new MultiSelectComboBox<>();
    private final Chart stackedChart = new Chart();
    private final Chart groupedChart = new Chart();
    private final VerticalLayout root = new VerticalLayout();

    @Override
    public String getPageTitle() {
        return T.tr("statistics.defectBreakdown.title");
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
        root.setPadding(true);
        root.setSpacing(false);

        picker.setLabel(T.tr("statistics.defectBreakdown.selector.label"));
        picker.setHelperText(T.tr("statistics.defectBreakdown.selector.helper"));
        picker.setItems(TraficomInspectionService.distinctMakeModels());
        picker.setItemLabelGenerator(mm -> mm.make() + " " + mm.model());
        picker.setClearButtonVisible(true);
        picker.setWidthFull();
        // Default selection: the curated BEV allow-list, so the page opens
        // showing the same comparison the previous version did.
        picker.setValue(TraficomInspectionService.bevAllowList());
        picker.addValueChangeListener(e -> {
            updateStacked();
            updateGrouped();
        });
        root.add(picker);

        stackedChart.setHeight("260px");
        stackedChart.getStyle().set("width", "100%");
        ChartExport.configure(stackedChart, "ev-defect-breakdown");
        root.add(stackedChart);

        groupedChart.setSizeFull();
        groupedChart.getStyle().set("min-height", "360px");
        ChartExport.configure(groupedChart, "ev-defect-breakdown-grouped");
        root.add(groupedChart);
        root.expand(groupedChart);

        add(root);
        updateStacked();
        updateGrouped();
    }

    private Set<MakeModel> selection() {
        Set<MakeModel> v = picker.getValue();
        return v == null ? new LinkedHashSet<>() : v;
    }

    private String selectedLabel() {
        int n = selection().size();
        return T.tr("statistics.defectBreakdown.segment.selected", n);
    }

    private void updateStacked() {
        List<ThemeBreakdown> all = TraficomInspectionService.breakdownAll();
        List<ThemeBreakdown> sel = TraficomInspectionService.breakdown(selection());

        var configuration = stackedChart.getConfiguration();
        configuration.setSeries(new ArrayList<>());
        configuration.setTitle(T.tr("statistics.defectBreakdown.title"));
        configuration.setSubTitle(T.tr("statistics.defectBreakdown.subtitle"));
        configuration.getChart().setType(ChartType.BAR);
        configuration.getChart().setStyledMode(true);
        configuration.getLegend().setEnabled(true);

        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.CATEGORY);
        xAxis.setCategories(
                T.tr("statistics.defectBreakdown.segment.all"),
                selectedLabel()
        );
        xAxis.setTitle("");

        YAxis yAxis = configuration.getyAxis();
        yAxis.setMin(0);
        yAxis.setMax(100);
        yAxis.setTitle("");
        yAxis.getLabels().setFormat("{value} %");

        PlotOptionsBar plot = new PlotOptionsBar();
        plot.setStacking(Stacking.PERCENT);
        plot.setAnimation(false);
        DataLabels labels = new DataLabels();
        labels.setEnabled(false);
        plot.setDataLabels(labels);
        configuration.setPlotOptions(plot);

        for (DefectTheme theme : DefectTheme.values()) {
            DataSeries series = new DataSeries(themeLabel(theme));
            series.add(new DataSeriesItem(
                    T.tr("statistics.defectBreakdown.segment.all"),
                    findShare(all, theme) * 100.0));
            series.add(new DataSeriesItem(
                    selectedLabel(),
                    findShare(sel, theme) * 100.0));
            configuration.addSeries(series);
        }

        Tooltip tooltip = new Tooltip();
        tooltip.setShared(false);
        tooltip.setHeaderFormat("<b>{point.key}</b><br/>");
        tooltip.setPointFormat("{series.name}: <b>{point.y:.1f} %</b>");
        configuration.setTooltip(tooltip);

        stackedChart.drawChart(true);
    }

    private void updateGrouped() {
        List<ThemeBreakdown> all = TraficomInspectionService.breakdownAll();
        List<ThemeBreakdown> sel = TraficomInspectionService.breakdown(selection());

        var configuration = groupedChart.getConfiguration();
        configuration.setSeries(new ArrayList<>());
        configuration.setTitle("");
        configuration.setSubTitle("");
        configuration.getChart().setType(ChartType.BAR);
        configuration.getChart().setStyledMode(true);
        configuration.getLegend().setEnabled(true);

        String[] categories = new String[DefectTheme.values().length];
        for (int i = 0; i < DefectTheme.values().length; i++) {
            categories[i] = themeLabel(DefectTheme.values()[i]);
        }
        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.CATEGORY);
        xAxis.setCategories(categories);
        xAxis.setTitle("");

        YAxis yAxis = configuration.getyAxis();
        yAxis.setMin(0);
        yAxis.setTitle("");
        yAxis.getLabels().setFormat("{value} %");

        PlotOptionsBar plot = new PlotOptionsBar();
        plot.setAnimation(false);
        configuration.setPlotOptions(plot);

        DataSeries allSeries = new DataSeries(T.tr("statistics.defectBreakdown.segment.all"));
        DataSeries selSeries = new DataSeries(selectedLabel());
        for (DefectTheme theme : DefectTheme.values()) {
            allSeries.add(new DataSeriesItem(themeLabel(theme), findShare(all, theme) * 100.0));
            selSeries.add(new DataSeriesItem(themeLabel(theme), findShare(sel, theme) * 100.0));
        }
        configuration.addSeries(allSeries);
        configuration.addSeries(selSeries);

        Tooltip tooltip = new Tooltip();
        tooltip.setShared(true);
        tooltip.setPointFormat("{series.name}: <b>{point.y:.1f} %</b><br/>");
        configuration.setTooltip(tooltip);

        groupedChart.drawChart(true);
    }

    private static double findShare(List<ThemeBreakdown> rows, DefectTheme theme) {
        for (ThemeBreakdown r : rows) {
            if (r.theme() == theme) {
                return r.share();
            }
        }
        return 0.0;
    }

    private static String themeLabel(DefectTheme theme) {
        return T.tr("statistics.defectBreakdown.theme." + theme.name().toLowerCase());
    }
}
