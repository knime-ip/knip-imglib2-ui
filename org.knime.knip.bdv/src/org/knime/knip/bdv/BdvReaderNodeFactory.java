package org.knime.knip.bdv;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;

public class BdvReaderNodeFactory extends NodeFactory<BdvReaderNodeModel> {

	@Override
	public BdvReaderNodeModel createNodeModel() {
		return new BdvReaderNodeModel();
	}

	@Override
	protected int getNrNodeViews() {
		return 1;
	}

	@Override
	public NodeView<BdvReaderNodeModel> createNodeView(final int viewIndex, final BdvReaderNodeModel nodeModel) {
		return new TableCellViewNodeView<BdvReaderNodeModel>(nodeModel);
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new BdvReaderNodeDialog();
	}

}
