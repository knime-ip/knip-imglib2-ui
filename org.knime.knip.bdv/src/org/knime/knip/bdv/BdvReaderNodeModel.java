package org.knime.knip.bdv;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import org.eclipse.osgi.internal.framework.ContextFinder;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip2.core.KNIP2;
import org.knime.knip2.core.cells.factories.DataCellFactory;

import bdv.spimdata.SpimDataMinimal;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHints;
import mpicbg.spim.data.sequence.ImgLoader;
import net.imagej.axis.Axes;
import net.imagej.axis.DefaultLinearAxis;
import net.imagej.space.CalibratedSpace;
import net.imagej.space.DefaultCalibratedSpace;

public class BdvReaderNodeModel extends NodeModel implements BufferedDataTableHolder {

	private final SettingsModelString xmlFileModel;
	private BufferedDataTable outputTable;
	private DataCellFactory m_cellFactory;

	static SettingsModelString createXmlFileModel() {
		return new SettingsModelString("myxml", "");
	}

	public BdvReaderNodeModel() {
		super(0, 1);
		xmlFileModel = createXmlFileModel();
	}

	private DataTableSpec createSpec() {
		m_cellFactory = KNIP2.ops().op(DataCellFactory.class, ExecutionContext.class, SpimDataMinimal.class, -1, -1,
				ImgLoader[].class, CalibratedSpace.class);

		return new DataTableSpec(
				new DataColumnSpec[] { new DataColumnSpecCreator("image", m_cellFactory.getDataType()).createSpec() });
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		return new DataTableSpec[] { createSpec() };
	}

	@SuppressWarnings("restriction")
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		// populate arguments

		System.out.println("BdvReaderNodeModel.execute()");
		System.out.println(xmlFileModel.getStringValue());
		Thread.currentThread().setContextClassLoader(new ContextFinder(getClass().getClassLoader()));

		final BufferedDataContainer container = exec.createDataContainer(createSpec());

		final DefaultCalibratedSpace cs = new DefaultCalibratedSpace(new DefaultLinearAxis(Axes.X),
				new DefaultLinearAxis(Axes.Y), new DefaultLinearAxis(Axes.Z));

		final SPIMDataMinimalSupplier supplier = new SPIMDataMinimalSupplier(xmlFileModel.getStringValue());

		int i = 0;
		for (Entry<Integer, ? extends BasicViewSetup> setup : supplier.get().getSequenceDescription().getViewSetups()
				.entrySet()) {

			// create simple container
			final DataCellFactory factory = (DataCellFactory) KNIP2.ops().module(m_cellFactory.getClass().newInstance(),
					exec, supplier, 0, 0, 0, i, new ImgLoaderHints[0], cs).getDelegateObject();

			container.addRowToTable(new DefaultRow("i" + i++, factory.createCell()));

		}

		container.close();
		outputTable = container.getTable();
		return new BufferedDataTable[] { outputTable };
	}

	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		xmlFileModel.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		xmlFileModel.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		xmlFileModel.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
	}

	@Override
	public BufferedDataTable[] getInternalTables() {
		return new BufferedDataTable[] { outputTable };
	}

	@Override
	public void setInternalTables(final BufferedDataTable[] tables) {
		outputTable = tables[0];
	}

}
