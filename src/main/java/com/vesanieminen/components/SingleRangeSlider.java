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

@Tag("single-range-slider")
@JsModule("./components/single-range-slider.ts")
public class SingleRangeSlider extends Component implements HasValue<HasValue.ValueChangeEvent<Integer>, Integer>, HasSize {

    private final List<ValueChangeListener<? super ValueChangeEvent<Integer>>> listeners = new ArrayList<>();
    private Integer oldValue;
    private DomListenerRegistration valueListener;

    public SingleRangeSlider() {
        this(1, 32, 16);
    }

    public SingleRangeSlider(int min, int max, int value) {
        setMin(min);
        setMax(max);
        setValue(value);
        this.oldValue = value;

        // Listen for changes from the client
        valueListener = getElement().addEventListener("value-changed", event -> {
            int newValue = (int) event.getEventData().getNumber("event.detail.value");
            // Update server-side property to stay in sync
            getElement().setProperty("value", newValue);
            fireValueChangeEvent(newValue, true);
        }).addEventData("event.detail.value");
    }

    public void setMin(int min) {
        getElement().setProperty("min", min);
    }

    public int getMin() {
        return getElement().getProperty("min", 1);
    }

    public void setMax(int max) {
        getElement().setProperty("max", max);
    }

    public int getMax() {
        return getElement().getProperty("max", 32);
    }

    public void setStep(int step) {
        getElement().setProperty("step", step);
    }

    public int getStep() {
        return getElement().getProperty("step", 1);
    }

    public void setLabel(String label) {
        getElement().setProperty("label", label);
    }

    public String getLabel() {
        return getElement().getProperty("label", "");
    }

    public void setUnit(String unit) {
        getElement().setProperty("unit", unit);
    }

    public String getUnit() {
        return getElement().getProperty("unit", "");
    }

    @Override
    public void setValue(Integer value) {
        Objects.requireNonNull(value, "Value cannot be null");
        Integer oldVal = getValue();
        getElement().setProperty("value", value);
        if (!Objects.equals(oldVal, value)) {
            fireValueChangeEvent(value, false);
        }
    }

    @Override
    public Integer getValue() {
        return getElement().getProperty("value", 16);
    }

    private void fireValueChangeEvent(Integer newValue, boolean fromClient) {
        if (!Objects.equals(oldValue, newValue)) {
            Integer previousOldValue = oldValue;
            oldValue = newValue;
            ValueChangeEvent<Integer> event = new ValueChangeEvent<>() {
                @Override
                public HasValue<?, Integer> getHasValue() {
                    return SingleRangeSlider.this;
                }

                @Override
                public boolean isFromClient() {
                    return fromClient;
                }

                @Override
                public Integer getOldValue() {
                    return previousOldValue;
                }

                @Override
                public Integer getValue() {
                    return newValue;
                }
            };
            listeners.forEach(listener -> listener.valueChanged(event));
        }
    }

    @Override
    public Registration addValueChangeListener(ValueChangeListener<? super ValueChangeEvent<Integer>> listener) {
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
