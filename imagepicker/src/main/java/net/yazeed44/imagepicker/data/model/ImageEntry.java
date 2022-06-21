package net.yazeed44.imagepicker.data.model;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by yazeed44
 * on 6/14/15.
 */
public class ImageEntry implements Parcelable {
    private final int imageId;
    private final String path;
    private final long dateAdded;
    private boolean isPicked;
    private boolean isVideo;

    private int orientation = 0;
    private int maxDimen = 1152;
    private int compressPercent = 100;
    private Bitmap bitmap;

    private String description = "";
    private boolean uploaded;
    private int progress;

    private String S3Url;
    private String genId;
    private int offset;
    private String pathCompress;

    protected ImageEntry(Parcel in) {
        imageId = in.readInt();
        path = in.readString();
        dateAdded = in.readLong();
        isPicked = in.readByte() != 0;
        isVideo = in.readByte() != 0;
        orientation = in.readInt();
        maxDimen = in.readInt();
        compressPercent = in.readInt();
        bitmap = in.readParcelable(Bitmap.class.getClassLoader());
        description = in.readString();
        uploaded = in.readByte() != 0;
        progress = in.readInt();
        S3Url = in.readString();
        genId = in.readString();
        offset = in.readInt();
        pathCompress = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(imageId);
        dest.writeString(path);
        dest.writeLong(dateAdded);
        dest.writeByte((byte) (isPicked ? 1 : 0));
        dest.writeByte((byte) (isVideo ? 1 : 0));
        dest.writeInt(orientation);
        dest.writeInt(maxDimen);
        dest.writeInt(compressPercent);
        dest.writeParcelable(bitmap, flags);
        dest.writeString(description);
        dest.writeByte((byte) (uploaded ? 1 : 0));
        dest.writeInt(progress);
        dest.writeString(S3Url);
        dest.writeString(genId);
        dest.writeInt(offset);
        dest.writeString(pathCompress);
    }

    public static final Creator<ImageEntry> CREATOR = new Creator<ImageEntry>() {
        @Override
        public ImageEntry createFromParcel(Parcel in) {
            return new ImageEntry(in);
        }

        @Override
        public ImageEntry[] newArray(int size) {
            return new ImageEntry[size];
        }
    };

    public ImageEntry setS3Url(String s3Url) {
        S3Url = s3Url;
        return this;
    }

    public ImageEntry setGenId(String genId) {
        this.genId = genId;
        return this;
    }

    public String getPathCompress() {
        return (pathCompress == null || pathCompress.trim().isEmpty()) ? path : pathCompress;
    }

    public ImageEntry setPathCompress(String pathCompress) {
        this.pathCompress = pathCompress;
        return this;
    }

    private void logException(Throwable e) {
        e.printStackTrace();
    }

    public String getBase64(int maxSizeKB) {
        String base64 = getBase64();
        int size = base64.getBytes().length / 1024;
        while (size > maxSizeKB) {
            if (compressPercent > 70) {
                setCompressPercent(getCompressPercent() - 10);
                base64 = getBase64();
                size = base64.getBytes().length / 1024;
            } else break;
        }
        return base64;
    }

    public String getBase64() {
        String rt = "";
        Bitmap bmp = getScaledBitmap();
        ExifInterface exif;
        int angle = 0;
        try {
            exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                angle = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                angle = 180;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            logException(e);
        }
        Matrix matrix1 = new Matrix();

        //set image rotation value to 45 degrees in matrix.
        matrix1.postRotate(angle);
        if (bmp != null)
            //Create bitmap with new values.
            bmp = Bitmap.createBitmap(bmp, 0, 0,
                    bmp.getWidth(), bmp.getHeight(), matrix1, true);
        if (bmp != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, compressPercent, baos);
            byte[] b = baos.toByteArray();
            return Base64.encodeToString(b, Base64.DEFAULT);
        }
        return rt;
    }

    /**
     * Rotate a bitmap based on orientation metadata.
     * src - image path
     */
    public static Bitmap rotateBitmap(String src) {
        Bitmap bitmap = BitmapFactory.decodeFile(src);
        try {
            ExifInterface exif = new ExifInterface(src);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.setScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setRotate(180);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(-90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(-90);
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                case ExifInterface.ORIENTATION_UNDEFINED:
                default:
                    return bitmap;
            }

            try {
                Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                return oriented;
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                return bitmap;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public Bitmap getBitmapRotated() {
        Bitmap bmp = getScaledBitmap();
        ExifInterface exif;
        int angle = 0;
        try {
            exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);


            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                angle = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                angle = 180;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            logException(e);
        }
        Matrix matrix1 = new Matrix();

        //set image rotation value to 45 degrees in matrix.
        matrix1.postRotate(angle);
        //Create bitmap with new values.
        bmp = Bitmap.createBitmap(bmp, 0, 0,
                bmp.getWidth(), bmp.getHeight(), matrix1, true);
        return bmp;
    }

    public String getBase64Rotated(Context context) {
        String rt = "";
        Bitmap bmp = null;
        try {
            bmp = getBitmapResignedAndRotated(getUri(), context);
            if (bmp != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, compressPercent, baos);
                byte[] b = baos.toByteArray();
                return Base64.encodeToString(b, Base64.DEFAULT);
            }
        } catch (IOException e) {
            logException(e);
        }

        return rt;
    }

    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {

        InputStream input = context.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23)
            ei = new ExifInterface(input);
        else
            ei = new ExifInterface(selectedImage.getPath());

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    public Bitmap getBitmapResignedAndRotated(Uri photoUri, Context context) throws IOException {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream imageStream = context.getContentResolver().openInputStream(photoUri);
        BitmapFactory.decodeStream(imageStream, null, options);
        imageStream.close();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, maxDimen, maxDimen);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        imageStream = context.getContentResolver().openInputStream(photoUri);
        Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);

        img = rotateImageIfRequired(context, img, photoUri);
        return img;

    }

    public int getOrientation(Uri photoUri, Context mContext) {
        /* it's on the external media. */
        Cursor cursor = mContext.getContentResolver().query(photoUri,
                new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);
        if (cursor == null) return -1;
        if (cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        return cursor.getInt(0);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public boolean getUploaded() {
        return uploaded;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public int getImageId() {
        return imageId;
    }

    public String getPath() {
        return path;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public boolean isPicked() {
        return isPicked;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public String getS3Url() {
        return S3Url;
    }

    public String getGenId() {
        return genId;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = Math.min(heightRatio, widthRatio);

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down further
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    public Bitmap decodeFile(File f) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        int originalWidth = o.outWidth;
        int originalHeight = o.outHeight;
        int newWidth = originalWidth;
        int newHeight = originalHeight;
        if (originalWidth > maxDimen) {
            newWidth = maxDimen;
            newHeight = (newWidth * originalHeight) / originalWidth;
        }
        if (newHeight > maxDimen) {
            newHeight = maxDimen;
            newWidth = (newHeight * originalWidth) / originalHeight;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(o, newWidth, newHeight);
        return BitmapFactory.decodeFile(f.getAbsolutePath(), options);
    }

    @Nullable
    public Bitmap getBitmap() {
        if (TextUtils.isEmpty(path)) {
            return bitmap;
        } else {
            File imgFile = new File(path);
            if (imgFile.exists()) {
                bitmap = decodeFile(path);
                if (bitmap == null) {
                    bitmap = rotateImage(decodeFile(imgFile), orientation);
                }
                return bitmap;
            }
        }
        return null;
    }

    private Bitmap decodeFile(String path) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(new File(path).getAbsolutePath(), o);
            int originalWidth = o.outWidth;
            int originalHeight = o.outHeight;
            int newWidth = originalWidth;
            int newHeight = originalHeight;
            if (originalWidth > maxDimen) {
                newWidth = maxDimen;
                newHeight = (newWidth * originalHeight) / originalWidth;
            }
            if (newHeight > maxDimen) {
                newHeight = maxDimen;
                newWidth = (newHeight * originalWidth) / originalHeight;
            }
            Matrix matrix = new Matrix();
            matrix.postScale(newWidth, newHeight);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            return rotateImage(bitmap, orientation);
        } catch (Exception exception) {
            logException(exception);
        }
        return null;
    }

    public Uri getUri() {
        if (TextUtils.isEmpty(path)) {
            return null;
        } else {
            File imgFile = new File(path);
            if (imgFile.exists()) {
                return Uri.fromFile(imgFile);
            }
        }
        return null;
    }

    @Nullable
    public Bitmap getScaledBitmap() {
        Bitmap originBm = getBitmap();
        if (originBm == null) return null;
        int originalWidth = originBm.getWidth();
        int originalHeight = originBm.getHeight();
        int newWidth = originalWidth;
        int newHeight = originalHeight;
        if (originalWidth > maxDimen) {
            newWidth = maxDimen;
            newHeight = (newWidth * originalHeight) / originalWidth;
        }
        if (newHeight > maxDimen) {
            newHeight = maxDimen;
            newWidth = (newHeight * originalWidth) / originalHeight;
        }
        return bitmapResizer(originBm, newWidth, newHeight);
    }

    private Bitmap bitmapResizer(Bitmap bitmap, int newWidth, int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float ratioX = newWidth / (float) bitmap.getWidth();
        float ratioY = newHeight / (float) bitmap.getHeight();
        float middleX = newWidth / 2.0f;
        float middleY = newHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setFilterBitmap(true);
        //canvas.drawBitmap(bitmap, matrix, paint);
        canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, paint);
        return scaledBitmap;

    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        if (img == null) return null;
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    private ImageEntry(Builder builder) {
        imageId = builder.imageId;
        path = builder.path;
        dateAdded = builder.dateAdded;
        isPicked = builder.isPicked;
        isVideo = builder.isVideo;
    }

//    public static ImageEntry from(final Cursor cursor) {
//        return Builder.from(cursor).build();
//    }

    @NonNull
    @Contract("_ -> new")
    public static ImageEntry from(final String path) {
        return Builder.from(path).build();
    }

    @NonNull
    @Contract("_ -> new")
    public static ImageEntry fromPathLocal(final String path) {
        return Builder.from(path).isPicked(true).build();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ImageEntry && ((ImageEntry) o).path.equals(path);
    }

    @NonNull
    @Override
    public String toString() {
        return "ImageEntry{" +
                "path='" + path + '\'' +
                '}';
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public int getCompressPercent() {
        return compressPercent;
    }

    public void setCompressPercent(int compressPercent) {
        this.compressPercent = compressPercent;
    }

    public int getMaxDimen() {
        return maxDimen;
    }

    public void setMaxDimen(int maxDimen) {
        this.maxDimen = maxDimen;
    }

    @NonNull
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


    public static final class Builder {
        private static int count = -1;
        private int imageId;
        private String path;
        private long dateAdded;
        private boolean isPicked;
        private boolean isVideo;


        public Builder() {
        }

        public static Builder from(final String path) {
            return new Builder()
                    .path(path)
                    .imageId(count--);
        }


        public Builder imageId(int imageId) {
            this.imageId = imageId;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder dateAdded(long dateAdded) {
            this.dateAdded = dateAdded;
            return this;
        }

        public Builder isPicked(boolean isPicked) {
            this.isPicked = isPicked;
            return this;
        }

        public Builder isVideo(boolean isVideo) {
            this.isVideo = isVideo;
            return this;
        }

        public ImageEntry build() {
            return new ImageEntry(this);
        }
    }
}
