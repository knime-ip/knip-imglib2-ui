package org.knime.knip.bdv;

import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;

public class ImgPlusSource< T extends NumericType< T > > implements Source< T >
{
	private final RandomAccessibleInterval< T > img;

	private final int dT;

	private final String name;

	private final T type;

	private final static int numInterpolationMethods = 2;

	private final static int iNearestNeighborMethod = 0;

	private final static int iNLinearMethod = 1;

	private final InterpolatorFactory< T, RandomAccessible< T > >[] interpolatorFactories;

	private final AffineTransform3D sourceTransform;

	private final VoxelDimensions voxelDimensions;

	@SuppressWarnings( "unchecked" )
	public ImgPlusSource( final RandomAccessibleInterval< T > img, final int dT, final int channel, final VoxelDimensions voxelDimensions, final AffineTransform3D sourceTransform )
	{
		this.img = img;
		this.dT = dT;
		this.voxelDimensions = voxelDimensions;
		this.sourceTransform = sourceTransform;
		name = "channel " + channel;
		type = Util.getTypeFromInterval( img );
		interpolatorFactories = new InterpolatorFactory[ numInterpolationMethods ];
		interpolatorFactories[ iNearestNeighborMethod ] = new NearestNeighborInterpolatorFactory< T >();
		interpolatorFactories[ iNLinearMethod ] = new NLinearInterpolatorFactory< T >();
	}

	@Override
	public boolean isPresent( final int t )
	{
		return ( dT == -1 && t == 0 ) || ( dT >= 0 && t >= img.min( dT ) && t <= img.max( dT ) );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return dT == -1 ? img : Views.hyperSlice( img, dT, t );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return Views.interpolate( Views.extendZero( getSource( t, level ) ), interpolatorFactories[ method == Interpolation.NLINEAR ? iNLinearMethod : iNearestNeighborMethod ] );
	}

	@Override
	public void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		transform.set( sourceTransform );
	}

	@Override
	public AffineTransform3D getSourceTransform( final int t, final int level )
	{
		final AffineTransform3D transform = new AffineTransform3D();
		getSourceTransform( t, level, transform );
		return transform;
	}

	@Override
	public T getType()
	{
		return type;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return voxelDimensions;
	}

	@Override
	public int getNumMipmapLevels()
	{
		return 1;
	}
}