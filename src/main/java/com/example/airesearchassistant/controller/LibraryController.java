package com.example.airesearchassistant.controller;

import com.example.airesearchassistant.model.ResearchPaper;
import com.example.airesearchassistant.service.ExportService;
import com.example.airesearchassistant.service.PaperService;
import com.example.airesearchassistant.util.AlertUtil;
import com.example.airesearchassistant.util.Constants;
import com.example.airesearchassistant.util.SceneManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * LibraryController — Full CRUD catalog for research papers.
 * Supports dynamic searching, topic and year filtering, favorite toggling,
 * viewing details, editing, deleting, launching PDFs, and exporting.
 */
public class LibraryController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> topicFilterComboBox;
    @FXML private ComboBox<String> yearFilterComboBox;
    @FXML private CheckBox favoritesCheckBox;
    @FXML private Label libraryStatusLabel;

    @FXML private TableView<ResearchPaper> papersTable;
    @FXML private TableColumn<ResearchPaper, Boolean> colFavorite;
    @FXML private TableColumn<ResearchPaper, String> colTitle;
    @FXML private TableColumn<ResearchPaper, String> colAuthors;
    @FXML private TableColumn<ResearchPaper, String> colYear;
    @FXML private TableColumn<ResearchPaper, String> colTopic;
    @FXML private TableColumn<ResearchPaper, String> colAnalyzed;
    @FXML private TableColumn<ResearchPaper, Void> colActions;

    @FXML private Button viewButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button openPdfButton;

    @FXML private Label paginationLabel;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;

    private final PaperService paperService = new PaperService();
    private final ExportService exportService = new ExportService();
    private final ObservableList<ResearchPaper> masterData = FXCollections.observableArrayList();
    private final ObservableList<ResearchPaper> currentPageData = FXCollections.observableArrayList();

    private int currentPage = 1;
    private static final int PAGE_SIZE = 50;
    public static boolean filterFavoritesInitial = false;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupTableSelection();
        loadFilterDropdowns();

        if (favoritesCheckBox != null) {
            if (filterFavoritesInitial) {
                favoritesCheckBox.setSelected(true);
                filterFavoritesInitial = false;
            }
            favoritesCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        }
        loadPapers();
        if (topicFilterComboBox != null) {
            topicFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        }
        if (yearFilterComboBox != null) {
            yearFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        }
    }

    private void setupTableColumns() {
        colFavorite.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().isFavorite()));
        colFavorite.setCellFactory(col -> new TableCell<>() {
            private final Button starBtn = new Button();
            {
                starBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 0;");
                starBtn.setOnAction(e -> {
                    ResearchPaper paper = getTableView().getItems().get(getIndex());
                    try {
                        boolean newFav = paperService.toggleFavorite(paper.getId());
                        paper.setFavorite(newFav);
                        updateStar(newFav);
                    } catch (SQLException ex) {
                        AlertUtil.showError("Database Error", "Failed to update favorite status", ex.getMessage());
                    }
                });
            }

            private void updateStar(boolean fav) {
                if (fav) {
                    starBtn.setText("★");
                    starBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f9e2af; -fx-font-size: 16px; -fx-cursor: hand;");
                } else {
                    starBtn.setText("☆");
                    starBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c7086; -fx-font-size: 16px; -fx-cursor: hand;");
                }
            }

            @Override
            protected void updateItem(Boolean isFav, boolean empty) {
                super.updateItem(isFav, empty);
                if (empty || isFav == null) {
                    setGraphic(null);
                } else {
                    updateStar(isFav);
                    setGraphic(starBtn);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colAuthors.setCellValueFactory(new PropertyValueFactory<>("authors"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("publicationYear"));
        colTopic.setCellValueFactory(new PropertyValueFactory<>("topic"));

        colAnalyzed.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isAnalyzed() ? "✓ Analyzed" : "—"));
        colAnalyzed.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setStyle("✓ Analyzed".equals(item)
                            ? "-fx-text-fill: #a6e3a1; -fx-font-weight: bold;"
                            : "-fx-text-fill: #6c7086;");
                }
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("👁 View");
            private final Button editBtn = new Button("✏ Edit");
            private final HBox pane = new HBox(6, viewBtn, editBtn);
            {
                pane.setAlignment(Pos.CENTER);
                viewBtn.setStyle("-fx-background-color: #313244; -fx-text-fill: #cdd6f4; -fx-font-size: 11px; -fx-padding: 3 8; -fx-cursor: hand;");
                editBtn.setStyle("-fx-background-color: #313244; -fx-text-fill: #cdd6f4; -fx-font-size: 11px; -fx-padding: 3 8; -fx-cursor: hand;");
                viewBtn.setOnAction(e -> openPaperDetails(getTableView().getItems().get(getIndex())));
                editBtn.setOnAction(e -> openPaperEdit(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        Label placeholder = new Label("No research papers found. Click 'Add Paper' or import a PDF to get started.");
        placeholder.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 14px; -fx-padding: 24;");
        papersTable.setPlaceholder(placeholder);
    }

    private void setupTableSelection() {
        papersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean hasSelection = (newSel != null);
            if (viewButton != null) viewButton.setDisable(!hasSelection);
            if (editButton != null) editButton.setDisable(!hasSelection);
            if (deleteButton != null) deleteButton.setDisable(!hasSelection);
            if (openPdfButton != null) {
                boolean hasPdf = hasSelection && newSel.getFilePath() != null && !newSel.getFilePath().trim().isEmpty();
                openPdfButton.setDisable(!hasPdf);
            }
        });

        papersTable.setRowFactory(tv -> {
            TableRow<ResearchPaper> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openPaperDetails(row.getItem());
                }
            });
            return row;
        });
    }

    private void loadFilterDropdowns() {
        if (topicFilterComboBox != null) {
            topicFilterComboBox.getItems().clear();
            topicFilterComboBox.getItems().add("All Topics");
            topicFilterComboBox.getItems().addAll(paperService.getAvailableTopics());
            topicFilterComboBox.setValue("All Topics");
        }
        if (yearFilterComboBox != null) {
            yearFilterComboBox.getItems().clear();
            yearFilterComboBox.getItems().add("All Years");
            yearFilterComboBox.getItems().addAll(paperService.getAvailableYears());
            yearFilterComboBox.setValue("All Years");
        }
    }

    public void loadPapers() { handleSearch(); }

    @FXML
    public void handleSearch() {
        String query = searchField != null ? searchField.getText() : null;
        String topic = topicFilterComboBox != null ? topicFilterComboBox.getValue() : null;
        String year = yearFilterComboBox != null ? yearFilterComboBox.getValue() : null;
        boolean favOnly = favoritesCheckBox != null && favoritesCheckBox.isSelected();

        List<ResearchPaper> results = paperService.searchPapers(query, topic, year, favOnly);
        masterData.setAll(results);
        currentPage = 1;
        updatePagedView();

        if (libraryStatusLabel != null) {
            int total = masterData.size();
            libraryStatusLabel.setText(String.format("Showing %d %s", total, total == 1 ? "paper" : "papers"));
        }
    }

    private void updatePagedView() {
        int total = masterData.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > totalPages) currentPage = totalPages;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, total);

        if (fromIndex <= toIndex && fromIndex < total) {
            currentPageData.setAll(masterData.subList(fromIndex, toIndex));
        } else {
            currentPageData.clear();
        }
        papersTable.setItems(currentPageData);

        if (paginationLabel != null) {
            paginationLabel.setText(String.format("Page %d of %d (%d items)", currentPage, totalPages, total));
        }
        if (prevPageButton != null) prevPageButton.setDisable(currentPage <= 1);
        if (nextPageButton != null) nextPageButton.setDisable(currentPage >= totalPages);
    }

    @FXML private void handlePrevPage() { if (currentPage > 1) { currentPage--; updatePagedView(); } }

    @FXML
    private void handleNextPage() {
        int totalPages = (int) Math.ceil((double) masterData.size() / PAGE_SIZE);
        if (currentPage < totalPages) { currentPage++; updatePagedView(); }
    }

    @FXML private void handleRefresh() { loadFilterDropdowns(); loadPapers(); }

    @FXML private void handleAddPaper() { SceneManager.getInstance().load(Constants.FXML_ADD_PAPER); }

    @FXML
    private void handleExport() {
        List<ResearchPaper> papers = new ArrayList<>(masterData);
        if (papers.isEmpty()) {
            AlertUtil.showWarning("Nothing to Export", "No Papers",
                    "There are no papers to export with the current filter.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Library");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md"),
                new FileChooser.ExtensionFilter("Plain Text (*.txt)", "*.txt")
        );
        chooser.setInitialFileName("research_library");
        File file = chooser.showSaveDialog(null);
        if (file == null) return;
        try {
            exportService.exportLibrary(papers, file.toPath(), file.getName().endsWith(".md"));
            AlertUtil.showInfo("Export Successful", "Library Exported",
                    papers.size() + " papers exported to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            AlertUtil.showError("Export Failed", "Could not write file", ex.getMessage());
        }
    }

    @FXML
    private void handleViewSelected() {
        ResearchPaper selected = papersTable.getSelectionModel().getSelectedItem();
        if (selected != null) openPaperDetails(selected);
    }

    @FXML
    private void handleEditSelected() {
        ResearchPaper selected = papersTable.getSelectionModel().getSelectedItem();
        if (selected != null) openPaperEdit(selected);
    }

    @FXML
    private void handleDeleteSelected() {
        ResearchPaper selected = papersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        boolean confirmed = AlertUtil.showConfirmation("Confirm Deletion", "Delete Paper",
                "Are you sure you want to permanently delete:\n\"" + selected.getTitle() + "\"?");
        if (confirmed) {
            try {
                paperService.deletePaper(selected.getId());
                handleSearch();
                AlertUtil.showInfo("Paper Deleted", "Success", "Paper removed successfully.");
            } catch (SQLException e) {
                AlertUtil.showError("Database Error", "Failed to delete paper", e.getMessage());
            }
        }
    }

    @FXML
    private void handleOpenPdfSelected() {
        ResearchPaper selected = papersTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getFilePath() == null || selected.getFilePath().trim().isEmpty()) {
            AlertUtil.showWarning("PDF Not Found", "No File Attached", "This paper does not have an attached PDF file.");
            return;
        }
        File file = new File(selected.getFilePath());
        if (!file.exists()) {
            AlertUtil.showError("File Not Found", "PDF file does not exist",
                    "Could not locate file at path:\n" + selected.getFilePath());
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            AlertUtil.showWarning("Not Supported", "Desktop Operation Not Supported",
                    "The current platform does not support launching external applications.");
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            AlertUtil.showError("Error Opening PDF", "Could not open document", e.getMessage());
        }
    }

    private void openPaperDetails(ResearchPaper paper) {
        Object controller = SceneManager.getInstance().load(Constants.FXML_PAPER_DETAILS);
        if (controller instanceof PaperDetailsController detailsCtrl) {
            detailsCtrl.setPaper(paper);
        }
    }

    private void openPaperEdit(ResearchPaper paper) {
        Object controller = SceneManager.getInstance().load(Constants.FXML_ADD_PAPER);
        if (controller instanceof AddPaperController addCtrl) {
            addCtrl.setPaperForEdit(paper);
        }
    }
}
