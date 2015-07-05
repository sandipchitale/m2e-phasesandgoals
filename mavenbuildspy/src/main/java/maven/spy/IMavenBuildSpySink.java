package maven.spy;

import org.apache.maven.execution.ExecutionEvent;

public interface IMavenBuildSpySink {
	public void start();
	public void onEvent(ExecutionEvent event) throws Exception;
	public void shutdown();
}
