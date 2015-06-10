package org.eclipse.m2e.core.ui.internal.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.console.MavenConsoleImpl;
import org.eclipse.m2e.internal.launch.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class ShowPhasesAndGoalsHandler extends AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(ShowPhasesAndGoalsHandler.class);

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

	private class PhasesAndGoalsContentProvider implements ITreeContentProvider {
		private Map<String, List<MojoExecutionKey>> phases;

		PhasesAndGoalsContentProvider(Map<String, List<MojoExecutionKey>> phases) {
			this.phases = phases;
		}

		@SuppressWarnings("unchecked")
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			phases = (Map<String, List<MojoExecutionKey>>) newInput;
		}

		public void dispose() {
		}

		public boolean hasChildren(Object element) {
			return getChildren(element) != null;
		}

		public Object getParent(Object element) {
			return null;
		}

		public Object[] getElements(Object inputElement) {
			return phases.keySet().toArray();

		}

		public Object[] getChildren(Object parentElement) {
			List<MojoExecutionKey> executions = phases.get(parentElement);
			if (executions == null || executions.isEmpty()) {
				return null;
			}
			return !executions.isEmpty() ? executions.toArray() : null;
		}
	};

	private class PhasesAndGoalsLabelProvider implements ILabelProvider {

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof String) {
				String phase = (String) element;
				String lifecycle = phaseToLikelyLifecycle.get(phase);
				if (lifecycle == null) {
					lifecycle = DEFAULT;
				}
				phase = phase + " (Likely lifecycle: " + lifecycle + ")";
				// phase
				return (String) phase;
			} else if (element instanceof MojoExecutionKey) {
				MojoExecutionKey execution = (MojoExecutionKey) element;
				return goal(execution);
			}
			return null;
		}

		@Override
		public void addListener(ILabelProviderListener listener) {

		}

		@Override
		public void dispose() {

		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}
	};

	@Override
	public Object execute(ExecutionEvent event) {
		Shell activeShell = HandlerUtil.getActiveShell(event);
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
							handleProject(project, activeShell);
							return null;
						}
					} catch (CoreException e) {
					}
				}
			}
		}

		ContainerSelectionDialog containerSelectionDialog = new ContainerSelectionDialog(
				activeShell, null, false, "Select a Maven project");
		containerSelectionDialog.open();
		Object[] result = containerSelectionDialog.getResult();
		if (result != null && result.length == 1) {
			if (result[0] instanceof Path) {
				Path path = (Path) result[0];
				IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace()
						.getRoot();
				IResource member = workspaceRoot.findMember(path);
				member = member.getProject();
				if (member instanceof IProject) {
					IProject project = (IProject) member;
					try {
						if (project
								.hasNature("org.eclipse.m2e.core.maven2Nature")) {
							handleProject(project, activeShell);
							return null;
						}
					} catch (CoreException ex) {
					}
				}
			}
		}
		return null;
	}

	private void handleProject(final IProject project, final Shell shell) {
		final MavenConsoleImpl mavenConsole = M2EUIPluginActivator.getDefault()
				.getMavenConsole();
		mavenConsole.show(true);
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

					shell.getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							PhasesAndGoalsLabelProvider phasesAndGoalsLabelProvider = new PhasesAndGoalsLabelProvider();
							final CheckedTreeSelectionDialog phasesAndGoalsDialog = new CheckedTreeSelectionDialog(
									shell, phasesAndGoalsLabelProvider,
									new PhasesAndGoalsContentProvider(phases)) {
								@Override
								protected void createButtonsForButtonBar(
										Composite parent) {
									((GridLayout) parent.getLayout()).numColumns++;
									Button button = new Button(parent, SWT.PUSH);
									button.setText("Launch selected goals");
									button.addSelectionListener(new SelectionListener() {

										@Override
										public void widgetSelected(
												SelectionEvent e) {
											computeResult();
											Object[] results = getResult();
											close();
											launch(project, mavenConsole,
													phases, results);
										}

										@Override
										public void widgetDefaultSelected(
												SelectionEvent e) {
											widgetSelected(e);
										}
									});
									setButtonLayoutData(button);
									((GridLayout) parent.getLayout()).numColumns++;
									button = new Button(parent, SWT.PUSH);
									button.setText("Log all");
									button.addSelectionListener(new SelectionListener() {

										@Override
										public void widgetSelected(
												SelectionEvent e) {
											close();
											toConsole(project, mavenConsole,
													phases);
										}

										@Override
										public void widgetDefaultSelected(
												SelectionEvent e) {
											widgetSelected(e);
										}
									});
									setButtonLayoutData(button);
									createButton(parent,
											IDialogConstants.CANCEL_ID,
											IDialogConstants.CANCEL_LABEL, true);
								}
							};
							phasesAndGoalsDialog
									.setTitle("Phases and Goals of "
											+ project.getName());
							phasesAndGoalsDialog
									.setMessage("Select Phases and Goals from: "
											+ project.getName()
											+ "\n"
											+ "Selected goals will be copied to clipboard.");
							phasesAndGoalsDialog.setInput(phases);
							phasesAndGoalsDialog.open();
						}

					});

					return null;
				}

			}, new NullProgressMonitor());
		} catch (CoreException ex) {
	        log.error(ex.getMessage(), ex);
		}
	}

	private void launch(IProject project, MavenConsoleImpl mavenConsole,
			Map<String, List<MojoExecutionKey>> phases, Object[] results) {

		Set<String> goals = new LinkedHashSet<String>();
		for (Object result : results) {
			if (result instanceof String) {
				String phase = (String) result;
				List<MojoExecutionKey> mojoExecutionKeys = phases.get(phase);
				for (MojoExecutionKey mojoExecutionKey : mojoExecutionKeys) {
					goals.add(goalToRun(mojoExecutionKey));
				}
			} else if (result instanceof MojoExecutionKey) {
				MojoExecutionKey mojoExecutionKey = (MojoExecutionKey) result;
				goals.add(goalToRun(mojoExecutionKey));
			}
		}

		StringBuilder sb = new StringBuilder();
		for (String goal : goals) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			if (goal.indexOf(" ") == -1) {				
				sb.append(goal);
			} else {				
				sb.append("\"" + goal + "\"");
			}
		}
		if (sb.length() > 0) {
			// First copy to clipboard
			copyToClipboard(sb.toString());
			
			ILaunchConfiguration launchConfiguration = createLaunchConfiguration(project, sb.toString());
			DebugUITools.launch(launchConfiguration, "run");
		}
	}

	private void toConsole(IProject project, MavenConsoleImpl mavenConsole,
			Map<String, List<MojoExecutionKey>> phases) {
		mavenConsole.info("Project: " + project.getName());
		Set<Entry<String, List<MojoExecutionKey>>> entrySet = phases.entrySet();
		for (Entry<String, List<MojoExecutionKey>> entry : entrySet) {
			String phase = entry.getKey();
			String lifecycle = phaseToLikelyLifecycle.get(phase);
			if (lifecycle == null) {
				lifecycle = DEFAULT;
			}
			List<MojoExecutionKey> goals = entry.getValue();
			mavenConsole.info("|");
			mavenConsole.info("+-- Phase: " + phase + " (Likely lifecycle: "
					+ lifecycle + ")");
			for (MojoExecutionKey pluginExecutionMetadata : goals) {
				mavenConsole.info("|   |");
				mavenConsole.info("|   +-- Goal: "
						+ goal(pluginExecutionMetadata));
			}
		}
		mavenConsole.info("O");
	}

	String goalToRun(MojoExecutionKey execution) {
		return shortArtifactId(execution) + ":" + execution.getGoal() + "@" + execution.getExecutionId();
	}

	String goal(MojoExecutionKey execution) {
		// http://maven.apache.org/guides/plugin/guide-java-plugin-development.html#Shortening_the_Command_Line

		StringBuilder sb = new StringBuilder();

		// TODO show groupId, but only if not a known plugin groupId

		String artifactId = shortArtifactId(execution);

		sb.append(artifactId).append(':').append(execution.getGoal());

		sb.append("@").append(execution.getExecutionId()); //$NON-NLS-1$ //$NON-NLS-2$
		return sb.toString();
	}

	private String shortArtifactId(MojoExecutionKey execution) {
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
		return artifactId;
	}

	private ILaunchConfiguration createLaunchConfiguration(IContainer basedir,
			String goal) {
		try {
			ILaunchManager launchManager = DebugPlugin.getDefault()
					.getLaunchManager();
			ILaunchConfigurationType launchConfigurationType = launchManager
					.getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

			String launchSafeGoalName = goal.replace(':', '-');

			ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType
					.newInstance(
							null, //
							NLS.bind(Messages.ExecutePomAction_executing,
									launchSafeGoalName, basedir.getLocation()
											.toString().replace('/', '-')));
			workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, basedir
					.getLocation().toOSString());
			workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goal);
			workingCopy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
			workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE,
					"${project}"); //$NON-NLS-1$
			workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);

			setProjectConfiguration(workingCopy, basedir);

			IPath path = getJREContainerPath(basedir);
			if (path != null) {
				workingCopy
						.setAttribute(
								IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
								path.toPortableString());
			}

			// TODO when launching Maven with debugger consider to add the
			// following property
			// -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -Xnoagent -Djava.compiler=NONE"

			return workingCopy;
		} catch (CoreException ex) {
		}
		return null;
	}

	private void setProjectConfiguration(
			ILaunchConfigurationWorkingCopy workingCopy, IContainer basedir) {
		IMavenProjectRegistry projectManager = MavenPlugin
				.getMavenProjectRegistry();
		IFile pomFile = basedir
				.getFile(new Path(IMavenConstants.POM_FILE_NAME));
		IMavenProjectFacade projectFacade = projectManager.create(pomFile,
				false, new NullProgressMonitor());
		if (projectFacade != null) {
			ResolverConfiguration configuration = projectFacade
					.getResolverConfiguration();

			String selectedProfiles = configuration.getSelectedProfiles();
			if (selectedProfiles != null && selectedProfiles.length() > 0) {
				workingCopy.setAttribute(MavenLaunchConstants.ATTR_PROFILES,
						selectedProfiles);
			}
		}
	}

	// TODO ideally it should use MavenProject, but it is faster to scan
	// IJavaProjects
	private IPath getJREContainerPath(IContainer basedir) throws CoreException {
		IProject project = basedir.getProject();
		if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
			IJavaProject javaProject = JavaCore.create(project);
			IClasspathEntry[] entries = javaProject.getRawClasspath();
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry entry = entries[i];
				if (JavaRuntime.JRE_CONTAINER
						.equals(entry.getPath().segment(0))) {
					return entry.getPath();
				}
			}
		}
		return null;
	}

	public static void copyToClipboard(String string) {
		// Get Clipboard
		Clipboard clipboard = new Clipboard(PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getShell().getDisplay());
		// Put the paths string into the Clipboard
		clipboard.setContents(new Object[] { string },
				new Transfer[] { TextTransfer.getInstance() });
	}
}
