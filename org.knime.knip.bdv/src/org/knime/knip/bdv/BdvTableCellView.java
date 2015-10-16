package org.knime.knip.bdv;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.knime.core.data.DataValue;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.nodes.view.TableCellView;

import bdv.tools.InitializeViewerState;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;

public class BdvTableCellView<T extends RealType<T>> implements TableCellView {

	private JPanel rootPanel;

	private DataValue dataValue;

	private BdvPanel bdvPanel;

	@Override
	public String getName() {
		return "BigDataViewer";
	}

	@Override
	public String getDescription() {
		return "BigDataViewer";
	}

	// TODO: @Christian: Always create a new panel? or re-use the same one?
	@Override
	public Component getViewComponent() {
		rootPanel = new JPanel(new BorderLayout());
		rootPanel.setVisible(true);
		return rootPanel;
	}

	@Override
	public void updateComponent(final DataValue valueToView) {
		if (dataValue == null || !(dataValue.equals(valueToView))) {
			@SuppressWarnings("unchecked")
			final ImgPlus<T> imgPlus = ((ImgPlusValue<T>) valueToView).getImgPlus();
			if (bdvPanel != null) {
				bdvPanel.stop();
				bdvPanel = null;
			}

			final ArrayList<ConverterSetup> converterSetups = new ArrayList<ConverterSetup>();
			final ArrayList<SourceAndConverter<?>> sources = new ArrayList<SourceAndConverter<?>>();

//			bdvPanel = new BdvPanel(converterSetups, sources, 800, 600,
//					BDVUtil.createSourcesAndSetups(imgPlus, imgPlus, converterSetups, sources));
			bdvPanel.addComponentListener(new ComponentAdapter() {
				boolean first = true;

				@Override
				public void componentResized(final ComponentEvent e) {
					if (first) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								bdvPanel.getViewer().setPreferredSize(null);
								InitializeViewerState.initTransform(bdvPanel.getViewer());
								InitializeViewerState.initBrightness(0.001, 0.999, bdvPanel.getViewer(),
										bdvPanel.getSetupAssignments());
							}
						});
						first = false;
					}
					System.out.println("componentResized done");
				}
			});
			rootPanel.removeAll();
			rootPanel.add(bdvPanel, BorderLayout.CENTER);
			bdvPanel.addKeybindingsTo(bdvPanel);
		}
	}

	@Override
	public void onClose() {
		if (bdvPanel != null) {
			rootPanel.removeAll();
			bdvPanel.stop();
			bdvPanel = null;
		}
	}

	@Override
	public void onReset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void loadConfigurationFrom(final ConfigRO config) {
	}

	@Override
	public void saveConfigurationTo(final ConfigWO config) {
	}
}
