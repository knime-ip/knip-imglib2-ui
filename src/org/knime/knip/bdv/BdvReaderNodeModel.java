package org.knime.knip.bdv;

import java.io.File;
import java.io.IOException;

import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

import org.eclipse.core.runtime.internal.adaptor.ContextFinder;
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
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;

public class BdvReaderNodeModel extends NodeModel implements BufferedDataTableHolder {

	private final SettingsModelString xmlFileModel;
	private BufferedDataTable outputTable;

	static SettingsModelString createXmlFileModel() {
		return new SettingsModelString("myxml", "");
	}

	public BdvReaderNodeModel() {
		super(0, 1);
		xmlFileModel = createXmlFileModel();
	}

	private DataTableSpec createSpec() {
		return new DataTableSpec(new DataColumnSpec[] { new DataColumnSpecCreator("image", ImgPlusCell.TYPE).createSpec() });
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		return new DataTableSpec[] { createSpec() };
	}

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
		final ImgPlusCellFactory factory = new ImgPlusCellFactory(exec);

		System.out.println("BdvReaderNodeModel.execute()");
		System.out.println(xmlFileModel.getStringValue());
		final XmlIoSpimDataMinimal io = new XmlIoSpimDataMinimal();

		Thread.currentThread().setContextClassLoader(new ContextFinder(getClass().getClassLoader()));
		final SpimDataMinimal spimData = io.load(xmlFileModel.getStringValue());
		final BufferedDataContainer container = exec.createDataContainer(createSpec());
		container.addRowToTable(new DefaultRow("0", getImg(spimData, new ViewId(18, 0), factory)));
		container.close();
		outputTable = container.getTable();
		return new BufferedDataTable[] { outputTable };
	}

	private static <T extends RealType<T> & NativeType<T>> ImgPlusCell<T> getImg(
			final SpimDataMinimal spimData,
			final ViewId id,
			final ImgPlusCellFactory factory) throws IOException {
		final BasicImgLoader<?> imgLoader = spimData.getSequenceDescription().getImgLoader();
		if (imgLoader.getImageType() instanceof RealType && imgLoader.getImageType() instanceof NativeType) {
			@SuppressWarnings("unchecked")
			final BasicImgLoader<T> typedImgLoader = (BasicImgLoader<T>) imgLoader;
			final RandomAccessibleInterval<T> img = typedImgLoader.getImage(id);
			final ImgView<T> imgView = new ImgView<T>(img, Util.getArrayOrCellImgFactory(img, Util.getTypeFromInterval(img)));
			return factory.createCell(new ImgPlus<T>(imgView));
		} else
			throw new IllegalArgumentException("wrong data type");
	}

	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
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
