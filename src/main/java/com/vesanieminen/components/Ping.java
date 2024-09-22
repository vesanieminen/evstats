package com.vesanieminen.components;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.util.css.Animate;
import com.vesanieminen.util.css.Opacity;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class Ping extends Span {

    private final Span ping;
    private final Span dot;

    @Getter
    @AllArgsConstructor
    public enum Type {
        HIGH(LumoUtility.Background.ERROR, LumoUtility.Background.ERROR_50),
        NORMAL(LumoUtility.Background.PRIMARY, LumoUtility.Background.PRIMARY_50),
        LOW(LumoUtility.Background.SUCCESS, LumoUtility.Background.SUCCESS_50);

        private String bgColorClassname;
        private String bgColor50Classname;

    }

    public Ping(String label) {
        addClassNames(
                LumoUtility.Display.INLINE_FLEX,
                LumoUtility.Position.RELATIVE
        );
        getElement().setAttribute("aria-label", label);
        setTitle(label);
        setHeight(8, Unit.PIXELS);
        setWidth(8, Unit.PIXELS);

        ping = new Span();
        addPingClassNames();
        ping.setSizeFull();

        dot = new Span();
        addDotClassnames();
        dot.setSizeFull();

        add(ping, dot);
    }

    private void addPingClassNames() {
        ping.addClassNames(
                Animate.PING,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Display.FLEX,
                Opacity._75,
                LumoUtility.Position.ABSOLUTE
        );
    }

    private void addDotClassnames() {
        dot.addClassNames(
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Display.FLEX,
                LumoUtility.Position.RELATIVE
        );
    }

    public void setType(Type type) {
        ping.setClassName(type.bgColor50Classname);
        addPingClassNames();
        dot.setClassName(type.bgColorClassname);
        addDotClassnames();
    }

}
