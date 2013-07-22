/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.helpers.pluginGen;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * JavaFX version of the plugin generator
 * @author Heiko W. Rupp
 */
public class Generator extends Application{

    Props props = new Props();
    private Text errorMessage;
    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        Button createButton = new Button();
        createButton.setText("Create!");
        createButton.setAlignment(Pos.BOTTOM_RIGHT);
        createButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {

                PluginGen pluginGen = new PluginGen();
                pluginGen.postprocess(props);
                try {
                    pluginGen.generate(props);
                    setInfoMessage("Generated!");

                }
                catch (Exception e) {
                    setErrorMessage("Error during generation: " + e.getMessage());
                }
            }
        });

        GridPane pluginLevelPane = new GridPane();
        pluginLevelPane.setPadding(new Insets(10));
        Text pluginLevelDescription = new Text("Plugin level properties");
        pluginLevelDescription.setTextAlignment(TextAlignment.CENTER);
        pluginLevelDescription.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        Text resourceLevelDescription = new Text("ResourceType level properties");
        resourceLevelDescription.setTextAlignment(TextAlignment.CENTER);
        resourceLevelDescription.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        Text descriptionDescription = new Text("Field description");
        descriptionDescription.setTextAlignment(TextAlignment.CENTER);
        descriptionDescription.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        HBox msgBox = getMessagesBox();

        GridPane resourceLevelPane = new GridPane();
        resourceLevelPane.setPadding(new Insets(10));
        Text descriptionText = new Text();
        descriptionText.setFont(Font.font("Arial", FontPosture.ITALIC,12));

        VBox innerBox = new VBox();
        innerBox.setAlignment(Pos.CENTER_LEFT);
        innerBox.setPadding(new Insets(25, 25, 25, 25));
        innerBox.setSpacing(8);

        addFields(pluginLevelPane, true, descriptionText);
        addFields(resourceLevelPane, false, descriptionText);

        ObservableList<Node> children = innerBox.getChildren();
        children.add(pluginLevelDescription);
        children.add(pluginLevelPane);
        children.add(resourceLevelDescription);
        children.add(resourceLevelPane);


        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(innerBox);

        BorderPane outerBox = new BorderPane();
        outerBox.setTop(msgBox);
        outerBox.setPadding(new Insets(5));
        outerBox.setCenter(scrollPane);

        VBox descriptionBox = new VBox();
        descriptionBox.getChildren().add(descriptionDescription);
        descriptionBox.getChildren().add(descriptionText);
        outerBox.setBottom(descriptionBox);

        outerBox.setRight(createButton);


        stage.setScene(new Scene(outerBox, 600, 550));
        stage.show();
    }

    private HBox getMessagesBox() {
        HBox msgBox = new HBox();
        Label label = new Label("Messages:");
        msgBox.getChildren().add(label);
        errorMessage = new Text();
        errorMessage.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 15));
        errorMessage.setId("errorMessage");
        msgBox.getChildren().add(errorMessage);
        msgBox.setPadding(new Insets(5));
        msgBox.setSpacing(3);
        msgBox.setAlignment(Pos.BASELINE_LEFT);
        return msgBox;
    }

    private int addFields(final GridPane root, boolean pluginLevel, final Text descriptionField) {

        int row = 0;
        for (final Prop prop : Prop.values()) {

            if (!prop.isPluginLevel()==pluginLevel) {
                continue;
            }

            // Add the label
            String name = prop.readableName();
            Label fieldName = new Label(name);
            root.add(fieldName,0,row);

            // Now add the field itself
            final Class propType = prop.getType();
            if (propType.equals(String.class)) {
                addStringField(root, descriptionField, row, prop);
            } else if (propType.equals(Boolean.class) || propType.equals(boolean.class)) {
                addBooleanField(root, row, prop);
            } else if (propType.equals(ResourceCategory.class)) {
                addResourceTypeChooser(root, row, prop);
            } else if (propType.equals(File.class)) {
                addDirectoryChooserField(root, row, prop, descriptionField);

            }

            row++;

        }

        return row;
    }

    private void addDirectoryChooserField(GridPane root, int row, final Prop prop, final Text descriptionField) {
        // Can not add this directly, so add a button to trigger it
        final TextField input = new TextField();
        root.add(input,1,row);
        Tooltip tooltip = new Tooltip(prop.getDescription());
        Button pickButton = new Button("Pick");
        input.setTooltip(tooltip);
        pickButton.setTooltip(tooltip);
        pickButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle(prop.getDescription());
                File dir = chooser.showDialog(primaryStage);
                if (dir != null) {
                    String dirName = dir.getAbsolutePath();
                    setPropsValue(prop.getVariableName(), dirName, String.class);
                    clearErrorMessage();
                    input.setText(dirName);
                } else {
                    setErrorMessage("No directory selected");
                    input.setText("Pick a directory");
                }
            }
        });
        input.focusedProperty().addListener(new ShowFieldDescriptionHandler(prop,descriptionField,input));
        // Add validation of the input
        input.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String newText) {
                File file = new File(newText);
                if (!file.isDirectory()) {
                    setErrorMessage(newText + " is no directory");
                } else if (prop.isDirectoryWriteable() && !file.canWrite()) {
                    setErrorMessage(newText + " is not writable");
                } else if (!prop.isDirectoryWriteable() && !file.canRead()) {
                    setErrorMessage(newText + " is not readable");
                } else {
                    clearErrorMessage();
                }

            }
        });



        root.add(pickButton,2,row);
    }

    private void addResourceTypeChooser(GridPane root, int row, final Prop prop) {
        final ChoiceBox choiceBox = new ChoiceBox();
        for (ResourceCategory cat : ResourceCategory.values()) {
            choiceBox.getItems().add(cat.getLowerName());
        }
        choiceBox.getSelectionModel().selectLast(); // service is default
        choiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String newValue) {
                ResourceCategory newCategory = ResourceCategory.valueOf(newValue.toUpperCase());
                setPropsValue(prop.getVariableName(),newCategory,prop.getType());
            }
        });
        Tooltip tooltip = new Tooltip(prop.getDescription()); // TODO make this a hover listener
        choiceBox.setTooltip(tooltip);
        root.add(choiceBox,1,row);
    }

    private void addBooleanField(GridPane root, int row, final Prop prop) {
        final ChoiceBox choiceBox = new ChoiceBox();
        choiceBox.getItems().addAll("Yes", "No");
        choiceBox.getSelectionModel().selectLast(); // NO is default
        choiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String newValue) {
                setPropsValue(prop.getVariableName(), newValue.equals("Yes"), prop.getType());
            }
        });
        Tooltip tooltip = new Tooltip(prop.getDescription()); // TODO make this a hover listener
        choiceBox.setTooltip(tooltip);

        root.add(choiceBox, 1, row);
    }

    private void addStringField(GridPane root, final Text descriptionField, int row, final Prop prop) {
        final Pattern pattern = Pattern.compile(prop.getValidationRegex());

        final TextField input = new TextField();
        if (prop.getDefaultValue()!=null && !prop.getDefaultValue().isEmpty()) {
            input.setText(prop.getDefaultValue());
            setPropsValue(prop.getVariableName(),prop.getDefaultValue(),prop.getType());
        }
        // Add field leave event to fill in the props with the result
        input.focusedProperty().addListener(new ShowFieldDescriptionHandler(prop,descriptionField,input));
        // Add validation of the input
        input.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String newText) {
                Matcher m = pattern.matcher(newText);
                if (!m.matches()) {
                    setErrorMessage("Input does not match " + prop.getValidationRegex());
                } else {
                    clearErrorMessage();
                }

            }
        });
        root.add(input, 1, row);
    }

    private void setInfoMessage(String message) {
        errorMessage.setText(message);
        errorMessage.setFill(Color.DARKGREEN);
    }
    private void setErrorMessage(String message) {
        errorMessage.setText(message);
        errorMessage.setFill(Color.RED);
    }

    private void clearErrorMessage() {
        errorMessage.setText("");
        errorMessage.setFill(Color.WHITE);
    }

    private void setPropsValue(String variableName, Object value, Class type) {

        String var = variableName.substring(0,1).toUpperCase() + variableName.substring(1);
        String setterName = "set"+ var;

        try {
            Method setter = Props.class.getDeclaredMethod(setterName,type);
            setter.invoke(props,value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            setErrorMessage(e.getMessage());
        }
    }


    private class ShowFieldDescriptionHandler implements ChangeListener<Boolean> {

        private Prop prop;
        private Text descriptionField;
        private TextField input;

        private ShowFieldDescriptionHandler(Prop prop,Text descriptionField, TextField input) {
            this.prop = prop;
            this.descriptionField = descriptionField;
            this.input = input;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldState,
                            Boolean newState) {
            if (newState) { // User entered input field
                descriptionField.setText(prop.getDescription());
            }
            else { // User left input field
                descriptionField.setText("");
                setPropsValue(prop.getVariableName(),input.getText(), String.class); // TODO right place?
            }
        }
    }
}
