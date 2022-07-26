package com.vaadin.recipes.recipe.filetree;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.recipes.recipe.Metadata;
import com.vaadin.recipes.recipe.Recipe;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Route("file-tree")
@Metadata(
    howdoI = "Show files and folders recursively in a TreeGrid",
    description = "Learn how to display file system folders easily using a Vaadin TreeGrid."
)
public class FileTree extends Recipe {

    /*
     * This static code block only sets up some artificial file tree, for users to browse.
     * Normally, you would likely want to expose some existing directory tree.
     */
    static {
        try {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            File root = new File(tmpDir.getAbsolutePath() + File.separator + "ROOT");
            boolean createdRoot = root.mkdir();
            if (createdRoot){
                File subdir = new File(root.getAbsolutePath() + File.separator + "Sub-Directory");
                boolean createdSubDir = subdir.mkdir();
                if (createdSubDir) {
                    File four = new File(subdir.getAbsolutePath() + File.separator + "four");
                    four.createNewFile();
                }
            }
            File one = new File(root.getAbsolutePath() + File.separator + "one");
            one.createNewFile();
            File two = new File(root.getAbsolutePath() + File.separator + "two");
            two.createNewFile();
            File three = new File(root.getAbsolutePath() + File.separator + "three");
            three.createNewFile();
        }
        catch (IOException ioException){
            ioException.printStackTrace();
        }
    }
    private static final FileWrapper rootFile = new FileWrapper(new File(System.getProperty("java.io.tmpdir") + File.separator + "ROOT"));

    private static class FileWrapper implements Comparable<FileWrapper> {
        private File wrappedFile;
        public FileWrapper(File toBeWrapped){
            this.wrappedFile = toBeWrapped;
        }

        public boolean isDirectory(){
            return wrappedFile.isDirectory();
        }

        public String getName(){
            return this.wrappedFile.getName();
        }
        public void setName(String newName){
            String parent = wrappedFile.getParent();
            File renameTo;
            if (parent != null){
                renameTo = new File(parent + File.separatorChar + newName);
            }
            else{
                renameTo = new File(newName);
            }

            if (renameTo.exists()){
                System.err.println("File already exists: Could not rename '"+wrappedFile.getAbsolutePath()+"' to '"+renameTo.getAbsolutePath()+"'.");
                return;
            }

            if (wrappedFile.renameTo(renameTo)){
                // iff true, then renaming succeeded
                System.err.println("Successfully renamed '"+wrappedFile.getAbsolutePath()+"' to '"+renameTo.getAbsolutePath()+"'.");
                wrappedFile = renameTo;
            }
            else{
                System.err.println("Could not rename '"+wrappedFile.getAbsolutePath()+"' to '"+renameTo.getAbsolutePath()+"'.");
            }
        }

        public FileWrapper[] listFiles() {
            File[] listing = wrappedFile.listFiles();
            FileWrapper[] retVal = new FileWrapper[listing.length];
            for (int i=0; i<listing.length; i++){
                retVal[i] = new FileWrapper(listing[i]);
            }
            return retVal;
        }

        @Override
        public int compareTo(FileWrapper o) {
            return this.wrappedFile.compareTo(o.wrappedFile);
        }
    }

    private static class Tree<T> extends TreeGrid<T> {

        Tree(ValueProvider<T, ?> valueProvider) {
            Column<T> only = addHierarchyColumn(valueProvider);
            only.setAutoWidth(true);
        }
    }

    public FileTree() {
        Tree<FileWrapper> filesTree = new Tree<>(FileWrapper::getName);

        Binder<FileWrapper> binder = new Binder<>();
        Editor<FileWrapper> editor = filesTree.getEditor();
        editor.setBinder(binder);

        filesTree.setItems(Collections.singleton(rootFile), this::getFiles);
        filesTree.setWidthFull();
        filesTree.setHeight("300px");
        filesTree.setSelectionMode(Grid.SelectionMode.NONE);
        TextField editorTextField = new TextField();
        editorTextField.setWidthFull();
        filesTree
            .getColumns()
            .stream()
            .findFirst()
            .ifPresent(
                fileColumn -> {
                    fileColumn.setComparator(Comparator.naturalOrder());
                    GridSortOrder<FileWrapper> sortOrder = new GridSortOrder<>(fileColumn, SortDirection.ASCENDING);
                    filesTree.sort(Collections.singletonList(sortOrder));

                    fileColumn.setEditorComponent(editorTextField);

//                    filesTree
//                        .asSingleSelect()
//                        .addValueChangeListener(
//                            event -> {
//                                File file = event.getValue();
//                                if (file != null && file.isFile()) { // deselecting: file == null
//                                    // do something
//                                } else {
//                                    // don't do anything
//                                }
//                            }
//                        );
                }
            );
        filesTree.addItemDoubleClickListener(e -> {
            editor.editItem(e.getItem());
            Component editorComponent = e.getColumn().getEditorComponent();
            if (editorComponent instanceof Focusable) {
                ((Focusable<?>) editorComponent).focus();
            }
            editorTextField.setValue(e.getItem().getName());
        });
        editorTextField.getElement().addEventListener("keydown", event -> editor.cancel()).setFilter("event.key === 'Escape' || event.key === 'Esc'");

        editorTextField.addBlurListener(e -> editor.getItem().setName(editorTextField.getValue()));

        this.add(filesTree);
        setSizeFull();
    }

    private List<FileWrapper> getFiles(FileWrapper parent) {
        if (parent.isDirectory()) {
            FileWrapper[] list = parent.listFiles();
            if (list != null) {
                return Arrays.asList(list);
            }
        }

        return Collections.emptyList();
    }
}
