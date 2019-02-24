/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.LocalCalendar

class AddCalendarDetailsFragment: Fragment() {

    private lateinit var credentialsModel: CredentialsFragment.CredentialsModel
    private lateinit var validationModel: AddCalendarValidationFragment.ValidationModel
    private lateinit var titleColorModel: TitleColorFragment.TitleColorModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleColorModel = ViewModelProviders.of(requireActivity()).get(TitleColorFragment.TitleColorModel::class.java)
        credentialsModel = ViewModelProviders.of(requireActivity()).get(CredentialsFragment.CredentialsModel::class.java)
        validationModel = ViewModelProviders.of(requireActivity()).get(AddCalendarValidationFragment.ValidationModel::class.java)

        val invalidateOptionsMenu = Observer<Any> {
            requireActivity().invalidateOptionsMenu()
        }
        titleColorModel = ViewModelProviders.of(requireActivity()).get(TitleColorFragment.TitleColorModel::class.java)
        titleColorModel.title.observe(this, invalidateOptionsMenu)
        titleColorModel.color.observe(this, invalidateOptionsMenu)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val v = inflater.inflate(R.layout.add_calendar_details, container, false)
        setHasOptionsMenu(true)

        return v
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_create_calendar, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val itemGo = menu.findItem(R.id.create_calendar)
        itemGo.isEnabled = !titleColorModel.title.value.isNullOrBlank()
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            if (item.itemId == R.id.create_calendar) {
                if (createCalendar())
                    requireActivity().finish()
                true
            } else
                false


    private fun createCalendar(): Boolean {
        val account = AppAccount.get(requireActivity())

        val calInfo = ContentValues(9)
        calInfo.put(Calendars.ACCOUNT_NAME, account.name)
        calInfo.put(Calendars.ACCOUNT_TYPE, account.type)
        calInfo.put(Calendars.NAME, titleColorModel.url.value)
        calInfo.put(Calendars.CALENDAR_DISPLAY_NAME, titleColorModel.title.value)
        calInfo.put(Calendars.CALENDAR_COLOR, titleColorModel.color.value)
        calInfo.put(Calendars.OWNER_ACCOUNT, account.name)
        calInfo.put(Calendars.SYNC_EVENTS, 1)
        calInfo.put(Calendars.VISIBLE, 1)
        calInfo.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ)

        val client: ContentProviderClient? = requireActivity().contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY)
        return try {
            client?.let {
                val uri = AndroidCalendar.create(account, it, calInfo)
                val calendar = LocalCalendar.findById(account, client, ContentUris.parseId(uri))
                CalendarCredentials.putCredentials(requireActivity(), calendar, credentialsModel.username.value, credentialsModel.password.value)
            }
            Toast.makeText(activity, getString(R.string.add_calendar_created), Toast.LENGTH_LONG).show()
            requireActivity().invalidateOptionsMenu()
            true
        } catch(e: Exception) {
            Log.e(Constants.TAG, "Couldn't create calendar", e)
            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
            false
        } finally {
            client?.release()
        }
    }

}
