package com.example.airesearchassistant.controller;

import com.example.airesearchassistant.model.Collection;
import com.example.airesearchassistant.model.ResearchPaper;
import com.example.airesearchassistant.service.CollectionService;
import com.example.airesearchassistant.service.ExportService;
import com.example.airesearchassistant.service.PaperService;
import com.example.airesearchassistant.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * CollectionsController — Manage custom paper collections.
 * Left: collection list with New/Rename/Delete.
 * Right: table of papers in selected collection with Add/Remove.
 * Deleting a collection does NOT delete its papers.
 */
public class CollectionsController {

    // Left panel
    @FXML private ListView<Collection> collectionsListView;
    @FXML private TextField newCollectionNameField;
    @FXML private TextField newCollectionDescField;
    @FXML private Label statusLabel;
    @FXML private Button renameBtn;
    @FXML private Button deleteColBtn;

    // Right panel
    @FXML private Label collectionTitleLabel;
    @FXML private Label collectionDescLabel;
    @FXML private TableView<ResearchPaper> papersTable;
    @FXML private TableColumn<ResearchPaper, String> colTitle;
    @FXML private TableColumn<ResearchPaper, String> colAuthors;
    @FXML private TableColumn<ResearchPaper, String> colYear;
    @FXML private TableColumn<ResearchPaper, String> colTopic;
    @FXML private ComboBox<ResearchPaper> addPaperComboBox;
    @FXML private Button removePaperBtn;

    private final CollectionService collectionService = new CollectionService();
    private final PaperService paperService = new PaperService();
    private final ExportService exportService = new ExportService();

    private final ObservableList<ResearchPaper> papersInCollection = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupCollectionSelection();
        setupPaperTableSelection();
        loadCollections();
        loadAllPapersComboBox();
    }

    // -----------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------

    private void setupTableColumns() {
        if (colTitle != null) colTitle.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTitle()));
        if (colAuthors != null) colAuthors.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getAuthors()));
        if (colYear != null) colYear.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPublicationYear()));
        if (colTopic != null) colTopic.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTopic()));

        if (papersTable != null) {
            papersTable.setItems(papersInCollection);
        }
    }

    private void setupCollectionSelection() {
        if (collectionsListView == null) return;

        collectionsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSel = newVal != null;
            if (renameBtn != null) renameBtn.setDisable(!hasSel);
            if (deleteColBtn != null) deleteColBtn.setDisable(!hasSel);

            if (hasSel) {
                loadPapersForCollection(newVal);
                if (collectionTitleLabel != null) {
                    collectionTitleLabel.setText(newVal.getName() + " (" + newVal.getPaperCount() + " papers)");
                }
                if (collectionDescLabel != null) {
                    String desc = newVal.getDescription();
                    collectionDescLabel.setText(desc != null && !desc.isBlank() ? desc : "");
                }
            } else {
                papersInCollection.clear();
                if (collectionTitleLabel != null) collectionTitleLabel.setText("Select a collection");
                if (collectionDescLabel != null) collectionDescLabel.setText("");
            }
        });
    }

    private void setupPaperTableSelection() {
        if (papersTable == null || removePaperBtn == null) return;
        papersTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) ->
                removePaperBtn.setDisable(n == null));
    }

    // -----------------------------------------------------------------------
    // Data loading
    // -----------------------------------------------------------------------

    private void loadCollections() {
        List<Collection> collections = collectionService.getAllCollections();
        if (collectionsListView != null) {
            collectionsListView.setItems(FXCollections.observableArrayList(collections));
        }
        updateStatus(collections.size());
    }

    private void loadAllPapersComboBox() {
        if (addPaperComboBox == null) return;
        List<ResearchPaper> papers = paperService.getAllPapers();
        addPaperComboBox.setItems(FXCollections.observableArrayList(papers));
    }

    private void loadPapersForCollection(Collection col) {
        List<ResearchPaper> papers = collectionService.getPapersInCollection(col.getId());
        papersInCollection.setAll(papers);
        // Update paper count in model
        col.setPaperCount(papers.size());
    }

    private void updateStatus(int count) {
        if (statusLabel != null) {
            statusLabel.setText(count + " collection" + (count == 1 ? "" : "s"));
        }
    }

    // -----------------------------------------------------------------------
    // Collection actions
    // -----------------------------------------------------------------------

    @FXML
    private void handleCreate() {
        String name = newCollectionNameField != null ? newCollectionNameField.getText().trim() : "";
        if (name.isEmpty()) {
            AlertUtil.showWarning("Name Required", "Collection Name Missing",
                    "Please enter a name for the new collection.");
            return;
        }
        String desc = newCollectionDescField != null ? newCollectionDescField.getText().trim() : "";
        try {
            collectionService.createCollection(name, desc);
            if (newCollectionNameField != null) newCollectionNameField.clear();
            if (newCollectionDescField != null) newCollectionDescField.clear();
            loadCollections();
        } catch (SQLException e) {
            AlertUtil.showError("Database Error", "Could not create collection", e.getMessage());
        }
    }

    @FXML
    private void handleRename() {
        Collection selected = collectionsListView != null
                ? collectionsListView.getSelectionModel().getSelectedItem() : null;
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Rename Collection");
        dialog.setHeaderText("Rename: " + selected.getName());
        dialog.setContentText("New name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            String trimmed = newName.trim();
            if (trimmed.isEmpty()) {
                AlertUtil.showWarning("Invalid Name", "Empty Name", "Collection name cannot be empty.");
                return;
            }
            selected.setName(trimmed);
            try {
                collectionService.updateCollection(selected);
                loadCollections();
            } catch (SQLException e) {
                AlertUtil.showError("Database Error", "Could not rename collection", e.getMessage());
            }
        });
    }

    @FXML
    private void handleDeleteCollection() {
        Collection selected = collectionsListView != null
                ? collectionsListView.getSelectionModel().getSelectedItem() : null;
        if (selected == null) return;

        boolean confirmed = AlertUtil.showConfirmation(
                "Delete Collection",
                "Delete: " + selected.getName(),
                "This will remove the collection but NOT delete the papers it contains."
        );
        if (!confirmed) return;

        try {
            collectionService.deleteCollection(selected.getId());
            papersInCollection.clear();
            if (collectionTitleLabel != null) collectionTitleLabel.setText("Select a collection");
            loadCollections();
        } catch (SQLException e) {
            AlertUtil.showError("Database Error", "Could not delete collection", e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Paper-in-collection actions
    // -----------------------------------------------------------------------

    @FXML
    private void handleAddPaper() {
        Collection selected = collectionsListView != null
                ? collectionsListView.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            AlertUtil.showWarning("No Collection", "Select a Collection", "Select a collection on the left first.");
            return;
        }
        ResearchPaper paper = addPaperComboBox != null ? addPaperComboBox.getValue() : null;
        if (paper == null) {
            AlertUtil.showWarning("No Paper", "Select a Paper", "Select a paper from the dropdown to add.");
            return;
        }
        try {
            collectionService.addPaperToCollection(paper.getId(), selected.getId());
            loadPapersForCollection(selected);
            loadCollections(); // refresh count
        } catch (SQLException e) {
            AlertUtil.showError("Database Error", "Could not add paper to collection", e.getMessage());
        }
    }

    @FXML
    private void handleRemovePaper() {
        Collection selectedCol = collectionsListView != null
                ? collectionsListView.getSelectionModel().getSelectedItem() : null;
        ResearchPaper selectedPaper = papersTable != null
                ? papersTable.getSelectionModel().getSelectedItem() : null;

        if (selectedCol == null || selectedPaper == null) return;

        boolean confirmed = AlertUtil.showConfirmation(
                "Remove Paper",
                "Remove from Collection",
                "Remove \"" + selectedPaper.getTitle() + "\" from collection \"" + selectedCol.getName() + "\"?"
        );
        if (!confirmed) return;

        try {
            collectionService.removePaperFromCollection(selectedPaper.getId(), selectedCol.getId());
            loadPapersForCollection(selectedCol);
            loadCollections();
        } catch (SQLException e) {
            AlertUtil.showError("Database Error", "Could not remove paper", e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Export
    // -----------------------------------------------------------------------

    @FXML
    private void handleExport() {
        Collection selected = collectionsListView != null
                ? collectionsListView.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            AlertUtil.showWarning("No Collection Selected", "Select a Collection",
                    "Select a collection to export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Collection");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md"),
                new FileChooser.ExtensionFilter("Plain Text (*.txt)", "*.txt")
        );
        chooser.setInitialFileName(selected.getName().replaceAll("[^a-zA-Z0-9_]", "_"));

        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        List<ResearchPaper> papers = collectionService.getPapersInCollection(selected.getId());
        boolean asMarkdown = file.getName().endsWith(".md");

        try {
            exportService.exportCollection(selected, papers, file.toPath(), asMarkdown);
            AlertUtil.showInfo("Export Successful", "File Saved",
                    "Collection exported to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            AlertUtil.showError("Export Failed", "Could not write file", ex.getMessage());
        }
    }
}
