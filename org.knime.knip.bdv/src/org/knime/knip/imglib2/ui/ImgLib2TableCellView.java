package org.knime.knip.imglib2.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;

import net.imagej.ImgPlus;
import net.imagej.axis.LinearAxis;
import net.imagej.space.AnnotatedSpace;
import net.imglib2.RandomAccessible;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.converter.RealARGBConverter;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineSet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.ui.AffineTransformType;
import net.imglib2.ui.AffineTransformType3D;
import net.imglib2.ui.InteractiveDisplayCanvas;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.Renderer;
import net.imglib2.ui.RendererFactory;
import net.imglib2.ui.TransformEventHandler3D;
import net.imglib2.ui.TransformListener;
import net.imglib2.ui.overlay.BoxOverlayRenderer;
import net.imglib2.ui.overlay.BufferedImageOverlayRenderer;
import net.imglib2.ui.util.Defaults;
import net.imglib2.ui.util.InterpolatingSource;
import net.imglib2.view.Views;

import org.knime.core.data.DataValue;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.nodes.view.TableCellView;

public class ImgLib2TableCellView<T extends RealType<T>> implements TableCellView {

	private JPanel rootPanel;

	private DataValue dataValue;

	@Override
	public String getName() {
		return "ImgLib2 Viewer";
	}

	@Override
	public String getDescription() {
		return "ImgLib2 Viewer";
	}

	// TODO: @Christian: Always create a new panel? or re-use the same one?
	@Override
	public Component getViewComponent() {
		rootPanel = new JPanel(new BorderLayout());
		rootPanel.setVisible(true);
		return rootPanel;
	}

	static class Ui<A extends AffineSet & AffineGet & Concatenable<AffineGet>, C extends JComponent & InteractiveDisplayCanvas<A>>
			implements TransformListener<A>, PainterThread.Paintable
	{
		/**
		 * Create an interactive viewer window displaying the specified
		 * <code>interactiveDisplayCanvas</code>, and create a {@link Renderer}
		 * which draws to that canvas.
		 * <p>
		 * A {@link Renderer} is created that paints to a
		 * {@link BufferedImageOverlayRenderer} render target which is displayed
		 * on the canvas as an {@link OverlayRenderer}. A {@link PainterThread}
		 * is created which queues repainting requests from the renderer and
		 * interactive canvas, and triggers {@link #paint() repainting} of the
		 * viewer.
		 *
		 * @param transformType
		 * @param interactiveDisplayCanvas
		 *            the canvas {@link JComponent} which will show the rendered
		 *            images.
		 * @param rendererFactory
		 *            is used to create the {@link Renderer}.
		 */
		public Ui(
				final AffineTransformType<A> transformType,
				final C interactiveDisplayCanvas,
				final RendererFactory<A> rendererFactory)
		{
			this.transformType = transformType;
			painterThread = new PainterThread(this);
			viewerTransform = transformType.createTransform();
			canvas = interactiveDisplayCanvas;
			canvas.addTransformListener(this);

			final BufferedImageOverlayRenderer target = new BufferedImageOverlayRenderer();
			imageRenderer = rendererFactory.create(target, painterThread);
			canvas.addOverlayRenderer(target);
			target.setCanvasSize(canvas.getWidth(), canvas.getHeight());

			painterThread.start();
		}

		// ==================================================
		// the following is more or less the same as
		// net.imglib2.ui.viewer.InteractiveRealViewer

		final protected AffineTransformType<A> transformType;

		/**
		 * Transformation set by the interactive viewer.
		 */
		final protected A viewerTransform;

		/**
		 * Canvas used for displaying the rendered {@link #screenImages screen
		 * image}.
		 */
		final protected C canvas;

		/**
		 * Thread that triggers repainting of the display.
		 */
		final protected PainterThread painterThread;

		/**
		 * Paints to a {@link RenderTarget} that is shown in the
		 * {@link #display canvas}.
		 */
		final protected Renderer<A> imageRenderer;

		/**
		 * Render the source using the current viewer transformation and
		 */
		@Override
		public void paint()
		{
			imageRenderer.paint(viewerTransform);
			canvas.repaint();
		}

		@Override
		public void transformChanged(final A transform)
		{
			transformType.set(viewerTransform, transform);
			requestRepaint();
		}

		/**
		 * Request a repaint of the display. Calls
		 * {@link Renderer#requestRepaint()} .
		 */
		public void requestRepaint()
		{
			imageRenderer.requestRepaint();
		}
	}

	private static double[] getcalib(final AnnotatedSpace<? extends LinearAxis> calib)
	{
		final double[] c = new double[calib.numDimensions()];
		System.out.println( "unit = " + calib.axis(0).unit() );
		for (int d = 0; d < c.length; ++d)
			c[d] = calib.axis(d).scale();
		return c;
	}

	@Override
	public void updateComponent(final DataValue valueToView) {
		if (dataValue == null || !(dataValue.equals(valueToView))) {
			@SuppressWarnings("unchecked")
			final ImgPlus<T> imgPlus = ((ImgPlusValue<T>) valueToView).getImgPlus();

			final int width = (int) imgPlus.dimension(0);
			final int height = (int) imgPlus.dimension(1);
			final RandomAccessible<T> source = Views.extendZero(imgPlus);

			final AffineTransform3D sourceTransform = new AffineTransform3D();
			if (imgPlus.axis(0) instanceof LinearAxis)
			{
				final double[] scales = getcalib((AnnotatedSpace<? extends LinearAxis>) imgPlus);
				for (int i = 0; i < 3; ++i)
					sourceTransform.set(scales[i], i, i);
			}

			final T type = imgPlus.firstElement();
			final RealARGBConverter<T> converter = new RealARGBConverter<T>(type.getMinValue(), type.getMaxValue());

			final InterpolatingSource<T, AffineTransform3D> renderSource = new InterpolatingSource<T, AffineTransform3D>(source, sourceTransform, converter);

			final Ui<AffineTransform3D, InteractiveDisplayCanvasComponent<AffineTransform3D>> ui =
					new Ui<AffineTransform3D, InteractiveDisplayCanvasComponent<AffineTransform3D>>(
							AffineTransformType3D.instance,
							new InteractiveDisplayCanvasComponent<AffineTransform3D>(width, height, TransformEventHandler3D.factory()),
							Defaults.rendererFactory(AffineTransformType3D.instance, renderSource));

			// add box overlay
			final BoxOverlayRenderer box = new BoxOverlayRenderer(width, height);
			box.setSource(imgPlus, renderSource.getSourceTransform());
			ui.canvas.addTransformListener(box);
			ui.canvas.addOverlayRenderer(box);

			// add KeyHandler for toggling interpolation
			ui.canvas.addHandler(new KeyAdapter()
			{
				@Override
				public void keyPressed(final KeyEvent e)
				{
					if (e.getKeyCode() == KeyEvent.VK_I)
					{
						renderSource.switchInterpolation();
						ui.requestRepaint();
					}
				}
			});

			rootPanel.removeAll();
			rootPanel.add(ui.canvas, BorderLayout.CENTER);
		}
	}

	@Override
	public void onClose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void loadConfigurationFrom(final ConfigRO config) {
	}

	@Override
	public void saveConfigurationTo(final ConfigWO config) {
	}
}
