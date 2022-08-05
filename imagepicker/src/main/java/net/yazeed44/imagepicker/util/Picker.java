package net.yazeed44.imagepicker.util;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.FragmentActivity;

import com.github.florent37.runtimepermission.PermissionResult;
import com.github.florent37.runtimepermission.RuntimePermission;
import com.pr.swalert.toast.ToastUtils;

import net.yazeed44.imagepicker.Matisse;
import net.yazeed44.imagepicker.MimeType;
import net.yazeed44.imagepicker.engine.impl.GlideEngine;
import net.yazeed44.imagepicker.filter.Filter;
import net.yazeed44.imagepicker.internal.entity.CaptureStrategy;
import net.yazeed44.imagepicker.library.R;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

/**
 * Created by yazeed44
 * on 6/14/15.
 */
public class Picker {

    private int limitPhoto = -1;
    private int limitVideo = -1;
    private final WeakReference<FragmentActivity> activityWeakReference;
    private long maxSizeFile = -1;
    private Set<MimeType> mimeTypes;
    private Filter filter;
    private boolean canCapture;
    private int gridExpectedSize = 124;
    private final ActivityResultLauncher<Intent> resultLauncher;
    private final String fileProvider;
    private long maxVideoDuration;

    private Picker(Builder builder) {
        limitPhoto = builder.limitPhoto;
        limitVideo = builder.limitVideo;
        activityWeakReference = builder.activityWeakReference;
        maxSizeFile = builder.maxSizeFile;
        mimeTypes = builder.mimeTypes;
        filter = builder.filter;
        canCapture = builder.canCapture;
        gridExpectedSize = builder.gridExpectedSize;
        resultLauncher = builder.resultLauncher;
        fileProvider = builder.fileProvider;
        maxVideoDuration = builder.maxVideoDuration;
    }


    public void startActivity() {
        if (limitPhoto <= 0 && limitVideo <= 0) {
            ToastUtils.showToastWarning(activityWeakReference.get(), R.string.you_picked_max_photo);
            return;
        }
        if (activityWeakReference.get() != null) {
            Activity activity = activityWeakReference.get();
            RuntimePermission.askPermission(
                            activity,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    .onDenied(result -> handleDeniedPermissions(activity, result, true))
                    .onForeverDenied(result -> handleDeniedPermissions(activity, result, false))
                    .onAccepted(result -> openPicker())
                    .ask();
        }
    }

    private void handleDeniedPermissions(Activity activity, PermissionResult result, boolean isDenied) {
        StringBuilder denied = getPermissionsString(activity, result, isDenied ? result.getDenied() : result.getForeverDenied());
        ToastUtils.alertYesNo(activity, String.format(activity.getString(R.string.ask_perrmission), denied), yesButtonConfirmed -> {
            if (yesButtonConfirmed) result.goToSettings();
        });
    }

    private void openPicker() {
        Matisse.from(activityWeakReference.get())
                .choose(mimeTypes, false)
                .countable(true)
                .capture(canCapture)
                .captureStrategy(
                        new CaptureStrategy(true, fileProvider, "Photo"))
                .maxSelectablePerMediaType(limitPhoto, limitVideo)
                .addFilter(filter)
                .gridExpectedSize(gridExpectedSize)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .thumbnailScale(0.7f)
                .imageEngine(new GlideEngine())
                .maxVideoDuration(maxVideoDuration)
                .showSingleMediaType(true)
                .picker(resultLauncher);
    }


    private StringBuilder getPermissionsString(Activity activity, PermissionResult result, List<String> foreverDenied) {
        StringBuilder denied = new StringBuilder();
        for (String permission : foreverDenied) {
            try {
                denied.append("- ").append(activity.getPackageManager().getPermissionInfo(permission, 0).loadLabel(activity.getPackageManager()));
                if (result.getDenied().indexOf(permission) != result.getDenied().size() - 1)
                    denied.append("\n");
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return denied;
    }

    public static final class Builder {
        public long maxVideoDuration;
        private int limitPhoto;
        private int limitVideo;
        private WeakReference<FragmentActivity> activityWeakReference;
        private long maxSizeFile;
        private Set<MimeType> mimeTypes;
        private Filter filter;
        private boolean canCapture;
        private int gridExpectedSize;
        private ActivityResultLauncher<Intent> resultLauncher;
        private String fileProvider;

        public Builder(FragmentActivity fragmentActivity) {
            this.activityWeakReference = new WeakReference<>(fragmentActivity);
        }

        public Builder setMaxVideoDuration(long maxVideoDuration) {
            this.maxVideoDuration = maxVideoDuration;
            return this;
        }

        public Builder limitPhoto(int limitPhoto) {
            this.limitPhoto = limitPhoto;
            return this;
        }

        public Builder limitVideo(int limitVideo) {
            this.limitVideo = limitVideo;
            return this;
        }

        public Builder activityWeakReference(WeakReference<FragmentActivity> activityWeakReference) {
            this.activityWeakReference = activityWeakReference;
            return this;
        }

        public Builder maxSizeFile(long maxSizeFile) {
            this.maxSizeFile = maxSizeFile;
            return this;
        }

        public Builder mimeTypes(Set<MimeType> mimeTypes) {
            this.mimeTypes = mimeTypes;
            return this;
        }

        public Builder filter(Filter filter) {
            this.filter = filter;
            return this;
        }

        public Builder canCapture(boolean canCapture) {
            this.canCapture = canCapture;
            return this;
        }

        public Builder gridExpectedSize(int gridExpectedSize) {
            this.gridExpectedSize = gridExpectedSize;
            return this;
        }

        public Builder resultLauncher(ActivityResultLauncher<Intent> val) {
            resultLauncher = val;
            return this;
        }

        public Builder fileProvider(String fileProvider) {
            this.fileProvider = fileProvider;
            return this;
        }

        public Picker build() {
            if (mimeTypes == null) {
                if (limitVideo > 0 && limitPhoto > 0)
                    mimeTypes = MimeType.ofAll();
                else {
                    if (limitPhoto > 0)
                        mimeTypes = MimeType.ofImage();
                    else
                        mimeTypes = MimeType.ofVideo();
                }
            }
            return new Picker(this);
        }
    }
}