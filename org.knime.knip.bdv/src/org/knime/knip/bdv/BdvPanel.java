package org.knime.knip.bdv;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import bdv.img.cache.Cache;
import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.bookmarks.BookmarksEditor;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.KeyProperties;
import bdv.viewer.InputActionBindings;
import bdv.viewer.NavigationActions;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class BdvPanel extends JPanel {
	private final ViewerPanel viewer;

	private final InputActionBindings keybindings;

	private final SetupAssignments setupAssignments;

	private final Bookmarks bookmarks;

	private final BookmarksEditor bookmarkEditor;

	private final BrightnessPanel brightnessPanel;

	private final VisibilityAndGroupingPanel visibilityAndGroupingPanel;

	public <T extends NumericType<T>> BdvPanel(final List<ImgPlus<T>> imgs, final int width, final int height) {
		super(new BorderLayout());
		final ViewerOptions optional = ViewerOptions.options();

		int numTimePoints = 1;
		int numSetups = imgs.size();

		final ArrayList<ConverterSetup> converterSetups = new ArrayList<ConverterSetup>();
		final ArrayList<SourceAndConverter<?>> sources = new ArrayList<SourceAndConverter<?>>();

		for (ImgPlus<T> imgPlus : imgs) {
			int dC = imgPlus.dimensionIndex(Axes.CHANNEL);
			int dT = imgPlus.dimensionIndex(Axes.TIME);
			int dX = imgPlus.dimensionIndex(Axes.X);
			int dY = imgPlus.dimensionIndex(Axes.Y);
			int dZ = imgPlus.dimensionIndex(Axes.Z);

			if (imgPlus.numDimensions() == 2) {
				throw new IllegalArgumentException("Input image must have at least 3 dimensions!");
			} else if (imgPlus.numDimensions() == 3) {
				dC = -1;
				dT = -1;
				dX = 0;
				dY = 1;
				dZ = 2;
			} else if (imgPlus.numDimensions() == 4) {
				if (dC == -1 && dT == -1)
					throw new IllegalArgumentException(
							"Four dimensional input image without CHANNEL axis must contain a TIME axis! Neither of both found...");

				if (dT != -1)
					numTimePoints = (int) imgPlus.dimension(dT);

				if (dC != -1)
					numSetups = (int) imgPlus.dimension(dC);
			} else if (imgPlus.numDimensions() == 5) {
				if (dC == -1 || dT == -1)
					throw new IllegalArgumentException(
							"Five dimensional input image must contain CHANNEL and TIME axis!");

				numTimePoints = (int) imgPlus.dimension(dT);
				numSetups = (int) imgPlus.dimension(dC);
			}

			final VoxelDimensions voxelDimensions = new FinalVoxelDimensions(imgPlus.axis(dX).unit(),
					imgPlus.averageScale(dX), imgPlus.averageScale(dY), imgPlus.averageScale(dZ));

			final AffineTransform3D sourceTransform = new AffineTransform3D();
			sourceTransform.set(voxelDimensions.dimension(0), 0, 0, 0, 0, voxelDimensions.dimension(1), 0, 0, 0, 0,
					8.676294982576698, 0);

			initSetups(imgPlus, dC, dT, numSetups, voxelDimensions, sourceTransform, converterSetups, sources);
		}
		viewer = new ViewerPanel(sources, numTimePoints, new Cache.Dummy(), optional.width(width).height(height));
		keybindings = new InputActionBindings();

		// setPreferredSize( new Dimension( width, height ) );
		add(viewer, BorderLayout.CENTER);

		bookmarks = new Bookmarks();
		bookmarkEditor = new BookmarksEditor(viewer, keybindings, bookmarks);

		for (final ConverterSetup cs : converterSetups)
			if (RealARGBColorConverterSetup.class.isInstance(cs))
				((RealARGBColorConverterSetup) cs).setViewer(viewer);

		setupAssignments = new SetupAssignments(converterSetups, 0, 65535);
		if (setupAssignments.getMinMaxGroups().size() > 0) {
			final MinMaxGroup group = setupAssignments.getMinMaxGroups().get(0);
			for (final ConverterSetup setup : setupAssignments.getConverterSetups())
				setupAssignments.moveSetupToGroup(setup, group);
		}

		final KeyProperties keyProperties = KeyProperties.readPropertyFile();
		NavigationActions.installActionBindings(keybindings, viewer, keyProperties);
		BdvPanelActions.installActionBindings(keybindings, this, keyProperties);

		brightnessPanel = new BrightnessPanel(setupAssignments);
		add(brightnessPanel, BorderLayout.SOUTH);
		brightnessPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "brightness and colors"),
						BorderFactory.createEmptyBorder(2, 2, 2, 2))));

		visibilityAndGroupingPanel = new VisibilityAndGroupingPanel(viewer.getVisibilityAndGrouping());
		add(visibilityAndGroupingPanel, BorderLayout.EAST);
	}

	public void stop() {
		viewer.stop();
	}

	public void initSetBookmark() {
		bookmarkEditor.initSetBookmark();
	}

	public void initGoToBookmark() {
		bookmarkEditor.initGoToBookmark();
	}

	public void initGoToBookmarkRotation() {
		bookmarkEditor.initGoToBookmarkRotation();
	}

	public void addKeybindingsTo(final JComponent component) {
		SwingUtilities.replaceUIActionMap(component, keybindings.getConcatenatedActionMap());
		SwingUtilities.replaceUIInputMap(component, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
				keybindings.getConcatenatedInputMap());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> void initSetups(final ImgPlus<T> imgPlus, final int dC, final int dT, final int numSetups,
			final VoxelDimensions voxelDimensions, final AffineTransform3D sourceTransform,
			final List<ConverterSetup> converterSetups, final List<SourceAndConverter<?>> sources) {
		final T type = Util.getTypeFromInterval(imgPlus);
		if (RealType.class.isInstance(type))
			initSetupsRealType((ImgPlus) imgPlus, dC, dT, numSetups, voxelDimensions, sourceTransform, converterSetups,
					sources);
		else if (ARGBType.class.isInstance(type))
			initSetupsARGBType((ImgPlus) imgPlus, dC, dT, numSetups, voxelDimensions, sourceTransform, converterSetups,
					sources);
		else
			throw new IllegalArgumentException(
					"Input image must be of RealType or ARGBType! (input image type is " + type.getClass() + ")");
	}

	private static <T extends RealType<T>> void initSetupsRealType(final ImgPlus<T> imgPlus, final int dC, final int dT,
			final int numSetups, final VoxelDimensions voxelDimensions, final AffineTransform3D sourceTransform,
			final List<ConverterSetup> converterSetups, final List<SourceAndConverter<?>> sources) {
		final T type = Util.getTypeFromInterval(imgPlus);
		final double typeMin = Math.max(0, Math.min(type.getMinValue(), 65535));
		final double typeMax = Math.max(0, Math.min(type.getMaxValue(), 65535));
		for (int setup = 0; setup < numSetups; ++setup) {
			final ImgPlusSource<T> source = (dC == -1)
					? new ImgPlusSource<T>(imgPlus, dT, setup, voxelDimensions, sourceTransform)
					: new ImgPlusSource<T>(Views.hyperSlice(imgPlus, dC, setup), dT > dC ? dT - 1 : dT, setup,
							voxelDimensions, sourceTransform);
			final RealARGBColorConverter<T> converter = new RealARGBColorConverter.Imp1<T>(typeMin, typeMax);
			converter.setColor(new ARGBType(0xffffffff));

			final SourceAndConverter<T> soc = new SourceAndConverter<T>(source, converter);

			sources.add(soc);
			converterSetups.add(new RealARGBColorConverterSetup(setup, converter));
		}
	}

	private static void initSetupsARGBType(final ImgPlus<ARGBType> imgPlus, final int dC, final int dT,
			final int numSetups, final VoxelDimensions voxelDimensions, final AffineTransform3D sourceTransform,
			final List<ConverterSetup> converterSetups, final List<SourceAndConverter<?>> sources) {
		final ARGBType type = Util.getTypeFromInterval(imgPlus);
		for (int setup = 0; setup < numSetups; ++setup) {
			final ImgPlusSource<ARGBType> source = (dC == -1)
					? new ImgPlusSource<ARGBType>(imgPlus, dT, setup, voxelDimensions, sourceTransform)
					: new ImgPlusSource<ARGBType>(Views.hyperSlice(imgPlus, dC, setup), dT > dC ? dT - 1 : dT, setup,
							voxelDimensions, sourceTransform);
			final ScaledARGBConverter.ARGB converter = new ScaledARGBConverter.ARGB(0, 255);

			final SourceAndConverter<ARGBType> soc = new SourceAndConverter<ARGBType>(source, converter);

			sources.add(soc);
			converterSetups.add(new RealARGBColorConverterSetup(setup, converter));
		}
	}

	public ViewerPanel getViewer() {
		return viewer;
	}

	public SetupAssignments getSetupAssignments() {
		return setupAssignments;
	}
}
