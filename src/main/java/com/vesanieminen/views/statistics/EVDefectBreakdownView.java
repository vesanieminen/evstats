package com.vesanieminen.views.statistics;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
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

    private static final List<MakeModel> DEFAULT_MODELS = List.of(
            new MakeModel("Tesla", "MODEL 3"),
            new MakeModel("Polestar", "2"),
            new MakeModel("Volkswagen", "ID.4")
    );

    private final ComboBox<MakeModel> picker = new ComboBox<>();
    private final HorizontalLayout chipsRow = new HorizontalLayout();
    private final Chart stackedChart = new Chart();
    private final Chart groupedChart = new Chart();
    private final VerticalLayout root = new VerticalLayout();

    private final LinkedHashSet<MakeModel> selected = new LinkedHashSet<>();

    @Override
    public String getPageTitle() {
        return T.tr("statistics.defectBreakdown.title");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // Don't clamp Main to viewport height — let it grow with content so the
        // AppLayout content slot's overflow:auto handles the scroll naturally.
        // Single body-level scroll is the right UX here, especially on iOS.
        setWidthFull();

        removeAll();
        root.removeAll();
        root.setWidthFull();
        root.setPadding(true);
        root.setSpacing(false);

        picker.setLabel(T.tr("statistics.defectBreakdown.selector.label"));
        picker.setHelperText(T.tr("statistics.defectBreakdown.selector.helper"));
        picker.setItems(TraficomInspectionService.distinctMakeModels());
        picker.setItemLabelGenerator(mm -> mm.make() + " " + mm.model());
        picker.setPlaceholder(T.tr("statistics.defectBreakdown.selector.placeholder"));
        picker.setClearButtonVisible(false);
        picker.setWidthFull();
        picker.addValueChangeListener(e -> {
            MakeModel mm = e.getValue();
            if (mm != null && selected.add(mm)) {
                renderChips();
                refreshCharts();
            }
            picker.clear();
        });
        root.add(picker);

        chipsRow.setWidthFull();
        chipsRow.addClassNames(LumoUtility.FlexWrap.WRAP, LumoUtility.Margin.Top.SMALL,
                LumoUtility.Margin.Bottom.MEDIUM);
        chipsRow.setSpacing(true);
        root.add(chipsRow);

        stackedChart.setWidthFull();
        ChartExport.configure(stackedChart, "ev-defect-breakdown");
        root.add(stackedChart);

        // Fixed height per theme row so the grouped chart is always readable on
        // mobile (no fighting with the stacked chart for leftover space).
        groupedChart.setWidthFull();
        groupedChart.setHeight((100 + DefectTheme.values().length * 50) + "px");
        ChartExport.configure(groupedChart, "ev-defect-breakdown-grouped");
        root.add(groupedChart);

        add(root);

        // Seed with a few contrasting BEVs so the page is informative on first load.
        if (selected.isEmpty()) {
            selected.addAll(DEFAULT_MODELS);
        }
        renderChips();
        refreshCharts();
    }

    private void renderChips() {
        chipsRow.removeAll();
        for (MakeModel mm : selected) {
            Button chip = new Button(mm.make() + " " + mm.model(), VaadinIcon.CLOSE_SMALL.create());
            chip.setIconAfterText(true);
            chip.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            chip.addClassNames(LumoUtility.Background.CONTRAST_10, LumoUtility.BorderRadius.LARGE);
            chip.addClickListener(e -> {
                selected.remove(mm);
                renderChips();
                refreshCharts();
            });
            chipsRow.add(chip);
        }
    }

    private void refreshCharts() {
        updateStacked();
        updateGrouped();
    }

    private void updateStacked() {
        List<ThemeBreakdown> all = TraficomInspectionService.breakdownAll();

        // Scale chart height with the number of bars so labels + legend + bars
        // never overlap. ~60 px per category, plus ~160 px for title + legend.
        int categoryCount = 1 + selected.size();
        int height = Math.max(260, 160 + categoryCount * 60);
        stackedChart.getStyle().set("height", height + "px");

        var configuration = stackedChart.getConfiguration();
        configuration.setSeries(new ArrayList<>());
        configuration.setTitle(T.tr("statistics.defectBreakdown.title"));
        configuration.setSubTitle(T.tr("statistics.defectBreakdown.subtitle"));
        configuration.getChart().setType(ChartType.BAR);
        configuration.getChart().setStyledMode(true);
        configuration.getLegend().setEnabled(true);

        // Categories: All cars + each selected model.
        String allLabel = T.tr("statistics.defectBreakdown.segment.all");
        List<String> categories = new ArrayList<>();
        categories.add(allLabel);
        List<List<ThemeBreakdown>> perModel = new ArrayList<>();
        for (MakeModel mm : selected) {
            categories.add(modelLabel(mm));
            perModel.add(TraficomInspectionService.breakdown(Set.of(mm)));
        }
        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.CATEGORY);
        xAxis.setCategories(categories.toArray(new String[0]));
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
            series.add(new DataSeriesItem(allLabel, findShare(all, theme) * 100.0));
            int idx = 0;
            for (MakeModel mm : selected) {
                series.add(new DataSeriesItem(modelLabel(mm), findShare(perModel.get(idx), theme) * 100.0));
                idx++;
            }
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
        for (DefectTheme theme : DefectTheme.values()) {
            allSeries.add(new DataSeriesItem(themeLabel(theme), findShare(all, theme) * 100.0));
        }
        configuration.addSeries(allSeries);

        for (MakeModel mm : selected) {
            List<ThemeBreakdown> b = TraficomInspectionService.breakdown(Set.of(mm));
            DataSeries series = new DataSeries(modelLabel(mm));
            for (DefectTheme theme : DefectTheme.values()) {
                series.add(new DataSeriesItem(themeLabel(theme), findShare(b, theme) * 100.0));
            }
            configuration.addSeries(series);
        }

        Tooltip tooltip = new Tooltip();
        tooltip.setShared(true);
        tooltip.setPointFormat("{series.name}: <b>{point.y:.1f} %</b><br/>");
        configuration.setTooltip(tooltip);

        groupedChart.drawChart(true);
    }

    private static String modelLabel(MakeModel mm) {
        return mm.make() + " " + mm.model();
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
