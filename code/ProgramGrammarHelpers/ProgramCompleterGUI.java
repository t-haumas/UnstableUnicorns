package code.ProgramGrammarHelpers;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import code.ProgramGrammarHelpers.Dependencies.SuggestionResultsList;

public class ProgramCompleterGUI {
    
    private final class LeftArrowDeletionAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
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
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }
    }

    private final ProgramCompleter suggestionGetter;
    private final Document draftTextDocument;
    private final JTextArea draftArea;
    private final JPopupMenu suggestionMenu;
    private final Robot robot;

    public ProgramCompleterGUI(ProgramCompleter suggestionGetter) {
        this.suggestionGetter = suggestionGetter;
        draftArea = new JTextArea();
        draftTextDocument = draftArea.getDocument();
        suggestionMenu = new JPopupMenu();
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }

    }

    public void start() {

        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());//UIManager.getCrossPlatformLookAndFeelClassName());
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
                if (! suggestionMenu.isVisible()) {
                    updateSuggestionBox();
                }
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
        draftArea.setLineWrap(true);
        draftArea.setWrapStyleWord(true);
        // THIS WAS For when I was trying to use an editable textarea.
        // draftArea.addCaretListener(new CaretListener() {

        //     @Override
        //     public void caretUpdate(CaretEvent e) {
        //         if (e.getDot() != draftArea.getDocument().getLength()) {
        //             draftArea.setCaretPosition(draftArea.getDocument().getLength());
        //         }
        //     }
            
        // });

        InputMap draftAreaInputMap = draftArea.getInputMap();
        ActionMap draftAreaActionMap = draftArea.getActionMap();

        KeyStroke cmdDelKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        draftAreaInputMap.put(cmdDelKeyStroke, "Cmd+Del");
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

        Action leftArrowDeletionAction = new LeftArrowDeletionAction();

        draftAreaInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "leftArrow");
        draftAreaActionMap.put("leftArrow", leftArrowDeletionAction);

        suggestionMenu.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "leftArrow");
        suggestionMenu.getActionMap().put("leftArrow", leftArrowDeletionAction);

        draftAreaInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "leftArrow");
        suggestionMenu.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "leftArrow");

        draftAreaInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "upArrow");
        draftAreaActionMap.put("upArrow", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                focusOnSuggestionsMenu();
                selectLastSuggestion();
            }

        });

        draftAreaInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "focusSuggestions");
        draftAreaInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "focusSuggestions");
        draftAreaActionMap.put("focusSuggestions", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                focusOnSuggestionsMenu();
            }

        });
            
        draftArea.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                handleKeyPressed(e);
            }

            public void handleKeyPressed(KeyEvent e) {
                if ('A' <= e.getKeyChar() && e.getKeyChar() <= 'Z' || 'a' <= e.getKeyChar() && e.getKeyChar() <= 'z') {
                    try {
                        draftArea.getDocument().insertString(draftArea.getDocument().getLength(), "" + e.getKeyChar(),
                                null);
                    } catch (BadLocationException e1) {
                        throw new RuntimeException(e1);
                    }
                    insertUpdate(e);
                }
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

            

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {}
            
        });

        mainPanel.add(draftArea);
        
        window.add(mainPanel);

        window.setVisible(true);

        updateSuggestionBox();

    }

    protected void selectLastSuggestion() {
        MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] { suggestionMenu, suggestionMenu.getSubElements()[suggestionMenu.getSubElements().length - 1] });
    }

    private void updateGivenDelete(String text) {
        updateSuggestionBox();
    }

    protected void focusOnSuggestionsMenu() {
        if (suggestionMenu.isVisible()) {
            suggestionMenu.requestFocusInWindow();
            MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] { suggestionMenu, suggestionMenu.getSubElements()[0] });
        }
    }

    private void updateSuggestionBox() {
        String currentText = getDraftText();
        SuggestionResultsList nextPossibilities = suggestionGetter.getPossibilities(currentText);
        if (nextPossibilities.size() == 0) {
            // Program is either complete or invalid.
            
            if (nextPossibilities.isComplete()) {
                //  Program is complete
                System.out.println("Complete program!!");
            } else {
                //  Program is invalid.
                System.out.println("Bad program, no suggestions.");
            }
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    suggestionMenu.setVisible(false);
                    draftArea.requestFocusInWindow();
                }
            });
            
        } else {
            showSuggestionBox(nextPossibilities);
        }
    }

    protected void click() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private void showSuggestionBox(SuggestionResultsList nextPossibilities) {
        suggestionMenu.removeAll();
        for (String possibility : nextPossibilities.getStringList()) {
            JMenuItem menuItem = new JMenuItem(possibility);

            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    insertTextToDraft(possibility);
                }
            });

            menuItem.addMenuKeyListener(new MenuKeyListener() {

                @Override
                public void menuKeyTyped(MenuKeyEvent e) {}

                @Override
                public void menuKeyPressed(MenuKeyEvent e) {
                    int keyCode = e.getKeyCode();
                    if ((keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_RIGHT)) {
                        System.out.println("pressed on menu");
                        MenuElement[] items = MenuSelectionManager.defaultManager().getSelectedPath();
                        if (items.length > 1) {
                            insertTextToDraft(((JMenuItem) items[1]).getText());
                            focusOnSuggestionsMenu();
                        }
                    }
                }

                @Override
                public void menuKeyReleased(MenuKeyEvent e) {}
                
            });

            suggestionMenu.add(menuItem);
        }

        // Show the suggestion box above the cursor
        try {
            Rectangle2D caretBounds = draftArea.modelToView2D(draftArea.getCaretPosition());
            suggestionMenu.show(draftArea, (int) caretBounds.getX(),
                    (int) (caretBounds.getY() + caretBounds.getHeight()));
            suggestionMenu.show(draftArea, Math.min((int) caretBounds.getX(), draftArea.getWidth() - suggestionMenu.getWidth()),
            (int) (caretBounds.getY() + caretBounds.getHeight()));
            draftArea.requestFocusInWindow();

        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertTextToDraft(String s) {
        draftArea.insert(s, draftArea.getCaretPosition());
        suggestionMenu.setVisible(false);
        updateSuggestionBox();
    }

    private String getDraftText() {
        try {
            return draftTextDocument.getText(0, draftTextDocument.getLength());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }


}
