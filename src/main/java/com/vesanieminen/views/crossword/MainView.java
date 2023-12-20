package com.vesanieminen.views.crossword;

import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.UIScope;

import java.util.UUID;

@org.springframework.stereotype.Component
@UIScope
public class MainView extends AppLayout {
    private Nav menu;
    private H1 viewTitle;
    private UserInfo localUser;
    private Span userLabel;

    public MainView() {
        localUser = new UserInfo(UUID.randomUUID().toString(),
                "Anonymous User");

        setPrimarySection(Section.DRAWER);
        addToNavbar(true, createHeaderContent());
        addToDrawer(createSideMenu());
    }

    private Component createHeaderContent() {
        viewTitle = new H1();
        viewTitle.addClassNames("m-0", "text-l");

        Header layout = new Header();
        layout.addClassNames("bg-base", "border-b", "border-contrast-10",
                "box-border", "flex", "h-xl", "items-center", "w-full");
        return layout;
    }

    private Component createSideMenu() {
        H2 header = new H2("Crossword Demo");
        header.addClassNames("flex", "items-center", "h-xl", "m-0", "px-m",
                "text-m");

        menu = createMenuLinks();
        Footer footer = createMenuFooter();

        com.vaadin.flow.component.html.Section section =
                new com.vaadin.flow.component.html.Section(header, menu,
                        footer);
        section.addClassNames("bg-base", "flex", "flex-col", "items-stretch",
                "min-h-full");
        return section;
    }

    private Nav createMenuLinks() {
        Nav menu = new Nav();
        menu.addClassName("mb-l");

        menu.add(createLink(VaadinIcon.GRID, "Crossword", CrosswordView.class));
        return menu;
    }

    private RouterLink createLink(VaadinIcon vaadinIcon, String text,
                                  Class<? extends Component> view) {
        RouterLink link = new RouterLink("", view);

        Icon icon = vaadinIcon.create();
        icon.addClassNames("box-border", "icon-s", "me-s");

        Span span = new Span(text);
        span.addClassNames("text-s", "font-medium");

        link.add(icon, span);
        link.addClassNames("flex", "h-m", "items-center", "mx-s", "px-s",
                "relative", "text-secondary");
        return link;
    }

    private Footer createMenuFooter() {
        userLabel = new Span(localUser.getName());
        userLabel.addClassNames("font-medium", "text-s", "text-secondary");

        Footer footer = new Footer(new Icon(VaadinIcon.USER), userLabel);
        footer.addClassNames("flex", "items-center", "mb-m", "mt-auto", "px-m",
                "pointer");
        footer.addClickListener(event -> footer.getUI().ifPresent(ui -> ui.navigate(ProfileView.class)));
        return footer;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        String title = getContent().getClass().getAnnotation(PageTitle.class)
                .value();
        viewTitle.setText(title);
    }

    public void setUserName(String userName) {
        localUser.setName(userName);
        userLabel.setText(userName);
    }

    public UserInfo getLocalUser() {
        return localUser;
    }
}
