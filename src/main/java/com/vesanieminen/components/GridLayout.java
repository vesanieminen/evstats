package com.vesanieminen.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class GridLayout extends Div {

    public GridLayout() {
        addClassNames(
                LumoUtility.Display.GRID,
                LumoUtility.Grid.Breakpoint.Large.COLUMNS_3,
                LumoUtility.Grid.Column.COLUMNS_2,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Gap.Column.MEDIUM
        );
    }
}
