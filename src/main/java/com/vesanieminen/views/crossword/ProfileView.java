package com.vesanieminen.views.crossword;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "profile", layout = MainView.class)
@PageTitle("Profile")
public class ProfileView extends VerticalLayout
        implements BeforeEnterObserver {
    MainView mainView;
    TextField name;

    public ProfileView(MainView mainView) {
        this.mainView = mainView;
        addClassNames("m-xl", "flex", "flex-col");

        Div wrapper = new Div();
        wrapper.addClassNames("flex", "flex-col", "max-w-40em", "items-start");

        name = new TextField("Your name", e -> {
            if (e.isFromClient()) {
                mainView.setUserName(e.getValue());
            }
        });
        name.setValueChangeMode(ValueChangeMode.LAZY);
        name.focus();

        Button startButton = new Button("Start Grid",
                e -> UI.getCurrent().navigate(CrosswordView.class));
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        wrapper.add(name, startButton);
        add(wrapper);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (mainView != null) {
            name.setValue(mainView.getLocalUser().getName());
        }
    }
}
