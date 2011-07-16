/*
 * Copyright (C) 2010-2011 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.tree;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import org.geometerplus.android.util.UIUtil;

import org.geometerplus.fbreader.tree.FBTree;

public abstract class BaseActivity extends ListActivity {
	private static final String OPEN_TREE_ACTION = "org.fbreader.intent.OPEN_TREE";

	public static final String TREE_KEY_KEY = "TreeKey";
	public static final String SELECTED_TREE_KEY_KEY = "SelectedTreeKey";

	private FBTree myCurrentTree;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Thread.setDefaultUncaughtExceptionHandler(new org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler(this));
	}

	@Override
	public ListAdapter getListAdapter() {
		return (ListAdapter)super.getListAdapter();
	}

	protected FBTree getCurrentTree() {
		return myCurrentTree;
	}

	public abstract boolean isTreeSelected(FBTree tree);

	protected boolean OLD_STYLE_FLAG = false;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (OLD_STYLE_FLAG) {
			return super.onKeyDown(keyCode, event);
		}

		if (keyCode == KeyEvent.KEYCODE_BACK && myCurrentTree.Parent != null) {
			openTree(myCurrentTree.Parent, myCurrentTree);
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	protected void openTree(final FBTree tree) {
		openTree(tree, null);
	}

	protected void openTree(final FBTree tree, final FBTree treeToSelect) {
		switch (tree.getOpeningStatus()) {
			case WAIT_FOR_OPEN:
			case ALWAYS_RELOAD_BEFORE_OPENING:
				final String messageKey = tree.getOpeningStatusMessage();
				if (messageKey != null) {
					UIUtil.runWithMessage(
						BaseActivity.this, messageKey,
						new Runnable() {
							public void run() {
								tree.waitForOpening();
							}
						},
						new Runnable() {
							public void run() {
								openTreeInternal(tree, treeToSelect);
							}
						}
					);
				} else {
					tree.waitForOpening();
					openTreeInternal(tree, treeToSelect);
				}
				break;
			default:
				openTreeInternal(tree, treeToSelect);
				break;
		}
	}

	protected abstract FBTree getTreeByKey(FBTree.Key key);

	@Override
	protected void onNewIntent(Intent intent) {
		if (OPEN_TREE_ACTION.equals(intent.getAction())) {
			init(intent);
		} else {
			super.onNewIntent(intent);
		}
	}

	protected void init(Intent intent) {
		final FBTree.Key key = (FBTree.Key)intent.getSerializableExtra(TREE_KEY_KEY);
		final FBTree.Key selectedKey = (FBTree.Key)intent.getSerializableExtra(SELECTED_TREE_KEY_KEY);
		myCurrentTree = getTreeByKey(key);
		final ListAdapter adapter = getListAdapter();
		adapter.replaceAll(myCurrentTree.subTrees());
		setTitle(myCurrentTree.getTreeTitle());
		final FBTree selectedTree =
			selectedKey != null ? getTreeByKey(selectedKey) : adapter.getFirstSelectedItem();
		setSelection(adapter.getIndex(selectedTree));
	}

	private void openTreeInternal(FBTree tree, FBTree treeToSelect) {
		switch (tree.getOpeningStatus()) {
			case READY_TO_OPEN:
			case ALWAYS_RELOAD_BEFORE_OPENING:
				startActivity(new Intent(this, getClass())
					.setAction(OPEN_TREE_ACTION)
					.putExtra(TREE_KEY_KEY, tree.getUniqueKey())
					.putExtra(
						SELECTED_TREE_KEY_KEY,
						treeToSelect != null ? treeToSelect.getUniqueKey() : null
					)
				);
				break;
			case CANNOT_OPEN:
				UIUtil.showErrorMessage(BaseActivity.this, tree.getOpeningStatusMessage());
				break;
		}
	}
}
