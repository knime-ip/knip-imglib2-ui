package org.knime.knip.bdv;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.knime.knip2.core.storage.Storage;
import org.knime.knip2.core.tree.Access;

import mpicbg.spim.data.generic.sequence.ImgLoaderHints;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;

public class SpimDataMinimalAccess<S extends Storage<S>, T extends NativeType<T>>
		implements Access<S, RandomAccessibleInterval<T>> {

	private S storage;

	private SPIMDataMinimalSupplier token;

	private int viewId;

	private int setupId;

	private int timeId;

	private ImgLoaderHints[] hints;

	public SpimDataMinimalAccess() {
		// deserialization
	}

	public SpimDataMinimalAccess(final S storage, final SPIMDataMinimalSupplier supplier, int setupId, int timeId,
			int viewId, ImgLoaderHints... hints) {

		this.hints = hints;
		this.token = supplier;
		this.storage = storage;
		this.viewId = viewId;
		this.timeId = timeId;
		this.setupId = setupId;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(token);
		out.writeInt(setupId);
		out.writeInt(timeId);
		out.writeInt(viewId);

		out.writeInt(hints.length);

		for (int i = 0; i < hints.length; i++) {
			out.writeInt(hints[i].ordinal());
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		token = (SPIMDataMinimalSupplier) in.readObject();
		setupId = in.readInt();
		timeId = in.readInt();
		viewId = in.readInt();

		ImgLoaderHints[] hints = new ImgLoaderHints[in.readInt()];
		for (int i = 0; i < hints.length; i++) {
			hints[i] = ImgLoaderHints.values()[in.readInt()];
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public RandomAccessibleInterval<T> get() {
		return (RandomAccessibleInterval<T>) token.get().getSequenceDescription().getImgLoader()
				.getSetupImgLoader(viewId).getImage(timeId, hints);
	}

	@Override
	public void setStorage(S storage) {
		this.storage = storage;
	}

	@Override
	public S getStorage() {
		return storage;
	}

}
