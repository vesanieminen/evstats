package com.vesanieminen.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.views.evstatistics.EVAdoptionRateView;
import com.vesanieminen.views.evstatistics.EVRegistrationsView;
import com.vesanieminen.views.evstatistics.TeslaRegistrationsBarView;
import com.vesanieminen.views.evstatistics.TeslaRegistrationsView;
import org.vaadin.lineawesome.LineAwesomeIcon;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainLayout extends AppLayout {

    private H2 viewTitle;

    public boolean darkMode = false;
    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    protected void onAttach(AttachEvent attachEvent) {
        Button theme = new Button(VaadinIcon.MOON_O.create());
        theme.getElement().setAttribute("aria-label", getTranslation("Switch to dark mode"));
        theme.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        theme.addClickListener(e -> {
            if (this.darkMode) {
                attachEvent.getUI().getPage().executeJs("document.documentElement.setAttribute('theme', '');");
                theme.setIcon(VaadinIcon.MOON_O.create());
                theme.getElement().setAttribute("aria-label", getTranslation("Switch to dark mode"));
            } else {
                attachEvent.getUI().getPage().executeJs("document.documentElement.setAttribute('theme', 'dark');");
                theme.setIcon(VaadinIcon.SUN_O.create());
                theme.getElement().setAttribute("aria-label", getTranslation("Switch to light mode"));
            }
            this.darkMode = !this.darkMode;
        });
        final var span = new Span();
        span.addClassNames(LumoUtility.Flex.GROW);
        addToNavbar(span, theme);
    }

        private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.getElement().setAttribute("aria-label", "Menu toggle");

        viewTitle = new H2();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(false, toggle, viewTitle);
    }

    private void addDrawerContent() {
        H1 appName = new H1("Finnish EV stats");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        final var header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        // SideNav is a production-ready official component under a feature flag.
        // However, it has accessibility issues and is missing some features.
        // Both will be addressed in an upcoming minor version.
        // These changes are likely to cause some breaking change to the custom css
        // applied to the component.
        SideNav nav = new SideNav();

        nav.addItem(
                new SideNavItem("Adoption curve", EVAdoptionRateView.class, LineAwesomeIcon.CHART_AREA_SOLID.create()),
                new SideNavItem("New cars", EVRegistrationsView.class, LineAwesomeIcon.CHART_BAR_SOLID.create()),
                new SideNavItem("Tesla registrations", TeslaRegistrationsView.class, LineAwesomeIcon.CAR_BATTERY_SOLID.create()),
                new SideNavItem("New Teslas / year", TeslaRegistrationsBarView.class, LineAwesomeIcon.CAR_BATTERY_SOLID.create())
        );

        return nav;
    }

    private Footer createFooter() {
        Footer layout = new Footer();
        layout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);

        final var sourceSpan = new Span("Source: ");
        final var link = new Anchor("https://www.aut.fi/tilastot/ensirekisteroinnit/ensirekisteroinnit_kayttovoimittain/henkiloautojen_kayttovoimatilastot", "aut.fi");
        final var footer = new Div(sourceSpan, link);
        footer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL, LumoUtility.Margin.Bottom.XSMALL, LumoUtility.Gap.XSMALL);
        layout.add(footer);

        // GitHub link
        final var githubIcon = new Image("icons/GitHub-Mark-32px.png", "GitHub icon");
        githubIcon.addClassNames("footer-icon");
        final var githubLink = new Anchor("https://github.com/vesanieminen/evstats", "Code on Github");
        githubLink.addClassNames(LumoUtility.Display.FLEX);
        githubLink.add(githubIcon);
        githubIcon.addClassNames(LumoUtility.IconSize.MEDIUM, LumoUtility.TextColor.PRIMARY, LumoUtility.Margin.Left.SMALL);
        layout.add(githubLink);

        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
}
