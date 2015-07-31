package org.eclipse.m2e.core.ui.internal.handlers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
import org.eclipse.core.runtime.FileLocator;
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
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.components.MavenProjectLabelProvider;
import org.eclipse.m2e.core.ui.internal.console.MavenConsoleImpl;
import org.eclipse.m2e.internal.launch.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class ShowPhasesAndGoalsHandler extends AbstractHandler {

	private static final String DEFAULT_CLI_FLAGS = "-B";
	private static final String EXPAND_ALL = "icons/expand_all.png";
	private static final String COLLAPSE_ALL = "icons/collapse_all.png";

	private static final String LAUNCH = "icons/launch.png";
	// private static final String LAUNCH_DEBUG = "icons/launch_debug.png";
	private static final String LOG = "icons/log.png";

	private static final String PHASES_AND_GOALS = "icons/phasesandgoals.png";
	private static final String PHASE = "icons/phase.png";
	private static final String GOAL = "icons/goal.png";
	private static final String DELETE = "icons/delete.png";
	private static final String SAVE = "icons/save.png";

	private static Map<String, ImageDescriptor> imageDescriptorMap = new HashMap<>();

	private static ImageDescriptor getImageDescriptor(String image) {
		ImageDescriptor imageDescriptor = imageDescriptorMap.get(image);
		if (imageDescriptor == null) {
			imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(M2EUIPluginActivator.PLUGIN_ID, image);
			if (imageDescriptor != null) {
				imageDescriptorMap.put(image, imageDescriptor);
			}
		}
		return imageDescriptor;
	}

	private static Map<String, Image> imageMap = new HashMap<>();

	private static Image getImageForName(Device device, String imageName) {
		Image image = imageMap.get(imageName);
		if (image == null) {
			ImageDescriptor imageDescriptor = getImageDescriptor(imageName);
			if (imageDescriptor == null) {
				return null;
			}
			image = new Image(device, imageDescriptor.getImageData());
			imageMap.put(imageName, image);
		}
		return image;
	}

	private static final Logger log = LoggerFactory.getLogger(ShowPhasesAndGoalsHandler.class);

	private static String CLEAN = "Clean";
	private static String DEFAULT = "Default";
	private static String SITE = "Site";
	private static String OTHER = "Other";
	private static Map<String, String> phaseToLikelyLifecycle = new HashMap<>();

	static {
		// Clean lifecycle
		phaseToLikelyLifecycle.put("pre-clean", CLEAN);
		phaseToLikelyLifecycle.put("clean", CLEAN);
		phaseToLikelyLifecycle.put("post-clean", CLEAN);

		// Default lifecycle
		phaseToLikelyLifecycle.put("validate", DEFAULT);
		phaseToLikelyLifecycle.put("initialize", DEFAULT);
		phaseToLikelyLifecycle.put("generate-sources", DEFAULT);
		phaseToLikelyLifecycle.put("process-sources", DEFAULT);
		phaseToLikelyLifecycle.put("generate-resources", DEFAULT);
		phaseToLikelyLifecycle.put("process-resources", DEFAULT);
		phaseToLikelyLifecycle.put("compile", DEFAULT);
		phaseToLikelyLifecycle.put("process-classes", DEFAULT);
		phaseToLikelyLifecycle.put("generate-test-sources", DEFAULT);
		phaseToLikelyLifecycle.put("process-test-sources", DEFAULT);
		phaseToLikelyLifecycle.put("generate-test-resources", DEFAULT);
		phaseToLikelyLifecycle.put("process-test-resources", DEFAULT);
		phaseToLikelyLifecycle.put("test-compile", DEFAULT);
		phaseToLikelyLifecycle.put("process-test-classes", DEFAULT);
		phaseToLikelyLifecycle.put("test", DEFAULT);
		phaseToLikelyLifecycle.put("prepare-package", DEFAULT);
		phaseToLikelyLifecycle.put("package", DEFAULT);
		phaseToLikelyLifecycle.put("pre-integration-test", DEFAULT);
		phaseToLikelyLifecycle.put("integration-test", DEFAULT);
		phaseToLikelyLifecycle.put("post-integration-test", DEFAULT);
		phaseToLikelyLifecycle.put("verify", DEFAULT);
		phaseToLikelyLifecycle.put("install", DEFAULT);
		phaseToLikelyLifecycle.put("deploy", DEFAULT);

		// Site lifecycle
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
		private Device device;

		private PhasesAndGoalsLabelProvider(Device device) {
			this.device = device;
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof String) {
				return getImageForName(device, PHASE);
			} else {
				return getImageForName(device, GOAL);
			}
		}

		@Override
		public String getText(Object element) {
			if (element instanceof String) {
				String phase = (String) element;
				String lifecycle = phaseToLikelyLifecycle.get(phase);
				if (lifecycle == null) {
					lifecycle = OTHER;
				}
				phase = phase + " (Lifecycle: " + lifecycle + ")";
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
					if (project != null) {
						try {
							if (project.hasNature(IMavenConstants.NATURE_ID)) {
								handleProject(project, activeShell);
								return null;
							}
						} catch (CoreException e) {
						}
					}
				}
			}
		} else {
			IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
			if (activePart instanceof IEditorPart) {
				IEditorPart editorPart = (IEditorPart) activePart;
				IEditorInput editorInput = editorPart.getEditorInput();
				if (editorInput instanceof IFileEditorInput) {
					IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
					IFile iFile = fileEditorInput.getFile();
					if (iFile != null) {
						IProject project = iFile.getProject();
						if (project != null) {
							try {
								if (project.hasNature(IMavenConstants.NATURE_ID)) {
									handleProject(project, activeShell);
									return null;
								}
							} catch (CoreException e) {
							}
						}
					}
				}
			}
		}

		// If only one open, maven project - use it
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		List<IProject> projectsList = new ArrayList<IProject>();
		for (IProject project : projects) {
			try {
				if (project.isOpen() && project.hasNature(IMavenConstants.NATURE_ID)) {
					projectsList.add(project);
				}
			} catch (CoreException e) {
			}
		}
		if (projectsList.size() == 1) {
			handleProject(projectsList.get(0), activeShell);
			return null;
		}

		ContainerSelectionDialog containerSelectionDialog = new ContainerSelectionDialog(activeShell, null, false,
				"Select a Maven project");
		containerSelectionDialog.showClosedProjects(false);
		containerSelectionDialog.open();
		Object[] result = containerSelectionDialog.getResult();
		if (result != null && result.length == 1) {
			if (result[0] instanceof Path) {
				Path path = (Path) result[0];
				IResource member = workspaceRoot.findMember(path);
				member = member.getProject();
				if (member instanceof IProject) {
					IProject project = (IProject) member;
					try {
						if (project.hasNature(IMavenConstants.NATURE_ID)) {
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
		final MavenConsoleImpl mavenConsole = M2EUIPluginActivator.getDefault().getMavenConsole();
		mavenConsole.show(true);
		final IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();
		final IMavenProjectFacade facade = projectRegistry.getProject(project);
		try {
			projectRegistry.execute(facade, new ICallable<Void>() {
				public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
					MavenProject mavenProject = facade.getMavenProject(monitor);
					List<MojoExecution> mojoExecutions = ((MavenProjectFacade) facade).getMojoExecutions(monitor);
					LifecycleMappingResult mappingResult = LifecycleMappingFactory.calculateLifecycleMapping(
							mavenProject, mojoExecutions, facade.getResolverConfiguration().getLifecycleMappingId(),
							monitor);
					Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mojoExecutionMapping = mappingResult
							.getMojoExecutionMapping();

					Map<String, List<MojoExecutionKey>> phases = new LinkedHashMap<String, List<MojoExecutionKey>>();
					for (MojoExecutionKey execution : mojoExecutionMapping.keySet()) {
						List<MojoExecutionKey> executions = phases.get(execution.getLifecyclePhase());
						if (executions == null) {
							executions = new ArrayList<MojoExecutionKey>();
							phases.put(execution.getLifecyclePhase(), executions);
						}
						executions.add(execution);
					}

					shell.getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							final String[] mavenVersion = new String[] { "0.0.0" };
							AbstractMavenRuntime runtime = MavenPluginActivator.getDefault().getMavenRuntimeManager()
									.getRuntime(org.eclipse.m2e.core.internal.launch.MavenRuntimeManagerImpl.DEFAULT);
							if (runtime != null) {
								mavenVersion[0] = runtime.getVersion();
							}
							PhasesAndGoalsLabelProvider phasesAndGoalsLabelProvider = new PhasesAndGoalsLabelProvider(
									shell.getDisplay());

							class ModelessCheckedTreeSelectionDialog extends CheckedTreeSelectionDialog {
								public ModelessCheckedTreeSelectionDialog(Shell parent, ILabelProvider labelProvider,
										ITreeContentProvider contentProvider) {
									super(parent, labelProvider, contentProvider);
									setShellStyle(SWT.DIALOG_TRIM | SWT.MODELESS | SWT.BORDER | SWT.RESIZE | SWT.TITLE | SWT.MAX | SWT.CLOSE);
									setBlockOnOpen(false);
								}

								protected boolean isResizable() {
									return true;
								}
							}

							final CheckedTreeSelectionDialog phasesAndGoalsDialog = new ModelessCheckedTreeSelectionDialog(
									shell, phasesAndGoalsLabelProvider, new PhasesAndGoalsContentProvider(phases)) {

								private Text flags;
								private Label goalsLabel;
								private Button runGoalsInSelectionOrderMode;
								private Button useMavenBuildSpy;
								private Text goals;

								private Set<Object> lastResults = new HashSet<Object>();

								private void setGoals() {
									final CheckboxTreeViewer treeViewer = getTreeViewer();

									computeResult();
									Object[] results = getResult();
									if (results != null && results.length > 0) {
										List<Object> resultsList = new LinkedList<Object>();
										for (Object result : results) {
											if (!treeViewer.getGrayed(result)) {
												// Check against remembered
												// results
												if (runGoalsInSelectionOrderMode.getSelection()) {
													if (!lastResults.contains(result)) {
														resultsList.add(result);
														lastResults.add(result);
													}
												} else {
													resultsList.add(result);
												}
											}
										}

										String goalsToRun = goalsToRun(project, mavenConsole, phases,
												resultsList.toArray());
										if (goalsToRun != null) {
											if (runGoalsInSelectionOrderMode.getSelection()) {
												goals.append(
														(goals.getText().trim().length() > 0 ? " " : "") + goalsToRun);
											} else {
												goals.setText(goalsToRun);
											}
										}
									} else {
										if (!runGoalsInSelectionOrderMode.getSelection()) {
											goals.setText("");
										}
									}
								}

								/*
								 * (non-Javadoc)
								 *
								 * @see org.eclipse.jface.dialogs.Dialog#
								 * createDialogArea(org.eclipse.swt.widgets.
								 * Composite)
								 */
								@Override
								protected Control createDialogArea(Composite parent) {
									Composite composite = (Composite) super.createDialogArea(parent);

									// If only one open, maven project - use it
									IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
									IProject[] projects = workspaceRoot.getProjects();
									final List<IProject> projectsList = new ArrayList<IProject>();
									for (IProject p : projects) {
										try {
											if (p.isOpen() && p.hasNature(IMavenConstants.NATURE_ID) && p != project) {
												projectsList.add(p);
											}
										} catch (CoreException e) {
										}
									}
									if (projectsList.size() > 0) {
										GridData otherProjectLabelLayoutData = new GridData(GridData.FILL_HORIZONTAL);
										Label otherProjectLabel = new Label(composite, SWT.LEFT);
										otherProjectLabel.setText("Phases and Goals of other Maven project:");
										otherProjectLabel.setLayoutData(otherProjectLabelLayoutData);

										GridData selectMavenProjectLayoutData = new GridData(GridData.FILL_HORIZONTAL);
										ComboViewer selectMavenProjectComboViewer = new ComboViewer(composite,
												SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
										selectMavenProjectComboViewer.setLabelProvider(new MavenProjectLabelProvider());
										Combo selectMavenProjectCombo = selectMavenProjectComboViewer.getCombo();
										selectMavenProjectCombo.setLayoutData(selectMavenProjectLayoutData);

										selectMavenProjectComboViewer
												.setContentProvider(new IStructuredContentProvider() {

											@Override
											public void dispose() {
											}

											@Override
											public Object[] getElements(Object inputElement) {
												return projectsList.toArray();
											}

											@Override
											public void inputChanged(org.eclipse.jface.viewers.Viewer viewer,
													Object oldInput, Object newInput) {
												// Nothing to do
											}

										});
										selectMavenProjectComboViewer.setInput(projectsList);
										selectMavenProjectComboViewer
												.addSelectionChangedListener(new ISelectionChangedListener() {

											private boolean suspended = false;
											@Override
											public void selectionChanged(SelectionChangedEvent event) {
												if (!suspended) {
													IStructuredSelection selection = (IStructuredSelection) event
															.getSelection();
													if (selection != null) {
														try {
															suspended = true;
															selectMavenProjectComboViewer.setSelection(new StructuredSelection());
														} finally {
															suspended = false;
														}
														handleProject((IProject) selection.getFirstElement(), shell);
													}
												}
											}
										});
									}

									GridData singleSelectionModeLayoutData = new GridData(GridData.FILL_HORIZONTAL);
									runGoalsInSelectionOrderMode = new Button(composite, SWT.CHECK);
									runGoalsInSelectionOrderMode.setText("Run goals in selection order");
									runGoalsInSelectionOrderMode.setLayoutData(singleSelectionModeLayoutData);
									runGoalsInSelectionOrderMode.addSelectionListener(new SelectionListener() {
										@Override
										public void widgetSelected(SelectionEvent e) {
											if (runGoalsInSelectionOrderMode.getSelection()) {
												lastResults.clear();
												getButton(IDialogConstants.SELECT_ALL_ID).setEnabled(false);
												getTreeViewer().setCheckedElements(new Object[0]);
											} else {
												getButton(IDialogConstants.SELECT_ALL_ID).setEnabled(true);
											}
											IDialogSettings dialogSettings = M2EUIPluginActivator.getDefault().getDialogSettings();
											dialogSettings.put("phasesandgoals.runGoalsInSelectionOrder", runGoalsInSelectionOrderMode.getSelection());
										}

										@Override
										public void widgetDefaultSelected(SelectionEvent e) {
											widgetSelected(e);
										}
									});

									GridData useMavenBuildSpyLayoutData = new GridData(GridData.FILL_HORIZONTAL);
									useMavenBuildSpy = new Button(composite, SWT.CHECK);
									useMavenBuildSpy.setText("Use Maven Build Spy");
									useMavenBuildSpy.setLayoutData(useMavenBuildSpyLayoutData);
									useMavenBuildSpy.setSelection(false);
									useMavenBuildSpy.addSelectionListener(new SelectionListener() {
										@Override
										public void widgetSelected(SelectionEvent e) {
											IDialogSettings dialogSettings = M2EUIPluginActivator.getDefault().getDialogSettings();
											dialogSettings.put("phasesandgoals.useMavenBuildSpy", useMavenBuildSpy.getSelection());
										}

										@Override
										public void widgetDefaultSelected(SelectionEvent e) {
											widgetSelected(e);
										}
									});

									GridData flagsLabelLayoutData = new GridData(GridData.FILL_HORIZONTAL);
									Label flagsLabel = new Label(composite, SWT.LEFT);
									flagsLabel.setText("Maven Command Line Options:");
									flagsLabel.setLayoutData(flagsLabelLayoutData);

									GridData flagsLayoutData = new GridData(GridData.FILL_HORIZONTAL);
									flags = new Text(composite, SWT.SINGLE | SWT.BORDER);
									flags.setText(DEFAULT_CLI_FLAGS);
									flags.setLayoutData(flagsLayoutData);

									GridData goalsLabelLayoutData = new GridData(GridData.FILL_HORIZONTAL);
									goalsLabel = new Label(composite, SWT.LEFT);
									goalsLabel.setText("Goals:");
									goalsLabel.setLayoutData(goalsLabelLayoutData);

									GridData goalsLayoutData = new GridData(GridData.FILL_BOTH);
									goalsLayoutData.heightHint = convertHeightInCharsToPixels(4);
									goals = new Text(composite, SWT.MULTI | SWT.WRAP | SWT.BORDER);
									goals.setLayoutData(goalsLayoutData);

									final IDialogSettings dialogSettings = M2EUIPluginActivator.getDefault().getDialogSettings();

									GridData manageConfigToolbarLayoutData = new GridData(GridData.FILL_HORIZONTAL);
									ToolBar manageConfigToolbar = new ToolBar(composite, SWT.FLAT | SWT.RIGHT);
									manageConfigToolbar.setLayoutData(manageConfigToolbarLayoutData);

									ToolItem separator = new ToolItem(manageConfigToolbar, SWT.SEPARATOR);
									Combo combo = new Combo(manageConfigToolbar, SWT.DROP_DOWN);

									combo.add("");
									IDialogSettings[] sections = dialogSettings.getSections();
									String sectionNamePrefix = "phasesandgoals." + project.getName() + ".";
									for (IDialogSettings section : sections) {
										String sectionName = section.getName();
										if (sectionName.startsWith(sectionNamePrefix)) {
											String configName = sectionName.substring(sectionNamePrefix.length());
											combo.add(configName);
										}
									}
								    combo.pack();
								    separator.setWidth(combo.getSize().x*3);
								    separator.setControl(combo);

								    combo.addSelectionListener(new SelectionListener() {
										@Override
										public void widgetSelected(SelectionEvent e) {
											String configName = combo.getText().trim();
											if (configName.length() == 0) {
												goals.setText("");
												flags.setText(DEFAULT_CLI_FLAGS);
											} else {
												IDialogSettings section = dialogSettings.getSection(sectionNamePrefix + configName);
												if (section != null) {
													String goalNames = section.get("goals");
													if (goalNames != null) {
														goals.setText(goalNames);
													}
													String cliFlags = section.get("flags");
													if (cliFlags != null) {
														flags.setText(cliFlags);
													}
													useMavenBuildSpy.setSelection(section.getBoolean("mavenBuildSpy"));
												}
											}
										}

										@Override
										public void widgetDefaultSelected(SelectionEvent e) {
											widgetSelected(e);
										}
									});

									separator = new ToolItem(manageConfigToolbar, SWT.SEPARATOR);

									ToolItem saveToolItem = new ToolItem(manageConfigToolbar, SWT.PUSH);
									saveToolItem.setImage(getImageForName(shell.getDisplay(), SAVE));

									saveToolItem.addSelectionListener(new SelectionListener() {
										@Override
										public void widgetSelected(SelectionEvent e) {
											String goalNames = goals.getText().trim();
											String cliFlags = flags.getText().trim();
											if (goalNames != null) {
												String configName = combo.getText().trim();
												if (configName.length()> 0) {
													IDialogSettings section = dialogSettings.getSection(sectionNamePrefix + configName);
													if (section == null) {
														section = dialogSettings.addNewSection(sectionNamePrefix + configName);
													}
													section.put("mavenBuildSpy", useMavenBuildSpy.getSelection());
													section.put("goals", goalNames);
													section.put("flags", cliFlags);
												}
											}
										}

										@Override
										public void widgetDefaultSelected(SelectionEvent e) {
											widgetSelected(e);
										}
									});
									
									ToolItem deleteToolItem = new ToolItem(manageConfigToolbar, SWT.PUSH);
									deleteToolItem.setImage(getImageForName(shell.getDisplay(), DELETE));

									deleteToolItem.addSelectionListener(new SelectionListener() {
										@Override
										public void widgetSelected(SelectionEvent e) {
											String configName = combo.getText().trim();
											if (configName.length() != 0) {
												IDialogSettings section = dialogSettings.getSection(sectionNamePrefix + configName);
												if (section != null) {
													((DialogSettings) dialogSettings).removeSection(section);
													combo.remove(configName);
													combo.setText("");
												}
											}
										}

										@Override
										public void widgetDefaultSelected(SelectionEvent e) {
											widgetSelected(e);
										}
									});
									
									ISelectionChangedListener selectionChangedListener = new ISelectionChangedListener() {

										@Override
										public void selectionChanged(SelectionChangedEvent event) {
										}
									};
									getTreeViewer().addPostSelectionChangedListener(selectionChangedListener);

									runGoalsInSelectionOrderMode.setSelection(dialogSettings.getBoolean("phasesandgoals.runGoalsInSelectionOrder"));
									useMavenBuildSpy.setSelection(dialogSettings.getBoolean("phasesandgoals.useMavenBuildSpy"));

									String cliflags = dialogSettings.get("phasesandgoals.cliflags");
									if (cliflags != null) {
										flags.setText(cliflags);
									}

									return composite;
								}

								@Override
								protected void updateOKStatus() {
									super.updateOKStatus();
									getShell().getDisplay().asyncExec(new Runnable() {
										@Override
										public void run() {
											setGoals();
										}
									});
								}

								@Override
								protected void createButtonsForButtonBar(Composite parent) {

									((GridLayout) parent.getLayout()).numColumns++;
									Button button = new Button(parent, SWT.PUSH);
									button.setImage(getImageForName(shell.getDisplay(), LAUNCH));
									button.setToolTipText("Launch selected goals");
									button.addSelectionListener(new SelectionListener() {

										@Override
										public void widgetSelected(SelectionEvent e) {

											String cliFlags = flags.getText();
											String goalsToRun = goals.getText();
											boolean useMBS = useMavenBuildSpy.getSelection();
											close();
											launch(project, mavenConsole, phases, cliFlags, goalsToRun, useMBS, "run");
										}

										@Override
										public void widgetDefaultSelected(SelectionEvent e) {
											widgetSelected(e);
										}
									});
									setButtonLayoutData(button);
									if (mavenVersion[0] != null && ("3.3.1".compareTo(mavenVersion[0]) > 0)) {
										button.setEnabled(false);
									}

									((GridLayout) parent.getLayout()).numColumns++;
									button = new Button(parent, SWT.PUSH);
									button.setImage(getImageForName(shell.getDisplay(), EXPAND_ALL));
									button.setToolTipText("Expand All");
									button.addSelectionListener(new SelectionListener() {

										@Override
										public void widgetSelected(SelectionEvent e) {
											getTreeViewer().expandAll();
										}

										@Override
										public void widgetDefaultSelected(SelectionEvent e) {
											widgetSelected(e);
										}
									});
									setButtonLayoutData(button);

									((GridLayout) parent.getLayout()).numColumns++;
									button = new Button(parent, SWT.PUSH);
									button.setImage(getImageForName(shell.getDisplay(), COLLAPSE_ALL));
									button.setToolTipText("Collapse All");
									button.addSelectionListener(new SelectionListener() {

										@Override
										public void widgetSelected(SelectionEvent e) {
											getTreeViewer().collapseAll();
										}

										@Override
										public void widgetDefaultSelected(SelectionEvent e) {
											widgetSelected(e);
										}
									});
									setButtonLayoutData(button);

									((GridLayout) parent.getLayout()).numColumns++;
									button = new Button(parent, SWT.PUSH);
									button.setImage(getImageForName(shell.getDisplay(), LOG));
									button.setToolTipText("Log all");
									button.addSelectionListener(new SelectionListener() {

										@Override
										public void widgetSelected(SelectionEvent e) {
											toConsole(project, mavenConsole, phases);
										}

										@Override
										public void widgetDefaultSelected(SelectionEvent e) {
											widgetSelected(e);
										}
									});
									setButtonLayoutData(button);
									createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL,
											true);
								}
							};
							phasesAndGoalsDialog.setTitle("Phases and Goals of " + project.getName());
							phasesAndGoalsDialog.setMessage("Select Phases and Goals from: " + project.getName()
									+ ((mavenVersion[0] != null && ("3.3.1".compareTo(mavenVersion[0]) > 0))
											? "\nLaunch selected goals disabled. Maven Version > 3.3.1 is required."
											: ""));
							phasesAndGoalsDialog.setImage(getImageForName(shell.getDisplay(), PHASES_AND_GOALS));
							phasesAndGoalsDialog.setHelpAvailable(false);
							phasesAndGoalsDialog.setContainerMode(true);
							phasesAndGoalsDialog.setInput(phases);
							phasesAndGoalsDialog.setExpandedElements(phases.keySet().toArray());
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

	private void launch(IProject project, MavenConsoleImpl mavenConsole, Map<String, List<MojoExecutionKey>> phases,
			String cliFlags, String goalsToRun, boolean useMavenBuildSpy, final String mode) {
		if (goalsToRun.length() > 0) {
			String eventSpyConsoleJarFilePath = "";
			if (useMavenBuildSpy) {
				try {
					URL eventSpyConsoleJarURL = FileLocator.toFileURL(new URL(
							"platform:/fragment/org.eclipse.m2e.core.ui.phasesandgoals/mavenbuildspy/mavenbuildspy.jar"));
					eventSpyConsoleJarFilePath = "\"-Dmaven.ext.class.path=" + eventSpyConsoleJarURL.getFile() + "\"";
				} catch (MalformedURLException e) {
				} catch (IOException e) {
				}
			}
			IDialogSettings dialogSettings = M2EUIPluginActivator.getDefault().getDialogSettings();
			if (cliFlags.trim().length() > 0) {
				dialogSettings.put("phasesandgoals.cliflags", cliFlags.trim());
			} else {
				dialogSettings.put("phasesandgoals.cliflags", "");
			}
			ILaunchConfiguration launchConfiguration = createLaunchConfiguration(project,
					(eventSpyConsoleJarFilePath.trim().length() > 0 ? eventSpyConsoleJarFilePath + " " : "")
							+ (cliFlags.trim().length() > 0 ? cliFlags.trim() + " " : "") + goalsToRun);
			DebugUITools.launch(launchConfiguration, mode);
		}
	}

	private String goalsToRun(IProject project, MavenConsoleImpl mavenConsole,
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
			return sb.toString();
		}
		return null;
	}

	private void toConsole(IProject project, MavenConsoleImpl mavenConsole,
			Map<String, List<MojoExecutionKey>> phases) {
		mavenConsole.info("Project: " + project.getName());
		Set<Entry<String, List<MojoExecutionKey>>> entrySet = phases.entrySet();
		for (Entry<String, List<MojoExecutionKey>> entry : entrySet) {
			String phase = entry.getKey();
			String lifecycle = phaseToLikelyLifecycle.get(phase);
			if (lifecycle == null) {
				lifecycle = OTHER;
			}
			List<MojoExecutionKey> goals = entry.getValue();
			mavenConsole.info("|");
			mavenConsole.info("+-- Phase: " + phase + " (Lifecycle: " + lifecycle + ")");
			for (MojoExecutionKey pluginExecutionMetadata : goals) {
				mavenConsole.info("|   |");
				mavenConsole.info("|   +-- Goal: " + goal(pluginExecutionMetadata));
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
			artifactId = artifactId.substring(0, artifactId.length() - "-maven-plugin".length()); //$NON-NLS-1$
		} else if (artifactId.startsWith("maven-") && artifactId.endsWith("-plugin")) { //$NON-NLS-1$ //$NON-NLS-2$
			artifactId = artifactId.substring("maven-".length(), artifactId.length() - "-plugin".length()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return artifactId;
	}

	private ILaunchConfiguration createLaunchConfiguration(IContainer basedir, String goals) {
		try {
			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType launchConfigurationType = launchManager
					.getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

			String launchSafeGoalName = goals.replace(':', '-');

			ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, //
					NLS.bind(Messages.ExecutePomAction_executing, launchSafeGoalName,
							basedir.getLocation().toString().replace('/', '-')));
			workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, basedir.getLocation().toOSString());
			workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goals);
			workingCopy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
			workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "${project}"); //$NON-NLS-1$
			workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);

			setProjectConfiguration(workingCopy, basedir);

			IPath path = getJREContainerPath(basedir);
			if (path != null) {
				workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
						path.toPortableString());
			}

			// TODO when launching Maven with debugger consider to add the
			// following property
			// -Dmaven.surefire.debug="-Xdebug
			// -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
			// -Xnoagent -Djava.compiler=NONE"

			return workingCopy;
		} catch (CoreException ex) {
		}
		return null;
	}

	private void setProjectConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IContainer basedir) {
		IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
		IFile pomFile = basedir.getFile(new Path(IMavenConstants.POM_FILE_NAME));
		IMavenProjectFacade projectFacade = projectManager.create(pomFile, false, new NullProgressMonitor());
		if (projectFacade != null) {
			ResolverConfiguration configuration = projectFacade.getResolverConfiguration();

			String selectedProfiles = configuration.getSelectedProfiles();
			if (selectedProfiles != null && selectedProfiles.length() > 0) {
				workingCopy.setAttribute(MavenLaunchConstants.ATTR_PROFILES, selectedProfiles);
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
				if (JavaRuntime.JRE_CONTAINER.equals(entry.getPath().segment(0))) {
					return entry.getPath();
				}
			}
		}
		return null;
	}

	public static void copyToClipboard(String string) {
		// Get Clipboard
		Clipboard clipboard = new Clipboard(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay());
		// Put the paths string into the Clipboard
		clipboard.setContents(new Object[] { string }, new Transfer[] { TextTransfer.getInstance() });
	}
}
