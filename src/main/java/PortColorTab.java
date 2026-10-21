import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

final class PortColorTab extends JPanel {
    private final BurpExtender extension;
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextArea script;
    private final JLabel status = new JLabel();
    private final JButton applyHistory = new JButton();
    private final JButton createListeners = new JButton();
    private final List<Runnable> textUpdates = new ArrayList<>();
    private boolean spanish;

    PortColorTab(BurpExtender extension) {
        super(new BorderLayout(10, 10));
        this.extension = extension;
        spanish = "es".equals(extension.language());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        model = new DefaultTableModel(new String[]{"Listener port", "Label", "Selected HEX", "HTTP history highlight"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column < 3;
            }

            @Override
            public String getColumnName(int column) {
                return switch (column) {
                    case 0 -> tr("Listener port", "Puerto del listener");
                    case 1 -> tr("Label", "Etiqueta");
                    case 2 -> tr("Selected HEX", "HEX elegido");
                    case 3 -> tr("HTTP history highlight", "Color en HTTP history");
                    default -> super.getColumnName(column);
                };
            }
        };
        for (PortRule rule : extension.currentRules()) {
            model.addRow(new Object[]{rule.port(), rule.label(), rule.hex(), rule.color()});
        }
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(2).setCellRenderer(new SwatchRenderer(true));
        table.getColumnModel().getColumn(3).setCellRenderer(new SwatchRenderer(false));
        model.addTableModelListener(event -> {
            if (event.getColumn() == 2 && event.getFirstRow() >= 0) {
                int row = event.getFirstRow();
                try {
                    model.setValueAt(ColorPalette.nearestNative(
                            String.valueOf(model.getValueAt(row, 2))), row, 3);
                } catch (IllegalArgumentException error) {
                    model.setValueAt(tr("Invalid HEX", "HEX inválido"), row, 3);
                }
            }
        });

        JPanel rulesPanel = new JPanel(new BorderLayout(6, 6));
        rulesPanel.add(label("One rule per port. The HEX is shown here; HTTP history uses the nearest native highlight.",
                "Una regla por puerto. El HEX se muestra aquí; HTTP history usa el resaltado nativo más cercano."),
                BorderLayout.NORTH);
        rulesPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton add = button("Add", "Añadir");
        add.addActionListener(event -> {
            model.addRow(new Object[]{"", "", "#0000FF", "BLUE"});
            int row = model.getRowCount() - 1;
            table.changeSelection(row, 0, false, false);
            table.editCellAt(row, 0);
        });
        JButton addPwnFox = button("Add PwnFox 8081–8088", "Añadir PwnFox 8081–8088");
        addPwnFox.addActionListener(event -> addPwnFoxDefaults());
        JButton pick = button("Choose color...", "Elegir color...");
        pick.addActionListener(event -> pickColor());
        JButton remove = button("Remove", "Eliminar");
        remove.addActionListener(event -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                model.removeRow(table.convertRowIndexToModel(row));
            }
        });
        JButton save = button("Save rules", "Guardar reglas");
        save.addActionListener(event -> saveRules());
        localize(applyHistory, "Color existing history", "Colorear historial existente");
        localize(createListeners, "Create listeners 8081–8088", "Crear listeners 8081–8088");
        applyHistory.addActionListener(event -> applyHistory());
        createListeners.addActionListener(event -> createPwnFoxListeners());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(add);
        buttons.add(addPwnFox);
        buttons.add(createListeners);
        buttons.add(pick);
        buttons.add(remove);
        buttons.add(save);
        buttons.add(applyHistory);
        rulesPanel.add(buttons, BorderLayout.SOUTH);

        script = new JTextArea(9, 60);
        script.setEditable(false);
        script.setLineWrap(false);
        script.setText(BambdaGenerator.generate(extension.currentRules()));

        JPanel columnPanel = new JPanel(new BorderLayout(6, 6));
        columnPanel.add(label("'Session' column: prepare the Bambda, then enable it in Proxy > HTTP history > ⋮ > Add custom column.",
                "Columna 'Session': prepara el Bambda y actívalo desde Proxy > HTTP history > menú ⋮ > Add custom column."),
                BorderLayout.NORTH);
        columnPanel.add(new JScrollPane(script), BorderLayout.CENTER);
        JButton prepareColumn = button("Prepare Session column", "Preparar columna Session");
        prepareColumn.addActionListener(event -> prepareSessionColumn());
        JButton copy = button("Copy column Bambda", "Copiar Bambda de la columna");
        copy.addActionListener(event -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(script.getText()), null);
            status.setText(tr("Bambda copied. Paste it into the custom HTTP history column.",
                    "Bambda copiado. Pégalo en la columna personalizada de HTTP history."));
        });
        JPanel columnButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        columnButtons.add(prepareColumn);
        columnButtons.add(copy);
        columnPanel.add(columnButtons, BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 12));
        center.add(rulesPanel);
        center.add(columnPanel);
        add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(label("The exact HEX is a visual reference; Burp applies one of its nine native colors. The column must be enabled manually in Burp Professional.",
                "El HEX exacto es una referencia visual; Burp aplica uno de sus nueve colores nativos. Burp Professional requiere activar la columna manualmente."),
                BorderLayout.NORTH);
        footer.add(label("After changing labels, prepare and load the column again. Recoloring history replaces matching manual highlights.",
                "Tras cambiar etiquetas, vuelve a preparar y cargar la columna. Recolorear el historial reemplaza resaltados manuales coincidentes."),
                BorderLayout.CENTER);
        footer.add(status, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);

        JPanel languagePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        languagePanel.add(label("Language", "Idioma"));
        JComboBox<String> languageSelector = new JComboBox<>(new String[]{"English", "Español"});
        languageSelector.setSelectedIndex(spanish ? 1 : 0);
        languageSelector.addActionListener(event -> {
            boolean selectedSpanish = languageSelector.getSelectedIndex() == 1;
            try {
                extension.saveLanguage(selectedSpanish ? "es" : "en");
                spanish = selectedSpanish;
                applyLanguage();
                status.setText(tr("Language set to English.", "Idioma cambiado a español."));
            } catch (RuntimeException error) {
                languageSelector.setSelectedIndex(spanish ? 1 : 0);
                status.setText(tr("Could not save language: ", "No se pudo guardar el idioma: ")
                        + error.getMessage());
            }
        });
        languagePanel.add(languageSelector);
        add(languagePanel, BorderLayout.NORTH);
        applyLanguage();
        status.setText(tr("Configure the rules and click Save rules.",
                "Configura las reglas y pulsa Guardar reglas."));
    }

    private String tr(String english, String spanishText) {
        return spanish ? spanishText : english;
    }

    private JLabel label(String english, String spanishText) {
        JLabel value = new JLabel();
        textUpdates.add(() -> value.setText(tr(english, spanishText)));
        return value;
    }

    private JButton button(String english, String spanishText) {
        JButton value = new JButton();
        localize(value, english, spanishText);
        return value;
    }

    private void localize(JButton button, String english, String spanishText) {
        textUpdates.add(() -> button.setText(tr(english, spanishText)));
    }

    private void applyLanguage() {
        textUpdates.forEach(Runnable::run);
        for (int column = 0; column < model.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setHeaderValue(model.getColumnName(column));
        }
        for (int row = 0; row < model.getRowCount(); row++) {
            String value = String.valueOf(model.getValueAt(row, 3));
            if ("Invalid HEX".equals(value) || "HEX inválido".equals(value)) {
                model.setValueAt(tr("Invalid HEX", "HEX inválido"), row, 3);
            }
        }
        table.getTableHeader().repaint();
    }

    private String errorMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null || spanish) {
            return message == null ? error.toString() : message;
        }
        if (message.startsWith("Usa un HEX #RRGGBB (o #RGB): ")) {
            return "Use HEX #RRGGBB (or #RGB): "
                    + message.substring("Usa un HEX #RRGGBB (o #RGB): ".length());
        }
        if (message.startsWith("Puerto repetido: ")) {
            return "Duplicate port: " + message.substring("Puerto repetido: ".length());
        }
        if (message.startsWith("Color de Burp no válido: ")) {
            return "Invalid Burp color: " + message.substring("Color de Burp no válido: ".length());
        }
        return switch (message) {
            case "El puerto debe estar entre 1 y 65535." -> "Port must be between 1 and 65535.";
            case "Etiqueta y color HEX son obligatorios." -> "Label and HEX color are required.";
            case "Burp rechazó el Bambda de la columna." -> "Burp rejected the column Bambda.";
            case "Burp no exportó la sección proxy del proyecto." -> "Burp did not export the project's proxy section.";
            case "Burp no exportó la lista de proxy listeners." -> "Burp did not export the proxy listener list.";
            case "La configuración contiene un listener inválido." -> "The configuration contains an invalid listener.";
            case "La configuración contiene un listener sin puerto válido." -> "The configuration contains a listener without a valid port.";
            default -> message;
        };
    }

    private void pickColor() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            status.setText(tr("Select a rule before choosing its color.",
                    "Selecciona primero una regla para elegir su color."));
            return;
        }
        row = table.convertRowIndexToModel(row);
        Color initial;
        try {
            initial = ColorPalette.asColor(String.valueOf(model.getValueAt(row, 2)));
        } catch (IllegalArgumentException error) {
            initial = Color.BLUE;
        }
        Color selected = JColorChooser.showDialog(this,
                tr("Container color", "Color del contenedor"), initial);
        if (selected != null) {
            model.setValueAt(String.format("#%02X%02X%02X",
                    selected.getRed(), selected.getGreen(), selected.getBlue()), row, 2);
            status.setText(tr("Color selected. Click Save rules to apply it to new traffic.",
                    "Color elegido. Pulsa Guardar reglas para aplicarlo al tráfico nuevo."));
        }
    }

    private boolean saveRules() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        try {
            List<PortRule> updated = readRules();
            extension.saveRules(updated);
            for (int row = 0; row < updated.size(); row++) {
                model.setValueAt(updated.get(row).hex(), row, 2);
            }
            refreshScript(updated);
            status.setText(tr("Saved " + updated.size()
                            + " rules. To update earlier rows, click Color existing history.",
                    "Guardadas " + updated.size()
                            + " reglas. Para filas anteriores, pulsa Colorear historial existente."));
            return true;
        } catch (RuntimeException error) {
            status.setText(tr("Could not save: ", "No se guardó: ") + errorMessage(error));
            return false;
        }
    }

    private void prepareSessionColumn() {
        if (!saveRules()) {
            return;
        }
        try {
            extension.importSessionColumn(script.getText());
            status.setText(tr("Session Bambda imported. Enable the column in HTTP history > ⋮ > Add custom column.",
                    "Bambda Session importado. Activa la columna en HTTP history > ⋮ > Add custom column."));
            JOptionPane.showMessageDialog(this,
                    tr("The Bambda is in Burp's library.\n\n"
                                    + "1. Open Proxy > HTTP history.\n"
                                    + "2. Open the ⋮ menu above the table and choose Add custom column.\n"
                                    + "   The column header menu only lists existing columns.\n"
                                    + "3. Click Load and select PwnFoxColor Session.\n"
                                    + "4. Enter Session in Column header and click Apply & close.",
                            "El Bambda está en la biblioteca de Burp.\n\n"
                                    + "1. Abre Proxy > HTTP history.\n"
                                    + "2. Pulsa el menú ⋮ de arriba de la tabla y elige Add custom column.\n"
                                    + "   El menú de casillas del encabezado solo muestra columnas existentes.\n"
                                    + "3. Pulsa Load y selecciona PwnFoxColor Session.\n"
                                    + "4. En Column header escribe Session y pulsa Apply & close."),
                    tr("Enable Session column", "Activar columna Session"), JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException error) {
            extension.logError("No se pudo importar el Bambda Session", error);
            status.setText(tr("Could not prepare the Session column: ",
                    "No se pudo preparar la columna Session: ") + errorMessage(error));
            JOptionPane.showMessageDialog(this,
                    tr("Could not import the Bambda: ", "No se pudo importar el Bambda: ")
                            + errorMessage(error)
                            + tr("\nYou can copy the column Bambda and paste it into Add custom column.",
                            "\nPuedes usar Copiar Bambda de la columna y pegarlo en Add custom column."),
                    tr("Session column", "Columna Session"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addPwnFoxDefaults() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        try {
            List<PortRule> current = readRules();
            List<PortRule> missing = PwnFoxDefaults.missingFrom(current);
            if (missing.isEmpty()) {
                status.setText(tr("PwnFox ports 8081–8088 already have rules; nothing changed.",
                        "Los puertos PwnFox 8081–8088 ya tienen reglas; no se cambió ninguna."));
                return;
            }
            List<PortRule> updated = new ArrayList<>(current);
            updated.addAll(missing);
            extension.saveRules(updated);
            for (PortRule rule : missing) {
                model.addRow(new Object[]{rule.port(), rule.label(), rule.hex(), rule.color()});
            }
            refreshScript(updated);
            status.setText(tr("Added and saved " + missing.size()
                            + " PwnFox colors. Existing port rules were kept.",
                    "Añadidos y guardados " + missing.size()
                            + " colores PwnFox. Se respetaron los puertos ya configurados."));
        } catch (RuntimeException error) {
            status.setText(tr("Could not add PwnFox colors: ",
                    "No se añadieron los colores PwnFox: ") + errorMessage(error));
        }
    }

    private List<PortRule> readRules() {
        List<PortRule> updated = new ArrayList<>();
        for (int row = 0; row < model.getRowCount(); row++) {
            String portText = String.valueOf(model.getValueAt(row, 0)).trim();
            int port = Integer.parseInt(portText);
            String label = String.valueOf(model.getValueAt(row, 1));
            String hex = String.valueOf(model.getValueAt(row, 2));
            updated.add(new PortRule(port, label, hex));
        }
        return updated;
    }

    private void refreshScript(List<PortRule> rules) {
        script.setText(BambdaGenerator.generate(rules));
        script.setCaretPosition(0);
    }

    private void applyHistory() {
        applyHistory.setEnabled(false);
        status.setText(tr("Coloring existing history entries...",
                "Coloreando entradas existentes del historial..."));
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return extension.applyToExistingHistory();
            }

            @Override
            protected void done() {
                applyHistory.setEnabled(true);
                try {
                    status.setText(tr("Colored " + get() + " existing entries.",
                            "Coloreadas " + get() + " entradas existentes."));
                } catch (Exception error) {
                    extension.logError("Error al colorear el historial", error);
                    status.setText(tr("Could not color history: ",
                            "Error al colorear el historial: ") + errorMessage(error));
                }
            }
        }.execute();
    }

    private void createPwnFoxListeners() {
        createListeners.setEnabled(false);
        status.setText(tr("Adding PwnFox listeners at 127.0.0.1:8081–8088...",
                "Añadiendo listeners PwnFox en 127.0.0.1:8081–8088..."));
        new SwingWorker<BurpExtender.ListenerCreationResult, Void>() {
            @Override
            protected BurpExtender.ListenerCreationResult doInBackground() {
                return extension.createPwnFoxListeners();
            }

            @Override
            protected void done() {
                createListeners.setEnabled(true);
                try {
                    BurpExtender.ListenerCreationResult result = get();
                    if (result.requested() == 0) {
                        status.setText(tr("Listeners 8081–8088 already exist; check that they are Running.",
                                "Los listeners 8081–8088 ya existen; comprueba que estén en Running."));
                    } else if (result.missingAfterImport().isEmpty()) {
                        status.setText(tr("Configured " + result.requested()
                                        + " new listeners. Check that they are Running in Proxy settings.",
                                "Configurados " + result.requested()
                                        + " listeners nuevos. Comprueba que figuren como Running en Proxy settings."));
                    } else {
                        status.setText(tr("Burp did not confirm these ports: ",
                                "Burp no confirmó estos puertos: ") + result.missingAfterImport()
                                + tr(". Check Proxy settings.", ". Revisa Proxy settings."));
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    status.setText(tr("Listener creation was interrupted.",
                            "Se interrumpió la creación de listeners."));
                } catch (ExecutionException error) {
                    Throwable cause = error.getCause();
                    extension.logError("Error al crear listeners PwnFox", cause);
                    status.setText(tr("Could not create all listeners: ",
                            "No se crearon todos los listeners: ") + errorMessage(cause));
                }
            }
        }.execute();
    }

    private final class SwatchRenderer extends DefaultTableCellRenderer {
        private final boolean exact;

        SwatchRenderer(boolean exact) {
            this.exact = exact;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            String text = String.valueOf(value);
            setText(text);
            try {
                Color color = exact ? ColorPalette.asColor(text) : extension.nativeColor(text);
                setIcon(new ColorSwatch(color));
            } catch (IllegalArgumentException error) {
                setIcon(null);
            }
            return this;
        }
    }

    private record ColorSwatch(Color color) implements Icon {
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            graphics.setColor(color);
            graphics.fillRect(x + 1, y + 1, 13, 13);
            graphics.setColor(Color.DARK_GRAY);
            graphics.drawRect(x, y, 15, 15);
        }

        @Override
        public int getIconWidth() {
            return 21;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }
}
