package com.vulcanice.vulcanice;

import android.content.Context;

import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.google.firebase.storage.StorageReference;
import com.vulcanice.vulcanice.Model.FirebaseImageLoader;

import java.io.InputStream;

@GlideModule
public class MyAppGlideModule extends AppGlideModule {
    public void registerComponents(Context context, Registry registry) {
        // Register FirebaseImageLoader to handle StorageReference
        registry.append(StorageReference.class, InputStream.class,
                new FirebaseImageLoader.Factory());
    }
}

