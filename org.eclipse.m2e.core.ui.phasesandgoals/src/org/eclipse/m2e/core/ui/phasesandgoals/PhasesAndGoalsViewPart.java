package org.eclipse.m2e.core.ui.phasesandgoals;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

public class PhasesAndGoalsViewPart extends ViewPart {

	private Text text;

	public PhasesAndGoalsViewPart() {
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gridLayout = new GridLayout(1, true);
		parent.setLayout(gridLayout);
		text = new Text(parent, SWT.MULTI);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		text.setLayoutData(gridData);
	}

	@Override
	public void setFocus() {
		text.setFocus();
	}

}
