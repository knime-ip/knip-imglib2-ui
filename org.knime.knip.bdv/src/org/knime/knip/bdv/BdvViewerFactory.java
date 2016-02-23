package org.knime.knip.bdv;

import java.util.List;

import org.knime.core.data.DataValue;
import org.knime.knip.base.nodes.view.TableCellView;
import org.knime.knip.base.nodes.view.TableCellViewFactory;
import org.knime.knip2.core.values.types.ImageValues.ImageValue;

import net.imglib2.type.numeric.RealType;

public class BdvViewerFactory<T extends RealType<T>> implements TableCellViewFactory {

	@Override
	public TableCellView[] createTableCellViews() {
		return new TableCellView[] { new BdvTableCellView<T>() };
	}

	@Override
	public final Class<? extends DataValue> getDataValueClass() {
		return ImageValue.class;
	}

	public boolean check(final List<Class<? extends DataValue>> values) {

		for (final Class<? extends DataValue> value : values) {
			if (!(ImageValue.class.isAssignableFrom(value))) {
				return false;
			}
		}
		return true;
	}

}
