package etn.app.danghoc.shoppingclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dmax.dialog.SpotsDialog;
import etn.app.danghoc.shoppingclient.Adapter.CategoryAdapter;
import etn.app.danghoc.shoppingclient.Adapter.CategoryProductAdapter;
import etn.app.danghoc.shoppingclient.Adapter.ImageAdapter;
import etn.app.danghoc.shoppingclient.Callback.IClickDeleteImage;
import etn.app.danghoc.shoppingclient.Common.Common;
import etn.app.danghoc.shoppingclient.Common.MoneyTextWatcher;
import etn.app.danghoc.shoppingclient.EventBus.UpLoadImageSuccess;
import etn.app.danghoc.shoppingclient.Model.CategoryProduct;
import etn.app.danghoc.shoppingclient.Model.ImageModel;
import etn.app.danghoc.shoppingclient.Model.LinkImageModel;
import etn.app.danghoc.shoppingclient.Model.Tinh;
import etn.app.danghoc.shoppingclient.Model.UpdateModel;
import etn.app.danghoc.shoppingclient.Retrofit.IMyShoppingAPI;
import etn.app.danghoc.shoppingclient.Retrofit.RetrofitClient;
import etn.app.danghoc.shoppingclient.Retrofit.RetrofitClientAddress;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import org.greenrobot.eventbus.EventBus;

public class UpdateProductActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int IMAGE_CODE = 1;

    CompositeDisposable compositeDisposable = new CompositeDisposable();
    CategoryProductAdapter adapter;
    CategoryAdapter adapterProvince;
    List<CategoryProduct> categoryProducts;
    //ApiService apiService;
    IMyShoppingAPI shoppingAPI;
    IMyShoppingAPI addressAPI;

    ImageButton btn_choose_img;
    Button btn_add_pd;

    List<Tinh> provinceList = new ArrayList<>();

    private static final int IMAGE_PICK_CODE=1000;
    private static final int PERMISSION_CODE=1001;

    AlertDialog dialog;


    Bitmap mBitmap;

    Spinner  spinner_khuvuc;

    @BindView(R.id.spinner_category)
    Spinner  spinner;



    @BindView(R.id.progress_bar)
    ProgressBar progress_bar;
    @BindView(R.id.edt_description_pd)
    EditText edt_description_pd;
    @BindView(R.id.edt_name_pd)
    EditText edt_name_pd;
    @BindView(R.id.edt_price_pd)
    EditText edt_price_pd;

    @BindView(R.id.recycler_view)
    RecyclerView recycler_view;
    ImageAdapter imageAdapter;
    List<ImageModel>listImages=new ArrayList<>();
    List<ImageModel>listImagesDeleteAdd=new ArrayList<>();
    List<ImageModel>listImageDelete=new ArrayList<>();
    List<ImageModel>listImageAdd=new ArrayList<>();
    StorageReference mStorageRef;

    boolean isUpdateInfoSP=false;

    int idDanhMuc = -99,provinceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_product);

        spinner_khuvuc=findViewById(R.id.spinner_khuvuc);
        mStorageRef= FirebaseStorage.getInstance().getReference();

        ButterKnife.bind(this);

        btn_add_pd = findViewById(R.id.btn_add_pd);
        btn_add_pd.setOnClickListener(this);



        btn_choose_img = findViewById(R.id.btn_choose_img);
        btn_choose_img.setOnClickListener(this);



        initRetrofitClient();
        initToolbar();
        loadSpinner();
        displayProvince();//
        displayView();

        compositeDisposable = new CompositeDisposable();
        dialog = new SpotsDialog.Builder().setContext(this).setTheme(R.style.CustomUpdate).setCancelable(false).build();
    }

    private void displayView() {
        edt_name_pd.setText(Common.productSelectEdit.getTenSP());
        edt_price_pd.setText(Common.productSelectEdit.getGiaSP()+"");
        edt_description_pd.setText(Common.productSelectEdit.getMoTa());


        recycler_view.setHasFixedSize(true);
        recycler_view.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL, false));
        // add link image
        for (LinkImageModel item: Common.productSelectEdit.getListLinkImage()) {
            listImages.add(new ImageModel(item.getLink()));

        }

        imageAdapter=new ImageAdapter(UpdateProductActivity.this, listImages, new IClickDeleteImage() {
            @Override
            public void onClick(int position) {
                if(listImages.get(position).getLinkImage()!=null){
                    listImageDelete.add(new ImageModel(listImages.get(position).getLinkImage()));
                }

                listImages.remove(position);
                imageAdapter.notifyDataSetChanged();
            }
        });

        recycler_view.setAdapter(imageAdapter);

    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

    }

    private void initRetrofitClient() {
        OkHttpClient client = new OkHttpClient.Builder().build();

        addressAPI = RetrofitClientAddress.getInstance("https://dev-online-gateway.ghn.vn/").create(IMyShoppingAPI.class);

        shoppingAPI = new RetrofitClient().getInstance(Common.API_RESTAURANT_ENDPOINT)
                .create(IMyShoppingAPI.class);

    }

    private void loadSpinner() {
        compositeDisposable.add(shoppingAPI.getDanhMuc("1234")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(danhMucModel -> {
                    if (danhMucModel.isSuccess()) {
                        progress_bar.setVisibility(View.GONE);
                        categoryProducts = danhMucModel.getResult();
                        adapter = new CategoryProductAdapter(this,
                                R.layout.item_select_product, categoryProducts);
                        spinner.setAdapter(adapter);
                        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                idDanhMuc = categoryProducts.get(i).getIdDanhMuc();
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {

                            }
                        });

                    } else{
                        progress_bar.setVisibility(View.GONE);
                        Toast.makeText(this, "load category fail", Toast.LENGTH_SHORT).show();

                    }

                }, throwable -> {
                    progress_bar.setVisibility(View.GONE);
                    Toast.makeText(this, "[LOAD CATEGORY PRODUCT]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_CODE && resultCode == RESULT_OK) {

            if (data.getClipData() != null) {

                int totalitem = data.getClipData().getItemCount();

                for (int i = 0; i < totalitem; i++) {

                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    String imagename = getFileName(imageUri);
                    ImageModel modalClass = new ImageModel(imagename, imageUri);
                    listImages.add(modalClass);

                    listImageAdd.add(new ImageModel(imagename,imageUri));
                    imageAdapter.notifyDataSetChanged();
                }

            } else if (data.getData() != null) {
                Toast.makeText(this, "single", Toast.LENGTH_SHORT).show();
                Uri imageUri = data.getData();
                String imagename = getFileName(imageUri);

                ImageModel modalClass = new ImageModel(imagename, imageUri);
                listImages.add(modalClass);
                listImageAdd.add(new ImageModel(imagename,imageUri));
                
                recycler_view.setAdapter(imageAdapter);


            }
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        // tao so random
        double cc= Math.random();
        String str=String.valueOf(cc);
        str= str.replaceAll("[.]","");
        return str+result;
    }


    private String getImageFromFilePath(Intent data) {


        return getPathFromURI(data.getData());

    }

    private String getImageFilePath(Intent data) {
        return getImageFromFilePath(data);
    }

    private String getPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Audio.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }


    private void displayProvince() {
        compositeDisposable.add(addressAPI.getProvince("8ce54678-f9b7-11eb-bfef-86bbb1a09031",
                "application/json")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    try {
                        if (provinceList.size() > 0)
                            provinceList.clear();

                        provinceList = s.getResult();

                        Collections.reverse(provinceList);
                        adapterProvince = new CategoryAdapter(this, R.layout.item_selected_province, provinceList);

                        spinner_khuvuc.setAdapter(adapterProvince);

                        progress_bar.setVisibility(View.GONE);

                    } catch (Exception e) {
                        progress_bar.setVisibility(View.GONE);

                        Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }, throwable -> {
                    progress_bar.setVisibility(View.GONE);

                    Toast.makeText(this, "loi" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("assas", throwable.getMessage());
                }));


        spinner_khuvuc.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                provinceId=provinceList.get(position).getProvinceID();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

    }

    private void multipartImageUpload() {

        dialog.show();

        progress_bar.setVisibility(View.VISIBLE);
        if (edt_description_pd.getText().toString().trim().length() == 0
                || edt_name_pd.getText().toString().trim().length() == 0
                || edt_price_pd.getText().toString().trim().length() == 0) {
            Toast.makeText(this, "chưa nhập đầy đủ thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File filesDir = getApplicationContext().getFilesDir();
            File file = new File(filesDir, "image" + ".png");

            OutputStream os;
            try {
                os = new FileOutputStream(file);
                mBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                os.close();
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Error writing bitmap", e);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            byte[] bitmapdata = bos.toByteArray();


            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();


            RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("myFile", file.getName(), reqFile);
            RequestBody name = RequestBody.create(MediaType.parse("text/plain"), "myFile");


            Call<UpdateModel> req = shoppingAPI.postImage2(body, name);
            req.enqueue(new Callback<UpdateModel>() {
                @Override
                public void onResponse(Call<UpdateModel> call, Response<UpdateModel> response) {

                    String tenSp = edt_name_pd.getText().toString();
                 //   float giaSp = Float.parseFloat(edt_price_pd.getText().toString());


                    float giaSp=Float.parseFloat(edt_price_pd.getText().toString()
                            .replaceAll("[,]","").replaceAll("[.]",""));


                    String mota = edt_description_pd.getText().toString();


                    if (response.code() == 200) {
                        progress_bar.setVisibility(View.GONE);
                        compositeDisposable.add(shoppingAPI.updateSanPham(
                                Common.API_KEY,
                                Common.productSelectEdit.getIdSP(),
                                tenSp, giaSp, mota, idDanhMuc,
                                new StringBuilder(Common.API_RESTAURANT_ENDPOINT)
                                        .append(response.body().getMessage()).toString(),
                                provinceId

                        )
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(uploadSanPhamModel -> {
                                    dialog.dismiss();
                                    Toast.makeText(UpdateProductActivity.this, "cap nhap pham thanh cong", Toast.LENGTH_SHORT).show();
                                }, throwable -> {
                                    dialog.dismiss();
                                    Toast.makeText(UpdateProductActivity.this, "[UPLOAD NEW PRODUCT]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                }));
                    }
                    else{
                        dialog.dismiss();
                        progress_bar.setVisibility(View.GONE);
                    }

                }

                @Override
                public void onFailure(Call<UpdateModel> call, Throwable t) {
                    progress_bar.setVisibility(View.GONE);
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Request failed", Toast.LENGTH_SHORT).show();
                    t.printStackTrace();
                    Log.e("ERROR", t.toString());
                }
            });


        } catch (FileNotFoundException e) {
            e.printStackTrace();
            dialog.dismiss();
        } catch (IOException e) {
            e.printStackTrace();
            dialog.dismiss();
        }
    }




    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.btn_add_pd:
                if (edt_description_pd.getText().toString().trim().length() == 0
                        || edt_name_pd.getText().toString().trim().length() == 0
                        || edt_price_pd.getText().toString().trim().length() == 0) {
                    Toast.makeText(this, "chưa nhập đầy đủ thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(listImages.size()==0){
                    Toast.makeText(this, "chưa chọn hình", Toast.LENGTH_SHORT).show();
                }else{
                    if(listImageAdd.size()>0){ // neu co xoa het add lai cung k sao
                        for (ImageModel item:listImageAdd) {
                            uploadImagesToFirebase(item);
                        }
                    }else{
                        if(listImages.size()>0)
                        updateSP(listImages.get(0).getLinkImage()); // trong cai update sp chi co xoa

                    }
                }
                break;
            case R.id.btn_choose_img:
                pickImage();
                break;

        }
    }



    private void updateSP(String linkImage) {
        dialog.show();
        if (edt_description_pd.getText().toString().trim().length() == 0
                || edt_name_pd.getText().toString().trim().length() == 0
                || edt_price_pd.getText().toString().trim().length() == 0) {
            Toast.makeText(this, "chưa nhập đầy đủ thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }
        float giaSp=Float.parseFloat(edt_price_pd.getText().toString()
                .replaceAll("[,]","").replaceAll("[.]",""));

        String mota = edt_description_pd.getText().toString().trim();
        String tenSP=edt_name_pd.getText().toString().trim();

        compositeDisposable.add(shoppingAPI.updateSanPham(
                Common.API_KEY,
                Common.productSelectEdit.getIdSP(),
                tenSP, giaSp, mota, idDanhMuc,
                linkImage,
                provinceId
        )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uploadSanPhamModel -> {
                    if(uploadSanPhamModel.isSuccess()){

                        if(listImageDelete.size()>0){
                            for (ImageModel item:listImageDelete) {
                                if(item.getLinkImage()!=null){// xoa di cai link cu
                                    deleteLinkImage(item.getLinkImage());

                                }
                            }
                        }


                    }else{
                        Toast.makeText(this, "fail update info sp"+uploadSanPhamModel.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();

                    Toast.makeText(UpdateProductActivity.this, "cap nhap info thanh cong", Toast.LENGTH_SHORT).show();
                }, throwable -> {
                    dialog.dismiss();
                    Toast.makeText(UpdateProductActivity.this, "[fail UPLOAD NEW PRODUCT]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    private void uploadImagesToFirebase(ImageModel item) {
        dialog.show();
        StorageReference mRef = mStorageRef.child("image").child(item.getImagename());
        mRef.putFile(item.getImage()).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                mRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        dialog.dismiss();
                        uploadLinkImage(uri.toString());

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                        Toast.makeText(UpdateProductActivity.this, "[fail load link ] " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(UpdateProductActivity.this, "[Fail upload iamge to firebase ] " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadLinkImage(String linkImage) {
        dialog.show();
        compositeDisposable.add(shoppingAPI.uploadLinkHinhAnh(Common.API_KEY,
             Common.productSelectEdit.getIdSP(),linkImage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(hinhAnhModel -> {
                    dialog.dismiss();
                    if(hinhAnhModel.isSuccess()){
                        Toast.makeText(this, "upload link success", Toast.LENGTH_SHORT).show();

                        if(!isUpdateInfoSP) // check neu chua update info sp thi update info
                        updateSP(linkImage);
                        isUpdateInfoSP=true;
                    }
                    else {
                        Toast.makeText(this, "upload link fail"+hinhAnhModel.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },throwable -> {
                    dialog.dismiss();
                    Toast.makeText(this, "[fail UPLOAD link image]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }


    private void deleteLinkImage(String linkImage) {
        dialog.show();
        compositeDisposable.add(shoppingAPI.deleteLinkImage(Common.API_KEY,
                linkImage
        )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(hinhAnhModel -> {
                    dialog.dismiss();
                    if(hinhAnhModel.isSuccess()){
                        Toast.makeText(this, "delete link success", Toast.LENGTH_SHORT).show();
                        deleteImageFromFirebase(linkImage);
                    }
                    else {
                        Toast.makeText(this, "delete link fail"+hinhAnhModel.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },throwable -> {
                    dialog.dismiss();
                    Toast.makeText(this, "[fail delete link image]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    private void deleteImageFromFirebase(String url) {
        dialog.show();
            StorageReference referenceDelete= FirebaseStorage.getInstance().getReferenceFromUrl(url);
            referenceDelete.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    dialog.dismiss();
                    Toast.makeText(UpdateProductActivity.this, "delete success", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, IMAGE_CODE);
    }

    private void pickImageFromGarelly() {
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,IMAGE_PICK_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CODE:{
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    pickImageFromGarelly();
                }
                else{
                    Toast.makeText(this, "permission denied ...", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }


}

/*


 */