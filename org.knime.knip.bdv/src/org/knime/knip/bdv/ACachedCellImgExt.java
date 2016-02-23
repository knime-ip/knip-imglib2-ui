package org.knime.knip.bdv;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.util.ArrayList;

import org.knime.knip2.core.storage.FileStoreStorage;
import org.knime.knip2.core.storage.Storage;
import org.knime.knip2.core.tree.ext.AbstractFlushableAccess;

import bdv.img.cache.CacheHints;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.LoadingStrategy;
import bdv.img.cache.VolatileGlobalCellCache.VolatileCellCache;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Partition;
import bdv.spimdata.SequenceDescriptionMinimal;
import mpicbg.spim.data.sequence.TimePoints;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.type.NativeType;

public class ACachedCellImgExt<S extends Storage<S>, T extends NativeType<T>>
		extends AbstractFlushableAccess<FileStoreStorage, CachedCellImg<T, VolatileAccess>> {

	@Override
	public void write(ObjectOutput output, CachedCellImg<T, VolatileAccess> obj) throws IOException {

		try {
			Field cacheField = obj.getClass().getDeclaredField("cache");
			cacheField.setAccessible(true);
			final CellCache<VolatileAccess> cache = (CellCache<VolatileAccess>) cacheField.get(obj);
			cacheField.setAccessible(false);

		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public CachedCellImg<T, VolatileAccess> read(ObjectInput input) throws IOException {

		LoadingStrategy strategy = LoadingStrategy.values()[input.readInt()];
		int timePoint = input.readInt();
		int setupId = input.readInt();
		int level = input.readInt();
		int queuePriority = input.readInt();

		boolean enqueueToFront = input.readBoolean();
		final CacheHints hints = new CacheHints(strategy, queuePriority, enqueueToFront);
		
		final String file = input.readUTF();
		ArrayList<Partition> list = new ArrayList<>();
		
		SequenceDescriptionMinimal minimal = new SequenceDescriptionMinimal(new TimePoints(timepoints), new );
		Hdf5ImageLoader loader = new Hdf5ImageLoader(path, list, );		
		
		CellCache<VolatileAccess> cache = new VolatileCellCache<>(timePoint, setupId, level, hints, );
		VolatileImgCells<VolatileAccess> cells = new VolatileImgCells<VolatileAccess>();
		CachedCellImg<T, VolatileAccess> img = new CachedCellImg<>(cells);

		return null;
	}

}
