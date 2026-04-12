package org.n0pocketworkstation.pckeyboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginManager extends BroadcastReceiver {
    private static String TAG = "PCKeyboard";
    private static String HK_INTENT_DICT = "org.n0pocketworkstation.DICT";
    private static String SOFTKEYBOARD_INTENT_DICT = "com.menny.android.anysoftkeyboard.DICTIONARY";
    private static String SOFTKEYBOARD_DICT_RESOURCE_METADATA_NAME = "com.menny.android.anysoftkeyboard.dictionaries";
    private LatinIME mIME;

    // Apparently anysoftkeyboard doesn't use ISO 639-1 language codes for its locales?
    // Add exceptions as needed.
    private static Map<String, String> SOFTKEYBOARD_LANG_MAP = new HashMap<String, String>();

    static {
        SOFTKEYBOARD_LANG_MAP.put("dk", "da");
    }

    private static Map<String, DictPluginSpec> mPluginDicts =
            new HashMap<String, DictPluginSpec>();

    PluginManager(LatinIME ime) {
        super();
        mIME = ime;
    }

    static interface DictPluginSpec {
        BinaryDictionary getDict(Context context);
    }

    static void getSoftKeyboardDictionaries(PackageManager packageManager) {
        Intent dictIntent = new Intent(SOFTKEYBOARD_INTENT_DICT);
        List<ResolveInfo> dictPacks = packageManager.queryBroadcastReceivers(
                dictIntent, PackageManager.GET_META_DATA);
        for (ResolveInfo ri : dictPacks) {
            ApplicationInfo appInfo = ri.activityInfo.applicationInfo;
            String pkgName = appInfo.packageName;
            boolean success = false;
            try {
                Resources res = packageManager.getResourcesForApplication(appInfo);
                //Log.i(TAG, "Found dictionary plugin package: " + pkgName);
                int dictId = res.getIdentifier("dictionaries", "xml", pkgName);
                if (dictId == 0) {
                    try {
                        dictId = ri.activityInfo.metaData.getInt(SOFTKEYBOARD_DICT_RESOURCE_METADATA_NAME);
                    } catch (Exception e) {
                    }
                }
                if (dictId == 0)
                    continue;
                XmlResourceParser xrp = res.getXml(dictId);

                try {
                    int current = xrp.getEventType();
                    while (current != XmlResourceParser.END_DOCUMENT) {
                        if (current == XmlResourceParser.START_TAG) {
                            String tag = xrp.getName();
                            if (tag != null && tag.equalsIgnoreCase("Dictionary")) {
                                String lang = xrp.getAttributeValue(null, "locale");
                                String convLang = SOFTKEYBOARD_LANG_MAP.get(lang);
                                if (convLang != null) lang = convLang;
                                
                                String type = xrp.getAttributeValue(null, "type");
                                if (type == null || type.equals("raw") || type.equals("binary") || type.equals("binary_resource")) {
                                    String assetName = xrp.getAttributeValue(null, "dictionaryAssetName");
                                    if (assetName == null) {
                                        assetName = xrp.getAttributeValue(null, "dictionaryAssertName"); // sic, legacy typo
                                    }
                                    int resId = xrp.getAttributeResourceValue(null, "dictionaryResourceId", 0);
                                    if (resId == 0) {
                                        // Try resolving by name if it's not a literal resource ID
                                        String resName = xrp.getAttributeValue(null, "dictionaryResourceId");
                                        if (resName != null) {
                                            if (resName.startsWith("@")) resName = resName.substring(1);
                                            if (resName.contains("/")) resName = resName.substring(resName.lastIndexOf("/") + 1);
                                            resId = res.getIdentifier(resName, "raw", pkgName);
                                        }
                                    }
                                    
                                    if (lang != null && (assetName != null || resId != 0)) {
                                        DictPluginSpec spec = new DictPluginSpecSoftKeyboard(pkgName, assetName, resId);
                                        mPluginDicts.put(lang, spec);
                                        Log.i(TAG, "Found plugin dictionary: lang=" + lang + ", pkg=" + pkgName + " (asset=" + assetName + ", resId=" + resId + ")");
                                        success = true;
                                    }
                                } else {
                                    Log.w(TAG, "Unsupported AnySoftKeyboard dict type " + type);
                                }
                            }
                        }
                        xrp.next();
                        current = xrp.getEventType();
                    }
                } catch (XmlPullParserException e) {
                    Log.e(TAG, "Dictionary XML parsing failure");
                } catch (IOException e) {
                    Log.e(TAG, "Dictionary XML IOException");
                }
            } catch (NameNotFoundException e) {
                Log.i(TAG, "Package not found: " + pkgName);
            } finally {
                if (!success) {
                    // Log.i(TAG, "failed to load plugin dictionary spec from " + pkgName);
                }
            }
        }
    }

    static void getHKDictionaries(PackageManager packageManager) {
        Intent dictIntent = new Intent(HK_INTENT_DICT);
        List<ResolveInfo> dictPacks = packageManager.queryIntentActivities(dictIntent, 0);
        for (ResolveInfo ri : dictPacks) {
            ApplicationInfo appInfo = ri.activityInfo.applicationInfo;
            String pkgName = appInfo.packageName;
            boolean success = false;
            try {
                Resources res = packageManager.getResourcesForApplication(appInfo);
                //Log.i(TAG, "Found dictionary plugin package: " + pkgName);
                int langId = res.getIdentifier("dict_language", "string", pkgName);
                if (langId == 0) continue;
                String lang = res.getString(langId);
                int[] rawIds = null;

                // Try single-file version first
                int rawId = res.getIdentifier("main", "raw", pkgName);
                if (rawId != 0) {
                    rawIds = new int[]{rawId};
                } else {
                    // try multi-part version
                    int parts = 0;
                    List<Integer> ids = new ArrayList<Integer>();
                    while (true) {
                        int id = res.getIdentifier("main" + parts, "raw", pkgName);
                        if (id == 0) break;
                        ids.add(id);
                        ++parts;
                    }
                    if (parts == 0) continue; // no parts found
                    rawIds = new int[parts];
                    for (int i = 0; i < parts; ++i) rawIds[i] = ids.get(i);
                }
                DictPluginSpec spec = new DictPluginSpecHK(pkgName, rawIds);
                mPluginDicts.put(lang, spec);
                Log.i(TAG, "Found plugin dictionary: lang=" + lang + ", pkg=" + pkgName);
                success = true;
            } catch (NameNotFoundException e) {
                Log.i(TAG, "bad");
            } finally {
                if (!success) {
                    Log.i(TAG, "failed to load plugin dictionary spec from " + pkgName);
                }
            }
        }
    }

    static private abstract class DictPluginSpecBase
            implements DictPluginSpec {
        String mPackageName;

        Resources getResources(Context context) {
            PackageManager packageManager = context.getPackageManager();
            Resources res = null;
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(mPackageName, 0);
                res = packageManager.getResourcesForApplication(appInfo);
            } catch (NameNotFoundException e) {
                Log.i(TAG, "couldn't get resources");
            }
            return res;
        }

        abstract InputStream[] getStreams(Resources res);

        public BinaryDictionary getDict(Context context) {
            Resources res = getResources(context);
            if (res == null) return null;

            InputStream[] dicts = getStreams(res);
            if (dicts == null) return null;
            
            // Wrap in GZIP if needed
            InputStream[] wrapped = new InputStream[dicts.length];
            for (int i = 0; i < dicts.length; i++) {
                try {
                    // Try to peek if it's GZipped (0x1f 0x8b)
                    java.io.PushbackInputStream pb = new java.io.PushbackInputStream(dicts[i], 2);
                    byte[] signature = new byte[2];
                    int read = pb.read(signature);
                    if (read == 2 && signature[0] == (byte) 0x1f && signature[1] == (byte) 0x8b) {
                        pb.unread(signature);
                        wrapped[i] = new java.util.zip.GZIPInputStream(pb);
                    } else {
                        if (read > 0) pb.unread(signature, 0, read);
                        wrapped[i] = pb;
                    }
                } catch (IOException e) {
                    wrapped[i] = dicts[i];
                }
            }
            
            BinaryDictionary dict = new BinaryDictionary(
                    context, wrapped, Suggest.DIC_MAIN);
            if (dict.getSize() == 0) {
                dict.close();
                return null;
            }
            //Log.i(TAG, "dict size=" + dict.getSize());
            return dict;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Package information changed, updating dictionaries.");
        getPluginDictionaries(context);
        Log.i(TAG, "Finished updating dictionaries.");
        mIME.toggleLanguage(true, true);
    }

    static private class DictPluginSpecHK
            extends DictPluginSpecBase {

        int[] mRawIds;

        public DictPluginSpecHK(String pkg, int[] ids) {
            mPackageName = pkg;
            mRawIds = ids;
        }

        @Override
        InputStream[] getStreams(Resources res) {
            if (mRawIds == null || mRawIds.length == 0) return null;
            InputStream[] streams = new InputStream[mRawIds.length];
            for (int i = 0; i < mRawIds.length; ++i) {
                streams[i] = res.openRawResource(mRawIds[i]);
            }
            return streams;
        }
    }

    static private class DictPluginSpecSoftKeyboard
            extends DictPluginSpecBase {

        String mAssetName;
        int mResId;

        public DictPluginSpecSoftKeyboard(String pkg, String asset, int resId) {
            mPackageName = pkg;
            mAssetName = asset;
            mResId = resId;
        }

        @Override
        InputStream[] getStreams(Resources res) {
            if (mAssetName == null) {
                if (mResId == 0) return null;
                
                // Modern AnySoftKeyboard packs often point directly to a raw resource.
                // Older ones used a TypedArray of resource IDs.
                try {
                    // Try opening as a single raw resource first
                    InputStream in = res.openRawResource(mResId);
                    return new InputStream[]{in};
                } catch (Exception e) {
                    // Fall back to treating it as a TypedArray of resources
                    try {
                        TypedArray a = res.obtainTypedArray(mResId);
                        int[] resIds;
                        try {
                            resIds = new int[a.length()];
                            for (int i = 0; i < a.length(); ++i) {
                                resIds[i] = a.getResourceId(i, 0);
                            }
                        } finally {
                            a.recycle();
                        }
                        InputStream[] in = new InputStream[resIds.length];
                        for (int i = 0; i < resIds.length; ++i) {
                            in[i] = res.openRawResource(resIds[i]);
                        }
                        return in;
                    } catch (Exception e2) {
                        Log.e(TAG, "Failed to load dictionary resource " + mResId, e2);
                        return null;
                    }
                }
            } else {
                try {
                    InputStream in = res.getAssets().open(mAssetName);
                    return new InputStream[]{in};
                } catch (IOException e) {
                    Log.e(TAG, "Dictionary asset loading failure");
                    return null;
                }
            }
        }
    }

    static void getPluginDictionaries(Context context) {
        mPluginDicts.clear();
        PackageManager packageManager = context.getPackageManager();
        getSoftKeyboardDictionaries(packageManager);
        getHKDictionaries(packageManager);
    }

    static BinaryDictionary getDictionary(Context context, String lang) {
        //Log.i(TAG, "Looking for plugin dictionary for lang=" + lang);
        DictPluginSpec spec = mPluginDicts.get(lang);
        if (spec == null && lang != null && lang.length() >= 2) spec = mPluginDicts.get(lang.substring(0, 2));
        if (spec == null) {
            //Log.i(TAG, "No plugin found.");
            return null;
        }
        BinaryDictionary dict = spec.getDict(context);
        Log.i(TAG, "Found plugin dictionary for " + lang + (dict == null ? " is null" : ", size=" + dict.getSize()));
        return dict;
    }

    static String getDictionaryPackageName(String lang) {
        if (lang == null) return null;
        DictPluginSpec spec = mPluginDicts.get(lang);
        if (spec == null && lang.length() >= 2) spec = mPluginDicts.get(lang.substring(0, 2));
        if (spec instanceof DictPluginSpecBase) {
            return ((DictPluginSpecBase) spec).mPackageName;
        }
        return null;
    }
}
