package com.vesanieminen.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class GridLayout extends Div {

    public GridLayout() {
        addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Grid.FLOW_COLUMN,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.Display.Breakpoint.Medium.GRID
        );
    }
}
