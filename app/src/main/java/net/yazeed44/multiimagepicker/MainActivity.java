package net.yazeed44.multiimagepicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;


import net.yazeed44.imagepicker.Matisse;
import net.yazeed44.imagepicker.data.model.ImageEntry;
import net.yazeed44.imagepicker.sample.BuildConfig;
import net.yazeed44.imagepicker.sample.R;
import net.yazeed44.imagepicker.util.Picker;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Sample activity";
    private RecyclerView mImageSampleRecycler;
    private ArrayList<ImageEntry> mSelectedImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mImageSampleRecycler = findViewById(R.id.images_sample);
        setupRecycler();


    }

    private void setupRecycler() {

        final GridLayoutManager gridLayoutManager = new GridLayoutManager(this, getResources().getInteger(R.integer.num_columns_image_samples));
        gridLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mImageSampleRecycler.setLayoutManager(gridLayoutManager);


    }

    private final ActivityResultLauncher<Intent> resultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) return;
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d("Trungnk", "1: ");
                } else {
                    Log.d("Trungnk", "2: ");
                }


                List<String> paths = Matisse.obtainPathResult(result.getData());
//                mSelectedImages = images;
//                setupImageSamples();
            });


    public void onClickPickImageSingle(View view) {
        Picker picker = new Picker.Builder(this)
                .limitPhoto(5)
                .limitVideo(5)
                .canCapture(true)
                .fileProvider(BuildConfig.APPLICATION_ID + ".fileprovider")
                .resultLauncher(resultLauncher)
                .build();
        picker.startActivity();
    }

    public void onClickPickImageMultipleWithLimit(View view) {
//        new Picker.Builder(this, this, R.style.MIP_theme)
//                .setPickMode(Picker.PickMode.MULTIPLE_IMAGES)
//                .setLimit(6)
//                .build()
//                .startActivity();
//    }
    }

    public void onPickImageMultipleInfinite(View view) {
//        new Picker.Builder(this, this, R.style.MIP_theme)
//                .setPickMode(Picker.PickMode.MULTIPLE_IMAGES)
//                .build()
//                .startActivity();

    }

    public void onClickPickImageWithVideos(View view) {
//        new Picker.Builder(this, this, R.style.MIP_theme)
//                .setPickMode(Picker.PickMode.MULTIPLE_IMAGES)
//                .setVideosEnabled(true)
//                .build()
//                .startActivity();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();


        if (id == R.id.action_about) {
            showAbout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAbout() {

        final Spanned aboutBody = Html.fromHtml(getResources().getString(R.string.about_body_html));


        new MaterialDialog.Builder(this)
                .title(R.string.about_title)
                .content(aboutBody)
                .contentLineSpacing(1.6f)
                .show();


    }

    private class ImageSamplesAdapter extends RecyclerView.Adapter<ImageSampleViewHolder> {


        @Override
        public ImageSampleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final ImageView imageView = new ImageView(parent.getContext());
            return new ImageSampleViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(ImageSampleViewHolder holder, int position) {
            final String path = mSelectedImages.get(position).getPath();
            loadImage(path, holder.thumbnail);
        }

        @Override
        public int getItemCount() {
            return mSelectedImages.size();
        }


        private void loadImage(final String path, final ImageView imageView) {
            imageView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 440));

            Glide.with(MainActivity.this)
                    .asBitmap()
                    .load(path)
                    .into(imageView);


        }


    }

    class ImageSampleViewHolder extends RecyclerView.ViewHolder {

        protected ImageView thumbnail;

        public ImageSampleViewHolder(View itemView) {
            super(itemView);
            thumbnail = (ImageView) itemView;
        }
    }
}
