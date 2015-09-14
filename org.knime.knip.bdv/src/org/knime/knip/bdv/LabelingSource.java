package org.knime.knip.bdv;

import bdv.viewer.Interpolation;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.numeric.IntegerType;

public class LabelingSource<L, I extends IntegerType<I>> extends ImgPlusSource<I> {

	public LabelingSource(ImgLabeling<L, I> img, int dT, int channel, VoxelDimensions voxelDimensions,
			AffineTransform3D sourceTransform) {
		super(img.getIndexImg(), dT, channel, voxelDimensions, sourceTransform);
	}

	@Override
	public RealRandomAccessible<I> getInterpolatedSource(int t, int level, Interpolation method) {
		return super.getInterpolatedSource(t, level, Interpolation.NEARESTNEIGHBOR);
	}

}