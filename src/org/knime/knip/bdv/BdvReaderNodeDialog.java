package org.knime.knip.bdv;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class BdvReaderNodeDialog extends DefaultNodeSettingsPane {
	public BdvReaderNodeDialog()
	{
		final SettingsModelString xmlFileModel = BdvReaderNodeModel.createXmlFileModel();
		addDialogComponent( new DialogComponentFileChooser( xmlFileModel, "bla", ".xml" ) );
	}
}
