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
    public static final String GITHUB_SVG = "<svg class=\"icon-s\" viewBox=\"0 0 98 96\" xmlns=\"http://www.w3.org/2000/svg\"><path fill-rule=\"evenodd\" clip-rule=\"evenodd\" d=\"M48.854 0C21.839 0 0 22 0 49.217c0 21.756 13.993 40.172 33.405 46.69 2.427.49 3.316-1.059 3.316-2.362 0-1.141-.08-5.052-.08-9.127-13.59 2.934-16.42-5.867-16.42-5.867-2.184-5.704-5.42-7.17-5.42-7.17-4.448-3.015.324-3.015.324-3.015 4.934.326 7.523 5.052 7.523 5.052 4.367 7.496 11.404 5.378 14.235 4.074.404-3.178 1.699-5.378 3.074-6.6-10.839-1.141-22.243-5.378-22.243-24.283 0-5.378 1.94-9.778 5.014-13.2-.485-1.222-2.184-6.275.486-13.038 0 0 4.125-1.304 13.426 5.052a46.97 46.97 0 0 1 12.214-1.63c4.125 0 8.33.571 12.213 1.63 9.302-6.356 13.427-5.052 13.427-5.052 2.67 6.763.97 11.816.485 13.038 3.155 3.422 5.015 7.822 5.015 13.2 0 18.905-11.404 23.06-22.324 24.283 1.78 1.548 3.316 4.481 3.316 9.126 0 6.6-.08 11.897-.08 13.526 0 1.304.89 2.853 3.316 2.364 19.412-6.52 33.405-24.935 33.405-46.691C97.707 22 75.788 0 48.854 0z\" fill=\"var(--lumo-primary-text-color)\"/></svg>";


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
        final var githubIcon = new Span();
        githubIcon.addClassNames(LumoUtility.Display.FLEX);
        githubIcon.getElement().setProperty("innerHTML", GITHUB_SVG);
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
