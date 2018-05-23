package com.vulcanice.vulcanice;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by paolo on 5/3/18.
 */

public class DashBoardTab2 extends Fragment {
    private static final String TAG = "Tab2Fragment";
    private Button btnTab2;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dash_board_1, container, false);
        btnTab2 = view.findViewById(R.id.btnTab2);

        btnTab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "TEST TAB2", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}