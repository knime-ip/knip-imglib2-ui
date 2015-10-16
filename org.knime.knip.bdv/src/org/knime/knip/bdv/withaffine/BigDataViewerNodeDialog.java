package org.knime.knip.bdv.withaffine;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.mvr.cells.AffineTransformValue;

public class BigDataViewerNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	public BigDataViewerNodeDialog() {
		super();

		addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString(BigDataViewerNodeModel.CFG_IMG_COL_A, ""), "Image Column ViewA", 0, false,
				false, ImgPlusValue.class));

		addDialogComponent(new DialogComponentColumnNameSelection(
				new SettingsModelString(BigDataViewerNodeModel.CFG_IMG_COL_B, ""), "Image Column ViewB", 0, false,
				false, ImgPlusValue.class));

		addDialogComponent(
				new DialogComponentColumnNameSelection(new SettingsModelString(BigDataViewerNodeModel.CFG_AFFINE_A, ""),
						"Affine Transform ViewA", 0, false, false, AffineTransformValue.class));

		addDialogComponent(
				new DialogComponentColumnNameSelection(new SettingsModelString(BigDataViewerNodeModel.CFG_AFFINE_B, ""),
						"Affine Transform ViewB", 0, false, false, AffineTransformValue.class));
	}
}
