package org.knime.knip.bdv;

import bdv.ViewerImgLoader;
import bdv.img.cache.Cache;
import bdv.img.hdf5.Hdf5ImageLoader.SetupImgLoader;
import mpicbg.spim.data.sequence.MultiResolutionImgLoader;

public class KNIPHdf5Loader implements ViewerImgLoader, MultiResolutionImgLoader {

	public KNIPHdf5Loader() {
//		this.existingHdf5Reade
		
	}

	@Override
	public SetupImgLoader getSetupImgLoader(int setupId) {
		return null;
	}

	@Override
	public Cache getCache() {
		return null;
	}

}
