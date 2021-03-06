package com.appian.decisionmaking.listpage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.appian.decisionmaking.R;
import com.appian.decisionmaking.choosing.ChoosingMessageDialog;
import com.appian.decisionmaking.common.Constants;
import com.appian.decisionmaking.database.DataSource;
import com.appian.decisionmaking.export.CsvExporter;
import com.appian.decisionmaking.export.TxtExporter;
import com.appian.decisionmaking.payments.BuyPremiumActivity;
import com.appian.decisionmaking.utils.PreferencesManager;
import com.appian.decisionmaking.utils.UIUtils;
import com.appian.decisionmaking.views.SimpleDividerItemDecoration;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class PremiumOptionsFragment extends Fragment
        implements ListOptionsAdapter.ItemSelectionListener, CsvExporter.Listener,
        TxtExporter.Listener, ChoosingMessageDialog.Listener {

    static PremiumOptionsFragment getInstance(int listId) {
        PremiumOptionsFragment fragment = new PremiumOptionsFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.LIST_ID_KEY, listId);
        fragment.setArguments(bundle);
        return fragment;
    }

    @BindView(R.id.recycler_view) RecyclerView premiumOptions;

    private int listId;
    private PreferencesManager preferencesManager;
    private DataSource dataSource;
    private CsvExporter csvExporter;
    private TxtExporter txtExporter;
    private ChoosingMessageDialog choosingMessageDialog;
    private Unbinder unbinder;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.simple_vertical_recyclerview,
                container,
                false);
        listId = getArguments().getInt(Constants.LIST_ID_KEY);
        unbinder = ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        preferencesManager = new PreferencesManager(getContext());
        premiumOptions.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));
        premiumOptions.setAdapter(new ListOptionsAdapter(
                getActivity(),
                this,
                R.array.premium_options,
                R.array.premium_options_icons));
        dataSource = new DataSource(getContext());
        csvExporter = new CsvExporter(this);
        txtExporter = new TxtExporter(this);
        choosingMessageDialog = new ChoosingMessageDialog(getContext(), this, listId);
    }

    @Override
    public void onItemClick(int position) {
        if (preferencesManager.isOnFreeVersion()) {
            UIUtils.showLongToast(R.string.premium_needed_message, getContext());
            Intent intent = new Intent(getActivity(), BuyPremiumActivity.class);
            getActivity().startActivity(intent);
            getActivity().overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.stay);
            return;
        }

        switch (position) {
            case 0:
                txtExporter.turnListIntoTxt(listId, getContext());
                break;
            case 1:
                csvExporter.turnListIntoCsv(listId, getContext());
                break;
            case 2:
                choosingMessageDialog.show();
                break;
        }
    }

    @Override
    public void onCsvFileCreated(Uri fileUri) {
        getActivity().runOnUiThread(() -> {
            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
            intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentShareFile.setType("application/csv");
            intentShareFile.putExtra(Intent.EXTRA_STREAM, fileUri);

            String listName = dataSource.getListName(listId);
            intentShareFile.putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.export_file_title, listName));
            startActivity(Intent.createChooser(intentShareFile, getString(R.string.export_file_with)));
        });
    }

    @Override
    public void onCsvExportFailed() {
        getActivity().runOnUiThread(() -> UIUtils.showLongToast(
                R.string.export_csv_failed, getContext()));
    }

    @Override
    public void onTxtFileCreated(Uri fileUri) {
        getActivity().runOnUiThread(() -> {
            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
            intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentShareFile.setType("text/*");
            intentShareFile.putExtra(Intent.EXTRA_STREAM, fileUri);

            String listName = dataSource.getListName(listId);
            intentShareFile.putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.export_file_title, listName));
            startActivity(Intent.createChooser(intentShareFile, getString(R.string.export_file_with)));
        });
    }

    @Override
    public void onTxtExportFailed() {
        getActivity().runOnUiThread(() -> UIUtils.showLongToast(
                R.string.export_txt_failed, getContext()));
    }

    @Override
    public void onNewChoosingMessageConfirmed(String newMessage) {
        dataSource.updateChoosingMessage(listId, newMessage);
        UIUtils.showShortToast(R.string.choosing_message_updated, getContext());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
