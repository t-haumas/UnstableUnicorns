package code.ProgramGrammarHelpers;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import code.ProgramGrammarHelpers.Dependencies.Grammar;
import code.ProgramGrammarHelpers.Dependencies.SuggestionResultsList;

public class ProgramCompleterGUI {
    
    private final ProgramCompleter suggestionGetter;
    private final Document draftTextDocument;
    private final JTextArea draftArea;
    private final JPopupMenu suggestionMenu;

    public ProgramCompleterGUI(ProgramCompleter suggestionGetter) {
        this.suggestionGetter = suggestionGetter;
        draftArea = new JTextArea();
        draftTextDocument = draftArea.getDocument();
        suggestionMenu = new JPopupMenu();

    }

    public void start() {
        //TODO: fix parenthesis issue. (())
        //TODO: make it not scrollable, make it wrap.
        //TODO: fix faster backspacing.
        //TODO: disable tips disappearing.

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        
        JFrame window = new JFrame("Program Completer");
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setSize(400, 400);

        draftArea.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            private void maybeShowPopup(MouseEvent e) {
                updateSuggestionBox();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                System.out.println("Mouse exited window!");
            }
        });

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        
        draftArea.setEditable(false);
        draftArea.setWrapStyleWord(true);


        draftArea.addCaretListener(new CaretListener() {

            @Override
            public void caretUpdate(CaretEvent e) {
                if (e.getDot() != draftArea.getDocument().getLength()) {
                    draftArea.setCaretPosition(draftArea.getDocument().getLength());
                }
            }
            
        });

        InputMap draftAreaInputMap = draftArea.getInputMap();
        KeyStroke cmdDelKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        draftAreaInputMap.put(cmdDelKeyStroke, "Cmd+Del");
        ActionMap draftAreaActionMap = draftArea.getActionMap();

        draftAreaActionMap.put("Cmd+Del", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                int caretOffset = draftArea.getCaretPosition();
                int lineNumber = 0;
                try {
                    lineNumber = draftArea.getLineOfOffset(caretOffset);
                    int startOffset = draftArea.getLineStartOffset(lineNumber);
                    int endOffset = draftArea.getLineEndOffset(lineNumber);
                    draftArea.replaceRange("", startOffset, endOffset);
                } catch (BadLocationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
            
        draftArea.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                handleKeyPressed(e);
            }

            public void handleKeyPressed(KeyEvent e) {
                try {
                    draftArea.getDocument().insertString(draftArea.getDocument().getLength(), "" + e.getKeyChar(), null);
                } catch (BadLocationException e1) {
                    throw new RuntimeException(e1);
                }
                insertUpdate(e);
            }

            public void insertUpdate(KeyEvent e) {
                Runnable update = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            updateGivenNewText(draftArea.getText(0, draftArea.getDocument().getLength()));
                        } catch (BadLocationException e1) {
                            e1.printStackTrace();
                        }
                    }
                };
                SwingUtilities.invokeLater(update);
            }

            private void updateGivenNewText(String text) {
                try {
                    System.out.println("new text!");
                    List<String> nextPossibilities = suggestionGetter.getPossibilities(text).getStringList();
                    System.out.println(nextPossibilities);
                    if (nextPossibilities.size() == 1) {
                        draftArea.getDocument().insertString(draftArea.getDocument().getLength(), nextPossibilities.get(0), null);
                        draftArea.setCaretPosition(draftArea.getDocument().getLength());
                    }
                    updateSuggestionBox();
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }
            }

            private void updateGivenDelete(String text) {
                updateSuggestionBox();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    System.out.println("Left pressed; change this to remove until a new suggestion is possible.");
                    try {
                        draftArea.getDocument().remove(draftArea.getDocument().getLength() - 1, 1);
                        Runnable update = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    updateGivenDelete(draftArea.getText(0, draftArea.getDocument().getLength()));
                                } catch (BadLocationException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        };
                        SwingUtilities.invokeLater(update);
                    } catch (BadLocationException e1) {}
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    // Right arrow key pressed
                    System.out.println("Right arrow key pressed");
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
            
        });

        mainPanel.add(draftArea);
        
        window.add(mainPanel);

        window.setVisible(true);

        updateSuggestionBox();
    }

    private void updateSuggestionBox() {
        String currentText = getDraftText();
        if (currentText.startsWith("shuffle.")) {
            System.out.println("here");
        }
        SuggestionResultsList nextPossibilities = suggestionGetter.getPossibilities(currentText);
        if (nextPossibilities.size() == 0) {
            // Program is either complete or invalid.
            suggestionMenu.setVisible(false);
            if (nextPossibilities.isComplete()) {
                //  Program is complete
                System.out.println("Complete program!!");
            } else {
                //  Program is invalid.
                System.out.println("Bad program, no suggestions.");
            }
        } else {
            showSuggestionBox(nextPossibilities);
        }
    }

    private void showSuggestionBox(SuggestionResultsList nextPossibilities) {
        suggestionMenu.removeAll();
        for (String possibility : nextPossibilities.getStringList()) {
            JMenuItem menuItem = new JMenuItem(possibility);

            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    draftArea.insert(possibility, draftArea.getCaretPosition());
                    suggestionMenu.setVisible(false);
                    updateSuggestionBox();
                }
            });

            suggestionMenu.add(menuItem);
        }

        // Show the suggestion box above the cursor
        try {
            Rectangle2D caretBounds = draftArea.modelToView2D(draftArea.getCaretPosition());
            suggestionMenu.show(draftArea, (int) caretBounds.getX(),
                    (int) (caretBounds.getY() + caretBounds.getHeight()));
            draftArea.requestFocus();

        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private String getDraftText() {
        try {
            return draftTextDocument.getText(0, draftTextDocument.getLength());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }


}
