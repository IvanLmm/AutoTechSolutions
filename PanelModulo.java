package vista;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/** Base de todas las pantallas: utilidades comunes para formularios y tablas. */
public abstract class PanelModulo extends JPanel {

    protected PanelModulo() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    }

    /** Se invoca al mostrar la pantalla y después de operaciones que cambian los datos. */
    public abstract void actualizar();

    protected static JPanel crearFormulario(String titulo) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(titulo));
        return p;
    }

    /** Coloca "etiqueta + campo" en la cuadrícula del formulario (columna 0 o 1). */
    protected static void agregarCampo(JPanel form, int fila, int columna, String etiqueta, JComponent campo) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = fila;
        c.gridx = columna * 2;
        form.add(new JLabel(etiqueta), c);
        c.gridx = columna * 2 + 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(campo, c);
    }

    protected static DefaultTableModel modeloNoEditable(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    protected static void configurarTabla(JTable tabla) {
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(24);
        tabla.setFillsViewportHeight(true);
        tabla.getTableHeader().setReorderingAllowed(false);
    }

    /** Recarga un combo con nuevos elementos conservando la selección si sigue existiendo. */
    protected static <T> void recargar(JComboBox<T> combo, List<T> elementos) {
        Object seleccionado = combo.getSelectedItem();
        DefaultComboBoxModel<T> m = new DefaultComboBoxModel<>(new Vector<>(elementos));
        combo.setModel(m);
        if (seleccionado != null && m.getIndexOf(seleccionado) >= 0) {
            combo.setSelectedItem(seleccionado);
        }
    }

    protected static JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> accion.run());
        return b;
    }

    protected static void alCambiarTexto(JTextField campo, Runnable accion) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { accion.run(); }
            @Override public void removeUpdate(DocumentEvent e) { accion.run(); }
            @Override public void changedUpdate(DocumentEvent e) { accion.run(); }
        });
    }
}
