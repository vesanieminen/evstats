package com.vesanieminen.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class Card extends Div {

    private final Div header;
    private final Div content;

    public Card() {
        addClassName("charging-card");

        header = new Div();
        header.addClassNames(
                "charging-card-header",
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.SMALL,
                LumoUtility.Margin.Bottom.SMALL,
                LumoUtility.FontSize.SMALL
        );
        header.setVisible(false);

        content = new Div();
        content.addClassName("charging-card-content");

        super.add(header, content);
    }

    public Card(String title) {
        this();
        setTitle(title);
    }

    public Card(Component icon, String title) {
        this();
        setIcon(icon);
        setTitle(title);
    }

    public void setTitle(String title) {
        if (title != null && !title.isEmpty()) {
            Span titleSpan = new Span(title);
            titleSpan.addClassNames(LumoUtility.TextColor.SECONDARY);

            // Remove existing title if present
            header.getChildren()
                    .filter(c -> c instanceof Span)
                    .forEach(header::remove);

            header.add(titleSpan);
            header.setVisible(true);
        }
    }

    public void setIcon(Component icon) {
        if (icon != null) {
            icon.getElement().getClassList().add(LumoUtility.TextColor.PRIMARY);

            // Add icon at the beginning
            header.getElement().insertChild(0, icon.getElement());
            header.setVisible(true);
        }
    }

    @Override
    public void add(Component... components) {
        content.add(components);
    }

    @Override
    public void remove(Component... components) {
        content.remove(components);
    }

    @Override
    public void removeAll() {
        content.removeAll();
    }

    public Div getContent() {
        return content;
    }
}
