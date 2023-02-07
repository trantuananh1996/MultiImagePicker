package com.omt.media.picker.util;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.florent37.runtimepermission.PermissionResult;
import com.github.florent37.runtimepermission.RuntimePermission;
import com.pr.swalert.toast.ToastUtils;

import com.omt.media.picker.Matisse;
import com.omt.media.picker.MimeType;
import com.omt.media.picker.engine.impl.GlideEngine;
import com.omt.media.picker.filter.Filter;
import com.omt.media.picker.internal.entity.CaptureStrategy;
import com.omt.media.picker.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import java8.util.stream.StreamSupport;

/**
 * Created by yazeed44
 * on 6/14/15.
 */
public class Picker {

    private int limitPhoto = -1;
    private int limitVideo = -1;
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
        maxSizeFile = builder.maxSizeFile;
        mimeTypes = builder.mimeTypes;
        filter = builder.filter;
        canCapture = builder.canCapture;
        gridExpectedSize = builder.gridExpectedSize;
        resultLauncher = builder.resultLauncher;
        fileProvider = builder.fileProvider;
        maxVideoDuration = builder.maxVideoDuration;
    }


    public void startActivity(FragmentActivity fragmentActivity) {
        if (fragmentActivity != null) {
            if (limitPhoto <= 0 && limitVideo <= 0) {
                ToastUtils.showToastWarning(fragmentActivity, String.format(fragmentActivity.getString(R.string.you_picked_max_photo), limitPhoto));
                return;
            }
            RuntimePermission.askPermission(fragmentActivity, getPermissions()).onDenied(result -> handleDeniedPermissions(fragmentActivity, result, true)).onForeverDenied(result -> handleDeniedPermissions(fragmentActivity, result, false)).onAccepted(result -> openPicker(fragmentActivity)).ask();
        }
    }

    public String[] getPermissions() {
        List<String> listPermissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            listPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            listPermissions.add(Manifest.permission.READ_MEDIA_IMAGES);
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            listPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return StreamSupport.stream(listPermissions).toArray(String[]::new);
    }

    public void startActivity(Fragment fragmentActivity) {
        if (fragmentActivity != null && fragmentActivity.getActivity() != null) {
            if (limitPhoto <= 0 && limitVideo <= 0) {
                ToastUtils.showToastWarning(fragmentActivity.getContext(), String.format(fragmentActivity.getString(R.string.you_picked_max_photo), limitPhoto));
                return;
            }
            RuntimePermission.askPermission(fragmentActivity, getPermissions()).onDenied(result -> handleDeniedPermissions(fragmentActivity.getActivity(), result, true)).onForeverDenied(result -> handleDeniedPermissions(fragmentActivity.getActivity(), result, false)).onAccepted(result -> openPicker(fragmentActivity)).ask();
        }
    }

    private void handleDeniedPermissions(Activity activity, PermissionResult result, boolean isDenied) {
        StringBuilder denied = getPermissionsString(activity, result, isDenied ? result.getDenied() : result.getForeverDenied());
        ToastUtils.alertYesNo(activity, String.format(activity.getString(R.string.ask_perrmission), denied), yesButtonConfirmed -> {
            if (yesButtonConfirmed) result.goToSettings();
        });
    }

    private void openPicker(FragmentActivity fragmentActivity) {
        setUpMatisse(Matisse.from(fragmentActivity));
    }

    private void openPicker(Fragment fragmentActivity) {
        setUpMatisse(Matisse.from(fragmentActivity));
    }


    private void setUpMatisse(Matisse matisse) {
        matisse.choose(mimeTypes, false).countable(true).capture(canCapture).captureStrategy(new CaptureStrategy(true, fileProvider, "Photo")).maxSelectablePerMediaType(limitPhoto, limitVideo).addFilter(filter).gridExpectedSize(gridExpectedSize).restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT).thumbnailScale(0.7f).imageEngine(new GlideEngine()).maxVideoDuration(maxVideoDuration).showSingleMediaType(true).picker(resultLauncher);
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
        private long maxSizeFile;
        private Set<MimeType> mimeTypes;
        private Filter filter;
        private boolean canCapture;
        private int gridExpectedSize;
        private ActivityResultLauncher<Intent> resultLauncher;
        private String fileProvider;

        public Builder() {
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
                if (limitVideo > 0 && limitPhoto > 0) mimeTypes = MimeType.ofAll();
                else {
                    if (limitPhoto > 0) mimeTypes = MimeType.ofImage();
                    else mimeTypes = MimeType.ofVideo();
                }
            }
            return new Picker(this);
        }
    }
}