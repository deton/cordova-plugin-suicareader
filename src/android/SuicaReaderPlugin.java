package io.github.deton.suicareader;

import com.guncy.android.cardreader.lib.FeliCa;
import com.guncy.android.cardreader.model.Station;

import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.util.Log;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;

public class SuicaReaderPlugin extends CordovaPlugin {
    private static final String TAG = SuicaReaderPlugin.class.getSimpleName();

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.d(TAG, "execute " + action);

        if ("getHistory".equals(action)) {
            int count = 10;
            if (args.length() > 0) {
                args.getInt(0);
            }
            getHistory(callbackContext, count);
            return true;
        }
        return false;
    }

    private void getHistory(final CallbackContext callbackContext, final int count) {
        CordovaPlugin nfcPlugin = webView.getPluginManager().getPlugin("NfcPlugin");
        if (nfcPlugin == null) {
            callbackContext.error("no NfcPlugin");
            return;
        }
        Intent savedIntent = null;
        try {
            savedIntent = (Intent)getPrivateField(nfcPlugin, "savedIntent");
        } catch (Exception ex) {
            Log.e(TAG, "savedIntent", ex);
            callbackContext.error("Failed to get savedIntent from NfcPlugin" + ex);
            return;
        }
        if (savedIntent == null) {
            callbackContext.error("savedIntent is null");
            return;
        }
        final Tag tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            callbackContext.error("Failed to get suica history, tag is null");
            return;
        }

        String[] techList = tag.getTechList();
        if (!Arrays.asList(techList).contains(NfcF.class.getName())) {
            callbackContext.error("tag is not Felica");
            return;
        }
        final byte[] felicaIDm = tag.getId();

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                NfcF felica = NfcF.get(tag);
                if (felica == null) {
                    callbackContext.error("tag is not Felica");
                    return;
                }

                byte[] res = null;
                try {
                    felica.connect();
                    byte[] req = FeliCa.readWithoutEncryption(felicaIDm, count);
                    res = felica.transceive(req);
                } catch (IOException e) {
                    Log.e(TAG, "getHistory", e);
                    String message;
                    if (e.getMessage() != null) {
                        message = e.getMessage();
                    } else {
                        message = e.toString();
                    }
                    callbackContext.error(message);
                    return;
                } finally {
                    try {
                        felica.close();
                    } catch (IOException ignore) {
                    }
                }
                if (res == null) {
                    callbackContext.error("no response");
                    return;
                }
                JSONArray json = parsePasmoHistory(res);
                callbackContext.success(json);
            }
        });
    }

    private static Object getPrivateField(Object target, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Class c = target.getClass();
        Field f = c.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    // https://github.com/dongri/CardReader
    // TODO: use https://github.com/metrodroid/nfc-felica-lib
    private JSONArray parsePasmoHistory(byte[] res) {
        // res[0] = データ長
        // res[1] = 0x07
        // res[2〜9] = カードID
        // res[10,11] = エラーコード。0=正常。
        if (res[10] != 0x00) {
            throw new RuntimeException("Felica error.");
        }

        Context context = cordova.getActivity().getBaseContext();
        JSONArray json = new JSONArray();

        // res[12] = 応答ブロック数
        // res[13+n*16] = 履歴データ。16byte/ブロックの繰り返し。
        int size = res[12];
        for (int i = 0; i < size; i++) {
            FeliCa felica = FeliCa.parse(res, 13 + i * 16);
            int payment = 0;
            if (i < size-1) {
                FeliCa nextFelica = FeliCa.parse(res, 13 + (i+1) * 16);
                payment = felica.remain - nextFelica.remain;
            }
            Station in = Station.getStation(context, felica.inLine, felica.inStation);
            Station out = Station.getStation(context, felica.outLine, felica.outStation);

            JSONObject o = new JSONObject();
            try {
                o.put("year", 2000 + felica.year);
                o.put("month", felica.month);
                o.put("day", felica.day);
                o.put("seqNo", felica.seqNo);
                o.put("kind", felica.kind);
                o.put("device", felica.device);
                o.put("action", felica.action);
                o.put("remain", felica.remain);
                o.put("inLine", in.getLineName());
                o.put("inStation", in.getStationName());
                o.put("outLine", out.getLineName());
                o.put("outStation", out.getStationName());
                o.put("payment", payment);
            } catch (JSONException ex) {
                Log.w(TAG, "JSONObject#put", ex);
            }
            json.put(o);
        }
        return json;
    }
}
