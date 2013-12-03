/*******************************************************************************
 * Copyright (c) 2013 ELAN e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package de.elanev.studip.android.app.frontend.contacts;

import android.app.Activity;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

import de.elanev.studip.android.app.R;
import de.elanev.studip.android.app.backend.db.ContactsContract;
import de.elanev.studip.android.app.backend.db.UsersContract;
import de.elanev.studip.android.app.widget.ListAdapterUsers;
import de.elanev.studip.android.app.widget.SectionedCursorAdapter;
import de.elanev.studip.android.app.widget.UserDetailsActivity;
import de.elanev.studip.android.app.widget.UserListFragment;

/**
 * @author joern
 */
public class ContactsGroupsFragment extends UserListFragment {
    protected final ContentObserver mObserver = new ContentObserver(
            new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (getActivity() == null) {
                return;
            }

            Loader<Cursor> loader = getLoaderManager().getLoader(0);
            if (loader != null) {
                loader.forceLoad();
            }
        }
    };
    private ListAdapterUsers mUserAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserAdapter = new ListAdapterUsers(mContext);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(R.string.Contacts);
        setEmptyMessage(R.string.no_contacts);

        mListView.setOnItemClickListener(this);
        mListView.setOnStickyHeaderOffsetChangedListener(this);

        mListView.setAdapter(mUserAdapter);
        // initialize CursorLoader
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.getContentResolver().registerContentObserver(
                ContactsContract.CONTENT_URI_CONTACT_GROUPS, true, mObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mObserver);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int,
     * android.os.Bundle)
     */
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        setLoadingViewVisible(true);
        return new CursorLoader(mContext,
                ContactsContract.CONTENT_URI_CONTACT_GROUP_MEMBERS,
                UsersQuery.projection, null, null, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android
     * .support.v4.content.Loader, java.lang.Object)
     */
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        List<SectionedCursorAdapter.Section> sections = new ArrayList<SectionedCursorAdapter.Section>();
        cursor.moveToFirst();
        String prevGroup = null;
        String currGroup = null;

        while (!cursor.isAfterLast()) {
            currGroup = cursor
                    .getString(cursor
                            .getColumnIndex(ContactsContract.Columns.ContactGroups.GROUP_NAME));
            if (!TextUtils.equals(currGroup, prevGroup)) {
                sections.add(new SectionedCursorAdapter.Section(cursor
                        .getPosition(), currGroup));
            }

            prevGroup = currGroup;

            cursor.moveToNext();
        }

        mUserAdapter.setSections(sections);
        mUserAdapter.swapCursor(cursor);

        setLoadingViewVisible(false);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android
     * .support.v4.content.Loader)
     */
    public void onLoaderReset(Loader<Cursor> loader) {
        mUserAdapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor c = (Cursor) mListView.getItemAtPosition(position);

        String userId = c.getString(c
                .getColumnIndex(UsersContract.Columns.USER_ID));
        if (userId != null) {
            Intent intent = new Intent(mContext, UserDetailsActivity.class);
            intent.putExtra(UsersContract.Columns.USER_ID, userId);
            mContext.startActivity(intent);
        }
    }

}
