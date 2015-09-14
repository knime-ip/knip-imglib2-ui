package org.knime.knip.bdv;

import java.util.List;
import java.util.Set;

import org.knime.knip.core.awt.labelingcolortable.ExtendedLabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.LabelingColorTable;
import org.knime.knip.core.awt.labelingcolortable.RandomMissingColorHandler;
import org.knime.knip.core.data.img.LabelingMetadata;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.space.CalibratedSpace;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class BDVUtil {

	public static <L, I extends IntegerType<I>> int createSourcesAndSetups(final ImgLabeling<L, I> rai,
			LabelingMetadata space, final List<ConverterSetup> converterSetups,
			final List<SourceAndConverter<?>> sources) {
		int numTimePoints = 1;
		int numSetups = 1;
		int dC = space.dimensionIndex(Axes.CHANNEL);
		int dT = space.dimensionIndex(Axes.TIME);
		int dX = space.dimensionIndex(Axes.X);
		int dY = space.dimensionIndex(Axes.Y);
		int dZ = space.dimensionIndex(Axes.Z);

		if (rai.numDimensions() == 2) {
			throw new IllegalArgumentException("Input image must have at least 3 dimensions!");
		} else if (rai.numDimensions() == 3) {
			dC = -1;
			dT = -1;
			dX = 0;
			dY = 1;
			dZ = 2;
		} else if (rai.numDimensions() == 4) {
			if (dC == -1 && dT == -1)
				throw new IllegalArgumentException(
						"Four dimensional input image without CHANNEL axis must contain a TIME axis! Neither of both found...");

			if (dT != -1)
				numTimePoints = (int) rai.dimension(dT);

			if (dC != -1)
				numSetups = (int) rai.dimension(dC);
		} else if (rai.numDimensions() == 5) {
			if (dC == -1 || dT == -1)
				throw new IllegalArgumentException("Five dimensional input image must contain CHANNEL and TIME axis!");

			numTimePoints = (int) rai.dimension(dT);
			numSetups = (int) rai.dimension(dC);
		}

		final VoxelDimensions voxelDimensions = new FinalVoxelDimensions(space.axis(dX).unit(), space.averageScale(dX),
				space.averageScale(dY), space.averageScale(dZ));

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.set(voxelDimensions.dimension(0), 0, 0, 0, 0, voxelDimensions.dimension(1), 0, 0, 0, 0,
				voxelDimensions.dimension(2), 0);

		initSetupsLabeling(rai, space, dC, dT, numSetups, voxelDimensions, sourceTransform, converterSetups, sources);

		return numTimePoints;
	}

	public static <T extends RealType<T>> int createSourcesAndSetups(final RandomAccessibleInterval<T> rai,
			CalibratedSpace<CalibratedAxis> space, final List<ConverterSetup> converterSetups,
			final List<SourceAndConverter<?>> sources) {
		int numTimePoints = 1;
		int numSetups = 1;
		int dC = space.dimensionIndex(Axes.CHANNEL);
		int dT = space.dimensionIndex(Axes.TIME);
		int dX = space.dimensionIndex(Axes.X);
		int dY = space.dimensionIndex(Axes.Y);
		int dZ = space.dimensionIndex(Axes.Z);

		if (rai.numDimensions() == 2) {
			throw new IllegalArgumentException("Input image must have at least 3 dimensions!");
		} else if (rai.numDimensions() == 3) {
			dC = -1;
			dT = -1;
			dX = 0;
			dY = 1;
			dZ = 2;
		} else if (rai.numDimensions() == 4) {
			if (dC == -1 && dT == -1)
				throw new IllegalArgumentException(
						"Four dimensional input image without CHANNEL axis must contain a TIME axis! Neither of both found...");

			if (dT != -1)
				numTimePoints = (int) rai.dimension(dT);

			if (dC != -1)
				numSetups = (int) rai.dimension(dC);
		} else if (rai.numDimensions() == 5) {
			if (dC == -1 || dT == -1)
				throw new IllegalArgumentException("Five dimensional input image must contain CHANNEL and TIME axis!");

			numTimePoints = (int) rai.dimension(dT);
			numSetups = (int) rai.dimension(dC);
		}

		final VoxelDimensions voxelDimensions = new FinalVoxelDimensions(space.axis(dX).unit(), space.averageScale(dX),
				space.averageScale(dY), space.averageScale(dZ));

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.set(voxelDimensions.dimension(0), 0, 0, 0, 0, voxelDimensions.dimension(1), 0, 0, 0, 0,
				voxelDimensions.dimension(2), 0);

		initSetupsRealType(rai, dC, dT, numSetups, voxelDimensions, sourceTransform, converterSetups, sources);

		return numTimePoints;
	}

	private static <T extends RealType<T>> void initSetupsRealType(final RandomAccessibleInterval<T> rai, final int dC,
			final int dT, final int numSetups, final VoxelDimensions voxelDimensions,
			final AffineTransform3D sourceTransform, final List<ConverterSetup> converterSetups,
			final List<SourceAndConverter<?>> sources) {
		final T type = Util.getTypeFromInterval(rai);
		final double typeMin = Math.max(0, Math.min(type.getMinValue(), 65535));
		final double typeMax = Math.max(0, Math.min(type.getMaxValue(), 65535));
		for (int setup = 0; setup < numSetups; ++setup) {
			final ImgPlusSource<T> source = (dC == -1)
					? new ImgPlusSource<T>(rai, dT, setup, voxelDimensions, sourceTransform)
					: new ImgPlusSource<T>(Views.hyperSlice(rai, dC, setup), dT > dC ? dT - 1 : dT, setup,
							voxelDimensions, sourceTransform);
			final RealARGBColorConverter<T> converter = new RealARGBColorConverter.Imp1<T>(typeMin, typeMax);
			converter.setColor(new ARGBType(0xffffffff));

			final SourceAndConverter<T> soc = new SourceAndConverter<T>(source, converter);

			sources.add(soc);
			converterSetups.add(new RealARGBColorConverterSetup(setup, converter));
		}
	}

	private static <L, I extends IntegerType<I>> void initSetupsLabeling(final ImgLabeling<L, I> rai,
			final LabelingMetadata metadata, final int dC, final int dT, final int numSetups,
			final VoxelDimensions voxelDimensions, final AffineTransform3D sourceTransform,
			final List<ConverterSetup> converterSetups, final List<SourceAndConverter<?>> sources) {
		for (int setup = 0; setup < numSetups; ++setup) {
			final ImgPlusSource<I> source = (dC == -1)
					? new ImgPlusSource<I>(rai.getIndexImg(), dT, setup, voxelDimensions, sourceTransform)
					: new ImgPlusSource<I>(Views.hyperSlice(rai.getIndexImg(), dC, setup), dT > dC ? dT - 1 : dT, setup,
							voxelDimensions, sourceTransform);

			final Converter<I, ARGBType> converter = new Converter<I, ARGBType>() {

				private final LabelingColorTable colorTable;

				{
					colorTable = new ExtendedLabelingColorTable(metadata.getLabelingColorTable(),
							new RandomMissingColorHandler());
				}

				@Override
				public void convert(I arg0, ARGBType arg1) {
					int curr = 0;
					for (final L label : rai.getMapping().labelsAtIndex(arg0.getInteger())) {
						curr += colorTable.getColor(label);
					}

					arg1.set(curr);
				}
			};

			sources.add(new SourceAndConverter<I>(source, converter));
		}
	}

}
