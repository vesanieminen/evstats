package com.vesanieminen.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.shared.Registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Tag("dual-range-slider")
@JsModule("./components/dual-range-slider.ts")
public class DualRangeSlider extends Component implements HasValue<HasValue.ValueChangeEvent<DualRangeSlider.DualRangeValue>, DualRangeSlider.DualRangeValue>, HasSize {

    private final List<ValueChangeListener<? super ValueChangeEvent<DualRangeValue>>> listeners = new ArrayList<>();
    private DualRangeValue oldValue;
    private DomListenerRegistration lowValueListener;
    private DomListenerRegistration highValueListener;

    public record DualRangeValue(double lowValue, double highValue) {
    }

    public DualRangeSlider() {
        this(0, 100, 20, 80);
    }

    public DualRangeSlider(double min, double max, double lowValue, double highValue) {
        setMin(min);
        setMax(max);
        setLowValue(lowValue);
        setHighValue(highValue);
        this.oldValue = new DualRangeValue(lowValue, highValue);

        // Listen for changes from the client
        lowValueListener = getElement().addEventListener("low-value-changed", event -> {
            double newLowValue = event.getEventData().getNumber("event.detail.value");
            // Update server-side property to stay in sync
            getElement().setProperty("lowValue", newLowValue);
            DualRangeValue newValue = new DualRangeValue(newLowValue, getHighValue());
            fireValueChangeEvent(newValue, true);
        }).addEventData("event.detail.value");

        highValueListener = getElement().addEventListener("high-value-changed", event -> {
            double newHighValue = event.getEventData().getNumber("event.detail.value");
            // Update server-side property to stay in sync
            getElement().setProperty("highValue", newHighValue);
            DualRangeValue newValue = new DualRangeValue(getLowValue(), newHighValue);
            fireValueChangeEvent(newValue, true);
        }).addEventData("event.detail.value");
    }

    public void setMin(double min) {
        getElement().setProperty("min", min);
    }

    public double getMin() {
        return getElement().getProperty("min", 0.0);
    }

    public void setMax(double max) {
        getElement().setProperty("max", max);
    }

    public double getMax() {
        return getElement().getProperty("max", 100.0);
    }

    public void setLowValue(double lowValue) {
        getElement().setProperty("lowValue", lowValue);
    }

    public double getLowValue() {
        return getElement().getProperty("lowValue", 0.0);
    }

    public void setHighValue(double highValue) {
        getElement().setProperty("highValue", highValue);
    }

    public double getHighValue() {
        return getElement().getProperty("highValue", 100.0);
    }

    public void setStep(double step) {
        getElement().setProperty("step", step);
    }

    public double getStep() {
        return getElement().getProperty("step", 1.0);
    }

    @Override
    public void setValue(DualRangeValue value) {
        Objects.requireNonNull(value, "Value cannot be null");
        DualRangeValue oldVal = getValue();
        setLowValue(value.lowValue());
        setHighValue(value.highValue());
        if (!Objects.equals(oldVal, value)) {
            fireValueChangeEvent(value, false);
        }
    }

    @Override
    public DualRangeValue getValue() {
        return new DualRangeValue(getLowValue(), getHighValue());
    }

    private void fireValueChangeEvent(DualRangeValue newValue, boolean fromClient) {
        if (!Objects.equals(oldValue, newValue)) {
            DualRangeValue previousOldValue = oldValue;
            oldValue = newValue;
            ValueChangeEvent<DualRangeValue> event = new ValueChangeEvent<>() {
                @Override
                public HasValue<?, DualRangeValue> getHasValue() {
                    return DualRangeSlider.this;
                }

                @Override
                public boolean isFromClient() {
                    return fromClient;
                }

                @Override
                public DualRangeValue getOldValue() {
                    return previousOldValue;
                }

                @Override
                public DualRangeValue getValue() {
                    return newValue;
                }
            };
            listeners.forEach(listener -> listener.valueChanged(event));
        }
    }

    @Override
    public Registration addValueChangeListener(ValueChangeListener<? super ValueChangeEvent<DualRangeValue>> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        getElement().setProperty("readonly", readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return getElement().getProperty("readonly", false);
    }

    @Override
    public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
        // Not applicable for this component
    }

    @Override
    public boolean isRequiredIndicatorVisible() {
        return false;
    }
}
