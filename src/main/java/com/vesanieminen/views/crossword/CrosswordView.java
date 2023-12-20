package com.vesanieminen.views.crossword;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationMap;
import com.vaadin.collaborationengine.PresenceManager;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vesanieminen.views.crossword.data.HighlightPosition;
import org.vaadin.addons.crossword.Crossword;
import org.vaadin.addons.crossword.Puzzle;

import java.io.InputStream;
import java.net.URL;

@Route(value = "Crossword", layout = MainView.class)
@PageTitle("Crossword")
public class CrosswordView extends VerticalLayout {
    private static final String TOPIC_ID = "crossword-demo";

    private CollaborationMap answerGrid;
    private CollaborationMap users;

    public CrosswordView(MainView mainView) {
        UserInfo localUser = mainView.getLocalUser();

        Crossword crossword = new Crossword(localUser.getId());

        try {
            URL jsonURL = CrosswordView.class.getResource(
                    "/dictionary-com-03-10-2022.json");
            InputStream jsonIn = jsonURL.openStream();
            Puzzle puzzle = new ObjectMapper()
                    .readerFor(Puzzle.class)
                    .readValue(jsonIn);
            crossword.setPuzzle(puzzle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        HorizontalLayout menuBar = new HorizontalLayout();

        CollaborationAvatarGroup avatarGroup =
                new CollaborationAvatarGroup(localUser, TOPIC_ID);
        menuBar.add(avatarGroup);

        Checkbox skipFilledCheck = new Checkbox("Skip Filled",
                event -> crossword.setSkipFilled(event.getValue()));
        skipFilledCheck.setValue(crossword.getSkipFilled());
        menuBar.add(skipFilledCheck);

        Checkbox showMistakesCheck = new Checkbox("Show Mistakes",
                event -> crossword.setShowMistakes(event.getValue()));
        showMistakesCheck.setValue(crossword.getShowMistakes());
        menuBar.add(showMistakesCheck);

        Button themeToggleButton = new Button(
                new Icon(VaadinIcon.ADJUST),
                event -> {
                    ThemeList themeList = UI.getCurrent().getElement()
                            .getThemeList();
                    if (themeList.contains(Lumo.DARK)) {
                        themeList.remove(Lumo.DARK);
                    } else {
                        themeList.add(Lumo.DARK);
                    }
                });
        menuBar.add(themeToggleButton);

        add(menuBar);
        add(new HorizontalLayout(crossword));

        // Handle the puzzle collaboration using Collaboration Engine
        CollaborationEngine ce = CollaborationEngine.getInstance();

        // Add users to the crossword using a PresenceManager
        PresenceManager manager = new PresenceManager(crossword, localUser,
                TOPIC_ID);
        manager.markAsPresent(true);
        manager.setPresenceHandler(event -> {
            UserInfo user = event.getUser();
            int colorIndex = ce.getUserColorIndex(user);
            crossword.addUser(user.getId(), colorIndex);
            return () -> crossword.removeUser(user.getId());
        });

        ce.openTopicConnection(this, TOPIC_ID,
                localUser, connection -> {
                    // Store the guessed answers and user highlight positions in a couple
                    // of CollaborationMaps
                    answerGrid = connection.getNamedMap("crossword-answers");
                    users = connection.getNamedMap("crossword-users");

                    // Listen to the crossword component for updates to the
                    // guessed letters and highlight positions
                    Registration registration = Registration.combine(
                            crossword.addUpdateLetterListener(event ->
                                    answerGrid.put(
                                            String.valueOf(event.getIndex()),
                                            event.getLetter())),
                            crossword.addUpdatePositionListener(event ->
                                    users.put(
                                            event.getId(),
                                            new HighlightPosition(
                                                    event.getInputIndex(),
                                                    event.getStartIndex(),
                                                    event.getEndIndex()))));

                    // Subscribe to the CollaborationMaps for updates to the
                    // guessed letters and highlight positions
                    answerGrid.subscribe(event -> {
                        int index = Integer.parseInt(event.getKey());
                        crossword.setLetter(index,
                                event.getValue(String.class));
                    });
                    users.subscribe(event -> {
                        HighlightPosition position =
                                event.getValue(HighlightPosition.class);
                        crossword.updateUser(event.getKey(),
                                position.inputIndex,
                                position.startIndex,
                                position.endIndex);
                    });
                    return registration;
                });
    }
}
