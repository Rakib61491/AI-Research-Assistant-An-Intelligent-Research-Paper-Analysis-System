package com.example.airesearchassistant.controller.lab;

import com.example.airesearchassistant.model.Person;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * LabControls1Controller — Demonstrates controls C1 through C10:
 * C1:  TableView<Person> with PropertyValueFactory
 * C2:  ImageView + getResourceAsStream() with graceful fallback
 * C3:  Code-based setOnAction() for Button and TextField
 * C4:  @FXML initialize() auto-run label
 * C5:  Two ToggleGroups (Gender & Skill)
 * C6:  CheckBoxes with empty-selection handling
 * C7:  ChoiceBox background color switching
 * C8:  ComboBox with 5 countries
 * C9:  DatePicker with dd MMM yyyy formatting
 * C10: ColorPicker with live setTextFill()
 */
public class LabControls1Controller {

    // C1: TableView
    @FXML private TableView<Person> c1TableView;
    @FXML private TableColumn<Person, String> c1FirstNameCol;
    @FXML private TableColumn<Person, String> c1LastNameCol;
    @FXML private TableColumn<Person, Integer> c1AgeCol;

    // C2: ImageView
    @FXML private ImageView c2ImageView;
    @FXML private Label c2StatusLabel;
    private boolean c2ToggleAlternative = false;

    // C3: setOnAction in code
    @FXML private Button c3Button;
    @FXML private TextField c3TextField;
    @FXML private Label c3Label;

    // C4: initialize()
    @FXML private Label c4Label;

    // C5: ToggleGroups
    @FXML private RadioButton c5Male;
    @FXML private RadioButton c5Female;
    @FXML private RadioButton c5Other;
    @FXML private RadioButton c5Beginner;
    @FXML private RadioButton c5Intermediate;
    @FXML private RadioButton c5Expert;
    @FXML private Label c5Label;
    private final ToggleGroup genderGroup = new ToggleGroup();
    private final ToggleGroup skillGroup = new ToggleGroup();

    // C6: CheckBoxes
    @FXML private CheckBox c6Reading;
    @FXML private CheckBox c6Gaming;
    @FXML private CheckBox c6Traveling;
    @FXML private Label c6Label;

    // C7: ChoiceBox & Box
    @FXML private ChoiceBox<String> c7ChoiceBox;
    @FXML private VBox c7Container;

    // C8: ComboBox
    @FXML private ComboBox<String> c8ComboBox;
    @FXML private Label c8Label;

    // C9: DatePicker
    @FXML private DatePicker c9DatePicker;
    @FXML private Label c9Label;

    // C10: ColorPicker
    @FXML private ColorPicker c10ColorPicker;
    @FXML private Label c10Label;

    @FXML
    public void initialize() {
        // C4: Automatic initialization demonstration
        c4Label.setText("Auto-set via @FXML initialize() at controller startup");

        // C1: TableView setup
        c1FirstNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        c1LastNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        c1AgeCol.setCellValueFactory(new PropertyValueFactory<>("age"));

        c1TableView.setItems(FXCollections.observableArrayList(
                new Person("Ada", "Lovelace", 36),
                new Person("Alan", "Turing", 41),
                new Person("Grace", "Hopper", 85),
                new Person("Claude", "Shannon", 84)
        ));

        // C2: Initial image load
        loadC2Image(false);

        // C3: Event handlers attached purely in CODE (not FXML)
        c3Button.setOnAction(e -> c3Label.setText("c3Button clicked via Java code setOnAction()!"));
        c3TextField.setOnAction(e -> c3Label.setText("Enter key pressed in c3TextField: \"" + c3TextField.getText() + "\""));

        // C5: RadioButtons into two ToggleGroups
        c5Male.setToggleGroup(genderGroup);
        c5Female.setToggleGroup(genderGroup);
        c5Other.setToggleGroup(genderGroup);
        c5Male.setSelected(true);

        c5Beginner.setToggleGroup(skillGroup);
        c5Intermediate.setToggleGroup(skillGroup);
        c5Expert.setToggleGroup(skillGroup);
        c5Beginner.setSelected(true);

        genderGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateC5Label());
        skillGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateC5Label());
        updateC5Label();

        // C7: ChoiceBox background styling
        c7ChoiceBox.setItems(FXCollections.observableArrayList("Default", "Red", "Green", "Blue"));
        c7ChoiceBox.setValue("Default");
        c7ChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if ("Red".equals(newVal)) {
                c7Container.setStyle("-fx-background-color: #592323; -fx-padding: 12; -fx-background-radius: 6;");
            } else if ("Green".equals(newVal)) {
                c7Container.setStyle("-fx-background-color: #235923; -fx-padding: 12; -fx-background-radius: 6;");
            } else if ("Blue".equals(newVal)) {
                c7Container.setStyle("-fx-background-color: #233559; -fx-padding: 12; -fx-background-radius: 6;");
            } else {
                c7Container.setStyle("-fx-background-color: #2a2a3e; -fx-padding: 12; -fx-background-radius: 6;");
            }
        });

        // C8: ComboBox with 5 countries
        c8ComboBox.setItems(FXCollections.observableArrayList(
                "Bangladesh", "United States", "United Kingdom", "Germany", "Japan"
        ));
        c8ComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                c8Label.setText("Selected Country: " + newVal);
            }
        });

        // C9: DatePicker formatting
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        c9DatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                c9Label.setText("Selected: " + newVal.format(formatter));
            } else {
                c9Label.setText("No date selected");
            }
        });

        // C10: ColorPicker live text fill
        c10ColorPicker.setValue(Color.web("#89b4fa"));
        c10Label.setTextFill(c10ColorPicker.getValue());
        c10ColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                c10Label.setTextFill(newVal);
            }
        });
    }

    // C2: Image toggle handler
    @FXML
    private void handleChangeImage() {
        c2ToggleAlternative = !c2ToggleAlternative;
        loadC2Image(c2ToggleAlternative);
    }

    private void loadC2Image(boolean requestMissing) {
        String resourcePath = requestMissing
                ? "/com/example/airesearchassistant/missing-image.png"
                : "/com/example/airesearchassistant/icons/sample.png";

        InputStream stream = getClass().getResourceAsStream(resourcePath);
        if (stream != null) {
            c2ImageView.setImage(new Image(stream));
            c2StatusLabel.setText("Loaded from resource: " + resourcePath);
        } else {
            // Graceful fallback placeholder color
            WritableImage placeholder = new WritableImage(64, 64);
            PixelWriter pw = placeholder.getPixelWriter();
            Color fallbackColor = requestMissing ? Color.web("#f38ba8") : Color.web("#89b4fa");
            for (int x = 0; x < 64; x++) {
                for (int y = 0; y < 64; y++) {
                    pw.setColor(x, y, fallbackColor);
                }
            }
            c2ImageView.setImage(placeholder);
            c2StatusLabel.setText("Missing resource fallback: generated solid " + (requestMissing ? "Pink" : "Blue") + " placeholder");
        }
    }

    // C5 helper
    private void updateC5Label() {
        RadioButton selectedGender = (RadioButton) genderGroup.getSelectedToggle();
        RadioButton selectedSkill = (RadioButton) skillGroup.getSelectedToggle();
        String genderText = selectedGender != null ? selectedGender.getText() : "None";
        String skillText = selectedSkill != null ? selectedSkill.getText() : "None";
        c5Label.setText(String.format("Gender: %s | Skill Level: %s", genderText, skillText));
    }

    // C6: CheckBoxes submit handler
    @FXML
    private void handleC6Submit() {
        List<String> hobbies = new ArrayList<>();
        if (c6Reading.isSelected()) hobbies.add("Reading");
        if (c6Gaming.isSelected()) hobbies.add("Gaming");
        if (c6Traveling.isSelected()) hobbies.add("Traveling");

        if (hobbies.isEmpty()) {
            c6Label.setText("No hobbies selected");
        } else {
            c6Label.setText("Hobbies: " + String.join(", ", hobbies));
        }
    }
}
