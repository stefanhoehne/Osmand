package net.osmand.plus.mapcontextmenu.other;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import net.osmand.AndroidUtils;
import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.activities.search.SearchPOIActivity;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;

public class DestinationReachedMenuFragment extends Fragment {
	public static final String TAG = "DestinationReachedMenuFragment";
	private DestinationReachedMenu menu;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (menu == null) {
			menu = new DestinationReachedMenu(getMapActivity());
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dest_reached_menu_fragment, container, false);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismissMenu();
			}
		});

		IconsCache iconsCache = getMapActivity().getMyApplication().getIconsCache();

		ImageButton closeImageButton = (ImageButton) view.findViewById(R.id.closeImageButton);
		closeImageButton.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_remove_dark));
		closeImageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismissMenu();
			}
		});

		Button removeDestButton = (Button) view.findViewById(R.id.removeDestButton);
		removeDestButton.setCompoundDrawablesWithIntrinsicBounds(
				iconsCache.getContentIcon(R.drawable.ic_action_delete_dark), null, null, null);
		removeDestButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getMapActivity().getMyApplication().getTargetPointsHelper().removeWayPoint(true, -1);
				Object contextMenuObj = getMapActivity().getContextMenu().getObject();
				if (getMapActivity().getContextMenu().isActive()
						&& contextMenuObj != null && contextMenuObj instanceof TargetPoint) {
					TargetPoint targetPoint = (TargetPoint) contextMenuObj;
					if (!targetPoint.start && !targetPoint.intermediate) {
						getMapActivity().getContextMenu().close();
					}
				}
				dismissMenu();
			}
		});

		Button findParkingButton = (Button) view.findViewById(R.id.findParkingButton);
		findParkingButton.setCompoundDrawablesWithIntrinsicBounds(
				iconsCache.getContentIcon(R.drawable.ic_action_parking_dark), null, null, null);
		findParkingButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PoiFiltersHelper helper = getMapActivity().getMyApplication().getPoiFilters();
				//PoiType place = getMapActivity().getMyApplication().getPoiTypes().getPoiTypeByKey("parking");
				PoiUIFilter parkingFilter = helper.getFilterById(PoiUIFilter.STD_PREFIX + "parking");
				if (parkingFilter != null) {
					final Intent newIntent = new Intent(getActivity(), SearchPOIActivity.class);
					newIntent.putExtra(SearchPOIActivity.AMENITY_FILTER, parkingFilter.getFilterId());
					newIntent.putExtra(SearchActivity.SEARCH_NEARBY, true);
					startActivityForResult(newIntent, 0);
				}
				dismissMenu();
			}
		});

		View mainView = view.findViewById(R.id.main_view);
		if (menu.isLandscapeLayout()) {
			AndroidUtils.setBackground(view.getContext(), mainView, !menu.isLight(),
					R.drawable.bg_left_menu_light, R.drawable.bg_left_menu_dark);
		} else {
			AndroidUtils.setBackground(view.getContext(), mainView, !menu.isLight(),
					R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		getMapActivity().getContextMenu().setBaseFragmentVisibility(false);
	}

	@Override
	public void onStop() {
		super.onStop();
		getMapActivity().getContextMenu().setBaseFragmentVisibility(true);
	}


	public static void showInstance(DestinationReachedMenu menu) {
		int slideInAnim = menu.getSlideInAnimation();
		int slideOutAnim = menu.getSlideOutAnimation();

		DestinationReachedMenuFragment fragment = new DestinationReachedMenuFragment();
		fragment.menu = menu;
		menu.getMapActivity().getSupportFragmentManager().beginTransaction()
				.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(TAG).commitAllowingStateLoss();
	}

	public void dismissMenu() {
		getMapActivity().getSupportFragmentManager().popBackStack();
	}

	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity != null && activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}
}
