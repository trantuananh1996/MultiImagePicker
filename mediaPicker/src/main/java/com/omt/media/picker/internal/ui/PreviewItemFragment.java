/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.omt.media.picker.internal.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import com.omt.media.picker.internal.entity.Item;
import com.omt.media.picker.internal.entity.SelectionSpec;
import com.omt.media.picker.internal.utils.PhotoMetadataUtils;
import  com.omt.media.picker.R;
import com.omt.media.picker.listener.OnFragmentInteractionListener;

import cn.jzvd.Jzvd;
import cn.jzvd.JzvdStd;
import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;


public class PreviewItemFragment extends Fragment {

    private static final String ARGS_ITEM = "args_item";
    private OnFragmentInteractionListener mListener;
    private JzvdStd videoPlayer;

    public static PreviewItemFragment newInstance(Item item) {
        PreviewItemFragment fragment = new PreviewItemFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARGS_ITEM, item);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_preview_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Item item = getArguments().getParcelable(ARGS_ITEM);
        if (item == null) {
            return;
        }

        videoPlayer = view.findViewById(R.id.jz_player);
        ImageViewTouch image = view.findViewById(R.id.image_view);

        if (item.isVideo()) {
            videoPlayer.setVisibility(View.VISIBLE);
            image.setVisibility(View.GONE);
            videoPlayer.setUp(getRealPathFromURI(getActivity(), item.uri), "");
            Point size = PhotoMetadataUtils.getBitmapSize(item.getContentUri(), getActivity());
            SelectionSpec.getInstance().imageEngine.loadImage(getActivity(), size.x, size.y, videoPlayer.posterImageView,
                    item.getContentUri());
        } else {
            videoPlayer.setVisibility(View.GONE);
            image.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
            image.setSingleTapListener(() -> {
                if (mListener != null) {
                    mListener.onClick();
                }
            });

            Point size = PhotoMetadataUtils.getBitmapSize(item.getContentUri(), getActivity());
            if (item.isGif()) {
                SelectionSpec.getInstance().imageEngine.loadGifImage(getContext(), size.x, size.y, image,
                        item.getContentUri());
            } else {
                SelectionSpec.getInstance().imageEngine.loadImage(getContext(), size.x, size.y, image,
                        item.getContentUri());
            }
        }
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public void resetView() {
        if (getView() != null) {
            ((ImageViewTouch) getView().findViewById(R.id.image_view)).resetMatrix();
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onPause() {
        Jzvd.releaseAllVideos();
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
