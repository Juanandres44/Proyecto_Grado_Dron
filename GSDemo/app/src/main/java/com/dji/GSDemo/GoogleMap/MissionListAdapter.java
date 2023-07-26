package com.dji.GSDemo.GoogleMap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class MissionListAdapter extends ArrayAdapter<Mission> {
    private Context mContext;
    private List<Mission> misiones;

    public MissionListAdapter(Context context, List<Mission> misiones) {
        super(context, 0, misiones);
        mContext = context;
        this.misiones = misiones;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(mContext).inflate(R.layout.mission_list, parent, false);
        }

        Mission mision = misiones.get(position);

        TextView textViewIdVuelo = listItem.findViewById(R.id.textViewIdVuelo);
        TextView textViewTipo = listItem.findViewById(R.id.textViewTipo);
        TextView textViewMissionCode = listItem.findViewById(R.id.textViewMissionCode);

        textViewIdVuelo.setText("ID Vuelo: " + mision.getIdVuelo());
        textViewTipo.setText("Tipo: " + mision.getTipo());
        textViewMissionCode.setText("Mission Code: " + mision.getMissionCode());

        return listItem;
    }
}
