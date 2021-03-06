package net.osmand.plus.dialogs;

import android.support.annotation.StringRes;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.RouteLayer;

public class RasterMapMenu {
	private static final String TAG = "RasterMapMenu";

	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity,
													   final OsmandRasterMapsPlugin.RasterMapType type) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity, false);
		adapter.setDefaultLayoutId(R.layout.drawer_list_material_item);
		createLayersItems(adapter, mapActivity, type);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter contextMenuAdapter,
										  final MapActivity mapActivity,
										  final OsmandRasterMapsPlugin.RasterMapType type) {
		OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);
		final MapTileLayer rasterMapLayer;
		final OsmandSettings.CommonPreference<Integer> mapTransparencyPreference;
		final OsmandSettings.CommonPreference<String> mapTypePreference;
		@StringRes final int mapTypeString;
		@StringRes final int mapTypeStringTransparency;
		if (type == OsmandRasterMapsPlugin.RasterMapType.OVERLAY) {
			rasterMapLayer = plugin.getOverlayLayer();
			mapTransparencyPreference = settings.MAP_OVERLAY_TRANSPARENCY;
			mapTypePreference = settings.MAP_OVERLAY;
			mapTypeString = R.string.map_overlay;
			mapTypeStringTransparency = R.string.overlay_transparency;
		} else if (type == OsmandRasterMapsPlugin.RasterMapType.UNDERLAY) {
			rasterMapLayer = plugin.getUnderlayLayer();
			mapTransparencyPreference = settings.MAP_TRANSPARENCY;
			mapTypePreference = settings.MAP_UNDERLAY;
			mapTypeString = R.string.map_underlay;
			mapTypeStringTransparency = R.string.map_transparency;
		} else {
			throw new RuntimeException("Unexpected raster map type");
		}
		final OsmandSettings.CommonPreference<Boolean> hidePolygonsPref =
				mapActivity.getMyApplication().getSettings().getCustomRenderBooleanProperty("noPolygons");

		String mapTypeDescr = mapTypePreference.get();
		final boolean selected = mapTypeDescr != null;
		final int toggleActionStringId = selected ? R.string.shared_string_enabled
				: R.string.shared_string_disabled;

		final OsmandRasterMapsPlugin.OnMapSelectedCallback onMapSelectedCallback =
				new OsmandRasterMapsPlugin.OnMapSelectedCallback() {
					@Override
					public void onMapSelected() {
						mapActivity.getDashboard().refreshContent(true);
						if (type == OsmandRasterMapsPlugin.RasterMapType.UNDERLAY && selected) {
							Toast.makeText(mapActivity, R.string.consider_turning_polygons_off, Toast.LENGTH_SHORT).show();
						}
					}
				};
		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<?> adapter, View view, int itemId, int pos) {
				if (itemId == mapTypeString) {
					if (selected) {
						plugin.selectMapOverlayLayer(mapActivity.getMapView(), mapTypePreference,
								mapActivity, onMapSelectedCallback);
					}
					return false;
				}
				return super.onRowItemClick(adapter, view, itemId, pos);
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<?> adapter,
											  int itemId, int pos, boolean isChecked) {
				if (itemId == toggleActionStringId) {
					MapActivityLayers mapLayers = mapActivity.getMapLayers();
					if (isChecked) {
						mapLayers.getMapControlsLayer().showTransparencyBar(mapTransparencyPreference);
					} else {
						mapLayers.getMapControlsLayer().hideTransparencyBar(mapTransparencyPreference);
					}
					plugin.toggleUnderlayState(mapActivity, type, onMapSelectedCallback);
				} else if (itemId == R.string.show_polygons) {
					hidePolygonsPref.set(!isChecked);
					refreshMapComplete(mapActivity);
				}
				return false;
			}
		};
		int selectedCode = selected ? 1 : 0;
		mapTypeDescr = selected ? mapTypeDescr : mapActivity.getString(R.string.shared_string_none);
		contextMenuAdapter.item(toggleActionStringId).listen(l).selected(selectedCode).reg();
		contextMenuAdapter.item(mapTypeString).listen(l).layout(R.layout.two_line_list_item)
				.description(mapTypeDescr).reg();
		ContextMenuAdapter.OnIntegerValueChangedListener integerListener =
				new ContextMenuAdapter.OnIntegerValueChangedListener() {
					@Override
					public boolean onIntegerValueChangedListener(int newValue) {
						mapTransparencyPreference.set(newValue);
						mapActivity.getMapView().refreshMap();
						return false;
					}
				};
		// android:max="255" in layout is expected
		contextMenuAdapter.item(mapTypeStringTransparency)
				.layout(R.layout.progress_list_item)
				.iconColor(R.drawable.ic_action_opacity)
				.progress(mapTransparencyPreference.get())
				.listenInteger(integerListener).reg();
		if (type == OsmandRasterMapsPlugin.RasterMapType.UNDERLAY) {
			contextMenuAdapter.item(R.string.show_polygons).listen(l)
					.selected(hidePolygonsPref.get() ? 0 : 1).reg();
		}
	}

	private static void refreshMapComplete(final MapActivity activity) {
		activity.getMyApplication().getResourceManager().getRenderer().clearCache();
		activity.updateMapSettings();
		GPXLayer gpx = activity.getMapView().getLayerByClass(GPXLayer.class);
		if (gpx != null) {
			gpx.updateLayerStyle();
		}
		RouteLayer rte = activity.getMapView().getLayerByClass(RouteLayer.class);
		if (rte != null) {
			rte.updateLayerStyle();
		}
		activity.getMapView().refreshMap(true);
	}
}
