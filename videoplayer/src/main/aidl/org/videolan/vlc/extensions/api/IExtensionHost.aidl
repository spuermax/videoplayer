package org.videolan.vlc.extensions.api;

import org.videolan.vlc.extensions.api.VLCExtensionItem;
import android.net.Uri;

interface IExtensionHost {
    // Protocol version 1
    oneway void updateList(in String role, in List<VLCExtensionItem> items, boolean showParams, boolean isRefresh);
    oneway void playUri(in Uri uri, String role);
    oneway void unBind(int index);
}
