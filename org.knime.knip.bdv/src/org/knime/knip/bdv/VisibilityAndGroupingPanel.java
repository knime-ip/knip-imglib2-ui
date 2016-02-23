package org.knime.knip.bdv;

import static bdv.viewer.VisibilityAndGrouping.Event.DISPLAY_MODE_CHANGED;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import bdv.tools.VisibilityAndGroupingDialog.GroupingPanel;
import bdv.tools.VisibilityAndGroupingDialog.VisibilityPanel;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.VisibilityAndGrouping.Event;

public class VisibilityAndGroupingPanel extends JPanel {
	private final VisibilityPanel visibilityPanel;

	private final GroupingPanel groupingPanel;

	private final ModePanel modePanel;

	public VisibilityAndGroupingPanel(final VisibilityAndGrouping visibilityAndGrouping) {
		super(new BorderLayout());

		visibilityPanel = new VisibilityPanel(visibilityAndGrouping);
		visibilityAndGrouping.addUpdateListener(visibilityPanel);
		visibilityPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "visibility"),
						BorderFactory.createEmptyBorder(2, 2, 2, 2))));

		groupingPanel = new GroupingPanel(visibilityAndGrouping);
		visibilityAndGrouping.addUpdateListener(groupingPanel);
		groupingPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "grouping"),
						BorderFactory.createEmptyBorder(2, 2, 2, 2))));
		groupingPanel.setVisible(false);

		modePanel = new ModePanel(visibilityAndGrouping, groupingPanel);
		visibilityAndGrouping.addUpdateListener(modePanel);

		final Box box = Box.createVerticalBox();
		box.add(visibilityPanel);
		box.add(modePanel);
		box.add(groupingPanel);
		add(box, BorderLayout.NORTH);
	}

	public static class ModePanel extends JPanel implements VisibilityAndGrouping.UpdateListener {
		private static final long serialVersionUID = 1L;

		private final VisibilityAndGrouping visibility;

		private JCheckBox groupingBox;

		private JCheckBox fusedModeBox;

		private final GroupingPanel groupingPanel;

		public ModePanel(final VisibilityAndGrouping visibilityAndGrouping, final GroupingPanel groupingPanel) {
			super(new GridBagLayout());
			this.visibility = visibilityAndGrouping;
			this.groupingPanel = groupingPanel;
			recreateContent();
			update();
		}

		protected void recreateContent() {
			final GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(0, 5, 0, 5);

			c.gridwidth = 1;
			c.anchor = GridBagConstraints.LINE_START;
			groupingBox = new JCheckBox();
			groupingBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					visibility.setGroupingEnabled(groupingBox.isSelected());
				}
			});
			c.gridx = 0;
			c.gridy = 0;
			add(groupingBox, c);
			c.gridx = 1;
			add(new JLabel("enable grouping"), c);

			fusedModeBox = new JCheckBox();
			fusedModeBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					visibility.setFusedEnabled(fusedModeBox.isSelected());
				}
			});
			c.gridx = 0;
			c.gridy = 1;
			add(fusedModeBox, c);
			c.gridx = 1;
			add(new JLabel("enable fused mode"), c);
		}

		protected void update() {
			synchronized (visibility) {
				groupingBox.setSelected(visibility.isGroupingEnabled());
				fusedModeBox.setSelected(visibility.isFusedEnabled());
				if (visibility.isGroupingEnabled())
					groupingPanel.setVisible(true);
			}
		}

		@Override
		public void visibilityChanged(final Event e) {
			switch (e.id) {
			case DISPLAY_MODE_CHANGED:
				update();
				break;
			}
		}
	}
}
