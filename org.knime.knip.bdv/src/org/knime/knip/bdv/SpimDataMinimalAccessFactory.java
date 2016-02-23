package org.knime.knip.bdv;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.knip2.core.cells.RichIterableRAIFileStoreCell;
import org.knime.knip2.core.cells.factories.AbstractDataCellFactory;
import org.knime.knip2.core.cells.factories.DataCellFactory;
import org.knime.knip2.core.cells.types.ImageFileStoreCell;
import org.knime.knip2.core.storage.FileStoreStorage;
import org.knime.knip2.core.tree.Access;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.axis.CalibratedAxis;
import net.imagej.space.CalibratedSpace;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin(type = DataCellFactory.class)
public class SpimDataMinimalAccessFactory<T extends NativeType<T> & RealType<T>> extends AbstractDataCellFactory {

	@Parameter
	private SPIMDataMinimalSupplier data;

	@Parameter
	private int setupId;

	@Parameter
	private int timeId;

	@Parameter
	private int viewId;

	@Parameter
	private CalibratedSpace<CalibratedAxis> cs;

	@Override
	public DataType getDataType() {
		return DataType.getType(RichIterableRAIFileStoreCell.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public DataCell createCell() {
		final FileStoreStorage storage = new FileStoreStorage(getExecutionContext());

		@SuppressWarnings("rawtypes")
		final Access access = getAccess(storage, cs, CalibratedSpace.class);

		return new ImageFileStoreCell<T>(
				new SpimDataMinimalAccess<FileStoreStorage, T>(storage, data, setupId, timeId, viewId), access);
	}

}
