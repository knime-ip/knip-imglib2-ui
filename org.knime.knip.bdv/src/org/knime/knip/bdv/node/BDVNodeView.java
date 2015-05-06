/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.bdv.node;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.imagej.ImgPlus;
import net.imagej.axis.LinearAxis;
import net.imagej.space.AnnotatedSpace;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealARGBConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.ui.AffineTransformType3D;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.ui.TransformEventHandler3D;
import net.imglib2.ui.overlay.BoxOverlayRenderer;
import net.imglib2.ui.util.Defaults;
import net.imglib2.ui.util.InterpolatingSource;
import net.imglib2.view.Views;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.ImgLabelingValue;

/**
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class BDVNodeView<T extends RealType<T>, L extends Comparable<L>, I extends IntegerType<I>>
		extends NodeView<BDVNodeModel<T, L>> implements ListSelectionListener {

	/* A node logger */
	static NodeLogger LOGGER = NodeLogger.getLogger(BDVNodeView.class);

	private Ui<AffineTransform3D, InteractiveDisplayCanvasComponent<AffineTransform3D>> m_imgView = null;

	/* Current row */
	private int m_row;

	/* The split pane for the view */
	private JSplitPane m_sp;

	/* Table for the images */
	private TableContentView m_tableContentView;

	/* The Table view */
	private TableView m_tableView;

	private final ExecutorService UPDATE_EXECUTOR = Executors
			.newCachedThreadPool(new ThreadFactory() {
				private final AtomicInteger m_counter = new AtomicInteger();

				@Override
				public Thread newThread(final Runnable r) {
					final Thread t = new Thread(r,
							"Segment Overlay Viewer-Updater-"
									+ m_counter.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			});

	/**
	 * Constructor
	 *
	 * @param model
	 */
	public BDVNodeView(final BDVNodeModel<T, L> model) {
		super(model);
		m_sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		m_row = -1;

		initTableView();
		m_sp.setLeftComponent(m_tableView);
		m_sp.setRightComponent(new JPanel());

		setComponent(m_sp);

		m_sp.setDividerLocation(300);
		loadPortContent();

	}

	private void loadPortContent() {

		m_tableContentView.setModel(getNodeModel().getTableContentModel());

		// Scale to thumbnail size
		m_tableView.validate();
		m_tableView.repaint();
	}

	/* Initializes the table view (left side of the split pane) */
	private void initTableView() {
		m_tableContentView = new TableContentView();
		m_tableContentView.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
		m_tableContentView.getSelectionModel().addListSelectionListener(this);
		m_tableContentView.getColumnModel().getSelectionModel()
				.addListSelectionListener(this);
		m_tableView = new TableView(m_tableContentView);
	}

	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
	protected void modelChanged() {
		m_tableContentView.setModel(getNodeModel().getTableContentModel());
		m_imgView = null;
		m_sp.setRightComponent(new JPanel());
		m_row = -1;
	}

	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
	protected void onClose() {
		UPDATE_EXECUTOR.shutdownNow();

		m_tableView.removeAll();
		m_tableContentView.removeAll();
		m_imgView = null;
		m_tableContentView = null;
		m_tableView = null;
		m_sp = null;
		m_row = -1;
	}

	/**
	 *
	 * {@inheritDoc}
	 */
	@Override
	protected void onOpen() {
		// Scale to thumbnail size
		m_tableView.validate();
		m_tableView.repaint();
	}

	/**
	 * Updates the ViewPane with the selected image and labeling
	 *
	 *
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void valueChanged(final ListSelectionEvent e) {

		final int row = m_tableContentView.getSelectionModel()
				.getLeadSelectionIndex();

		if ((row == m_row) || e.getValueIsAdjusting()) {
			return;
		}

		m_row = row;

		try {
			final ImgLabelingValue<L> currentLabelingCell = (ImgLabelingValue<L>) m_tableContentView
					.getContentModel().getValueAt(row, 1);
			final ImgPlus<T> imgPlus = ((ImgPlusValue<T>) m_tableContentView
					.getContentModel().getValueAt(row, 0)).getImgPlus();

			String imgName = imgPlus.getName();
			String imgSource = imgPlus.getSource();
			RandomAccessibleInterval<T> underlyingInterval = imgPlus;

			// Update Labeling Mapping for Hiliting
			ImgLabeling<L, ?> labeling = currentLabelingCell.getLabeling();
			final int width = (int) imgPlus.dimension(0);
			final int height = (int) imgPlus.dimension(1);
			final RandomAccessible<T> source = Views.extendZero(imgPlus);

			final AffineTransform3D sourceTransform = new AffineTransform3D();
			if (imgPlus.axis(0) instanceof LinearAxis) {
				final double[] scales = getcalib((AnnotatedSpace<? extends LinearAxis>) imgPlus);
				for (int i = 0; i < scales.length; ++i)
					sourceTransform.set(scales[i], i, i);
			}

			final T type = imgPlus.firstElement();
			final RealARGBConverter<T> converter = new RealARGBConverter<T>(
					type.getMinValue(), type.getMaxValue());

			final InterpolatingSource<T, AffineTransform3D> renderSource = new InterpolatingSource<T, AffineTransform3D>(
					source, sourceTransform, converter);

			boolean init = m_imgView == null;

			m_imgView = new Ui<AffineTransform3D, InteractiveDisplayCanvasComponent<AffineTransform3D>>(
					AffineTransformType3D.instance,
					new InteractiveDisplayCanvasComponent<AffineTransform3D>(
							width, height, TransformEventHandler3D.factory()),
					Defaults.rendererFactory(AffineTransformType3D.instance,
							renderSource));

			if (init) {
				int tmp = m_sp.getDividerLocation();
				m_sp.setRightComponent(m_imgView.canvas);
				m_sp.setDividerLocation(tmp);
			}

			// TODO INTERESSANT FUER DEN TOBIIII
			
			// add box overlay
			final BoxOverlayRenderer box = new BoxOverlayRenderer(width, height);
			box.setSource(imgPlus, renderSource.getSourceTransform());
			m_imgView.canvas.addTransformListener(box);
			m_imgView.canvas.addOverlayRenderer(box);

			// add KeyHandler for toggling interpolation
			m_imgView.canvas.addHandler(new KeyAdapter() {
				@Override
				public void keyPressed(final KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_I) {
						renderSource.switchInterpolation();
						m_imgView.requestRepaint();
					}
				}
			});

		} catch (final IndexOutOfBoundsException e2) {
			return;
		}
	}

	private static double[] getcalib(
			final AnnotatedSpace<? extends LinearAxis> calib) {
		final double[] c = new double[calib.numDimensions()];
		for (int d = 0; d < c.length; ++d)
			c[d] = calib.axis(d).scale();
		return c;
	}
}
