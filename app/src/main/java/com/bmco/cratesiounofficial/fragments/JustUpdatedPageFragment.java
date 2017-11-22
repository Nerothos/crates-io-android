package com.bmco.cratesiounofficial.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.bmco.cratesiounofficial.R;
import com.bmco.cratesiounofficial.interfaces.OnSummaryChangeListener;
import com.bmco.cratesiounofficial.models.Summary;
import com.bmco.cratesiounofficial.recyclers.CrateRecyclerAdapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by Bertus on 25-5-2017.
 */

public class JustUpdatedPageFragment extends Fragment {
    private RecyclerView itemList;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.trending_page, container, false);
        progressBar = view.findViewById(R.id.progress);

        itemList = view.findViewById(R.id.recycler);
        itemList.setLayoutManager(new LinearLayoutManager(itemList.getContext()));
        SummaryFragment.listener.add(new OnSummaryChangeListener() {
            @Override
            public void summary(Summary summary) {
                refreshSummary(summary);
            }

            @Override
            public void downloadStarted() {
                progressBar.post(() -> progressBar.setVisibility(VISIBLE));
            }
        });
        return view;
    }

    private void refreshSummary(final Summary summary) {
        itemList.post(() -> {
            CrateRecyclerAdapter adapter = new CrateRecyclerAdapter(itemList.getContext(), summary.getJustUpdated());
            itemList.setAdapter(adapter);
            progressBar.setVisibility(GONE);
        });
    }
}
