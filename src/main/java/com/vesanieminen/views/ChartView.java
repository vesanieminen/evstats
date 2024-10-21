package com.vesanieminen.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vesanieminen.services.EuriborRate;
import com.vesanieminen.services.EuriborService;

import java.util.List;
import java.util.stream.Collectors;

@Route("euribor")
public class ChartView extends VerticalLayout {

    public ChartView() {
        setSizeFull();
        add(createChart());
    }

    private Component createChart() {
        // Fetch the data
        EuriborService service = new EuriborService();
        List<EuriborRate> rates = service.getEuribor12MonthRates();

        // Prepare data for the chart
        List<Number> data = rates.stream()
                .map(EuriborRate::getRate)
                .collect(Collectors.toList());

        List<String> categories = rates.stream()
                .map(rate -> rate.getDate().toString())
                .toList();

        // Create the chart
        Chart chart = new Chart(ChartType.LINE);
        Configuration conf = chart.getConfiguration();

        conf.setTitle("Euribor 12-Month Rates");

        XAxis xAxis = new XAxis();
        xAxis.setCategories(categories.toArray(new String[0]));
        conf.addxAxis(xAxis);

        YAxis yAxis = new YAxis();
        yAxis.setTitle("Rate (%)");
        conf.addyAxis(yAxis);

        ListSeries series = new ListSeries("Euribor 12M", data);
        conf.addSeries(series);

        return chart;
    }
}