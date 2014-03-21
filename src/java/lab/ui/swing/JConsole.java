/*****************************************************************************
 *                                                                           *
 *  This file is part of the BeanShell Java Scripting distribution.          *
 *  Documentation and updates may be found at http://www.beanshell.org/      *
 *                                                                           *
 *  Sun Public License Notice:                                               *
 *                                                                           *
 *  The contents of this file are subject to the Sun Public License Version  *
 *  1.0 (the "License"); you may not use this file except in compliance with *
 *  the License. A copy of the License is available at http://www.sun.com    *
 *                                                                           *
 *  The Original Code is BeanShell. The Initial Developer of the Original    *
 *  Code is Pat Niemeyer. Portions created by Pat Niemeyer are Copyright     *
 *  (C) 2000.  All Rights Reserved.                                          *
 *                                                                           *
 *  GNU Public License Notice:                                               *
 *                                                                           *
 *  Alternatively, the contents of this file may be used under the terms of  *
 *  the GNU Lesser General Public License (the "LGPL"), in which case the    *
 *  provisions of LGPL are applicable instead of those above. If you wish to *
 *  allow use of your version of this file only under the  terms of the LGPL *
 *  and not to allow others to use your version of this file under the SPL,  *
 *  indicate your decision by deleting the provisions above and replace      *
 *  them with the notice and other provisions required by the LGPL.  If you  *
 *  do not delete the provisions above, a recipient may use your version of  *
 *  this file under either the SPL or the LGPL.                              *
 *                                                                           *
 *  Patrick Niemeyer (pat@pat.net)                                           *
 *  Author of Learning Java, O'Reilly & Associates                           *
 *  http://www.pat.net/~pat/                                                 *
 *                                                                           *
 *****************************************************************************/

package lab.ui.swing;

import java.awt.Component;
import java.awt.Font;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.Vector;
import java.awt.Cursor;
import javax.swing.text.*;
import javax.swing.*;
import clojure.lang.IFn;

/**
 * A JFC/Swing based console for the BeanShell desktop.
 * This is a descendant of the old AWTConsole.
 * <p/>
 * Improvements by: Mark Donszelmann <Mark.Donszelmann@cern.ch>
 * including Cut & Paste
 * <p/>
 * Improvements by: Daniel Leuck
 * including Color and Image support, key press bug workaround
 */
public class JConsole extends JTextPane
	implements Runnable, KeyListener, MouseListener,
	ActionListener, PropertyChangeListener {
	private final static String CUT = "Cut";
	private final static String COPY = "Copy";
	private final static String PASTE = "Paste";

	private IFn interruptFunction;
	private IFn eofFunction;
	private Writer outPipe;
	private Reader inPipe;
	private Reader in;
	private PrintWriter out;

	public Reader getIn() {
		return in;
	}

	public PrintWriter getOut() {
		return out;
	}

	public PrintWriter getErr() {
		return out;
	}

	private int cmdStart = 0;
	private Vector<String> history = new Vector<String>();
	private String startedLine;
	private int histLine = 0;

	private JPopupMenu menu;
	private DefaultStyledDocument doc;

	final int SHOW_AMBIG_MAX = 10;

	// hack to prevent key repeat for some reason?
	private boolean gotUp = true;

	private static Font defaultFont = new Font("Monospaced", Font.PLAIN, 14);

	public JConsole() {
		this(null, null, JConsole.defaultFont);
	}

	public JConsole(Font font) {
		this(null, null, font);
	}

	public JConsole(Reader cin, Writer cout) {
		this(cin, cout, JConsole.defaultFont);
	}

	public JConsole(Reader cin, Writer cout, Font font) {
		super();

		this.setFont(font);
		this.setText("");
		this.setMargin(new Insets(7, 5, 7, 5));
		this.addKeyListener(this);

		// create popup	menu
		menu = new JPopupMenu("JConsole	Menu");
		menu.add(new JMenuItem(CUT)).addActionListener(this);
		menu.add(new JMenuItem(COPY)).addActionListener(this);
		menu.add(new JMenuItem(PASTE)).addActionListener(this);

		this.addMouseListener(this);

		// make	sure popup menu	follows	Look & Feel
		UIManager.addPropertyChangeListener(this);

		outPipe = cout;
		if (outPipe == null) {
			outPipe = new PipedWriter();
			try {
				in = new PipedReader((PipedWriter) outPipe);
			} catch (IOException e) {
				print("Console internal	error (1)...", Color.red);
			}
		}

		inPipe = cin;
		if (inPipe == null) {
			PipedWriter pout = new PipedWriter();
			out = new PrintWriter(pout);
			try {
				inPipe = new PipedReader(pout);
			} catch (IOException e) {
				print("Console internal error: " + e);
			}
		}
		// Start the inpipe watcher
		new Thread(this).start();

		requestFocus();
	}

	public void cut() {
		if (this.getCaretPosition() < cmdStart) {
			super.copy();
		} else {
			super.cut();
		}
	}

	public void paste() {
		forceCaretMoveToEnd();
		super.paste();
	}

	public void setInterruptFunction(IFn interruptFunction) {
		this.interruptFunction = interruptFunction;
	}

	public void setEOFFunction(IFn eofFunction) {
		this.eofFunction = eofFunction;
	}

	public void keyPressed(KeyEvent e) {
		type(e);
		gotUp = false;
	}

	public void keyTyped(KeyEvent e) {
		type(e);
	}

	public void keyReleased(KeyEvent e) {
		gotUp = true;
		type(e);
	}

	private synchronized void type(KeyEvent e) {
		switch (e.getKeyCode()) {
			case (KeyEvent.VK_ENTER):
				if (e.getID() == KeyEvent.KEY_PRESSED) {
					enter();
					resetCommandStart();
					this.setCaretPosition(cmdStart);
				}
				e.consume();
				this.repaint();
				break;

			case (KeyEvent.VK_UP):
				if (e.getID() == KeyEvent.KEY_PRESSED) {
					historyUp();
				}
				e.consume();
				break;

			case (KeyEvent.VK_DOWN):
				if (e.getID() == KeyEvent.KEY_PRESSED) {
					historyDown();
				}
				e.consume();
				break;

			case (KeyEvent.VK_LEFT):
				if (this.getCaretPosition() <= cmdStart) {
					e.consume();
				}
				break;

			case (KeyEvent.VK_BACK_SPACE):
				if (this.getSelectedText() == null) {
					if(this.getCaretPosition() <= cmdStart) {
						e.consume();
					}
				} else {
					if(this.getCaretPosition() < cmdStart) {
						e.consume();
					}
					// TODO: prevent deletion when the caret is at
					// the end of the user=> marker
				}
				// See also default: case for backspace workaround
				break;

			case (KeyEvent.VK_DELETE):
				if (this.getCaretPosition() < cmdStart) {
					e.consume();
				}
				// TODO: prevent deletion when the caret is at
				// the end of the user=> marker
				break;

			case (KeyEvent.VK_RIGHT):
				forceCaretMoveToStart();
				break;

			case (KeyEvent.VK_HOME):
				if ((e.getModifiers() & InputEvent.SHIFT_MASK) > 0) {
					this.moveCaretPosition(cmdStart);
				} else {
					this.setCaretPosition(cmdStart);
				}
				e.consume();
				break;

			case (KeyEvent.VK_U):      // clear line
				if ((e.getModifiers() & InputEvent.CTRL_MASK) > 0) {
					replaceRange("", cmdStart, this.textLength());
					histLine = 0;
					e.consume();
				}
				break;

			case (KeyEvent.VK_D):      // "end of input"
				if ((e.getModifiers() & InputEvent.CTRL_MASK) > 0) {
					e.consume();
					if(this.eofFunction != null) {
						this.eofFunction.invoke();
					}
				}
				break;

			case (KeyEvent.VK_ALT):
			case (KeyEvent.VK_CAPS_LOCK):
			case (KeyEvent.VK_CONTROL):
			case (KeyEvent.VK_META):
			case (KeyEvent.VK_SHIFT):
			case (KeyEvent.VK_PRINTSCREEN):
			case (KeyEvent.VK_SCROLL_LOCK):
			case (KeyEvent.VK_PAUSE):
			case (KeyEvent.VK_INSERT):
			case (KeyEvent.VK_F1):
			case (KeyEvent.VK_F2):
			case (KeyEvent.VK_F3):
			case (KeyEvent.VK_F4):
			case (KeyEvent.VK_F5):
			case (KeyEvent.VK_F6):
			case (KeyEvent.VK_F7):
			case (KeyEvent.VK_F8):
			case (KeyEvent.VK_F9):
			case (KeyEvent.VK_F10):
			case (KeyEvent.VK_F11):
			case (KeyEvent.VK_F12):
			case (KeyEvent.VK_ESCAPE):

				// only	modifier pressed
				break;

			// Control-C
			case (KeyEvent.VK_C):
				if (this.getSelectedText() == null) { // Ctrl-C also copies text
					if (((e.getModifiers() & InputEvent.CTRL_MASK) > 0)
						&& (e.getID() == KeyEvent.KEY_PRESSED)) {
						//append("^C");
						if(interruptFunction != null) {
							interruptFunction.invoke("User pressed Ctrl-C");
						}
					}
					e.consume();
				}
				break;

			default:
				if (
					(e.getModifiers() &
						(InputEvent.CTRL_MASK
							| InputEvent.ALT_MASK | InputEvent.META_MASK)) == 0) {
					// plain character
					forceCaretMoveToEnd();
				}

				/*
				The getKeyCode function always returns VK_UNDEFINED for
				keyTyped events, so backspace is not fully consumed.
				*/
				if (e.paramString().indexOf("Backspace") != -1) {
					if (this.getCaretPosition() <= cmdStart) {
						e.consume();
						break;
					}
				}

				break;
		}
	}

	private void resetCommandStart() {
		cmdStart = textLength();
	}

	private void append(String string) {
		int slen = textLength();
		this.select(slen, slen);
		this.replaceSelection(string);
	}

	private String replaceRange(Object s, int start, int end) {
		String st = s.toString();
		this.select(start, end);
		this.replaceSelection(st);
		return st;
	}

	private void forceCaretMoveToEnd() {
		if (this.getCaretPosition() < cmdStart) {
			// move caret first!
			this.setCaretPosition(textLength());
		}
		this.repaint();
	}

	private void forceCaretMoveToStart() {
		if (this.getCaretPosition() < cmdStart) {
			// move caret first!
		}
		this.repaint();
	}


	private void enter() {
		String s = getCmd();

		if (s.length() == 0)      // special hack	for empty return!
		{
			s = ";\n";
		} else {
			history.addElement(s);
			s = s + "\n";
		}

		append("\n");
		histLine = 0;
		acceptLine(s);
		this.repaint();
	}

	private String getCmd() {
		String s = "";
		try {
			s = this.getText(cmdStart, textLength() - cmdStart);
		} catch (BadLocationException e) {
			// should not happen
			System.out.println("Internal JConsole Error: " + e);
		}
		return s;
	}

	private void historyUp() {
		if (history.size() == 0) {
			return;
		}
		if (histLine == 0)  // save current line
		{
			startedLine = getCmd();
		}
		if (histLine < history.size()) {
			histLine++;
			showHistoryLine();
		}
	}

	private void historyDown() {
		if (histLine == 0) {
			return;
		}

		histLine--;
		showHistoryLine();
	}

	private void showHistoryLine() {
		String showline;
		if (histLine == 0) {
			showline = startedLine;
		} else {
			showline = (String) history.elementAt(history.size() - histLine);
		}

		replaceRange(showline, cmdStart, textLength());
		this.setCaretPosition(textLength());
		this.repaint();
	}

	String ZEROS = "000";

	private void acceptLine(String line) {
		//FIXME: what did this do?
		//line = buf.toString();

		if (outPipe == null) {
			print("Console internal	error: cannot output ...", Color.red);
		} else {
			try {
				outPipe.write(line);
				outPipe.flush();
			} catch (IOException e) {
				outPipe = null;
				throw new RuntimeException("Console pipe broken...");
			}
		}
	}

	public void println(Object o) {
		print(String.valueOf(o) + "\n");
		this.repaint();
	}

	public void print(final Object o) {
		invokeAndWait(new Runnable() {
			public void run() {
				append(String.valueOf(o));
				resetCommandStart();
				JConsole.this.setCaretPosition(cmdStart);
			}
		});
	}

	/**
	 * Prints "\\n" (i.e. newline)
	 */
	public void println() {
		print("\n");
		this.repaint();
	}

	public void error(Object o) {
		print(o, Color.red);
	}

	public void println(Icon icon) {
		print(icon);
		println();
		this.repaint();
	}

	public void print(final Icon icon) {
		if (icon == null) {
			return;
		}

		invokeAndWait(new Runnable() {
			public void run() {
				JConsole.this.insertIcon(icon);
				resetCommandStart();
				JConsole.this.setCaretPosition(cmdStart);
			}
		});
	}

	public void print(Object s, Font font) {
		print(s, font, null);
	}

	public void print(Object s, Color color) {
		print(s, null, color);
	}

	public void print(final Object o, final Font font, final Color color) {
		invokeAndWait(new Runnable() {
			public void run() {
				AttributeSet old = getStyle();
				setStyle(font, color);
				append(String.valueOf(o));
				resetCommandStart();
				JConsole.this.setCaretPosition(cmdStart);
				setStyle(old, true);
			}
		});
	}

	public void print(
		Object s,
		String fontFamilyName,
		int size,
		Color color
	) {

		print(s, fontFamilyName, size, color, false, false, false);
	}

	public void print(
		final Object o,
		final String fontFamilyName,
		final int size,
		final Color color,
		final boolean bold,
		final boolean italic,
		final boolean underline
	) {
		invokeAndWait(new Runnable() {
			public void run() {
				AttributeSet old = getStyle();
				setStyle(fontFamilyName, size, color, bold, italic, underline);
				append(String.valueOf(o));
				resetCommandStart();
				JConsole.this.setCaretPosition(cmdStart);
				setStyle(old, true);
			}
		});
	}

	private AttributeSet setStyle(Font font) {
		return setStyle(font, null);
	}

	private AttributeSet setStyle(Color color) {
		return setStyle(null, color);
	}

	private AttributeSet setStyle(Font font, Color color) {
		if (font != null) {
			return setStyle(font.getFamily(), font.getSize(), color,
				font.isBold(), font.isItalic(),
				StyleConstants.isUnderline(getStyle()));
		} else {
			return setStyle(null, -1, color);
		}
	}

	private AttributeSet setStyle(
		String fontFamilyName, int size, Color color) {
		MutableAttributeSet attr = new SimpleAttributeSet();
		if (color != null) {
			StyleConstants.setForeground(attr, color);
		}
		if (fontFamilyName != null) {
			StyleConstants.setFontFamily(attr, fontFamilyName);
		}
		if (size != -1) {
			StyleConstants.setFontSize(attr, size);
		}

		setStyle(attr);

		return getStyle();
	}

	private AttributeSet setStyle(
		String fontFamilyName,
		int size,
		Color color,
		boolean bold,
		boolean italic,
		boolean underline
	) {
		MutableAttributeSet attr = new SimpleAttributeSet();
		if (color != null) {
			StyleConstants.setForeground(attr, color);
		}
		if (fontFamilyName != null) {
			StyleConstants.setFontFamily(attr, fontFamilyName);
		}
		if (size != -1) {
			StyleConstants.setFontSize(attr, size);
		}
		StyleConstants.setBold(attr, bold);
		StyleConstants.setItalic(attr, italic);
		StyleConstants.setUnderline(attr, underline);

		setStyle(attr);

		return getStyle();
	}

	private void setStyle(AttributeSet attributes) {
		setStyle(attributes, false);
	}

	private void setStyle(AttributeSet attributes, boolean overWrite) {
		this.setCharacterAttributes(attributes, overWrite);
	}

	private AttributeSet getStyle() {
		return this.getCharacterAttributes();
	}

	private void inPipeWatcher() throws IOException {
		char[] ca = new char[256]; //	arbitrary blocking factor
		int read;
		while ((read = inPipe.read(ca)) != -1) {
			print(new String(ca, 0, read));
		}

		println("Console: Input	closed...");
	}

	public void run() {
		try {
			inPipeWatcher();
		} catch (IOException e) {
			print("Console: I/O Error: " + e + "\n", Color.red);
		}
	}

	public String toString() {
		return "BeanShell console";
	}

	// MouseListener Interface
	public void mouseClicked(MouseEvent event) {
	}

	public void mousePressed(MouseEvent event) {
		if (event.isPopupTrigger()) {
			menu.show(
				(Component) event.getSource(), event.getX(), event.getY());
		}
	}

	public void mouseReleased(MouseEvent event) {
		if (event.isPopupTrigger()) {
			menu.show((Component) event.getSource(), event.getX(),
				event.getY());
		}
		this.repaint();
	}

	public void mouseEntered(MouseEvent event) {
	}

	public void mouseExited(MouseEvent event) {
	}

	// property	change
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("lookAndFeel")) {
			SwingUtilities.updateComponentTreeUI(menu);
		}
	}

	// handle cut, copy	and paste
	public void actionPerformed(ActionEvent event) {
		String cmd = event.getActionCommand();
		if (cmd.equals(CUT)) {
			this.cut();
		} else if (cmd.equals(COPY)) {
			this.copy();
		} else if (cmd.equals(PASTE)) {
			this.paste();
		}
	}

	/**
	 * If not in the event thread run via SwingUtilities.invokeAndWait()
	 */
	private void invokeAndWait(Runnable run) {
		if (!SwingUtilities.isEventDispatchThread()) {
			try {
				SwingUtilities.invokeAndWait(run);
			} catch (Exception e) {
				// shouldn't happen
				e.printStackTrace();
			}
		} else {
			run.run();
		}
	}

	public void setWaitFeedback(boolean on) {
		if (on) {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		} else {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	private int textLength() {
		return this.getDocument().getLength();
	}
}