package maven.spy;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
class JFrameMavenBuildSpySink extends JFrame implements IMavenBuildSpySink {
	private class Message {
		String message;
		STATUS status;
		Exception exception;

		Message(String message, STATUS status, Exception exception) {
			this.message = message;
			this.status = status;
			this.exception = exception;
		}

		public String toString() {
			return status + " " + message + (exception == null ? "" : " [" + exception.getLocalizedMessage() + "]" );
		}
	}

	private DefaultListModel<Message> messageModel;
	private JList<Message> console;
//	private static Icon GOAL = new ImageIcon(JFrameMavenBuildSpySink.class.getResource("goal.png"));
	private static ImageIcon PHASES_AND_GOALS = new ImageIcon(JFrameMavenBuildSpySink.class.getResource("phasesandgoals.png"));
	private static ImageIcon BLANK = new ImageIcon(JFrameMavenBuildSpySink.class.getResource("blank.png"));
	private static ImageIcon GOAL_OK = new ImageIcon(JFrameMavenBuildSpySink.class.getResource("goal_OK.png"));
	private static ImageIcon GOAL_KO = new ImageIcon(JFrameMavenBuildSpySink.class.getResource("goal_KO.png"));

	public JFrameMavenBuildSpySink() {
		super("Maven Build Spy Console");
		setIconImage(PHASES_AND_GOALS.getImage());
		messageModel = new DefaultListModel<>();
		console = new JList<Message>(messageModel);
		console.setFont(new Font(java.awt.Font.MONOSPACED, Font.BOLD, 12));
		console.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				JLabel renderer = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				Message message = (Message) value;
				renderer.setFont(console.getFont());
				renderer.setText(" " + message.message);
				switch (message.status) {
 				case KO:
					renderer.setIcon(GOAL_KO);
					break;
 				case OK:
 					renderer.setIcon(GOAL_OK);
					break;
				default:
					renderer.setIcon(BLANK);
					break;
				}
				if (message.exception == null) {
					renderer.setToolTipText(null);
				} else {
					renderer.setToolTipText(message.exception.getMessage());
				}
				return renderer;
			}
		});
		JComponent contentPane = (JComponent) getContentPane();
		contentPane.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		contentPane.setLayout(new BorderLayout(5,5));
		contentPane.add(new JLabel("Progress:"), BorderLayout.NORTH);
		contentPane.add(new JScrollPane(console), BorderLayout.CENTER);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 700, 400);

	}

	@Override
	public void clear() {
		messageModel.clear();
	}

	@Override
	public void message(String message) {
		messageModel.addElement(new Message(message, STATUS.NONE, null));
	}

	@Override
	public void message(String message, STATUS status) {
		if (status == STATUS.OK || status == STATUS.KO) {
			messageModel.addElement(new Message(message, status, null));
		}
	}
	
	@Override
	public void message(String message, STATUS status, Exception e) {
		if (status == STATUS.OK || status == STATUS.KO) {
			messageModel.addElement(new Message(message, status, e));
		}
	}

	public void await() {
		if (isVisible()) {
			final CountDownLatch waitForClose = new CountDownLatch(1);
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					removeWindowListener(this);
					waitForClose.countDown();
				}
			});
			try {
				waitForClose.await();
			} catch (InterruptedException e1) {
			}
		}
	}
}
