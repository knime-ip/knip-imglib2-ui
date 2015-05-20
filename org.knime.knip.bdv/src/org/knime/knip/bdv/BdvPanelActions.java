package org.knime.knip.bdv;

import java.awt.event.ActionEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import bdv.BigDataViewer;
import bdv.util.AbstractNamedAction;
import bdv.util.AbstractNamedAction.NamedActionAdder;
import bdv.util.KeyProperties;
import bdv.util.KeyProperties.KeyStrokeAdder;
import bdv.viewer.InputActionBindings;

public class BdvPanelActions
{
	public static final String SHOW_HELP = "help";
	public static final String SET_BOOKMARK = "set bookmark";
	public static final String GO_TO_BOOKMARK = "go to bookmark";
	public static final String GO_TO_BOOKMARK_ROTATION = "go to bookmark rotation";

	/**
	 * Create BigDataViewer actions and install them in the specified
	 * {@link InputActionBindings}.
	 *
	 * @param inputActionBindings
	 *            {@link InputMap} and {@link ActionMap} are installed here.
	 * @param bdv
	 *            Actions are targeted at this {@link BigDataViewer}.
	 * @param keyProperties
	 *            user-defined key-bindings.
	 */
	public static void installActionBindings(
			final InputActionBindings inputActionBindings,
			final BdvPanel bdvPanel,
			final KeyProperties keyProperties )
	{
		inputActionBindings.addActionMap( "bdv", createActionMap( bdvPanel ) );
		inputActionBindings.addInputMap( "bdv", createInputMap( keyProperties ) );
	}

	public static InputMap createInputMap( final KeyProperties keyProperties )
	{
		final InputMap inputMap = new InputMap();
		final KeyStrokeAdder map = keyProperties.adder( inputMap );

		map.put( GO_TO_BOOKMARK, "B" );
		map.put( GO_TO_BOOKMARK_ROTATION, "O" );
		map.put( SET_BOOKMARK, "shift B" );

		return inputMap;
	}

	public static ActionMap createActionMap( final BdvPanel bdvPanel )
	{
		final ActionMap actionMap = new ActionMap();
		final NamedActionAdder map = new NamedActionAdder( actionMap );

//		map.put( new ToggleDialogAction( SHOW_HELP, bdv.helpDialog ) );
		map.put( new SetBookmarkAction( bdvPanel ) );
		map.put( new GoToBookmarkAction( bdvPanel ) );
		map.put( new GoToBookmarkRotationAction( bdvPanel ) );

		return actionMap;
	}

	private static abstract class BdvPanelAction extends AbstractNamedAction
	{
		protected final BdvPanel bdvPanel;

		public BdvPanelAction( final String name, final BdvPanel bdvPanel )
		{
			super( name );
			this.bdvPanel = bdvPanel;
		}

		private static final long serialVersionUID = 1L;
	}

	public static class SetBookmarkAction extends BdvPanelAction
	{
		public SetBookmarkAction( final BdvPanel bdvPanel )
		{
			super( SET_BOOKMARK, bdvPanel );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bdvPanel.initSetBookmark();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class GoToBookmarkAction extends BdvPanelAction
	{
		public GoToBookmarkAction( final BdvPanel bdvPanel )
		{
			super( GO_TO_BOOKMARK, bdvPanel );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bdvPanel.initGoToBookmark();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class GoToBookmarkRotationAction extends BdvPanelAction
	{
		public GoToBookmarkRotationAction( final BdvPanel bdvPanel )
		{
			super( GO_TO_BOOKMARK_ROTATION, bdvPanel );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bdvPanel.initGoToBookmarkRotation();
		}

		private static final long serialVersionUID = 1L;
	}

	private BdvPanelActions()
	{}
}
