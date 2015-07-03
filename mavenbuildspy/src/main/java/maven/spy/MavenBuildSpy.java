package maven.spy;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;

@Named
@Singleton
public class MavenBuildSpy extends AbstractEventSpy {
	private IMavenBuildSpySink console;

	@Override
	public void init(Context context) throws Exception {
		console = new JFrameMavenBuildSpySink();
	}

	@Override
	public void onEvent(Object event) throws Exception {
		if (event instanceof ExecutionEvent) {
			ExecutionEvent executionEvent = (ExecutionEvent) event;
			if (executionEvent.getType() == ExecutionEvent.Type.SessionStarted) {
				console.setVisible(true);
			} else if (executionEvent.getType() == ExecutionEvent.Type.SessionEnded) {
				if (console.isVisible()) {
					console.await();
				}
			} else if (executionEvent.getType() == ExecutionEvent.Type.ProjectStarted) {
			} else if (executionEvent.getType() == ExecutionEvent.Type.ProjectSucceeded) {
			} else if (executionEvent.getType() == ExecutionEvent.Type.ProjectFailed) {
			} else if (executionEvent.getType() == ExecutionEvent.Type.MojoStarted) {
				console.message(
						executionEvent.getMojoExecution().getArtifactId() + ":"
								+ executionEvent.getMojoExecution().getGoal() + "@"
								+ executionEvent.getMojoExecution().getExecutionId() + " ", IMavenBuildSpySink.STATUS.STARTED);
			} else if (executionEvent.getType() == ExecutionEvent.Type.MojoSucceeded) {
				console.message(
						executionEvent.getMojoExecution().getArtifactId() + ":"
								+ executionEvent.getMojoExecution().getGoal() + "@"
								+ executionEvent.getMojoExecution().getExecutionId() + " ", IMavenBuildSpySink.STATUS.OK);
			} else if (executionEvent.getType() == ExecutionEvent.Type.MojoFailed) {
				console.message(
						executionEvent.getMojoExecution().getArtifactId() + ":"
								+ executionEvent.getMojoExecution().getGoal() + "@"
								+ executionEvent.getMojoExecution().getExecutionId() + " ", IMavenBuildSpySink.STATUS.KO);
			}
		}
	}
}
