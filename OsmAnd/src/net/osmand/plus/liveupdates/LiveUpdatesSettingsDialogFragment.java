package net.osmand.plus.liveupdates;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.download.AbstractDownloadActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.Calendar;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.TimesOfDay;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.UpdateFrequency;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.formatDateTime;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getNameToDisplay;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceDownloadViaWiFi;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceForLocalIndex;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLastCheck;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceTimeOfDayToUpdate;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;

public class LiveUpdatesSettingsDialogFragment extends DialogFragment {
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesAlarmReceiver.class);
	private static final String LOCAL_INDEX = "local_index";
	public static final String LOCAL_INDEX_INFO = "local_index_info";


	private static final int MORNING_UPDATE_TIME = 8;
	private static final int NIGHT_UPDATE_TIME = 21;
	private static final int SHIFT = 1000;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final LocalIndexInfo localIndexInfo = getArguments().getParcelable(LOCAL_INDEX);

		View view = LayoutInflater.from(getActivity())
				.inflate(R.layout.dialog_live_updates_item_settings, null);
		final TextView regionNameTextView = (TextView) view.findViewById(R.id.regionNameTextView);
		final TextView lastCheckTextView = (TextView) view.findViewById(R.id.lastCheckTextView);
		final TextView lastUpdateTextView = (TextView) view.findViewById(R.id.lastUpdateTextView);
		final SwitchCompat liveUpdatesSwitch = (SwitchCompat) view.findViewById(R.id.liveUpdatesSwitch);
		final CheckBox downloadOverWiFiCheckBox = (CheckBox) view.findViewById(R.id.downloadOverWiFiSwitch);
		final Spinner updateFrequencySpinner = (Spinner) view.findViewById(R.id.updateFrequencySpinner);
		final Spinner updateTimesOfDaySpinner = (Spinner) view.findViewById(R.id.updateTimesOfDaySpinner);
		final TextView updateTimesOfDayTextView = (TextView) view.findViewById(R.id.updateTimesOfDayLabel);
		final TextView sizeTextView = (TextView) view.findViewById(R.id.sizeTextView);
		final Button removeUpdatesButton = (Button) view.findViewById(R.id.removeUpdatesButton);

		regionNameTextView.setText(getNameToDisplay(localIndexInfo, getMyActivity()));
		final String fileNameWithoutExtension =
				Algorithms.getFileNameWithoutExtension(new File(localIndexInfo.getFileName()));
		final IncrementalChangesManager changesManager = getMyApplication().getResourceManager().getChangesManager();
		final long timestamp = changesManager.getTimestamp(fileNameWithoutExtension);
		String lastUpdateDate = formatDateTime(getActivity(), timestamp);
		OsmandSettings.CommonPreference<Long> lastCheckPreference = preferenceLastCheck(localIndexInfo, getSettings());
		String lastCheckDate = formatDateTime(getActivity(), lastCheckPreference.get());
		String lastCheck = lastCheckPreference.get() != -1 ? lastCheckDate : lastUpdateDate;
		lastCheckTextView.setText(getString(R.string.last_check_date, lastCheck));
		lastUpdateTextView.setText(getString(R.string.update_date_pattern, lastUpdateDate));
		final OsmandSettings.CommonPreference<Boolean> liveUpdatePreference =
				preferenceForLocalIndex(localIndexInfo, getSettings());
		final OsmandSettings.CommonPreference<Boolean> downloadViaWiFiPreference =
				preferenceDownloadViaWiFi(localIndexInfo, getSettings());
		final OsmandSettings.CommonPreference<Integer> updateFrequencePreference =
				preferenceUpdateFrequency(localIndexInfo, getSettings());
		final OsmandSettings.CommonPreference<Integer> timeOfDayPreference =
				preferenceTimeOfDayToUpdate(localIndexInfo, getSettings());

		downloadOverWiFiCheckBox.setChecked(!liveUpdatePreference.get() || downloadViaWiFiPreference.get());

		updateSize(fileNameWithoutExtension, changesManager, sizeTextView);

		updateFrequencySpinner.setSelection(updateFrequencePreference.get());
		updateFrequencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				UpdateFrequency updateFrequency = UpdateFrequency.values()[position];
				switch (updateFrequency) {
					case HOURLY:
						updateTimesOfDaySpinner.setVisibility(View.GONE);
						updateTimesOfDayTextView.setVisibility(View.GONE);
						break;
					case DAILY:
					case WEEKLY:
						updateTimesOfDaySpinner.setVisibility(View.VISIBLE);
						updateTimesOfDayTextView.setVisibility(View.VISIBLE);
						break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
		removeUpdatesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				changesManager.deleteUpdates(fileNameWithoutExtension);
				getLiveUpdatesFragment().notifyLiveUpdatesChanged();
				updateSize(fileNameWithoutExtension, changesManager, sizeTextView);
			}
		});

		builder.setView(view)
				.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final int updateFrequencyInt = updateFrequencySpinner.getSelectedItemPosition();
						updateFrequencePreference.set(updateFrequencyInt);
						UpdateFrequency updateFrequency = UpdateFrequency.values()[updateFrequencyInt];

						AlarmManager alarmMgr = (AlarmManager) getActivity()
								.getSystemService(Context.ALARM_SERVICE);
						Intent intent = new Intent(getActivity(), LiveUpdatesAlarmReceiver.class);
						final File file = new File(localIndexInfo.getFileName());
						final String fileName = Algorithms.getFileNameWithoutExtension(file);
						intent.putExtra(LOCAL_INDEX_INFO, localIndexInfo);
						intent.setAction(fileName);
						PendingIntent alarmIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);

						final int timeOfDayInt = updateTimesOfDaySpinner.getSelectedItemPosition();
						timeOfDayPreference.set(timeOfDayInt);
						TimesOfDay timeOfDayToUpdate = TimesOfDay.values()[timeOfDayInt];
						long timeOfFirstUpdate;
						long updateInterval;
						switch (updateFrequency) {
							case HOURLY:
								timeOfFirstUpdate = System.currentTimeMillis() + SHIFT;
								updateInterval = AlarmManager.INTERVAL_HOUR;
								break;
							case DAILY:
								timeOfFirstUpdate = getNextUpdateTime(timeOfDayToUpdate);
								updateInterval = AlarmManager.INTERVAL_DAY;
								break;
							case WEEKLY:
								timeOfFirstUpdate = getNextUpdateTime(timeOfDayToUpdate);
								updateInterval = AlarmManager.INTERVAL_DAY * 7;
								break;
							default:
								throw new IllegalStateException("Unexpected update frequency:"
										+ updateFrequency);
						}

						liveUpdatePreference.set(liveUpdatesSwitch.isChecked());
						downloadViaWiFiPreference.set(downloadOverWiFiCheckBox.isChecked());
						alarmMgr.cancel(alarmIntent);
						if (liveUpdatesSwitch.isChecked()) {
							alarmMgr.setInexactRepeating(AlarmManager.RTC,
									timeOfFirstUpdate, updateInterval, alarmIntent);
						}
						getLiveUpdatesFragment().notifyLiveUpdatesChanged();
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setNeutralButton(R.string.update_now, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						runLiveUpdate(localIndexInfo);
						updateSize(fileNameWithoutExtension, changesManager, sizeTextView);
					}
				});
		return builder.create();
	}

	void runLiveUpdate(final LocalIndexInfo info) {
		final String fnExt = Algorithms.getFileNameWithoutExtension(new File(info.getFileName()));
		new PerformLiveUpdateAsyncTask(getActivity(), info).execute(new String[]{fnExt});
		getLiveUpdatesFragment().notifyLiveUpdatesChanged();
	}

	private void updateSize(String fileNameWithoutExtension,
							IncrementalChangesManager changesManager,
							TextView sizeTextView) {
		String size;
		long updatesSize = changesManager.getUpdatesSize(fileNameWithoutExtension);
		updatesSize /= (1 << 10);
		if (updatesSize > 100) {
			size = DownloadActivity.formatMb.format(new Object[]{(float) updatesSize / (1 << 10)});
		} else {
			size = updatesSize + " KB";
		}
		sizeTextView.setText(getString(R.string.size_pattern, size));
	}

	private long getNextUpdateTime(TimesOfDay timeOfDayToUpdate) {
		Calendar calendar = Calendar.getInstance();
		if (timeOfDayToUpdate == TimesOfDay.MORNING) {
			calendar.add(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, MORNING_UPDATE_TIME);
		} else if (timeOfDayToUpdate == TimesOfDay.NIGHT) {
			calendar.add(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, NIGHT_UPDATE_TIME);
		}
		return calendar.getTimeInMillis();
	}

	private LiveUpdatesFragment getLiveUpdatesFragment() {
		return (LiveUpdatesFragment) getParentFragment();
	}

	private OsmandSettings getSettings() {
		return getMyApplication().getSettings();
	}

	private OsmandApplication getMyApplication() {
		return getMyActivity().getMyApplication();
	}

	private AbstractDownloadActivity getMyActivity() {
		return (AbstractDownloadActivity) this.getActivity();
	}

	public static LiveUpdatesSettingsDialogFragment createInstance(LocalIndexInfo localIndexInfo) {
		LiveUpdatesSettingsDialogFragment fragment = new LiveUpdatesSettingsDialogFragment();
		Bundle args = new Bundle();
		args.putParcelable(LOCAL_INDEX, localIndexInfo);
		fragment.setArguments(args);
		return fragment;
	}
}