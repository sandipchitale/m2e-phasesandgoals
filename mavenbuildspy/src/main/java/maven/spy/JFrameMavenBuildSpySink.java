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
import javax.swing.SwingUtilities;

import org.apache.maven.execution.ExecutionEvent;

@SuppressWarnings("serial")
class JFrameMavenBuildSpySink extends JFrame implements IMavenBuildSpySink {
	enum STATUS {
		STARTED, OK, KO
	}

	enum ITEM_TYPE {
		PROJECT, LIFECYCLE, PHAE, GOAL, EXECUTION
	}

	private class BuildProgressItem {
		String message;
		ITEM_TYPE itemType;
		STATUS status;
		Exception exception;

		BuildProgressItem(String message, ITEM_TYPE itemType, STATUS status) {
			this(message, itemType, status, null);
		}

		BuildProgressItem(String message, ITEM_TYPE itemType, STATUS status, Exception exception) {
			this.message = message;
			this.itemType = itemType;
			this.status = status;
			this.exception = exception;
		}

		public String toString() {
			return itemType + " [" + status + "] " + message
					+ (exception == null ? "" : " [" + exception.getLocalizedMessage() + "]");
		}
	}

	private DefaultListModel<BuildProgressItem> buildProgressItems;
	private JList<BuildProgressItem> buildProgressConsole;
	// private static Icon GOAL = new
	// ImageIcon(JFrameMavenBuildSpySink.class.getResource("goal.png"));
	private static ImageIcon PHASES_AND_GOALS = new ImageIcon(
			JFrameMavenBuildSpySink.class.getResource("phasesandgoals.png"));
	private static ImageIcon BLANK = new ImageIcon(JFrameMavenBuildSpySink.class.getResource("blank.png"));
	private static ImageIcon PROJECT = new ImageIcon(JFrameMavenBuildSpySink.class.getResource("project.png"));
	private static ImageIcon PROJECT_OK = new ImageIcon(JFrameMavenBuildSpySink.class.getResource("project_OK.png"));
	private static ImageIcon PROJECT_KO = new ImageIcon(JFrameMavenBuildSpySink.class.getResource("project_KO.png"));
	private static ImageIcon GOAL = new ImageIcon(JFrameMavenBuildSpySink.class.getResource("goal.png"));
	private static ImageIcon GOAL_OK = new ImageIcon(JFrameMavenBuildSpySink.class.getResource("goal_OK.png"));
	private static ImageIcon GOAL_KO = new ImageIcon(JFrameMavenBuildSpySink.class.getResource("goal_KO.png"));

	public JFrameMavenBuildSpySink() {
		super("Maven Build Spy Console");
		setIconImage(PHASES_AND_GOALS.getImage());
		buildProgressItems = new DefaultListModel<>();
		buildProgressConsole = new JList<BuildProgressItem>(buildProgressItems);
		buildProgressConsole.setFont(new Font(java.awt.Font.MONOSPACED, Font.BOLD, 12));
		buildProgressConsole.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				JLabel renderer = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
						cellHasFocus);
				BuildProgressItem buildProgressItem = (BuildProgressItem) value;
				renderer.setFont(buildProgressConsole.getFont());
				renderer.setText(" " + buildProgressItem.message);
				switch (buildProgressItem.status) {
				case STARTED:
					if (buildProgressItem.itemType == ITEM_TYPE.PROJECT) {
						renderer.setIcon(PROJECT);
					} else if (buildProgressItem.itemType == ITEM_TYPE.GOAL) {
						renderer.setIcon(GOAL);
					} else {
						renderer.setIcon(BLANK);
					}
					break;
				case KO:
					if (buildProgressItem.itemType == ITEM_TYPE.PROJECT) {
						renderer.setIcon(PROJECT_KO);
					} else if (buildProgressItem.itemType == ITEM_TYPE.GOAL) {
						renderer.setIcon(GOAL_KO);
					} else {
						renderer.setIcon(BLANK);
					}
					break;
				case OK:
					if (buildProgressItem.itemType == ITEM_TYPE.PROJECT) {
						renderer.setIcon(PROJECT_OK);
					} else if (buildProgressItem.itemType == ITEM_TYPE.GOAL) {
						renderer.setIcon(GOAL_OK);
					} else {
						renderer.setIcon(BLANK);
					}
					break;
				default:
					renderer.setIcon(BLANK);
					break;
				}
				if (buildProgressItem.exception == null) {
					renderer.setToolTipText(null);
				} else {
					renderer.setToolTipText(buildProgressItem.exception.getMessage());
				}
				return renderer;
			}
		});
		JComponent contentPane = (JComponent) getContentPane();
		contentPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		contentPane.setLayout(new BorderLayout(5, 5));
		contentPane.add(new JLabel("Progress:"), BorderLayout.NORTH);
		contentPane.add(new JScrollPane(buildProgressConsole), BorderLayout.CENTER);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 700, 400);
	}

	@Override
	public void start() {
		setVisible(true);
	}

	private long projectStartMillis;
	private long projectEndMillis;

	private long startMillis;
	private long endMillis;

	@Override
	public void onEvent(ExecutionEvent executionEvent) throws Exception {
		if (executionEvent.getType() == ExecutionEvent.Type.SessionStarted) {
		} else if (executionEvent.getType() == ExecutionEvent.Type.SessionEnded) {
		} else if (executionEvent.getType() == ExecutionEvent.Type.ProjectStarted) {
			projectStartMillis = System.currentTimeMillis();
			addElement(new BuildProgressItem(
					"[ |   " + String.format("%12s", "Started") + " ] " + executionEvent.getProject().getName(),
					ITEM_TYPE.PROJECT, STATUS.STARTED));
		} else if (executionEvent.getType() == ExecutionEvent.Type.ProjectSucceeded) {
			projectEndMillis = System.currentTimeMillis();
			addElement(new BuildProgressItem("[ +   " + String.format("%9d", (projectEndMillis - projectStartMillis))
					+ " ms ] " + executionEvent.getProject().getName(), ITEM_TYPE.PROJECT, STATUS.OK));

		} else if (executionEvent.getType() == ExecutionEvent.Type.ProjectFailed) {
			projectEndMillis = System.currentTimeMillis();
			addElement(new BuildProgressItem(
					"[ -   " + String.format("%9d", (projectEndMillis - projectStartMillis)) + " ms ] "
							+ executionEvent.getProject().getName(),
					ITEM_TYPE.PROJECT, STATUS.KO, executionEvent.getException()));
		} else if (executionEvent.getType() == ExecutionEvent.Type.MojoStarted) {
			startMillis = System.currentTimeMillis();
			addElement(new BuildProgressItem(
					"[   | " + String.format("%12s", "Started") + " ] " + executionEvent.getMojoExecution().getArtifactId() + ":"
							+ executionEvent.getMojoExecution().getGoal() + "@"
							+ executionEvent.getMojoExecution().getExecutionId(),
					ITEM_TYPE.GOAL, STATUS.STARTED));
		} else if (executionEvent.getType() == ExecutionEvent.Type.MojoSucceeded) {
			endMillis = System.currentTimeMillis();
			addElement(new BuildProgressItem("[   + " + String.format("%9d", (endMillis - startMillis)) + " ms ] "
					+ executionEvent.getMojoExecution().getArtifactId() + ":"
					+ executionEvent.getMojoExecution().getGoal() + "@"
					+ executionEvent.getMojoExecution().getExecutionId(), ITEM_TYPE.GOAL, STATUS.OK));
		} else if (executionEvent.getType() == ExecutionEvent.Type.MojoFailed) {
			endMillis = System.currentTimeMillis();
			addElement(new BuildProgressItem(
					"[   - " + String.format("%9d", (endMillis - startMillis)) + " ms ] "
							+ executionEvent.getMojoExecution().getArtifactId() + ":"
							+ executionEvent.getMojoExecution().getGoal() + "@"
							+ executionEvent.getMojoExecution().getExecutionId(),
					ITEM_TYPE.GOAL, STATUS.KO, executionEvent.getException()));
		}
	}

	private void addElement(final BuildProgressItem buildProgressItem) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				buildProgressItems.addElement(buildProgressItem);
			}
		});
	}

	public void shutdown() {
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
