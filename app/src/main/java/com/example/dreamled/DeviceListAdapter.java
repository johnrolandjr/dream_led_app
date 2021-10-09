package com.example.dreamled;

import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.mViewHolder>
{
    private Context context;
    private List<DeviceInfoModel> deviceList;
    onClickInterface onClickInterface;

    public static class mViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textAddress;
        LinearLayout linearLayout;

        public mViewHolder(View v) {
            super(v);
            textName = v.findViewById(R.id.textViewDeviceName);
            textAddress = v.findViewById(R.id.textViewDeviceAddress);
            linearLayout = v.findViewById(R.id.linearLayoutDeviceInfo);
        }
    }

    public DeviceListAdapter(Context context, List<DeviceInfoModel> deviceList, onClickInterface oci) {
        this.context = context;
        this.deviceList = deviceList;
        this.onClickInterface = oci;
    }

    @NonNull
    @Override
    public DeviceListAdapter.mViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_info_layout, parent, false);
        return new mViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceListAdapter.mViewHolder holder, int position) {
        String devName = deviceList.get(position).getDeviceName();
        String devHwAddr = deviceList.get(position).getDeviceHardwareAddress();
        holder.textName.setText((devName));
        holder.textAddress.setText((devHwAddr));

        // When a ble device is selected from the discovered device list, we want to try to move to
        holder.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickInterface.setClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public interface RecyclerViewClickListener {
        public void recyclerViewListClicked(View v, int position);
    }

    /**
     * Search the adapter for an existing device address and return it, otherwise return -1.
     */
    private int getPosition(String address) {
        int position = -1;
        for (int i = 0; i < deviceList.size(); i++) {
            if (deviceList.get(i).getDeviceHardwareAddress().equals(address)) {
                position = i;
                break;
            }
        }
        return position;
    }

    /**
     * Add a ScanResult item to the adapter if a result from that device isn't already present.
     * Otherwise updates the existing position with the new ScanResult.
     */
    public void add(ScanResult scanResult) {

        int existingPosition = getPosition(scanResult.getDevice().getAddress());
        DeviceInfoModel discoveredDevice = new DeviceInfoModel(scanResult.getDevice());

        if (existingPosition >= 0) {
            // Device is already in list, update its record.
            deviceList.set(existingPosition, discoveredDevice);
        } else {
            // Add new Device's ScanResult to list.
            deviceList.add(discoveredDevice);
        }
    }

    /**
     * Clear out the adapter.
     */
    public void clear() {
        deviceList.clear();
    }
}
