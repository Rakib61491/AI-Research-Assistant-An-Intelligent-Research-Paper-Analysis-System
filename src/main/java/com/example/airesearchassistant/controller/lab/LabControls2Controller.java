package com.example.airesearchassistant.controller.lab;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * LabControls2Controller — Demonstrates controls C11 through C20:
 * C11: ListView<String> fruits + selection listener
 * C12: TreeView<String> fruits & vegetables hierarchy
 * C13: Progress accumulation with validation and one-line increment rule
 * C14: Slider controlling font size dynamically
 * C15: Spinner<Integer> with SpinnerValueFactory.IntegerSpinnerValueFactory
 * C16: MenuBar with File -> New / Open / Exit
 * C17: 3 Alert dialogs (Information, Warning, Error) with title, header, content
 * C18: TextArea with Clear button
 * C19: PasswordField with Show/Hide toggle (no logging/storing)
 * C20: FileChooser with image filter, error/cancel handling
 */
public class LabControls2Controller {

    // C11: ListView
    @FXML private ListView<String> c11ListView;
    @FXML private Label c11Label;

    // C12: TreeView
    @FXML private TreeView<String> c12TreeView;
    @FXML private Label c12Label;

    // C13: Progress Accumulation
    @FXML private TextField c13Input;
    @FXML private ProgressBar c13ProgressBar;
    @FXML private Label c13StatusLabel;
    @FXML private Label c13ErrorLabel;
    private int currentSum = 0;
    private int pressCount = 0;

    // C14: Slider
    @FXML private Slider c14Slider;
    @FXML private Label c14Label;

    // C15: Spinner
    @FXML private Spinner<Integer> c15Spinner;
    @FXML private Label c15Label;

    // C18: TextArea
    @FXML private TextArea c18TextArea;

    // C19: PasswordField & Show/Hide
    @FXML private PasswordField c19PasswordField;
    @FXML private TextField c19PlainTextField;
    @FXML private CheckBox c19ShowPasswordCheckBox;

    // C20: FileChooser & ImageView
    @FXML private ImageView c20ImageView;
    @FXML private Label c20StatusLabel;

    @FXML
    public void initialize() {
        // C11: ListView fruits
        c11ListView.setItems(FXCollections.observableArrayList(
                "Apple", "Banana", "Cherry", "Mango", "Orange"
        ));
        c11ListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                c11Label.setText("Selected Fruit: " + newVal);
            }
        });

        // C12: TreeView Fruits & Vegetables
        TreeItem<String> rootItem = new TreeItem<>("Produce");
        rootItem.setExpanded(true);

        TreeItem<String> fruitsBranch = new TreeItem<>("Fruits");
        fruitsBranch.setExpanded(true);
        fruitsBranch.getChildren().add(new TreeItem<>("Apple"));
        fruitsBranch.getChildren().add(new TreeItem<>("Banana"));
        fruitsBranch.getChildren().add(new TreeItem<>("Orange"));

        TreeItem<String> vegBranch = new TreeItem<>("Vegetables");
        vegBranch.setExpanded(true);
        vegBranch.getChildren().add(new TreeItem<>("Carrot"));
        vegBranch.getChildren().add(new TreeItem<>("Broccoli"));
        vegBranch.getChildren().add(new TreeItem<>("Spinach"));

        rootItem.getChildren().add(fruitsBranch);
        rootItem.getChildren().add(vegBranch);
        c12TreeView.setRoot(rootItem);
        c12TreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                c12Label.setText("Selected Tree Node: " + newVal.getValue());
            }
        });

        // C14: Slider (10 to 48) font size
        c14Slider.setMin(10);
        c14Slider.setMax(48);
        c14Slider.setValue(16);
        c14Slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            c14Label.setStyle(String.format("-fx-font-size: %.0fpx; -fx-text-fill: #cdd6f4;", newVal.doubleValue()));
            c14Label.setText(String.format("Adjustable Text (%.0fpx)", newVal.doubleValue()));
        });

        // C15: Spinner 1-10
        c15Spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));

        // C19: Password toggle synchronization
        c19ShowPasswordCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                c19PlainTextField.setText(c19PasswordField.getText());
                c19PlainTextField.setVisible(true);
                c19PlainTextField.setManaged(true);
                c19PasswordField.setVisible(false);
                c19PasswordField.setManaged(false);
            } else {
                c19PasswordField.setText(c19PlainTextField.getText());
                c19PlainTextField.clear(); // do not store revealed value
                c19PlainTextField.setVisible(false);
                c19PlainTextField.setManaged(false);
                c19PasswordField.setVisible(true);
                c19PasswordField.setManaged(true);
            }
        });
    }

    // ----- C13: Accumulate Handler -------------------------------------------
    @FXML
    private void handleC13Accumulate() {
        String text = c13Input.getText() != null ? c13Input.getText().trim() : "";
        if (text.isEmpty()) {
            c13ErrorLabel.setText("Error: N cannot be empty.");
            return;
        }

        int n;
        try {
            n = Integer.parseInt(text);
            if (n <= 0) {
                c13ErrorLabel.setText("Error: N must be greater than 0.");
                return;
            }
        } catch (NumberFormatException e) {
            c13ErrorLabel.setText("Error: N must be a valid integer.");
            return;
        }

        c13ErrorLabel.setText("");

        // Increment rule: add 1 per press (documented as a one-line change):
        currentSum += 1; // <--- Increment rule: add 1 per press (change this line to adjust increment)

        pressCount++;
        double progress = Math.min(1.0, (double) pressCount / n);
        c13ProgressBar.setProgress(progress);
        c13StatusLabel.setText(String.format(
                "currentSum = %d | pressCount = %d | N = %d | progress = %.0f%%%s",
                currentSum, pressCount, n, progress * 100, (progress >= 1.0 ? " (Target Reached!)" : "")
        ));
    }

    @FXML
    private void handleC13Reset() {
        currentSum = 0;
        pressCount = 0;
        c13ProgressBar.setProgress(0);
        c13StatusLabel.setText("Reset. Ready for accumulation.");
        c13ErrorLabel.setText("");
    }

    // ----- C15: Spinner Show Button ------------------------------------------
    @FXML
    private void handleC15Show() {
        c15Label.setText("Spinner Value: " + c15Spinner.getValue());
    }

    // ----- C16: Menu Handlers ------------------------------------------------
    @FXML
    private void handleMenuNew() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Menu Action");
        alert.setHeaderText("New File");
        alert.setContentText("You clicked File -> New.");
        alert.showAndWait();
    }

    @FXML
    private void handleMenuOpen() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Menu Action");
        alert.setHeaderText("Open File");
        alert.setContentText("You clicked File -> Open.");
        alert.showAndWait();
    }

    @FXML
    private void handleMenuExit() {
        Platform.exit();
    }

    // ----- C17: 3 Alert Dialogs ----------------------------------------------
    @FXML
    private void handleShowInfoAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information Dialog (C17)");
        alert.setHeaderText("Operation Completed");
        alert.setContentText("This is an Information Alert with custom title, header, and content.");
        alert.showAndWait();
    }

    @FXML
    private void handleShowWarningAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning Dialog (C17)");
        alert.setHeaderText("Caution Required");
        alert.setContentText("This is a Warning Alert with custom title, header, and content.");
        alert.showAndWait();
    }

    @FXML
    private void handleShowErrorAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error Dialog (C17)");
        alert.setHeaderText("Operation Failed");
        alert.setContentText("This is an Error Alert with custom title, header, and content.");
        alert.showAndWait();
    }

    // ----- C18: Clear TextArea -----------------------------------------------
    @FXML
    private void handleC18Clear() {
        c18TextArea.clear();
    }

    // ----- C20: FileChooser Image --------------------------------------------
    @FXML
    private void handleC20ChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose an Image File (C20)");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.gif, *.bmp)",
                        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        Stage stage = (Stage) c20StatusLabel.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile == null) {
            c20StatusLabel.setText("File selection cancelled by user.");
            return;
        }

        try {
            Image img = new Image(selectedFile.toURI().toString());
            if (img.isError()) {
                throw img.getException() != null ? img.getException() : new Exception("Invalid image data");
            }
            c20ImageView.setImage(img);
            c20StatusLabel.setText("Loaded: " + selectedFile.getName());
        } catch (Exception e) {
            c20StatusLabel.setText("Invalid image file: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Image Load Error");
            alert.setHeaderText("Could not load image");
            alert.setContentText("The selected file is not a valid or readable image: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
