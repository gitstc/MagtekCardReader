package com.stc.CDVMagtekPlugin;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CDVMagtekPlugin extends CordovaPlugin {
	private CallbackContext mEventListenerCb;
    private MagtekPlugin magtekPlugin = null;

    @Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
		if(magtekPlugin == null) {
             magtekPlugin = new MagtekPlugin(cordova.getActivity(), CDVMagtekPlugin.this);
        }

		if(action.equals("openDevice")) {
            magtekPlugin.open(new MagtekPlugin.OpenCallback() {
                @Override
                public void onResult(int result) {
                    PluginResult pr = null;
                    if(result < 0) {
                        pr = new PluginResult(PluginResult.Status.ERROR);
                    }
                    else {
                        pr = new PluginResult(PluginResult.Status.OK);
                    }

                    callbackContext.sendPluginResult(pr);
                };
            });
		}
		else if(action.equals("closeDevice")) {
			magtekPlugin.close(new MagtekPlugin.CloseCallback() {
                @Override
                public void onResult(int result) {
                    PluginResult pr = null;
                    if(result < 0) {
                        pr = new PluginResult(PluginResult.Status.ERROR);
                    }
                    else {
                        pr = new PluginResult(PluginResult.Status.OK);
                    }

                    callbackContext.sendPluginResult(pr);
                };
            });
		}
		else if(action.equals("readCard")) {
			magtekPlugin.readCard(new MagtekPlugin.ReadCallback() {
                @Override
                public void onResult(JSONObject data) {
                    PluginResult pr = new PluginResult(PluginResult.Status.OK, data);
                    callbackContext.sendPluginResult(pr);
                }
            });
		}
        else {
            return false;
        }

        return true;
    }
}