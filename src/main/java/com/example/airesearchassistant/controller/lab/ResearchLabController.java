package com.example.airesearchassistant.controller.lab;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ResearchLabController {
    private static final List<String> CONTROL_DEMOS = List.of(
            "RadioButton — Paper category", "CheckBox — Paper properties",
            "ChoiceBox — Publication year", "ComboBox — Research topic",
            "DatePicker — Publication date", "ColorPicker — Paper highlight",
            "ListView — Paper manager", "TreeView — Subject organization",
            "ProgressBar — AI paper analysis", "Slider — Similarity threshold",
            "Spinner — Batch analysis", "PasswordField — AI connection",
            "FileChooser — Import papers"
    );
    private static final List<String> THREAD_DEMOS = List.of(
            "Thread subclass — start, run, join", "Runnable — concurrent processing",
            "Thread states — live inspection", "sleep, join, interruption",
            "Race condition — shared counter", "synchronized method and block",
            "wait and notifyAll — producer/consumer", "ExecutorService — fixed pool",
            "Callable, Future, invokeAll, shutdown"
    );
    private static final List<String> TOPICS = List.of(
            "Deep Learning", "Neural Networks", "Computer Vision",
            "Natural Language Processing", "Reinforcement Learning"
    );
    private static final List<PaperSample> PAPERS = List.of(
            new PaperSample("Attention Is All You Need", 2017, "Natural Language Processing", 98),
            new PaperSample("ResNet for Image Recognition", 2020, "Computer Vision", 91),
            new PaperSample("A Survey of Deep Learning", 2021, "Deep Learning", 86),
            new PaperSample("Neural Architecture Search", 2022, "Neural Networks", 79),
            new PaperSample("Language Models and Reasoning", 2023, "Natural Language Processing", 94),
            new PaperSample("Policy Learning with Feedback", 2024, "Reinforcement Learning", 74),
            new PaperSample("Vision Transformers Revisited", 2025, "Computer Vision", 88),
            new PaperSample("Efficient Foundation Models", 2026, "Deep Learning", 82)
    );

    @FXML private BorderPane host;
    @FXML private ChoiceBox<String> groupChoice;
    @FXML private ListView<String> demoList;
    @FXML private VBox demoContent;
    @FXML private Label groupLabel;
    @FXML private Label demoTitle;
    @FXML private Label demoCounter;
    @FXML private Button previousButton;
    @FXML private Button nextButton;

    private final Set<Thread> activeThreads = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private final List<ExecutorService> executors = new CopyOnWriteArrayList<>();
    private final List<Timeline> timelines = new CopyOnWriteArrayList<>();
    private boolean attached;
    private volatile boolean closed;
    private Thread interruptiblePaperTask;
    private ExecutorService callableExecutor;

    @FXML
    public void initialize() {
        groupChoice.setItems(FXCollections.observableArrayList("JavaFX Controls", "Multithreading and Runnables"));
        groupChoice.setValue("JavaFX Controls");
        groupChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> populateDemoList());
        demoList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> renderSelectedDemo());
        host.parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent != null) {
                attached = true;
            } else if (attached) {
                closeBackgroundWork();
            }
        });
        host.sceneProperty().addListener((obs, oldScene, newScene) -> observeTheme(newScene));
        if (host.getScene() != null) observeTheme(host.getScene());
        populateDemoList();
    }

    @FXML
    private void showPrevious() {
        int index = demoList.getSelectionModel().getSelectedIndex();
        if (index > 0) demoList.getSelectionModel().select(index - 1);
    }

    @FXML
    private void showNext() {
        int index = demoList.getSelectionModel().getSelectedIndex();
        if (index >= 0 && index + 1 < demoList.getItems().size()) {
            demoList.getSelectionModel().select(index + 1);
        }
    }

    private void populateDemoList() {
        boolean controls = "JavaFX Controls".equals(groupChoice.getValue());
        List<String> names = controls ? CONTROL_DEMOS : THREAD_DEMOS;
        demoList.setItems(FXCollections.observableArrayList(names));
        demoList.getSelectionModel().selectFirst();
    }

    private void renderSelectedDemo() {
        String selected = demoList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        stopPageTimelines();
        int index = demoList.getSelectionModel().getSelectedIndex();
        boolean controls = "JavaFX Controls".equals(groupChoice.getValue());
        groupLabel.setText(controls ? "GROUP A  /  JAVAFX CONTROLS" : "GROUP B  /  MULTITHREADING AND RUNNABLES");
        demoTitle.setText(selected);
        demoCounter.setText((index + 1) + " / " + demoList.getItems().size());
        previousButton.setDisable(index == 0);
        nextButton.setDisable(index + 1 == demoList.getItems().size());
        demoContent.getChildren().setAll(controls ? buildControlDemo(index) : buildThreadDemo(index));
        applyDemoTheme();
    }

    private Node buildControlDemo(int index) {
        return switch (index) {
            case 0 -> radioButtonDemo();
            case 1 -> checkBoxDemo();
            case 2 -> choiceBoxDemo();
            case 3 -> comboBoxDemo();
            case 4 -> datePickerDemo();
            case 5 -> colorPickerDemo();
            case 6 -> listViewDemo();
            case 7 -> treeViewDemo();
            case 8 -> analysisProgressDemo();
            case 9 -> similaritySliderDemo();
            case 10 -> batchSpinnerDemo();
            case 11 -> passwordFieldDemo();
            case 12 -> fileChooserDemo();
            default -> new VBox();
        };
    }

    private Node buildThreadDemo(int index) {
        return switch (index) {
            case 0 -> threadSubclassDemo();
            case 1 -> runnableDemo();
            case 2 -> threadStatesDemo();
            case 3 -> coordinationDemo();
            case 4 -> raceConditionDemo(false);
            case 5 -> raceConditionDemo(true);
            case 6 -> producerConsumerDemo();
            case 7 -> fixedThreadPoolDemo();
            case 8 -> callableFutureDemo();
            default -> new VBox();
        };
    }

    private VBox radioButtonDemo() {
        VBox card = card();
        ToggleGroup group = new ToggleGroup();
        VBox choices = new VBox(8);
        for (String category : List.of("Artificial Intelligence", "Machine Learning", "Computer Vision", "Natural Language Processing")) {
            RadioButton choice = new RadioButton(category);
            choice.getStyleClass().add("lab-category-radio");
            choice.setToggleGroup(group);
            choices.getChildren().add(choice);
        }
        Label result = resultLabel("Choose a category and submit.");
        Button submit = new Button("Submit category");
        submit.setOnAction(e -> {
            RadioButton selected = (RadioButton) group.getSelectedToggle();
            result.setText(selected == null ? "Select a category first." : "Selected category: " + selected.getText());
        });
        card.getChildren().addAll(heading("Add a research paper"), choices, submit, result);
        return card;
    }

    private VBox checkBoxDemo() {
        VBox card = card();
        List<CheckBox> checks = new ArrayList<>();
        for (String property : List.of("Peer-reviewed", "Open Access", "Contains Experimental Results", "Includes Dataset")) {
            CheckBox check = new CheckBox(property);
            check.getStyleClass().add("lab-paper-property-checkbox");
            checks.add(check);
        }
        Label result = resultLabel("Select any number of properties.");
        Button submit = new Button("Show selected properties");
        submit.setOnAction(e -> {
            List<String> selected = checks.stream().filter(CheckBox::isSelected).map(CheckBox::getText).toList();
            result.setText(selected.isEmpty() ? "No properties selected." : "Selected: " + String.join(", ", selected));
        });
        card.getChildren().addAll(heading("Paper properties"), new VBox(8, checks.toArray(Node[]::new)), submit, result);
        return card;
    }

    private VBox choiceBoxDemo() {
        VBox card = card();
        ChoiceBox<Integer> years = new ChoiceBox<>(FXCollections.observableArrayList(2020, 2021, 2022, 2023, 2024, 2025, 2026));
        years.getStyleClass().add("lab-publication-year-choice");
        years.setStyle("-fx-background-color: #ffffff; -fx-border-color: #111827;");
        years.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> setChoiceBoxDisplayTextColor(years, Color.BLACK));
            }
        });
        years.skinProperty().addListener((obs, oldSkin, newSkin) ->
                Platform.runLater(() -> setChoiceBoxDisplayTextColor(years, Color.BLACK)));
        years.setValue(2024);
        ListView<String> matches = new ListView<>();
        matches.setPrefHeight(230);
        Label selection = resultLabel("");
        Runnable filter = () -> {
            int year = years.getValue();
            selection.setText("Showing sample papers published in " + year);
            matches.setItems(FXCollections.observableArrayList(PAPERS.stream()
                    .filter(p -> p.year() == year).map(PaperSample::title).toList()));
        };
        years.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) filter.run();
        });
        filter.run();
        card.getChildren().addAll(heading("Filter by publication year"), labeled("Publication year", years), selection, matches);
        return card;
    }

    private VBox comboBoxDemo() {
        VBox card = card();
        ComboBox<String> topics = new ComboBox<>(FXCollections.observableArrayList(TOPICS));
        topics.setEditable(true);
        topics.setPromptText("Choose or type a topic");
        ListView<String> papers = new ListView<>();
        papers.setPrefHeight(230);
        Label selectedTopic = resultLabel("Choose a topic or enter a custom topic.");
        Runnable refresh = () -> {
            String topic = topics.getEditor().getText().trim();
            if (topic.isEmpty()) topic = topics.getValue();
            if (topic == null || topic.isBlank()) return;
            String requestedTopic = topic;
            selectedTopic.setText("Topic: " + requestedTopic);
            papers.setItems(FXCollections.observableArrayList(PAPERS.stream()
                    .filter(p -> p.topic().equalsIgnoreCase(requestedTopic))
                    .map(PaperSample::title).toList()));
            if (papers.getItems().isEmpty()) papers.getItems().add("No sample papers match this custom topic yet.");
        };
        topics.setOnAction(e -> refresh.run());
        topics.getEditor().setOnAction(e -> refresh.run());
        card.getChildren().addAll(heading("Select a research topic"), labeled("Topic (editable)", topics), selectedTopic, papers);
        return card;
    }

    private VBox datePickerDemo() {
        VBox card = card();
        DatePicker date = new DatePicker(LocalDate.now());
        Label result = resultLabel("Select a publication date.");
        Button submit = new Button("Submit publication date");
        submit.setOnAction(e -> result.setText(date.getValue() == null
                ? "Select a date first." : "Publication date: " + date.getValue()));
        card.getChildren().addAll(heading("Paper publication date"), labeled("Publication date", date), submit, result);
        return card;
    }

    private VBox colorPickerDemo() {
        VBox card = card();
        ObservableList<String> papers = FXCollections.observableArrayList(PAPERS.stream().map(PaperSample::title).toList());
        ListView<String> list = new ListView<>(papers);
        list.setPrefHeight(270);
        ColorPicker picker = new ColorPicker(Color.web("#ffe08a"));
        Runnable apply = () -> {
            String selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            String color = toHex(picker.getValue());
            list.setStyle("-fx-control-inner-background: " + color + ";");
            list.setCellFactory(view -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    if (empty) {
                        setStyle("");
                    } else if (item.equals(selected)) {
                        String textColor = contrastText(picker.getValue());
                        setStyle("-fx-background-color: " + color + "; -fx-text-fill: " + textColor + ";");
                        Node text = lookup(".text");
                        if (text != null) text.setStyle("-fx-fill: " + textColor + ";");
                    } else {
                        setStyle("");
                    }
                }
            });
        };
        list.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> apply.run());
        picker.valueProperty().addListener((obs, oldValue, newValue) -> apply.run());
        card.getChildren().addAll(heading("Highlight a sample paper"), labeled("Highlight color", picker), list,
                resultLabel("Select a paper, then choose its highlight color."));
        return card;
    }

    private VBox listViewDemo() {
        VBox card = card();
        ObservableList<String> papers = FXCollections.observableArrayList(
                "Attention Is All You Need", "ResNet for Image Recognition", "Efficient Foundation Models");
        ListView<String> list = new ListView<>(papers);
        list.setPrefHeight(230);
        TextArea details = new TextArea();
        details.setEditable(false);
        details.setPromptText("Selected paper details appear here.");
        details.setPrefRowCount(3);
        Button add = new Button("Add paper");
        add.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add Sample Paper");
            dialog.setHeaderText("Add a research paper to this list");
            dialog.setContentText("Paper title:");
            dialog.showAndWait().map(String::trim).filter(s -> !s.isEmpty()).ifPresent(papers::add);
        });
        Button remove = new Button("Remove selected");
        remove.setOnAction(e -> {
            int selected = list.getSelectionModel().getSelectedIndex();
            if (selected >= 0) {
                String removed = papers.remove(selected);
                details.setText("Removed: " + removed);
            }
        });
        Button view = new Button("View selected title");
        view.setOnAction(e -> {
            String selected = list.getSelectionModel().getSelectedItem();
            details.setText(selected == null ? "Select a paper first." : "Selected title: " + selected);
        });
        list.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                details.setText(newValue == null ? "" : "Paper: " + newValue + "\nSample paper record selected."));
        card.getChildren().addAll(heading("Manage sample research papers"), list, new HBox(8, add, remove, view), details);
        return card;
    }

    private VBox treeViewDemo() {
        VBox card = card();
        TreeItem<String> root = new TreeItem<>("Research Subjects");
        TreeItem<String> cs = tree("Computer Science");
        TreeItem<String> ai = tree("Artificial Intelligence");
        TreeItem<String> ml = tree("Machine Learning");
        ml.getChildren().add(tree("Neural Networks: A Practical Survey"));
        TreeItem<String> deep = tree("Deep Learning");
        deep.getChildren().add(tree("Efficient Foundation Models"));
        ai.getChildren().addAll(ml, deep, tree("AI Planning: Recent Advances"));
        TreeItem<String> vision = tree("Computer Vision");
        vision.getChildren().add(tree("Vision Transformers Revisited"));
        TreeItem<String> nlp = tree("Natural Language Processing");
        nlp.getChildren().add(tree("Language Models and Reasoning"));
        cs.getChildren().addAll(ai, vision, nlp);
        TreeItem<String> ds = tree("Data Science");
        TreeItem<String> mining = tree("Data Mining");
        mining.getChildren().add(tree("Mining Large Research Corpora"));
        TreeItem<String> bigData = tree("Big Data");
        bigData.getChildren().add(tree("Scalable Paper Discovery"));
        ds.getChildren().addAll(mining, bigData);
        root.getChildren().addAll(cs, ds);
        root.setExpanded(true);
        cs.setExpanded(true);
        ai.setExpanded(true);
        TreeView<String> tree = new TreeView<>(root);
        tree.setPrefHeight(340);
        Label selected = resultLabel("Select a subject or sample paper.");
        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) selected.setText("Selected node: " + newValue.getValue());
        });
        card.getChildren().addAll(heading("Browse paper subjects"), tree, selected);
        return card;
    }

    private VBox analysisProgressDemo() {
        VBox card = card();
        ProgressBar progress = new ProgressBar(0);
        progress.setMaxWidth(Double.MAX_VALUE);
        Label status = resultLabel("Waiting");
        Button start = new Button("Start analysis");
        Button reset = new Button("Reset");
        AtomicReference<Task<Void>> currentTask = new AtomicReference<>();
        AtomicReference<Boolean> resetRequested = new AtomicReference<>(false);
        start.setOnAction(e -> {
            resetRequested.set(false);
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws InterruptedException {
                    String[] stages = {"Extracting text", "Analyzing content", "Generating summary"};
                    int stepsPerStage = 10;
                    for (int stage = 0; stage < stages.length; stage++) {
                        updateMessage(stages[stage]);
                        for (int step = 1; step <= stepsPerStage; step++) {
                            if (isCancelled()) return null;
                            Thread.sleep(120);
                            updateProgress(stage * stepsPerStage + step, stages.length * stepsPerStage);
                        }
                    }
                    return null;
                }
            };
            currentTask.set(task);
            progress.setProgress(0);
            start.setDisable(true);
            task.progressProperty().addListener((obs, oldValue, newValue) -> progress.setProgress(newValue.doubleValue()));
            task.messageProperty().addListener((obs, oldValue, newValue) -> status.setText(newValue));
            task.setOnSucceeded(event -> {
                status.setText("Completed");
                start.setDisable(false);
            });
            task.setOnCancelled(event -> {
                status.setText(resetRequested.get() ? "Waiting" : "Cancelled");
                start.setDisable(false);
            });
            task.setOnFailed(event -> {
                status.setText("Analysis failed: " + task.getException().getMessage());
                start.setDisable(false);
            });
            startTask(task, "LabPaperAnalysis");
        });
        reset.setOnAction(e -> {
            resetRequested.set(true);
            if (currentTask.get() != null) currentTask.get().cancel();
            progress.setProgress(0);
            status.setText("Waiting");
            start.setDisable(false);
        });
        card.getChildren().addAll(heading("Simulated paper analysis"), progress, status, new HBox(8, start, reset));
        return card;
    }

    private VBox similaritySliderDemo() {
        VBox card = card();
        Slider threshold = new Slider(0, 100, 75);
        threshold.setShowTickLabels(true);
        threshold.setShowTickMarks(true);
        threshold.setMajorTickUnit(25);
        Label value = resultLabel("");
        ListView<String> matching = new ListView<>();
        matching.setPrefHeight(260);
        Runnable update = () -> {
            int minimum = (int) Math.round(threshold.getValue());
            value.setText("Similarity threshold: " + minimum + "%");
            matching.setItems(FXCollections.observableArrayList(PAPERS.stream()
                    .filter(p -> p.similarity() >= minimum)
                    .map(p -> p.title() + " — " + p.similarity() + "%").toList()));
        };
        threshold.valueProperty().addListener((obs, oldValue, newValue) -> update.run());
        update.run();
        card.getChildren().addAll(heading("Filter by similarity score"), threshold, value, matching);
        return card;
    }

    private VBox batchSpinnerDemo() {
        VBox card = card();
        Spinner<Integer> count = new Spinner<>(1, 20, 5);
        count.setEditable(true);
        ProgressBar progress = new ProgressBar(0);
        progress.setMaxWidth(Double.MAX_VALUE);
        Label status = resultLabel("Ready to analyze 5 papers.");
        Button start = new Button("Start batch analysis");
        Button reset = new Button("Reset");
        AtomicReference<Task<Void>> currentTask = new AtomicReference<>();
        AtomicReference<Boolean> resetRequested = new AtomicReference<>(false);
        start.setOnAction(e -> {
            resetRequested.set(false);
            int total = count.getValue();
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws InterruptedException {
                    for (int i = 1; i <= total; i++) {
                        if (isCancelled()) return null;
                        updateMessage("Analyzing paper " + i + " of " + total);
                        Thread.sleep(350);
                        updateProgress(i, total);
                    }
                    return null;
                }
            };
            currentTask.set(task);
            progress.setProgress(0);
            start.setDisable(true);
            task.progressProperty().addListener((obs, oldValue, newValue) -> progress.setProgress(newValue.doubleValue()));
            task.messageProperty().addListener((obs, oldValue, newValue) -> status.setText(newValue));
            task.setOnSucceeded(event -> {
                status.setText("Completed " + total + " papers.");
                start.setDisable(false);
            });
            task.setOnCancelled(event -> {
                status.setText(resetRequested.get() ? "Ready to analyze " + count.getValue() + " papers." : "Batch cancelled.");
                start.setDisable(false);
            });
            task.setOnFailed(event -> {
                status.setText("Batch failed: " + task.getException().getMessage());
                start.setDisable(false);
            });
            startTask(task, "LabBatchAnalysis");
        });
        reset.setOnAction(e -> {
            resetRequested.set(true);
            if (currentTask.get() != null) currentTask.get().cancel();
            progress.setProgress(0);
            status.setText("Ready to analyze " + count.getValue() + " papers.");
            start.setDisable(false);
        });
        count.valueProperty().addListener((obs, oldValue, newValue) -> status.setText("Ready to analyze " + newValue + " papers."));
        card.getChildren().addAll(heading("Analyze a selected batch"), labeled("Number of papers", count), progress, status, new HBox(8, start, reset));
        return card;
    }

    private VBox passwordFieldDemo() {
        VBox card = card();
        PasswordField key = new PasswordField();
        key.getStyleClass().add("lab-api-key-field");
        key.setPromptText("Simulated key (never saved or sent)");
        Label status = resultLabel("Not connected");
        Button connect = new Button("Connect");
        connect.setOnAction(e -> {
            boolean provided = !key.getText().isBlank();
            status.setText(provided ? "Demo connection successful (input was not stored or sent)." : "Enter a non-empty simulated key.");
            key.clear();
        });
        card.getChildren().addAll(heading("Simulated AI configuration"), labeled("Demo API key", key), connect, status);
        return card;
    }

    private VBox fileChooserDemo() {
        VBox card = card();
        Label details = resultLabel("No paper file selected.");
        File[] selected = new File[1];
        Button browse = new Button("Browse for PDF or TXT");
        browse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select a Research Paper");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Research papers (*.pdf, *.txt)", "*.pdf", "*.txt"),
                    new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"),
                    new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt")
            );
            Window window = host.getScene() == null ? null : host.getScene().getWindow();
            File file = chooser.showOpenDialog(window);
            if (file != null) {
                selected[0] = file;
                details.setText("Selected: " + file.getName() + "\nPath: " + file.getAbsolutePath());
            }
        });
        Button importButton = new Button("Import selected file");
        importButton.setOnAction(e -> details.setText(selected[0] == null
                ? "Browse and select a PDF or TXT file first."
                : "Ready to import: " + selected[0].getName() + "\n" + selected[0].getAbsolutePath()));
        card.getChildren().addAll(heading("Choose a paper file"), new HBox(8, browse, importButton), details);
        return card;
    }

    private VBox threadSubclassDemo() {
        VBox card = card();
        TextArea log = logArea();
        Button start = new Button("Start two PaperAnalysisThread instances");
        Label status = resultLabel("Not started");
        start.setOnAction(e -> {
            start.setDisable(true);
            status.setText("Threads running; coordinator is waiting with join().");
            launchManaged("ThreadJoinCoordinator", () -> {
                Thread first = track(new PaperAnalysisThread("PaperAnalysis-A", "Attention Is All You Need",
                        message -> append(log, message), () -> activeThreads.remove(Thread.currentThread())));
                Thread second = track(new PaperAnalysisThread("PaperAnalysis-B", "Vision Transformers Revisited",
                        message -> append(log, message), () -> activeThreads.remove(Thread.currentThread())));
                first.start();
                second.start();
                try {
                    first.join();
                    second.join();
                    append(log, "join() returned: both paper threads completed.");
                    Platform.runLater(() -> {
                        status.setText("Both paper threads completed.");
                        start.setDisable(false);
                    });
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    append(log, "Join coordinator interrupted.");
                }
            });
        });
        card.getChildren().addAll(heading("Thread subclass, start(), run(), and join()"), start, status, log);
        return card;
    }

    private VBox runnableDemo() {
        VBox card = card();
        TextArea log = logArea();
        Button start = new Button("Run three Runnable tasks");
        Label status = resultLabel("Each task reports from its own thread; order may vary.");
        start.setOnAction(e -> {
            start.setDisable(true);
            CountDownLatch done = new CountDownLatch(3);
            List<String> titles = List.of("Paper A: NLP", "Paper B: Computer Vision", "Paper C: Reinforcement Learning");
            for (String title : titles) {
                launchManaged("Runnable-" + title.substring(6, 7), () -> {
                    try {
                        Random random = new Random();
                        for (int step = 1; step <= 4; step++) {
                            Thread.sleep(100 + random.nextInt(250));
                            append(log, Thread.currentThread().getName() + " processed " + title + " (step " + step + ")");
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        append(log, title + " interrupted.");
                    } finally {
                        done.countDown();
                    }
                });
            }
            launchManaged("RunnableJoinCoordinator", () -> {
                try {
                    done.await();
                    Platform.runLater(() -> {
                        status.setText("All Runnable tasks completed.");
                        start.setDisable(false);
                    });
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
        });
        card.getChildren().addAll(heading("Concurrent Runnable paper tasks"), start, status, log);
        return card;
    }

    private VBox threadStatesDemo() {
        VBox card = card();
        GridPane states = new GridPane();
        states.setHgap(16);
        states.setVgap(9);
        Map<String, Label> stateLabels = new java.util.LinkedHashMap<>();
        for (String name : List.of("NeverStarted", "RunnableProbe", "MonitorHolder", "BlockedProbe", "WaitingProbe", "TimedSleepProbe")) {
            int row = stateLabels.size();
            states.add(new Label(name), 0, row);
            Label state = resultLabel("NEW");
            stateLabels.put(name, state);
            states.add(state, 1, row);
        }
        Button start = new Button("Start coordinated state probes");
        Button release = new Button("Release waiting threads");
        release.setDisable(true);
        Label status = resultLabel("NEW thread is shown before start; probes expose live states.");
        AtomicReference<CountDownLatch> releaseHolder = new AtomicReference<>();
        AtomicReference<List<Thread>> probesHolder = new AtomicReference<>(List.of());
        Thread neverStarted = new Thread(() -> { }, "NeverStarted");
        Runnable refresh = () -> {
            stateLabels.get("NeverStarted").setText(neverStarted.getState().toString());
            for (Thread probe : probesHolder.get()) {
                Label label = stateLabels.get(probe.getName());
                if (label != null) label.setText(probe.getState().toString());
            }
        };
        Timeline monitor = new Timeline(new KeyFrame(Duration.millis(100), e -> refresh.run()));
        monitor.setCycleCount(Timeline.INDEFINITE);
        start.setOnAction(e -> {
            if (closed) return;
            start.setDisable(true);
            release.setDisable(false);
            CountDownLatch ownerHasLock = new CountDownLatch(1);
            CountDownLatch unlock = new CountDownLatch(1);
            CountDownLatch waiterGate = new CountDownLatch(1);
            Object lock = new Object();
            releaseHolder.set(unlock);
            Thread holder = launchManaged("MonitorHolder", () -> {
                synchronized (lock) {
                    ownerHasLock.countDown();
                    awaitLatch(unlock);
                }
            });
            launchManaged("StateProbeCoordinator", () -> {
                try {
                    ownerHasLock.await();
                    Thread blocked = launchManaged("BlockedProbe", () -> {
                        synchronized (lock) {
                            appendToStatus(status, "BLOCKED probe entered monitor and finished.");
                        }
                    });
                    Thread waiting = launchManaged("WaitingProbe", () -> awaitLatch(waiterGate));
                    Thread timed = launchManaged("TimedSleepProbe", () -> {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    });
                    Thread runnable = launchManaged("RunnableProbe", () -> {
                        long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1400);
                        while (System.nanoTime() < until && !Thread.currentThread().isInterrupted()) Thread.onSpinWait();
                    });
                    List<Thread> probes = List.of(holder, blocked, waiting, timed, runnable);
                    Platform.runLater(() -> {
                        probesHolder.set(probes);
                        refresh.run();
                        if (!timelines.contains(monitor)) timelines.add(monitor);
                        monitor.playFromStart();
                    });
                    unlock.await(8, TimeUnit.SECONDS);
                    unlock.countDown();
                    waiterGate.countDown();
                    for (Thread probe : probes) {
                        if (probe.isAlive()) probe.interrupt();
                    }
                    for (Thread probe : probes) probe.join();
                    Platform.runLater(() -> {
                        monitor.stop();
                        timelines.remove(monitor);
                        refresh.run();
                        status.setText("Probe run finished. Select Start to observe the states again.");
                        start.setDisable(false);
                        release.setDisable(true);
                    });
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    unlock.countDown();
                    waiterGate.countDown();
                }
            });
        });
        release.setOnAction(e -> {
            CountDownLatch unlock = releaseHolder.get();
            if (unlock != null) unlock.countDown();
            probesHolder.get().forEach(Thread::interrupt);
            release.setDisable(true);
            status.setText("Wait gates released.");
        });
        card.getChildren().addAll(heading("Observe coordinated Java thread states"), states, new HBox(8, start, release), status);
        return card;
    }

    private VBox coordinationDemo() {
        VBox card = card();
        TextArea log = logArea();
        Label status = resultLabel("Idle");
        Button start = new Button("Start paper processing");
        Button interrupt = new Button("Interrupt task");
        Button join = new Button("Wait with join()");
        interrupt.setDisable(true);
        join.setDisable(true);
        start.setOnAction(e -> {
            if (interruptiblePaperTask != null && interruptiblePaperTask.isAlive()) return;
            Thread task = new Thread(() -> {
                try {
                    append(log, "Started " + Thread.currentThread().getName());
                    try {
                        for (int step = 1; step <= 12; step++) {
                            Thread.sleep(400);
                            append(log, "Processing section " + step + " of 12");
                        }
                        append(log, "Paper processing completed.");
                        Platform.runLater(() -> status.setText("Completed"));
                    } catch (InterruptedException ex) {
                        append(log, "sleep() was interrupted; processing cancelled.");
                        Thread.currentThread().interrupt();
                        Platform.runLater(() -> status.setText("Interrupted"));
                    }
                } finally {
                    activeThreads.remove(Thread.currentThread());
                }
            }, "InterruptiblePaperProcessor");
            interruptiblePaperTask = track(task);
            task.setDaemon(true);
            task.start();
            status.setText("Running (sleeping between paper sections)");
            interrupt.setDisable(false);
            join.setDisable(false);
            start.setDisable(true);
            task.setUncaughtExceptionHandler((t, ex) -> append(log, "Task failed: " + ex.getMessage()));
            launchManaged("PaperCompletionWatcher", () -> {
                try {
                    task.join();
                    Platform.runLater(() -> {
                        if (!closed) {
                            status.setText(task.isInterrupted() ? "Interrupted" : "Task finished");
                            start.setDisable(false);
                            interrupt.setDisable(true);
                        }
                    });
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
        });
        interrupt.setOnAction(e -> {
            if (interruptiblePaperTask != null) interruptiblePaperTask.interrupt();
        });
        join.setOnAction(e -> {
            Thread task = interruptiblePaperTask;
            if (task == null) {
                status.setText("Start processing first.");
                return;
            }
            join.setDisable(true);
            launchManaged("PaperJoinWaiter", () -> {
                try {
                    task.join();
                    append(log, "join() returned: paper task has terminated.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    append(log, "join() waiter was interrupted.");
                } finally {
                    Platform.runLater(() -> join.setDisable(false));
                }
            });
        });
        card.getChildren().addAll(heading("Start, sleep, interrupt, and join"), new HBox(8, start, interrupt, join), status, log);
        return card;
    }

    private VBox raceConditionDemo(boolean synchronizedOptions) {
        VBox card = card();
        TextArea log = logArea();
        Label status = resultLabel("Run multiple workers to compare expected and actual counts.");
        int workerCount = 8;
        int increments = 100_000;
        Button unsafe = new Button(synchronizedOptions ? "Run unsafe counter" : "Run race condition");
        unsafe.setOnAction(e -> runCounterExperiment(log, status, workerCount, increments, "unsafe"));
        card.getChildren().addAll(heading(synchronizedOptions
                ? "Compare unsynchronized and synchronized increments"
                : "Race condition in a shared processed-paper counter"),
                new Label("Workers: " + workerCount + "  |  increments per worker: " + increments),
                unsafe);
        if (synchronizedOptions) {
            Button syncMethod = new Button("Run synchronized method");
            Button syncBlock = new Button("Run synchronized block");
            syncMethod.setOnAction(e -> runCounterExperiment(log, status, workerCount, increments, "method"));
            syncBlock.setOnAction(e -> runCounterExperiment(log, status, workerCount, increments, "block"));
            card.getChildren().add(new HBox(8, syncMethod, syncBlock));
        }
        card.getChildren().addAll(status, log);
        return card;
    }

    private void runCounterExperiment(TextArea log, Label status, int workers, int increments, String mode) {
        Counter counter = new Counter();
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch go = new CountDownLatch(1);
        status.setText("Running " + mode + " counter...");
        append(log, "Starting " + workers + " workers × " + increments + " increments (" + mode + ").");
        launchManaged("CounterCoordinator", () -> {
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                Thread worker = launchManaged("CounterWorker-" + i, () -> {
                    ready.countDown();
                    try {
                        go.await();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int n = 0; n < increments && !Thread.currentThread().isInterrupted(); n++) {
                        switch (mode) {
                            case "method" -> counter.incrementSynchronizedMethod();
                            case "block" -> counter.incrementSynchronizedBlock();
                            default -> counter.incrementUnsynchronized();
                        }
                        if ("unsafe".equals(mode) && (n & 15) == 0) Thread.yield();
                    }
                });
                threads.add(worker);
            }
            try {
                ready.await();
                go.countDown();
                for (Thread thread : threads) thread.join();
                long expected = (long) workers * increments;
                int actual = counter.value;
                append(log, "Expected " + expected + " | actual " + actual + " | mode " + mode);
                Platform.runLater(() -> status.setText("Finished — expected " + expected + ", actual " + actual + "."));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                go.countDown();
                append(log, "Counter run interrupted.");
            }
        });
    }

    private VBox producerConsumerDemo() {
        VBox card = card();
        TextArea log = logArea();
        Label queueSize = resultLabel("Queue size: 0");
        Button start = new Button("Start producer and consumer");
        Button stop = new Button("Stop producer/consumer");
        stop.setDisable(true);
        final Thread[] consumerRef = new Thread[1];
        final Thread[] producerRef = new Thread[1];
        final Object monitor = new Object();
        final Queue<String> queue = new ArrayDeque<>();
        final boolean[] producerFinished = {false};
        final AtomicInteger waitingQueueSize = new AtomicInteger();
        start.setOnAction(e -> {
            if (start.isDisabled()) return;
            start.setDisable(true);
            stop.setDisable(false);
            synchronized (monitor) {
                queue.clear();
                waitingQueueSize.set(0);
            }
            queueSize.setText("Queue size: 0");
            producerFinished[0] = false;
            Thread consumer = launchManaged("PaperTaskConsumer", () -> {
                while (!Thread.currentThread().isInterrupted()) {
                    String item;
                    synchronized (monitor) {
                        while (queue.isEmpty() && !producerFinished[0]) {
                            try {
                                monitor.wait();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                        if (queue.isEmpty() && producerFinished[0]) return;
                        item = queue.remove();
                        waitingQueueSize.set(queue.size());
                    }
                    append(log, "Consumer processing: " + item);
                    Platform.runLater(() -> queueSize.setText("Queue size: " + waitingQueueSize.get()));
                    try {
                        Thread.sleep(420);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    append(log, "Consumer completed: " + item);
                }
            });
            consumerRef[0] = consumer;
            Thread producer = launchManaged("PaperTaskProducer", () -> {
                try {
                    for (int i = 1; i <= 10; i++) {
                        synchronized (monitor) {
                            String item = "Research paper analysis task " + i;
                            queue.add(item);
                            waitingQueueSize.set(queue.size());
                            monitor.notifyAll();
                            append(log, "Producer generated task " + i);
                        }
                        Platform.runLater(() -> queueSize.setText("Queue size: " + waitingQueueSize.get()));
                        Thread.sleep(250);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    append(log, "Producer interrupted.");
                } finally {
                    synchronized (monitor) {
                        producerFinished[0] = true;
                        monitor.notifyAll();
                    }
                    try {
                        consumer.join();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    Platform.runLater(() -> {
                        if (!closed) {
                            start.setDisable(false);
                            stop.setDisable(true);
                            queueSize.setText("Queue size: " + waitingQueueSize.get() + " (producer finished)");
                        }
                    });
                }
            });
            producerRef[0] = producer;
        });
        stop.setOnAction(e -> {
            Thread producer = producerRef[0];
            Thread consumer = consumerRef[0];
            if (producer != null) producer.interrupt();
            synchronized (monitor) {
                producerFinished[0] = true;
                monitor.notifyAll();
            }
            if (consumer != null) consumer.interrupt();
            append(log, "Stop requested for producer and consumer.");
        });
        card.getChildren().addAll(heading("Queue paper tasks with wait() and notifyAll()"),
                new HBox(8, start, stop), queueSize, log);
        return card;
    }

    private VBox fixedThreadPoolDemo() {
        VBox card = card();
        Spinner<Integer> workerCount = new Spinner<>(1, 8, 3);
        ProgressBar progress = new ProgressBar(0);
        progress.setMaxWidth(Double.MAX_VALUE);
        Label status = resultLabel("Ready");
        TextArea log = logArea();
        Button start = new Button("Process sample papers");
        start.setOnAction(e -> {
            start.setDisable(true);
            progress.setProgress(0);
            ExecutorService pool = Executors.newFixedThreadPool(workerCount.getValue());
            executors.add(pool);
            int total = 8;
            AtomicInteger completed = new AtomicInteger();
            for (PaperSample paper : PAPERS) {
                pool.submit(() -> {
                    append(log, "Started " + paper.title() + " on " + Thread.currentThread().getName());
                    try {
                        Thread.sleep(500 + new Random().nextInt(600));
                        int done = completed.incrementAndGet();
                        Platform.runLater(() -> {
                            if (!closed) {
                                progress.setProgress((double) done / total);
                                status.setText("Completed " + done + " / " + total);
                            }
                        });
                        append(log, "Completed " + paper.title() + " on " + Thread.currentThread().getName());
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        append(log, "Interrupted " + paper.title());
                    }
                });
            }
            pool.shutdown();
            launchManaged("FixedPoolCompletionWatcher", () -> {
                try {
                    boolean terminated = pool.awaitTermination(1, TimeUnit.MINUTES);
                    Platform.runLater(() -> {
                        if (!closed) {
                            status.setText(terminated ? "All paper tasks completed." : "Timed out waiting for paper tasks.");
                            start.setDisable(false);
                            executors.remove(pool);
                        }
                    });
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
        });
        card.getChildren().addAll(heading("Process papers on a fixed worker pool"),
                labeled("Worker threads", workerCount), start, progress, status, log);
        return card;
    }

    private VBox callableFutureDemo() {
        VBox card = card();
        TextArea results = logArea();
        Label status = resultLabel("Executor ready to accept tasks.");
        Button submit = new Button("Run Callable tasks with invokeAll()");
        Button shutdown = new Button("Graceful shutdown");
        submit.setOnAction(e -> {
            submit.setDisable(true);
            shutdown.setDisable(false);
            ExecutorService executor = Executors.newFixedThreadPool(3);
            callableExecutor = executor;
            executors.add(executor);
            List<Callable<String>> tasks = new ArrayList<>();
            for (PaperSample paper : PAPERS.subList(0, 5)) {
                tasks.add(() -> {
                    Thread.sleep(350 + new Random().nextInt(400));
                    int words = 700 + new Random().nextInt(6300);
                    return paper.title() + " | estimated word count: " + words + " | worker: " + Thread.currentThread().getName();
                });
            }
            launchManaged("CallableInvokeAllCoordinator", () -> {
                try {
                    append(results, "invokeAll() submitted " + tasks.size() + " Callable tasks.");
                    List<Future<String>> futures = executor.invokeAll(tasks);
                    for (Future<String> future : futures) append(results, "Future result: " + future.get());
                    Platform.runLater(() -> {
                        if (!closed) {
                            status.setText("All Callable results received. Gracefully shut down the executor to run again.");
                        }
                    });
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    append(results, "Callable execution interrupted.");
                } catch (java.util.concurrent.ExecutionException ex) {
                    append(results, "Could not retrieve Callable result: " + ex.getMessage());
                    Platform.runLater(() -> status.setText("Callable task failed."));
                }
            });
        });
        shutdown.setDisable(true);
        shutdown.setOnAction(e -> {
            ExecutorService executor = callableExecutor;
            if (executor == null) {
                status.setText("There is no active executor.");
                return;
            }
            executor.shutdown();
            shutdown.setDisable(true);
            status.setText("Graceful shutdown requested; waiting for submitted work.");
            launchManaged("CallableShutdownWaiter", () -> {
                try {
                    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow();
                    executors.remove(executor);
                    Platform.runLater(() -> {
                        if (!closed) {
                            status.setText("Executor terminated.");
                            submit.setDisable(false);
                            callableExecutor = null;
                        }
                    });
                } catch (InterruptedException ex) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                    Platform.runLater(() -> status.setText("Shutdown waiter interrupted; executor stopped."));
                }
            });
        });
        card.getChildren().addAll(heading("Callable paper analysis results"), new HBox(8, submit, shutdown), status, results);
        return card;
    }

    private void startTask(Task<?> task, String name) {
        launchManaged(name, task);
    }

    private Thread launchManaged(String name, Runnable action) {
        Thread thread = new Thread(() -> {
            try {
                action.run();
            } catch (RuntimeException ex) {
                System.err.println(name + " failed: " + ex.getMessage());
            } finally {
                activeThreads.remove(Thread.currentThread());
            }
        }, name);
        thread.setDaemon(true);
        track(thread);
        thread.start();
        return thread;
    }

    private Thread track(Thread thread) {
        activeThreads.add(thread);
        thread.setDaemon(true);
        return thread;
    }

    private void closeBackgroundWork() {
        if (closed) return;
        closed = true;
        stopPageTimelines();
        for (Thread thread : activeThreads) thread.interrupt();
        for (ExecutorService executor : executors) executor.shutdownNow();
        executors.clear();
    }

    private void observeTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().addListener((javafx.collections.ListChangeListener<String>) change -> applyDemoTheme());
        Platform.runLater(this::applyDemoTheme);
    }

    private void applyDemoTheme() {
        Scene scene = host.getScene();
        String darkStylesheet = getClass().getResource("/com/example/airesearchassistant/dark.css").toExternalForm();
        boolean darkMode = scene != null && scene.getStylesheets().contains(darkStylesheet);
        applyDemoTheme(demoContent, darkMode);
    }

    private void applyDemoTheme(Node node, boolean darkMode) {
        if (node instanceof RadioButton radio && radio.getStyleClass().contains("lab-category-radio")) {
            radio.setTextFill(darkMode ? Color.web("#f8fafc") : Color.BLACK);
        } else if (node instanceof CheckBox checkBox && checkBox.getStyleClass().contains("lab-paper-property-checkbox")) {
            checkBox.setTextFill(darkMode ? Color.web("#f8fafc") : Color.BLACK);
        } else if (node instanceof ChoiceBox<?> choiceBox
                && choiceBox.getStyleClass().contains("lab-publication-year-choice")) {
            choiceBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #111827; -fx-text-fill: #000000;");
            setChoiceBoxDisplayTextColor(choiceBox, Color.BLACK);
        } else if (node instanceof PasswordField password
                && password.getStyleClass().contains("lab-api-key-field")) {
            password.setStyle(darkMode
                    ? "-fx-background-color: #111827; -fx-control-inner-background: #111827; -fx-text-fill: #f8fafc; -fx-prompt-text-fill: #cbd5e1; -fx-border-color: #f8fafc; -fx-border-width: 2; -fx-border-radius: 4;"
                    : "-fx-background-color: #ffffff; -fx-control-inner-background: #ffffff; -fx-text-fill: #111827; -fx-prompt-text-fill: #475569; -fx-border-color: #000000; -fx-border-width: 2; -fx-border-radius: 4;");
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyDemoTheme(child, darkMode);
            }
        }
    }

    private void stopPageTimelines() {
        for (Timeline timeline : timelines) timeline.stop();
        timelines.clear();
    }

    private void append(TextArea area, String message) {
        if (closed) return;
        Platform.runLater(() -> {
            if (closed) return;
            if (area != null) area.appendText(message + System.lineSeparator());
        });
    }

    private void appendToStatus(Label label, String message) {
        if (closed) return;
        Platform.runLater(() -> {
            if (!closed) label.setText(message);
        });
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    private static String contrastText(Color background) {
        double luminance = 0.2126 * background.getRed()
                + 0.7152 * background.getGreen()
                + 0.0722 * background.getBlue();
        return luminance > 0.52 ? "#111827" : "#ffffff";
    }

    private static void setChoiceBoxDisplayTextColor(ChoiceBox<?> choiceBox, Color color) {
        Node display = choiceBox.lookup(".label");
        if (display instanceof Labeled label) {
            label.setTextFill(color);
        }
    }

    private static TreeItem<String> tree(String value) {
        return new TreeItem<>(value);
    }

    private static VBox card() {
        VBox box = new VBox(12);
        box.getStyleClass().add("card-section");
        box.getStyleClass().add("lab-demo-card");
        box.setFillWidth(true);
        return box;
    }

    private static Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-header");
        return label;
    }

    private static Label resultLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("info-label");
        label.getStyleClass().add("lab-demo-result");
        label.setWrapText(true);
        return label;
    }

    private static VBox labeled(String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        return new VBox(5, label, control);
    }

    private static TextArea logArea() {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(12);
        area.setPromptText("Worker output appears here.");
        return area;
    }

    private record PaperSample(String title, int year, String topic, int similarity) { }

    private static final class PaperAnalysisThread extends Thread {
        private final String paperTitle;
        private final java.util.function.Consumer<String> output;
        private final Runnable onFinish;

        private PaperAnalysisThread(String threadName, String paperTitle,
                                    java.util.function.Consumer<String> output, Runnable onFinish) {
            super(threadName);
            this.paperTitle = paperTitle;
            this.output = output;
            this.onFinish = onFinish;
        }

        @Override
        public void run() {
            try {
                for (int part = 1; part <= 5; part++) {
                    if (isInterrupted()) return;
                    output.accept(getName() + " analyzing " + paperTitle + " (section " + part + ")");
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ex) {
                        interrupt();
                        return;
                    }
                }
                output.accept(getName() + " completed " + paperTitle);
            } finally {
                onFinish.run();
            }
        }
    }

    private static final class Counter {
        private int value;
        private final Object lock = new Object();

        private void incrementUnsynchronized() {
            int current = value;
            value = current + 1;
        }

        private synchronized void incrementSynchronizedMethod() {
            value++;
        }

        private void incrementSynchronizedBlock() {
            synchronized (lock) {
                value++;
            }
        }
    }
}
