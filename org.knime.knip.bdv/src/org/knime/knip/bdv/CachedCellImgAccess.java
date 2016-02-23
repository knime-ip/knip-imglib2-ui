//package org.knime.knip.bdv;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.Flushable;
//import java.io.IOException;
//import java.io.ObjectInput;
//import java.io.ObjectOutput;
//import java.lang.reflect.Field;
//import java.util.ArrayList;
//
//import org.knime.core.data.filestore.FileStore;
//import org.knime.knip.core.io.externalization.BufferedDataInputStream;
//import org.knime.knip2.core.ext.buffered.BufferedDataOutputStream;
//import org.knime.knip2.core.storage.FileStoreStorage;
//import org.knime.knip2.core.tree.ext.AbstractFlushableAccess;
//
//import bdv.img.cache.CacheArrayLoader;
//import bdv.img.cache.CacheHints;
//import bdv.img.cache.CachedCellImg;
//import bdv.img.cache.LoadingStrategy;
//import bdv.img.cache.VolatileGlobalCellCache.VolatileCellCache;
//import bdv.img.cache.VolatileImgCells;
//import bdv.img.cache.VolatileImgCells.CellCache;
//import bdv.img.hdf5.Hdf5VolatileShortArrayLoader;
//import bdv.img.hdf5.Partition;
//import bdv.img.hdf5.ViewLevelId;
//import ch.systemsx.cisd.hdf5.HDF5Factory;
//import ch.systemsx.cisd.hdf5.IHDF5Writer;
//import net.imglib2.img.NativeImg;
//import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
//import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
//import net.imglib2.img.cell.CellImg;
//import net.imglib2.type.NativeType;
//import net.imglib2.type.numeric.integer.UnsignedShortType;
//import net.imglib2.type.volatiles.VolatileUnsignedShortType;
//import net.imglib2.util.Fraction;
//
//public class CachedCellImgAccess extends AbstractFlushableAccess<FileStoreStorage, CachedCellImg<?, VolatileAccess>>
//		implements Flushable {
//
//	private String key;
//	private long offset;
//
//	public CachedCellImgAccess(final FileStoreStorage storage, final CachedCellImg<?, VolatileAccess> img) {
//		super(storage, img);
//	}
//
//	public CachedCellImgAccess() {
//		//
//	}
//
//	@Override
//	protected CachedCellImg<?, VolatileAccess> read(final FileStoreStorage storage) throws IOException {
//		
//		BufferedDataInputStream input = new BufferedDataInputStream(new FileInputStream(storage.getFileStore(key).getFile()));
//		
//		LoadingStrategy strategy = LoadingStrategy.values()[input.readInt()];
//		int timePoint = input.readInt();
//		int setupId = input.readInt();
//		int level = input.readInt();
//		int queuePriority = input.readInt();
//
//		boolean enqueueToFront = input.readBoolean();
//		
//		final CacheHints hints = new CacheHints(strategy, queuePriority, enqueueToFront);
//		
//		final String file = input.readUTF();
//		ArrayList<Partition> list = new ArrayList<>();
//		
//		IHDF5Writer open = HDF5Factory.open(file);	
//		
//		final CachedCellImg< UnsignedShortType, VolatileShortArray >  img = prepareCachedImage( id, LoadingStrategy.BLOCKING );
//		final UnsignedShortType linkedType = new UnsignedShortType( img );
//		img.setLinkedType( linkedType );
//
//		
//		Hdf5VolatileShortArrayLoader loader = new Hdf5VolatileShortArrayLoader(HDF5Factory.openForReading(file));
//		
//		CellCache<VolatileAccess> cache = new VolatileCellCache<>(timePoint, setupId, level, hints, );
//		VolatileImgCells<VolatileAccess> cells = new VolatileImgCells<VolatileAccess>();
//		CachedCellImg<T, VolatileAccess> img = new CachedCellImg<>(cells);
//		
//	
//	}
//
//	/**
//	 * (Almost) create a {@link CellImg} backed by the cache. The created image
//	 * needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked
//	 * type} before it can be used. The type should be either
//	 * {@link UnsignedShortType} and {@link VolatileUnsignedShortType}.
//	 */
//	protected <T extends NativeType<T>> CachedCellImg<T, VolatileShortArray> prepareCachedImage(final ViewLevelId id,
//			final LoadingStrategy loadingStrategy) {
//		open();
//		final int timepointId = id.getTimePointId();
//		final int level = id.getLevel();
//
//		final long[] dimensions = getDimsAndExistence(id).getDimensions();
//		final int[] cellDimensions = mipmapInfo.getSubdivisions()[level];
//
//		final int priority = mipmapInfo.getMaxLevel() - level;
//		final CacheHints cacheHints = new CacheHints(loadingStrategy, priority, false);
//		final CellCache<VolatileShortArray> c = cache.new VolatileCellCache<VolatileShortArray>(timepointId, setupId,
//				level, cacheHints, shortLoader);
//		final VolatileImgCells<VolatileShortArray> cells = new VolatileImgCells<VolatileShortArray>(c, new Fraction(),
//				dimensions, cellDimensions);
//		final CachedCellImg<T, VolatileShortArray> img = new CachedCellImg<T, VolatileShortArray>(cells);
//		return img;
//	}
//
//	@SuppressWarnings("unchecked")
//	private <X> X dirtyAccess(final Object object, String fieldname) {
//		try {
//			final Field field = object.getClass().getDeclaredField(fieldname);
//			field.setAccessible(true);
//			final X x = (X) field.get(object);
//			field.setAccessible(false);
//			return x;
//		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
//			e.printStackTrace();
//			throw new RuntimeException(e);
//		}
//	}
//
//	@Override
//	protected void write(final FileStoreStorage storage) throws IOException {
//
//		// after persist has been written we _always_ get the file from
//		// disc. bad luck if you don't cache my friend ;-)
//		try {
//			final FileStore store = getStorage().getFileStore(key);
//			key = store.toString();
//			final File file = store.getFile();
//			offset = file.length();
//
//			final BufferedDataOutputStream out = new BufferedDataOutputStream(new FileOutputStream(file));
//
//			out.flush();
//
//			releaseToCache();
//		} catch (IOException e) {
//			// TODO logging
//			e.printStackTrace();
//		}
//
//		final FileStore store = storage.getFileStore(key);
//
//		try {
//			final VolatileCellCache<VolatileAccess> cache = dirtyAccess(get(), "cache");
//
//			int timePoint = dirtyAccess(get(), "timepoint");
//			int setupField = dirtyAccess(get(), "setup");
//			int levelField = dirtyAccess(get(), "level");
//
//			CacheHints cacheHintsField = dirtyAccess(get(), "cacheHints");
//
//			CacheArrayLoader<VolatileAccess> loader = dirtyAccess(get(), "loader");
//
//		} catch (SecurityException | IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}
//
//	@Override
//	protected void finalize() throws Throwable {
//
//	}
//
//	@Override
//	protected void writeState(ObjectOutput out) {
//		try {
//			out.writeUTF(getStorage().toString());
//		} catch (IOException e) {
//			e.printStackTrace();
//			throw new RuntimeException(e);
//		}
//	}
//
//	@Override
//	protected void readState(ObjectInput in) {
//		try {
//			key = in.readUTF();
//		} catch (IOException e) {
//			e.printStackTrace();
//			throw new RuntimeException(e);
//		}
//	}
//
//}
