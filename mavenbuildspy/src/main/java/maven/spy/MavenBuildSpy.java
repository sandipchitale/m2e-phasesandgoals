package maven.spy;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;

@Named
@Singleton
public class MavenBuildSpy extends AbstractEventSpy {
	private IMavenBuildSpySink mavenBuildSpySink;

	@Override
	public void init(Context context) throws Exception {
		ServiceLoader<IMavenBuildSpySink> serviceLoader = ServiceLoader.load(IMavenBuildSpySink.class);
		try {
			Iterator<IMavenBuildSpySink> mavenBuildSpySinks = serviceLoader.iterator();
			if (mavenBuildSpySinks.hasNext()) {
				mavenBuildSpySink = mavenBuildSpySinks.next();
				return;
			}
		} catch (ServiceConfigurationError serviceError) {
		}
		mavenBuildSpySink = new JFrameMavenBuildSpySink();
		mavenBuildSpySink.start();
	}

	@Override
	public void onEvent(Object event) throws Exception {
		if (event instanceof ExecutionEvent) {
			ExecutionEvent executionEvent = (ExecutionEvent) event;
			mavenBuildSpySink.onEvent(executionEvent);
		}
	}

	@Override
	public void close() throws Exception {
		mavenBuildSpySink.shutdown();
	}
}
