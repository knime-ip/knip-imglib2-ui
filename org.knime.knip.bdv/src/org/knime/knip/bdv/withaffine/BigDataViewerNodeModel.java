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
package org.knime.knip.bdv.withaffine;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.mvr.cells.AffineTransformCell;
import org.knime.knip.mvr.cells.AffineTransformValue;

import net.imglib2.type.numeric.RealType;

/**
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class BigDataViewerNodeModel<T extends RealType<T>, L extends Comparable<L>> extends NodeModel
		implements BufferedDataTableHolder {

	private class ClearableTableContentModel extends TableContentModel {
		/** */
		private static final long serialVersionUID = 1L;

		// make clearCache accessible to allow clearing before the
		// content model is thrown away
		// in order to free large image resources from the java swing
		// caching strategy
		public void clearCacheBeforeClosing() {
			clearCache();
		}

	}

	public static final String CFG_IMG_COL_A = "cfg_img_col_a";

	public static final String CFG_IMG_COL_B = "cfg_img_col_b";

	public static final String CFG_AFFINE_A = "cfg_affine_a";

	public static final String CFG_AFFINE_B = "cfg_affine_b";

	/*
	 * Logging
	 */
	private static final NodeLogger LOGGER = NodeLogger.getLogger(BigDataViewerNodeModel.class);

	private ClearableTableContentModel m_contentModel;

	private final SettingsModelString m_imgColA = new SettingsModelString(CFG_IMG_COL_A, "");

	private final SettingsModelString m_imgColB = new SettingsModelString(CFG_IMG_COL_B, "");

	private final SettingsModelString m_affineA = new SettingsModelString(CFG_AFFINE_A, "");

	private final SettingsModelString m_affineB = new SettingsModelString(CFG_AFFINE_B, "");

	private BufferedDataTable m_imgTable;

	private boolean m_isDataSetToModel;

	protected BigDataViewerNodeModel() {
		super(1, 0);
		m_contentModel = new ClearableTableContentModel();
		m_isDataSetToModel = false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		if (m_affineA.getStringValue() != null) {
			NodeUtils.autoColumnSelection(inSpecs[0], m_affineA, AffineTransformValue.class, this.getClass());
		}

		if (m_affineB.getStringValue() != null) {
			NodeUtils.autoColumnSelection(inSpecs[0], m_affineB, AffineTransformValue.class, this.getClass());
		}

		if (m_imgColA.getStringValue() == null || m_imgColA.getStringValue().length() == 0) {
			NodeUtils.autoOptionalColumnSelection(inSpecs[0], m_imgColA, ImgPlusValue.class);
		} else {
			NodeUtils.silentOptionalAutoColumnSelection(inSpecs[0], m_imgColA, ImgPlusValue.class);
		}

		if (m_imgColB.getStringValue() == null || m_imgColB.getStringValue().length() == 0) {
			NodeUtils.autoOptionalColumnSelection(inSpecs[0], m_imgColB, ImgPlusValue.class);
		} else {
			NodeUtils.silentOptionalAutoColumnSelection(inSpecs[0], m_imgColB, ImgPlusValue.class);
		}

		return new DataTableSpec[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		m_imgTable = inData[0];

		m_isDataSetToModel = true;

		int imgColIdxA = -1;
		if (m_imgColA.getStringValue() == null || m_imgColA.getStringValue().length() == 0) {
			imgColIdxA = NodeUtils.autoOptionalColumnSelection(inData[0].getDataTableSpec(), m_imgColA,
					ImgPlusValue.class);
		} else {
			imgColIdxA = NodeUtils.silentOptionalAutoColumnSelection(inData[0].getDataTableSpec(), m_imgColA,
					ImgPlusValue.class);
		}

		int imgColIdxB = -1;
		if (m_imgColB.getStringValue() == null || m_imgColB.getStringValue().length() == 0) {
			imgColIdxB = NodeUtils.autoOptionalColumnSelection(inData[0].getDataTableSpec(), m_imgColB,
					ImgPlusValue.class);
		} else {
			imgColIdxB = NodeUtils.silentOptionalAutoColumnSelection(inData[0].getDataTableSpec(), m_imgColB,
					ImgPlusValue.class);
		}

		int affineColIdxA = -1;
		if (m_affineA.getStringValue() != null) {
			affineColIdxA = NodeUtils.autoColumnSelection(inData[0].getDataTableSpec(), m_affineA,
					AffineTransformValue.class, this.getClass());
		}

		int affineColIdxB = -1;
		if (m_affineA.getStringValue() != null) {
			affineColIdxB = NodeUtils.autoColumnSelection(inData[0].getDataTableSpec(), m_affineB,
					AffineTransformValue.class, this.getClass());
		}

		DataTableSpec spec = new DataTableSpec(DataTableSpec.createColumnSpecs(
				new String[] { "Image View A", "Image View B", "Affine View A", "Affine View B" }, new DataType[] {
						ImgPlusCell.TYPE, ImgPlusCell.TYPE, AffineTransformCell.TYPE, AffineTransformCell.TYPE }));

		final BufferedDataContainer con = exec.createDataContainer(spec);
		final RowIterator imgIt = m_imgTable.iterator();

		if (m_imgTable.getRowCount() == 0) {
			return new BufferedDataTable[0];
		}

		int rowCount = 0;
		while (imgIt.hasNext()) {
			final DataRow row = imgIt.next();

			// load
			final DataCell affineCellA = row.getCell(affineColIdxA);
			final DataCell affineCellB = row.getCell(affineColIdxB);

			final DataCell imgCellA = row.getCell(imgColIdxA);
			final DataCell imgCellB = row.getCell(imgColIdxB);

			con.addRowToTable(new DefaultRow(row.getKey(), imgCellA, imgCellB, affineCellA, affineCellB));

			exec.checkCanceled();
			exec.setProgress((double) rowCount++ / m_imgTable.getRowCount());
		}

		con.close();
		m_contentModel.setDataTable(con.getTable());

		return new BufferedDataTable[0];

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BufferedDataTable[] getInternalTables() {

		return new BufferedDataTable[] { (BufferedDataTable) m_contentModel.getDataTable() };
	}

	public TableContentModel getTableContentModel() {
		// temporary workaround since setDataTable blocks
		if (!m_isDataSetToModel) {
			m_contentModel.setDataTable(m_imgTable);
			m_isDataSetToModel = true;
		}
		return m_contentModel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_imgColA.loadSettingsFrom(settings);
		m_imgColB.loadSettingsFrom(settings);

		m_affineA.loadSettingsFrom(settings);
		m_affineB.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		m_imgTable = null;
		m_contentModel.clearCacheBeforeClosing();
		m_contentModel = new ClearableTableContentModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_imgColA.saveSettingsTo(settings);
		m_imgColB.saveSettingsTo(settings);

		m_affineA.saveSettingsTo(settings);
		m_affineB.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setInternalTables(final BufferedDataTable[] tables) {
		if ((tables.length != 1) && (tables.length != 2)) {
			throw new IllegalArgumentException();
		}
		m_imgTable = tables[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_imgColA.validateSettings(settings);
		m_imgColB.validateSettings(settings);

		m_affineA.validateSettings(settings);
		m_affineB.validateSettings(settings);
	}
}
