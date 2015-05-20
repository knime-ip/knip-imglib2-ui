package org.knime.knip.bdv;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import bdv.tools.brightness.BrightnessDialog.ColorsPanel;
import bdv.tools.brightness.BrightnessDialog.MinMaxPanels;
import bdv.tools.brightness.SetupAssignments;

public class BrightnessPanel extends JPanel
{
	public BrightnessPanel( final SetupAssignments setupAssignments )
	{
		super( new BorderLayout() );
		final MinMaxPanels minMaxPanels = new MinMaxPanels( setupAssignments, null, false );
		final ColorsPanel colorsPanel = new ColorsPanel( setupAssignments );
		add( minMaxPanels, BorderLayout.NORTH );
		add( colorsPanel, BorderLayout.SOUTH );
		setupAssignments.setUpdateListener( new SetupAssignments.UpdateListener()
		{
			@Override
			public void update()
			{
				colorsPanel.recreateContent();
				minMaxPanels.recreateContent();
			}
		} );
	}
}
