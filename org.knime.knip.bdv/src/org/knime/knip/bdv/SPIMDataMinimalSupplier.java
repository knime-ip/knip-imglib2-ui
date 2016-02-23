package org.knime.knip.bdv;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.function.Supplier;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;

public class SPIMDataMinimalSupplier implements Externalizable, Supplier<SpimDataMinimal> {

	private String path;

	private SpimDataMinimal data;

	public SPIMDataMinimalSupplier(final String path) {
		this.path = path;
		this.data = null;
	}

	public SPIMDataMinimalSupplier(final SpimDataMinimal data, final String path) {
		this(path);
		this.data = data; 
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(path);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		path = in.readUTF();
	}

	@Override
	public SpimDataMinimal get() {
		
		
		final XmlIoSpimDataMinimal io = new XmlIoSpimDataMinimal();
		try {
			if (data == null)
				data = io.load(path);

			return data;
		} catch (SpimDataException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
