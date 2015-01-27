package org.knime.knip.imglib2.ui;

import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataValue;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.nodes.view.TableCellView;
import org.knime.knip.base.nodes.view.TableCellViewFactory;

public class ImgLib2Viewer<T extends RealType<T>> implements
		TableCellViewFactory {

	@Override
	public TableCellView[] createTableCellViews() {
		return new TableCellView[] { new ImgLib2TableCellView<T>() };
	}

	@Override
	public final Class<? extends DataValue> getDataValueClass() {
		return ImgPlusValue.class;
	}

}
