package ir.sospans.lalastories.ui.home;

import com.adivery.sdk.AdiveryNativeCallback;
import com.adivery.sdk.NativeAd;

public abstract class NativeAdCallbackHelper extends AdiveryNativeCallback {

    @Override
    public void onAdLoaded(NativeAd nativeAd) {
        onLoaded(nativeAd);
    }

    @Override
    public void onAdLoadFailed(String error) {
        onFailed();
    }

    public abstract void onLoaded(NativeAd nativeAd);
    public abstract void onFailed();
}
