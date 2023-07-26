package com.dji.GSDemo.GoogleMap;

public class Mission {
    private String idVuelo;
    private String tipo;
    private String missionCode;

    public Mission(String idVuelo, String tipo, String missionCode )  {
        this.idVuelo = idVuelo;
        this.tipo = tipo;
        this.missionCode = missionCode;
    }

    public String getIdVuelo() {
        return idVuelo;
    }

    public void setIdVuelo(String idVuelo) {
        this.idVuelo = idVuelo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMissionCode() {
        return missionCode;
    }

    public void setMissionCode(String missionCode) {
        this.missionCode = missionCode;
    }
}

