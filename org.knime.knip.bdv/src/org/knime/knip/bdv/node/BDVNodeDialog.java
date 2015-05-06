package org.knime.knip.bdv.node;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;

public class BDVNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public BDVNodeDialog() {
		super();

		addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString(BDVNodeModel.CFG_IMG_COL, ""),
				"Image Column", 0, false, false, ImgPlusValue.class));

		addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString(BDVNodeModel.CFG_LABELING_COL,
						""), "Labeling Column", 0, true, LabelingValue.class));
	}
}
