package com.example.airesearchassistant.controller.lab;

import com.example.airesearchassistant.controller.MainShellController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TreeView;
import javafx.scene.text.Text;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchLabControllerSmokeTest {
    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void loadsEveryDemoPage() throws Exception {
        onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/airesearchassistant/lab/lab-view.fxml"));
            Parent root = loader.load();
            ResearchLabController controller = loader.getController();
            ChoiceBox<String> groups = field(controller, "groupChoice");
            ListView<String> demos = field(controller, "demoList");
            VBox content = field(controller, "demoContent");

            assertEquals(13, demos.getItems().size());
            Class<?>[] controlTypes = {
                    RadioButton.class, CheckBox.class, ChoiceBox.class, ComboBox.class, DatePicker.class,
                    ColorPicker.class, ListView.class, TreeView.class, ProgressBar.class, Slider.class,
                    Spinner.class, PasswordField.class, Button.class
            };
            for (int i = 0; i < demos.getItems().size(); i++) {
                demos.getSelectionModel().select(i);
                assertFalse(content.getChildren().isEmpty(), demos.getItems().get(i) + " did not render");
                assertTrue(containsType(content, controlTypes[i]),
                        demos.getItems().get(i) + " did not include its required JavaFX control");
            }

            groups.setValue("Multithreading and Runnables");
            assertEquals(9, demos.getItems().size());
            for (int i = 0; i < demos.getItems().size(); i++) {
                demos.getSelectionModel().select(i);
                assertFalse(content.getChildren().isEmpty(), demos.getItems().get(i) + " did not render");
            }
            return root;
        });
    }

    @Test
    void sidebarOpensTheReplacementLab() throws Exception {
        MainShellController shell = onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/airesearchassistant/fxml/main-shell.fxml"));
            loader.load();
            return loader.getController();
        });

        onFxThread(() -> {
            Button labButton = field(shell, "navLab");
            Button libraryButton = field(shell, "navLibrary");
            StackPane contentArea = field(shell, "contentArea");
            labButton.fire();
            assertEquals("host", contentArea.getChildren().getFirst().getId());
            libraryButton.fire();
            assertFalse(contentArea.getChildren().isEmpty());
            return null;
        });
    }

    @Test
    void labTextAndFieldsRemainReadableInBothThemes() throws Exception {
        onFxThread(() -> {
            for (boolean darkMode : new boolean[]{false, true}) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(
                        "/com/example/airesearchassistant/lab/lab-view.fxml"));
                Parent root = loader.load();
                ResearchLabController controller = loader.getController();
                VBox content = field(controller, "demoContent");
                ChoiceBox<String> groups = field(controller, "groupChoice");
                ListView<String> demos = field(controller, "demoList");
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource(
                        "/com/example/airesearchassistant/style.css").toExternalForm());
                if (darkMode) {
                    scene.getStylesheets().add(getClass().getResource(
                            "/com/example/airesearchassistant/dark.css").toExternalForm());
                }

                var applyTheme = ResearchLabController.class.getDeclaredMethod("applyDemoTheme");
                applyTheme.setAccessible(true);
                demos.getSelectionModel().select(0);
                root.applyCss();
                root.layout();
                applyTheme.invoke(controller);
                RadioButton radio = find(content, RadioButton.class);
                Text radioText = (Text) radio.lookup(".text");
                assertEquals(darkMode ? Color.web("#f8fafc") : Color.BLACK, radioText.getFill());

                demos.getSelectionModel().select(1);
                root.applyCss();
                root.layout();
                applyTheme.invoke(controller);
                CheckBox checkBox = find(content, CheckBox.class);
                Text checkBoxText = (Text) checkBox.lookup(".text");
                assertEquals(darkMode ? Color.web("#f8fafc") : Color.BLACK, checkBoxText.getFill());

                demos.getSelectionModel().select(2);
                root.applyCss();
                root.layout();
                applyTheme.invoke(controller);
                ChoiceBox<?> years = findByStyleClass(content, "lab-publication-year-choice", ChoiceBox.class);
                Labeled yearLabel = (Labeled) years.lookup(".label");
                assertEquals(Color.BLACK, yearLabel.getTextFill());

                demos.getSelectionModel().select(11);
                root.applyCss();
                root.layout();
                applyTheme.invoke(controller);
                root.applyCss();
                PasswordField password = findByStyleClass(content, "lab-api-key-field", PasswordField.class);
                assertTrue(password.getStyle().contains(darkMode ? "#f8fafc" : "#000000"));
            }
            return null;
        });
    }

    private static boolean containsType(Node node, Class<?> type) {
        if (type.isInstance(node)) return true;
        if (node instanceof Parent parent) {
            return parent.getChildrenUnmodifiable().stream().anyMatch(child -> containsType(child, type));
        }
        return false;
    }

    private static <T> T find(Node node, Class<T> type) {
        if (type.isInstance(node)) return type.cast(node);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T match = find(child, type);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static <T> T findByStyleClass(Node node, String styleClass, Class<T> type) {
        if (type.isInstance(node) && node.getStyleClass().contains(styleClass)) return type.cast(node);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T match = findByStyleClass(child, styleClass, type);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static <T> T field(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        T value = (T) field.get(target);
        return value;
    }

    private static <T> T onFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(20, TimeUnit.SECONDS);
    }
}
