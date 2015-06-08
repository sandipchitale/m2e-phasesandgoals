package org.eclipse.m2e.core.ui.internal.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.console.MavenConsoleImpl;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

@SuppressWarnings("restriction")
public class ShowPhasesAndGoalsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		if (currentSelection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) currentSelection;
			Object firstElement = structuredSelection.getFirstElement();
			if (firstElement instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) firstElement;
				Object adapter = adaptable.getAdapter(IResource.class);
				if (adapter instanceof IResource) {
					IProject project = ((IResource) adapter).getProject();
					try {
						if (project
								.hasNature("org.eclipse.m2e.core.maven2Nature")) {
							handleProject(project);
							return null;
						}
					} catch (CoreException e) {
					}
				}
			}
		}

		Shell activeShell = HandlerUtil.getActiveShell(event);
		ContainerSelectionDialog containerSelectionDialog = new ContainerSelectionDialog(
				activeShell, null, false, "Select a Maven project");
		containerSelectionDialog.open();
		Object[] result = containerSelectionDialog.getResult();
		if (result != null && result.length == 1) {
			if (result[0] instanceof Path) {
				Path path = (Path) result[0];
				IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
				IResource member = workspaceRoot.findMember(path);
				member = member.getProject();
				if (member instanceof IProject) {
					IProject project = (IProject) member;
					try {
						if (project
								.hasNature("org.eclipse.m2e.core.maven2Nature")) {
							handleProject(project);
							return null;
						}
					} catch (CoreException ex) {
					}
				}
			}
		}
		return null;
	}
	
	private static String CLEAN = "clean";
	private static String DEFAULT = "default";
	private static String SITE = "site";
	private static Map<String, String> phaseToLikelyLifecycle = new HashMap<>();
	static {
		phaseToLikelyLifecycle.put("pre-clean", CLEAN);
		phaseToLikelyLifecycle.put("clean", CLEAN);
		phaseToLikelyLifecycle.put("post-clean", CLEAN);
		
		phaseToLikelyLifecycle.put("pre-site", SITE);
		phaseToLikelyLifecycle.put("site", SITE);
		phaseToLikelyLifecycle.put("post-site", SITE);
		phaseToLikelyLifecycle.put("site-deploy", SITE);
	}

	private void handleProject(IProject project) {
		final MavenConsoleImpl mavenConsole = M2EUIPluginActivator.getDefault()
				.getMavenConsole();
		mavenConsole.show(true);
		mavenConsole.info("Project: " + project.getName());
		final IMavenProjectRegistry projectRegistry = MavenPlugin
				.getMavenProjectRegistry();
		final IMavenProjectFacade facade = projectRegistry.getProject(project);
		try {
			projectRegistry.execute(facade, new ICallable<Void>() {
				public Void call(IMavenExecutionContext context,
						IProgressMonitor monitor) throws CoreException {
					MavenProject mavenProject = facade.getMavenProject(monitor);
					List<MojoExecution> mojoExecutions = ((MavenProjectFacade) facade)
							.getMojoExecutions(monitor);
					LifecycleMappingResult mappingResult = LifecycleMappingFactory
							.calculateLifecycleMapping(mavenProject,
									mojoExecutions, facade
											.getResolverConfiguration()
											.getLifecycleMappingId(), monitor);
					Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mojoExecutionMapping = mappingResult
							.getMojoExecutionMapping();

					Map<String, List<MojoExecutionKey>> phases = new LinkedHashMap<String, List<MojoExecutionKey>>();
					for (MojoExecutionKey execution : mojoExecutionMapping
							.keySet()) {
						List<MojoExecutionKey> executions = phases
								.get(execution.getLifecyclePhase());
						if (executions == null) {
							executions = new ArrayList<MojoExecutionKey>();
							phases.put(execution.getLifecyclePhase(),
									executions);
						}
						executions.add(execution);
					}
					Set<Entry<String, List<MojoExecutionKey>>> entrySet = phases
							.entrySet();
					for (Entry<String, List<MojoExecutionKey>> entry : entrySet) {
						String phase = entry.getKey();
						String lifecycle = phaseToLikelyLifecycle.get(phase);
						if (lifecycle == null) {
							lifecycle = DEFAULT;
						}
						List<MojoExecutionKey> goals = entry.getValue();
						mavenConsole.info("|");
						mavenConsole.info("+-- Phase: " + phase + " (Likely lifecycle: " + lifecycle + ")");
						for (MojoExecutionKey pluginExecutionMetadata : goals) {
							mavenConsole.info("|   |");
							mavenConsole.info("|   +-- Goal: "
									+ goal(pluginExecutionMetadata,
											mojoExecutionMapping));
						}
					}
					mavenConsole.info("O");
					return null;
				}
			}, new NullProgressMonitor());
		} catch (CoreException ex) {
		}
	}

	String goal(MojoExecutionKey execution,
			Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mappings) {
		// http://maven.apache.org/guides/plugin/guide-java-plugin-development.html#Shortening_the_Command_Line

		StringBuilder sb = new StringBuilder();

		// TODO show groupId, but only if not a known plugin groupId

		// shorten artifactId
		String artifactId = execution.getArtifactId();
		if (artifactId.endsWith("-maven-plugin")) { //$NON-NLS-1$
			artifactId = artifactId.substring(0, artifactId.length()
					- "-maven-plugin".length()); //$NON-NLS-1$
		} else if (artifactId.startsWith("maven-") && artifactId.endsWith("-plugin")) { //$NON-NLS-1$ //$NON-NLS-2$
			artifactId = artifactId
					.substring(
							"maven-".length(), artifactId.length() - "-plugin".length()); //$NON-NLS-1$ //$NON-NLS-2$
		}

		sb.append(artifactId).append(':').append(execution.getGoal());

		// only show execution id if necessary
//		int count = 0;
//		for (MojoExecutionKey other : mappings.keySet()) {
//			if (eq(execution.getGroupId(), other.getGroupId())
//					&& eq(execution.getArtifactId(), other.getArtifactId())
//					&& eq(execution.getGoal(), other.getGoal())) {
//				count++;
//			}
//		}
		//if (count > 1) {
			sb.append(" (").append(execution.getExecutionId()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		//}
		return sb.toString();
	}

	private static <T> boolean eq(T a, T b) {
		return a != null ? a.equals(b) : b == null;
	}
}
